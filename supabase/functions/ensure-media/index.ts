import { withSupabase } from "npm:@supabase/server@1.4.1";

const TMDB_API_BASE_URL = "https://api.themoviedb.org/3";
const MAX_REQUEST_BYTES = 1_024;
const TMDB_TIMEOUT_MS = 8_000;

type MediaType = "movie" | "tv";

type TmdbMedia = {
  id?: unknown;
  title?: unknown;
  name?: unknown;
  poster_path?: unknown;
  release_date?: unknown;
  first_air_date?: unknown;
  original_language?: unknown;
};

function jsonResponse(
  body: Record<string, unknown>,
  status = 200,
  extraHeaders: HeadersInit = {},
): Response {
  return Response.json(body, {
    status,
    headers: {
      "Cache-Control": "no-store",
      ...extraHeaders,
    },
  });
}

function isMediaType(value: unknown): value is MediaType {
  return value === "movie" || value === "tv";
}

function optionalString(value: unknown, maxLength: number): string | null {
  if (typeof value !== "string") return null;
  const normalized = value.trim();
  if (normalized.length === 0 || normalized.length > maxLength) return null;
  return normalized;
}

function normalizedDate(value: unknown): string | null {
  const date = optionalString(value, 10);
  return date !== null && /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : null;
}

function normalizedPosterPath(value: unknown): string | null {
  const path = optionalString(value, 500);
  return path !== null && /^\/[A-Za-z0-9._/-]+$/.test(path) ? path : null;
}

function normalizedLanguage(value: unknown): string | null {
  const language = optionalString(value, 3)?.toLowerCase() ?? null;
  return language !== null && /^[a-z]{2,3}$/.test(language) ? language : null;
}

async function parseInput(
  request: Request,
): Promise<{ mediaType: MediaType; tmdbId: number } | Response> {
  const contentType = request.headers.get("content-type") ?? "";
  if (!contentType.toLowerCase().startsWith("application/json")) {
    return jsonResponse({ code: "unsupported_media_type" }, 415);
  }

  const declaredLength = Number(request.headers.get("content-length") ?? 0);
  if (Number.isFinite(declaredLength) && declaredLength > MAX_REQUEST_BYTES) {
    return jsonResponse({ code: "request_too_large" }, 413);
  }

  const rawBody = await request.text();
  if (new TextEncoder().encode(rawBody).byteLength > MAX_REQUEST_BYTES) {
    return jsonResponse({ code: "request_too_large" }, 413);
  }

  let payload: Record<string, unknown>;
  try {
    const parsed: unknown = JSON.parse(rawBody);
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== "object") {
      return jsonResponse({ code: "invalid_request" }, 400);
    }
    payload = parsed as Record<string, unknown>;
  } catch {
    return jsonResponse({ code: "invalid_json" }, 400);
  }

  const mediaType = payload.media_type;
  const tmdbId = payload.tmdb_id;
  if (
    !isMediaType(mediaType) ||
    typeof tmdbId !== "number" ||
    !Number.isSafeInteger(tmdbId) ||
    tmdbId <= 0 ||
    tmdbId > 2_147_483_647
  ) {
    return jsonResponse({ code: "invalid_request" }, 400);
  }

  return { mediaType, tmdbId };
}

export default {
  fetch: withSupabase({ auth: "user" }, async (request, context) => {
    if (request.method !== "POST") {
      return jsonResponse(
        { code: "method_not_allowed" },
        405,
        { Allow: "POST" },
      );
    }

    const input = await parseInput(request);
    if (input instanceof Response) return input;

    const { data: existingMedia, error: existingMediaError } =
      await context.supabase
        .from("media_items")
        .select("id")
        .eq("media_type", input.mediaType)
        .eq("tmdb_id", input.tmdbId)
        .maybeSingle();

    if (existingMediaError) {
      console.error("ensure-media existing lookup failed", {
        code: existingMediaError.code,
      });
      return jsonResponse({ code: "backend_unavailable" }, 503);
    }

    if (existingMedia !== null) {
      return jsonResponse({ media_id: existingMedia.id, cached: true });
    }

    const { data: quotaAvailable, error: quotaError } =
      await context.supabaseAdmin.rpc("consume_ensure_media_quota", {
        p_user_id: context.userClaims!.id,
      });

    if (quotaError) {
      console.error("ensure-media quota check failed", { code: quotaError.code });
      return jsonResponse({ code: "backend_unavailable" }, 503);
    }

    if (quotaAvailable !== true) {
      return jsonResponse(
        { code: "rate_limit_exceeded" },
        429,
        { "Retry-After": "600" },
      );
    }

    const tmdbToken = Deno.env.get("TMDB_BEARER_TOKEN")
      ?.replace(/^Bearer\s+/i, "")
      .trim();

    if (!tmdbToken) {
      console.error("ensure-media is missing TMDB_BEARER_TOKEN");
      return jsonResponse({ code: "backend_not_configured" }, 503);
    }

    let tmdbResponse: Response;
    try {
      tmdbResponse = await fetch(
        `${TMDB_API_BASE_URL}/${input.mediaType}/${input.tmdbId}?language=en-US`,
        {
          headers: {
            Accept: "application/json",
            Authorization: `Bearer ${tmdbToken}`,
          },
          signal: AbortSignal.timeout(TMDB_TIMEOUT_MS),
        },
      );
    } catch (error) {
      console.error("ensure-media TMDB request failed", {
        name: error instanceof Error ? error.name : "UnknownError",
      });
      return jsonResponse({ code: "upstream_unavailable" }, 503);
    }

    if (tmdbResponse.status === 404) {
      return jsonResponse({ code: "media_not_found" }, 404);
    }

    if (!tmdbResponse.ok) {
      console.error("ensure-media TMDB returned an error", {
        status: tmdbResponse.status,
      });
      const retryAfter = tmdbResponse.headers.get("retry-after") ?? "60";
      return jsonResponse(
        { code: "upstream_unavailable" },
        503,
        { "Retry-After": retryAfter },
      );
    }

    let tmdbMedia: TmdbMedia;
    try {
      tmdbMedia = await tmdbResponse.json() as TmdbMedia;
    } catch {
      return jsonResponse({ code: "invalid_upstream_response" }, 502);
    }

    const verifiedTmdbId = tmdbMedia.id;
    const title = optionalString(
      input.mediaType === "movie" ? tmdbMedia.title : tmdbMedia.name,
      300,
    );

    if (verifiedTmdbId !== input.tmdbId || title === null) {
      return jsonResponse({ code: "invalid_upstream_response" }, 502);
    }

    const mediaRow = {
      media_type: input.mediaType,
      tmdb_id: input.tmdbId,
      title,
      poster_path: normalizedPosterPath(tmdbMedia.poster_path),
      release_date: normalizedDate(
        input.mediaType === "movie"
          ? tmdbMedia.release_date
          : tmdbMedia.first_air_date,
      ),
      original_language: normalizedLanguage(tmdbMedia.original_language),
    };

    const { data: storedMedia, error: storeError } =
      await context.supabaseAdmin
        .from("media_items")
        .upsert(mediaRow, { onConflict: "media_type,tmdb_id" })
        .select("id")
        .single();

    if (storeError || storedMedia === null) {
      console.error("ensure-media database upsert failed", {
        code: storeError?.code ?? "missing_row",
      });
      return jsonResponse({ code: "backend_unavailable" }, 503);
    }

    return jsonResponse({ media_id: storedMedia.id, cached: false });
  }),
};
