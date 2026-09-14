begin;

-- RLS bypass does not imply table privileges. Keep the Edge Function's
-- service role limited to the media metadata operations used by ensure-media.
revoke all on table public.media_items from service_role;

grant select on table public.media_items to service_role;

grant insert (
  media_type,
  tmdb_id,
  title,
  poster_path,
  release_date,
  original_language
)
on public.media_items
to service_role;

grant update (
  media_type,
  tmdb_id,
  title,
  poster_path,
  release_date,
  original_language
)
on public.media_items
to service_role;

grant usage on sequence public.media_items_id_seq to service_role;

commit;
