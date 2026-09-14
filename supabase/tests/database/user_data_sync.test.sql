begin;

create extension if not exists pgtap with schema extensions;

select extensions.plan(31);

insert into auth.users (
  id, aud, role, email, raw_app_meta_data, raw_user_meta_data, created_at, updated_at
)
values
  (
    '33333333-3333-4333-8333-333333333333',
    'authenticated', 'authenticated', 'sync-a@filmera.test',
    '{"provider":"email","providers":["email"]}', '{}', now(), now()
  ),
  (
    '44444444-4444-4444-8444-444444444444',
    'authenticated', 'authenticated', 'sync-b@filmera.test',
    '{"provider":"email","providers":["email"]}', '{}', now(), now()
  );

insert into public.media_items (
  media_type, tmdb_id, title, poster_path, release_date, original_language
)
values ('movie', 680, 'Pulp Fiction', '/pulp-fiction.jpg', '1994-09-10', 'en');

select extensions.is(
  (
    select count(*)
    from pg_class as class
    join pg_namespace as namespace on namespace.oid = class.relnamespace
    where namespace.nspname = 'public'
      and class.relname in (
        'user_preferences', 'user_library', 'user_search_history', 'user_sync_state'
      )
      and class.relrowsecurity
  ),
  4::bigint,
  'RLS is enabled on every user sync table'
);

select extensions.is(
  has_table_privilege('authenticated', 'public.user_preferences', 'SELECT'),
  false,
  'authenticated clients cannot bypass preference RPCs with direct table reads'
);

select extensions.is(
  has_table_privilege('authenticated', 'public.user_library', 'INSERT'),
  false,
  'authenticated clients cannot forge direct library rows'
);

select extensions.is(
  has_table_privilege('authenticated', 'public.user_search_history', 'DELETE'),
  false,
  'authenticated clients cannot hard-delete search tombstones'
);

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.sync_user_preferences(text[],integer[],text[],integer[],integer[],integer[],text[],boolean,timestamp with time zone)',
    'EXECUTE'
  ),
  'authenticated clients can sync preferences'
);

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.sync_user_library(bigint,text,boolean,timestamp with time zone,boolean,timestamp with time zone)',
    'EXECUTE'
  ),
  'authenticated clients can sync library mutations'
);

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.sync_search_history(text,timestamp with time zone,boolean,timestamp with time zone)',
    'EXECUTE'
  ),
  'authenticated clients can sync search mutations'
);

select extensions.is(
  has_function_privilege(
    'anon',
    'public.sync_user_library(bigint,text,boolean,timestamp with time zone,boolean,timestamp with time zone)',
    'EXECUTE'
  ),
  false,
  'anonymous clients cannot mutate a cloud library'
);

select extensions.is(
  has_function_privilege('anon', 'public.get_user_library_sync()', 'EXECUTE'),
  false,
  'anonymous clients cannot read a cloud library'
);

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"33333333-3333-4333-8333-333333333333","role":"authenticated"}';

select extensions.is(
  (
    select onboarding_completed
    from public.sync_user_preferences(
      array['MOVIE', 'ANIME'], array[16, 18, 878], array['ID'],
      array[550], '{}', '{}', array['id'], true,
      now() - interval '2 minutes'
    )
  ),
  true,
  'preference sync stores completed onboarding'
);

select extensions.is(
  (
    select preferred_genre_ids
    from public.get_user_preferences_sync()
  ),
  array[16, 18, 878],
  'preference pull returns genre ids for the current account'
);

select extensions.is(
  (
    select onboarding_completed
    from public.sync_user_preferences(
      array['MOVIE'], array[28], array['US'], '{}', '{}', '{}', '{}', false,
      now() - interval '10 minutes'
    )
  ),
  true,
  'an older preference mutation cannot overwrite newer cloud data'
);

select extensions.is(
  (
    select watch_status
    from public.sync_user_library(
      (select id from public.media_items where tmdb_id = 680 and media_type = 'movie'),
      'watchlist', true, now() - interval '3 minutes', false,
      now() - interval '2 minutes'
    )
  ),
  'watchlist'::text,
  'library sync stores watchlist and favorite independently'
);

select extensions.is(
  (
    select is_favorite
    from public.get_user_library_sync()
    where tmdb_id = 680
  ),
  true,
  'library pull returns the canonical favorite state'
);

select extensions.is(
  (
    select watch_status
    from public.sync_user_library(
      (select id from public.media_items where tmdb_id = 680 and media_type = 'movie'),
      'completed', false, now() - interval '20 minutes', false,
      now() - interval '20 minutes'
    )
  ),
  'watchlist'::text,
  'an older library mutation cannot overwrite newer cloud data'
);

select extensions.is(
  (
    select query
    from public.sync_search_history(
      '  Dune: Part Two  ', now() - interval '2 minutes', false,
      now() - interval '2 minutes'
    )
  ),
  'Dune: Part Two'::text,
  'search sync stores a trimmed display query'
);

select extensions.is(
  (
    select normalized_query
    from public.get_user_search_history_sync()
    where deleted_at is null
  ),
  'dune: part two'::text,
  'search pull exposes a stable case-insensitive key'
);

select extensions.ok(
  (
    select client_updated_at <= now() + interval '5 minutes 1 second'
    from public.sync_search_history(
      'Future query', now() + interval '2 days', false, now() + interval '2 days'
    )
  ),
  'server clamps untrusted future client timestamps'
);

-- Remove the synthetic future row so it does not intentionally outrank the
-- following clear watermark. Equal canonical timestamps still accept delete.
select * from public.sync_search_history(
  'Future query', now() + interval '2 days', true, now() + interval '2 days'
);

select extensions.throws_ok(
  $$
    select * from public.sync_user_library(
      (select id from public.media_items where tmdb_id = 680 and media_type = 'movie'),
      'none', false, now(), false, now()
    )
  $$,
  '22023',
  'An active library item needs a value',
  'empty active library mutations are rejected'
);

select extensions.ok(
  public.clear_search_history(now()) is not null,
  'clear search records a durable per-account watermark'
);

select extensions.is(
  (
    select count(*)
    from public.get_user_search_history_sync()
    where deleted_at is null
  ),
  0::bigint,
  'clear search tombstones all existing active queries'
);

select extensions.ok(
  (
    select deleted_at is not null
    from public.sync_search_history(
      'Old offline device', now() - interval '1 hour', false,
      now() - interval '1 hour'
    )
  ),
  'a stale offline device cannot resurrect search history cleared elsewhere'
);

select extensions.is(
  (
    select deleted_at is null
    from public.sync_search_history(
      'New search', now() + interval '1 second', false, now() + interval '1 second'
    )
  ),
  true,
  'a search made after clear remains active'
);

select extensions.ok(
  (
    select deleted_at is not null
    from public.sync_user_library(
      (select id from public.media_items where tmdb_id = 680 and media_type = 'movie'),
      'none', false, now() - interval '3 minutes', true, now() + interval '2 seconds'
    )
  ),
  'library removal creates a cross-device tombstone'
);

reset role;
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"44444444-4444-4444-8444-444444444444","role":"authenticated"}';

select extensions.is(
  (select count(*) from public.get_user_preferences_sync()),
  0::bigint,
  'another account cannot read the first account preferences'
);

select extensions.is(
  (select count(*) from public.get_user_library_sync()),
  0::bigint,
  'another account cannot read the first account library'
);

select extensions.is(
  (select count(*) from public.get_user_search_history_sync()),
  0::bigint,
  'another account cannot read the first account search history'
);

select extensions.is(
  (
    select is_favorite
    from public.sync_user_library(
      (select id from public.media_items where tmdb_id = 680 and media_type = 'movie'),
      'none', true, now(), false, now()
    )
  ),
  true,
  'another account can keep an independent favorite for the same media'
);

reset role;

select extensions.is(
  (select count(*) from public.user_preferences),
  1::bigint,
  'preference rows remain isolated by user id'
);

select extensions.is(
  (select count(*) from public.user_library),
  2::bigint,
  'library rows use a user and media composite identity'
);

select extensions.is(
  (select count(*) from public.user_sync_state),
  1::bigint,
  'search clear watermark exists only for the account that cleared history'
);

select * from extensions.finish();

rollback;
