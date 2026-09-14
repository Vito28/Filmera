begin;

create extension if not exists pgtap with schema extensions;

select extensions.plan(14);

insert into auth.users (
  id,
  aud,
  role,
  email,
  raw_app_meta_data,
  raw_user_meta_data,
  created_at,
  updated_at
)
values (
  '33333333-3333-4333-8333-333333333333',
  'authenticated',
  'authenticated',
  'legacy-rating@filmera.test',
  '{"provider":"email","providers":["email"]}',
  '{"display_name":"Legacy Rating User","username":"legacy_rating"}',
  now(),
  now()
);

-- Simulate an Auth account that predates the profile trigger.
delete from public.profiles
where id = '33333333-3333-4333-8333-333333333333';

select extensions.is(
  private.repair_missing_profiles(),
  1::bigint,
  'one missing legacy profile is repaired'
);

select extensions.ok(
  exists (
    select 1
    from public.profiles
    where id = '33333333-3333-4333-8333-333333333333'
      and account_status = 'active'
  ),
  'the repaired profile is active and can pass rating RLS'
);

select extensions.is(
  (
    select username::text
    from public.profiles
    where id = '33333333-3333-4333-8333-333333333333'
  ),
  'legacy_rating'::text,
  'valid public username metadata is preserved during repair'
);

select extensions.is(
  has_function_privilege(
    'service_role',
    'private.repair_missing_profiles()',
    'EXECUTE'
  ),
  false,
  'the profile repair is not exposed to service_role or the Data API'
);

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.set_user_rating_with_summary(bigint,smallint)',
    'EXECUTE'
  ),
  'authenticated users can execute the verified rating write'
);

select extensions.is(
  has_function_privilege(
    'anon',
    'public.set_user_rating_with_summary(bigint,smallint)',
    'EXECUTE'
  ),
  false,
  'anonymous callers cannot write ratings'
);

select extensions.is(
  has_function_privilege(
    'anon',
    'public.remove_user_rating_with_summary(bigint)',
    'EXECUTE'
  ),
  false,
  'anonymous callers cannot remove ratings'
);

insert into public.media_items (
  media_type,
  tmdb_id,
  title,
  release_date,
  original_language
)
values ('movie', 991003, 'Rating Read Write Test', '2026-08-17', 'en');

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"33333333-3333-4333-8333-333333333333","role":"authenticated"}';

select extensions.is(
  (
    select viewer_rating
    from public.set_user_rating_with_summary(
      (select id from public.media_items where tmdb_id = 991003),
      8::smallint
    )
  ),
  8::smallint,
  'rating write returns the authoritative viewer rating'
);

select extensions.is(
  (
    select filmera_rating
    from public.get_media_rating_summary('movie', 991003)
  ),
  8.0::numeric,
  'rating read returns the aggregate written by the trigger'
);

select extensions.is(
  (
    select rating_count
    from public.get_media_rating_summary('movie', 991003)
  ),
  1::bigint,
  'rating read returns the authoritative rating count'
);

select extensions.is(
  (
    select rating
    from public.user_ratings
    where user_id = '33333333-3333-4333-8333-333333333333'
  ),
  8::smallint,
  'the verified RPC persists the owned rating row'
);

select extensions.is(
  (
    select viewer_rating
    from public.remove_user_rating_with_summary(
      (select id from public.media_items where tmdb_id = 991003)
    )
  ),
  null::smallint,
  'rating removal returns a null authoritative viewer rating'
);

select extensions.is(
  (
    select rating_count
    from public.get_media_rating_summary('movie', 991003)
  ),
  0::bigint,
  'rating removal updates the aggregate count'
);

select extensions.is(
  (
    select count(*)
    from public.user_ratings
    where user_id = '33333333-3333-4333-8333-333333333333'
  ),
  0::bigint,
  'rating removal deletes the owned rating row'
);

select * from extensions.finish();

rollback;
