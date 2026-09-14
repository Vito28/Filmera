begin;

create table public.user_preferences (
  user_id uuid primary key
    references auth.users(id)
    on delete cascade,
  preferred_media_types text[] not null default '{}',
  preferred_genre_ids integer[] not null default '{}',
  preferred_countries text[] not null default '{}',
  preferred_movie_ids integer[] not null default '{}',
  preferred_tv_ids integer[] not null default '{}',
  preferred_person_ids integer[] not null default '{}',
  preferred_language_codes text[] not null default '{}',
  onboarding_completed boolean not null default false,
  client_updated_at timestamptz not null,
  updated_at timestamptz not null default now(),

  constraint user_preferences_media_types_check
    check (
      preferred_media_types <@ array[
        'MOVIE', 'TV_SERIES', 'ANIME', 'DOCUMENTARY', 'ANIMATION'
      ]::text[]
    ),
  constraint user_preferences_genres_check
    check (
      array_position(preferred_genre_ids, null) is null
      and 0 < all(preferred_genre_ids)
    ),
  constraint user_preferences_movies_check
    check (
      array_position(preferred_movie_ids, null) is null
      and 0 < all(preferred_movie_ids)
    ),
  constraint user_preferences_tv_check
    check (
      array_position(preferred_tv_ids, null) is null
      and 0 < all(preferred_tv_ids)
    ),
  constraint user_preferences_people_check
    check (
      array_position(preferred_person_ids, null) is null
      and 0 < all(preferred_person_ids)
    ),
  constraint user_preferences_countries_check
    check (
      cardinality(preferred_countries) <= 30
      and array_position(preferred_countries, null) is null
    ),
  constraint user_preferences_languages_check
    check (
      cardinality(preferred_language_codes) <= 30
      and array_position(preferred_language_codes, null) is null
    )
);

create table public.user_library (
  user_id uuid not null
    references auth.users(id)
    on delete cascade,
  media_id bigint not null
    references public.media_items(id)
    on delete cascade,
  watch_status text not null default 'none'
    check (watch_status in ('none', 'watchlist', 'watching', 'completed')),
  is_favorite boolean not null default false,
  added_at timestamptz not null,
  deleted_at timestamptz,
  client_updated_at timestamptz not null,
  updated_at timestamptz not null default now(),

  primary key (user_id, media_id),
  constraint user_library_active_value_check
    check (
      deleted_at is not null
      or is_favorite
      or watch_status <> 'none'
    )
);

create table public.user_search_history (
  user_id uuid not null
    references auth.users(id)
    on delete cascade,
  normalized_query text not null,
  query text not null,
  searched_at timestamptz not null,
  deleted_at timestamptz,
  client_updated_at timestamptz not null,
  updated_at timestamptz not null default now(),

  primary key (user_id, normalized_query),
  constraint user_search_history_normalized_check
    check (
      normalized_query = lower(btrim(normalized_query))
      and char_length(normalized_query) between 1 and 200
    ),
  constraint user_search_history_query_check
    check (char_length(btrim(query)) between 1 and 200)
);

create table public.user_sync_state (
  user_id uuid primary key
    references auth.users(id)
    on delete cascade,
  search_cleared_at timestamptz,
  updated_at timestamptz not null default now()
);

create index user_library_media_id_idx
  on public.user_library(media_id);

create index user_library_active_recent_idx
  on public.user_library(user_id, client_updated_at desc)
  where deleted_at is null;

create index user_search_history_active_recent_idx
  on public.user_search_history(user_id, searched_at desc)
  where deleted_at is null;

create trigger user_preferences_set_updated_at
before update on public.user_preferences
for each row execute function private.set_updated_at();

create trigger user_library_set_updated_at
before update on public.user_library
for each row execute function private.set_updated_at();

create trigger user_search_history_set_updated_at
before update on public.user_search_history
for each row execute function private.set_updated_at();

create trigger user_sync_state_set_updated_at
before update on public.user_sync_state
for each row execute function private.set_updated_at();

alter table public.user_preferences enable row level security;
alter table public.user_library enable row level security;
alter table public.user_search_history enable row level security;
alter table public.user_sync_state enable row level security;

create policy user_preferences_owner_select
on public.user_preferences
for select
to authenticated
using ((select auth.uid()) = user_id);

create policy user_library_owner_select
on public.user_library
for select
to authenticated
using ((select auth.uid()) = user_id);

create policy user_search_history_owner_select
on public.user_search_history
for select
to authenticated
using ((select auth.uid()) = user_id);

create policy user_sync_state_owner_select
on public.user_sync_state
for select
to authenticated
using ((select auth.uid()) = user_id);

-- The app uses the RPCs below. Direct Data API writes stay closed even if a
-- client attempts to forge another user_id.
revoke all on table public.user_preferences from anon, authenticated;
revoke all on table public.user_library from anon, authenticated;
revoke all on table public.user_search_history from anon, authenticated;
revoke all on table public.user_sync_state from anon, authenticated;

create or replace function public.sync_user_preferences(
  p_preferred_media_types text[],
  p_preferred_genre_ids integer[],
  p_preferred_countries text[],
  p_preferred_movie_ids integer[],
  p_preferred_tv_ids integer[],
  p_preferred_person_ids integer[],
  p_preferred_language_codes text[],
  p_onboarding_completed boolean,
  p_client_updated_at timestamptz
)
returns table (
  preferred_media_types text[],
  preferred_genre_ids integer[],
  preferred_countries text[],
  preferred_movie_ids integer[],
  preferred_tv_ids integer[],
  preferred_person_ids integer[],
  preferred_language_codes text[],
  onboarding_completed boolean,
  client_updated_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  owner_id uuid := auth.uid();
  safe_updated_at timestamptz := least(p_client_updated_at, now() + interval '5 minutes');
begin
  if owner_id is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  if p_client_updated_at is null then
    raise exception 'client_updated_at is required' using errcode = '22004';
  end if;

  insert into public.user_preferences (
    user_id,
    preferred_media_types,
    preferred_genre_ids,
    preferred_countries,
    preferred_movie_ids,
    preferred_tv_ids,
    preferred_person_ids,
    preferred_language_codes,
    onboarding_completed,
    client_updated_at
  )
  values (
    owner_id,
    coalesce(p_preferred_media_types, '{}'),
    coalesce(p_preferred_genre_ids, '{}'),
    coalesce(p_preferred_countries, '{}'),
    coalesce(p_preferred_movie_ids, '{}'),
    coalesce(p_preferred_tv_ids, '{}'),
    coalesce(p_preferred_person_ids, '{}'),
    coalesce(p_preferred_language_codes, '{}'),
    p_onboarding_completed,
    safe_updated_at
  )
  on conflict on constraint user_preferences_pkey do update set
    preferred_media_types = excluded.preferred_media_types,
    preferred_genre_ids = excluded.preferred_genre_ids,
    preferred_countries = excluded.preferred_countries,
    preferred_movie_ids = excluded.preferred_movie_ids,
    preferred_tv_ids = excluded.preferred_tv_ids,
    preferred_person_ids = excluded.preferred_person_ids,
    preferred_language_codes = excluded.preferred_language_codes,
    onboarding_completed = excluded.onboarding_completed,
    client_updated_at = excluded.client_updated_at
  where excluded.client_updated_at >= public.user_preferences.client_updated_at;

  return query
  select
    preference.preferred_media_types,
    preference.preferred_genre_ids,
    preference.preferred_countries,
    preference.preferred_movie_ids,
    preference.preferred_tv_ids,
    preference.preferred_person_ids,
    preference.preferred_language_codes,
    preference.onboarding_completed,
    preference.client_updated_at
  from public.user_preferences as preference
  where preference.user_id = owner_id;
end;
$$;

create or replace function public.get_user_preferences_sync()
returns table (
  preferred_media_types text[],
  preferred_genre_ids integer[],
  preferred_countries text[],
  preferred_movie_ids integer[],
  preferred_tv_ids integer[],
  preferred_person_ids integer[],
  preferred_language_codes text[],
  onboarding_completed boolean,
  client_updated_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
  select
    preference.preferred_media_types,
    preference.preferred_genre_ids,
    preference.preferred_countries,
    preference.preferred_movie_ids,
    preference.preferred_tv_ids,
    preference.preferred_person_ids,
    preference.preferred_language_codes,
    preference.onboarding_completed,
    preference.client_updated_at
  from public.user_preferences as preference
  where preference.user_id = auth.uid();
$$;

create or replace function public.sync_user_library(
  p_media_id bigint,
  p_watch_status text,
  p_is_favorite boolean,
  p_added_at timestamptz,
  p_deleted boolean,
  p_client_updated_at timestamptz
)
returns table (
  media_id bigint,
  media_type text,
  tmdb_id integer,
  title text,
  poster_path text,
  release_date date,
  original_language text,
  watch_status text,
  is_favorite boolean,
  added_at timestamptz,
  deleted_at timestamptz,
  client_updated_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  owner_id uuid := auth.uid();
  safe_updated_at timestamptz := least(p_client_updated_at, now() + interval '5 minutes');
begin
  if owner_id is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  if p_media_id is null or p_media_id <= 0 or p_client_updated_at is null then
    raise exception 'Invalid library mutation' using errcode = '22023';
  end if;

  if p_watch_status not in ('none', 'watchlist', 'watching', 'completed') then
    raise exception 'Invalid watch status' using errcode = '22023';
  end if;

  if not p_deleted and not p_is_favorite and p_watch_status = 'none' then
    raise exception 'An active library item needs a value' using errcode = '22023';
  end if;

  insert into public.user_library (
    user_id,
    media_id,
    watch_status,
    is_favorite,
    added_at,
    deleted_at,
    client_updated_at
  )
  values (
    owner_id,
    p_media_id,
    p_watch_status,
    p_is_favorite,
    least(coalesce(p_added_at, safe_updated_at), now() + interval '5 minutes'),
    case when p_deleted then safe_updated_at else null end,
    safe_updated_at
  )
  on conflict on constraint user_library_pkey do update set
    watch_status = excluded.watch_status,
    is_favorite = excluded.is_favorite,
    added_at = least(public.user_library.added_at, excluded.added_at),
    deleted_at = excluded.deleted_at,
    client_updated_at = excluded.client_updated_at
  where excluded.client_updated_at >= public.user_library.client_updated_at;

  return query
  select
    library.media_id,
    media.media_type,
    media.tmdb_id,
    media.title,
    media.poster_path,
    media.release_date,
    media.original_language,
    library.watch_status,
    library.is_favorite,
    library.added_at,
    library.deleted_at,
    library.client_updated_at
  from public.user_library as library
  join public.media_items as media on media.id = library.media_id
  where library.user_id = owner_id
    and library.media_id = p_media_id;
end;
$$;

create or replace function public.get_user_library_sync()
returns table (
  media_id bigint,
  media_type text,
  tmdb_id integer,
  title text,
  poster_path text,
  release_date date,
  original_language text,
  watch_status text,
  is_favorite boolean,
  added_at timestamptz,
  deleted_at timestamptz,
  client_updated_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
  select
    library.media_id,
    media.media_type,
    media.tmdb_id,
    media.title,
    media.poster_path,
    media.release_date,
    media.original_language,
    library.watch_status,
    library.is_favorite,
    library.added_at,
    library.deleted_at,
    library.client_updated_at
  from public.user_library as library
  join public.media_items as media on media.id = library.media_id
  where library.user_id = auth.uid()
  order by library.client_updated_at;
$$;

create or replace function public.sync_search_history(
  p_query text,
  p_searched_at timestamptz,
  p_deleted boolean,
  p_client_updated_at timestamptz
)
returns table (
  normalized_query text,
  query text,
  searched_at timestamptz,
  deleted_at timestamptz,
  client_updated_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  owner_id uuid := auth.uid();
  normalized text := lower(btrim(p_query));
  safe_updated_at timestamptz := least(p_client_updated_at, now() + interval '5 minutes');
  cleared_at timestamptz;
  must_delete boolean;
begin
  if owner_id is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  if p_client_updated_at is null or char_length(normalized) not between 1 and 200 then
    raise exception 'Invalid search mutation' using errcode = '22023';
  end if;

  select state.search_cleared_at
  into cleared_at
  from public.user_sync_state as state
  where state.user_id = owner_id;

  must_delete := p_deleted or (cleared_at is not null and safe_updated_at <= cleared_at);

  insert into public.user_search_history (
    user_id,
    normalized_query,
    query,
    searched_at,
    deleted_at,
    client_updated_at
  )
  values (
    owner_id,
    normalized,
    btrim(p_query),
    least(coalesce(p_searched_at, safe_updated_at), now() + interval '5 minutes'),
    case when must_delete then safe_updated_at else null end,
    safe_updated_at
  )
  on conflict on constraint user_search_history_pkey do update set
    query = excluded.query,
    searched_at = excluded.searched_at,
    deleted_at = excluded.deleted_at,
    client_updated_at = excluded.client_updated_at
  where excluded.client_updated_at >= public.user_search_history.client_updated_at;

  return query
  select
    history.normalized_query,
    history.query,
    history.searched_at,
    history.deleted_at,
    history.client_updated_at
  from public.user_search_history as history
  where history.user_id = owner_id
    and history.normalized_query = normalized;
end;
$$;

create or replace function public.clear_search_history(
  p_client_updated_at timestamptz
)
returns timestamptz
language plpgsql
security definer
set search_path = ''
as $$
declare
  owner_id uuid := auth.uid();
  safe_updated_at timestamptz := least(p_client_updated_at, now() + interval '5 minutes');
begin
  if owner_id is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  if p_client_updated_at is null then
    raise exception 'client_updated_at is required' using errcode = '22004';
  end if;

  insert into public.user_sync_state (user_id, search_cleared_at)
  values (owner_id, safe_updated_at)
  on conflict on constraint user_sync_state_pkey do update set
    search_cleared_at = greatest(
      public.user_sync_state.search_cleared_at,
      excluded.search_cleared_at
    );

  select state.search_cleared_at
  into safe_updated_at
  from public.user_sync_state as state
  where state.user_id = owner_id;

  update public.user_search_history as history
  set
    deleted_at = safe_updated_at,
    client_updated_at = safe_updated_at
  where history.user_id = owner_id
    and history.client_updated_at <= safe_updated_at;

  return safe_updated_at;
end;
$$;

create or replace function public.get_user_search_history_sync()
returns table (
  normalized_query text,
  query text,
  searched_at timestamptz,
  deleted_at timestamptz,
  client_updated_at timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
  select
    history.normalized_query,
    history.query,
    history.searched_at,
    history.deleted_at,
    history.client_updated_at
  from public.user_search_history as history
  where history.user_id = auth.uid()
  order by history.searched_at desc
  limit 100;
$$;

revoke all on function public.sync_user_preferences(
  text[], integer[], text[], integer[], integer[], integer[], text[], boolean, timestamptz
) from public, anon;
revoke all on function public.get_user_preferences_sync() from public, anon;
revoke all on function public.sync_user_library(
  bigint, text, boolean, timestamptz, boolean, timestamptz
) from public, anon;
revoke all on function public.get_user_library_sync() from public, anon;
revoke all on function public.sync_search_history(
  text, timestamptz, boolean, timestamptz
) from public, anon;
revoke all on function public.clear_search_history(timestamptz) from public, anon;
revoke all on function public.get_user_search_history_sync() from public, anon;

grant execute on function public.sync_user_preferences(
  text[], integer[], text[], integer[], integer[], integer[], text[], boolean, timestamptz
) to authenticated;
grant execute on function public.get_user_preferences_sync() to authenticated;
grant execute on function public.sync_user_library(
  bigint, text, boolean, timestamptz, boolean, timestamptz
) to authenticated;
grant execute on function public.get_user_library_sync() to authenticated;
grant execute on function public.sync_search_history(
  text, timestamptz, boolean, timestamptz
) to authenticated;
grant execute on function public.clear_search_history(timestamptz) to authenticated;
grant execute on function public.get_user_search_history_sync() to authenticated;

commit;
