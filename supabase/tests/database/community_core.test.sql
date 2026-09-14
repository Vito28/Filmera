begin;

create extension if not exists pgtap with schema extensions;

select extensions.plan(77);

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
values
  (
    '11111111-1111-4111-8111-111111111111',
    'authenticated',
    'authenticated',
    'community-a@filmera.test',
    '{"provider":"email","providers":["email"]}',
    '{"display_name":"Community A","username":"community_a"}',
    now(),
    now()
  ),
  (
    '22222222-2222-4222-8222-222222222222',
    'authenticated',
    'authenticated',
    'community-b@filmera.test',
    '{"provider":"email","providers":["email"]}',
    '{"display_name":"Community B"}',
    now(),
    now()
  );

insert into public.media_items (
  media_type,
  tmdb_id,
  title,
  poster_path,
  release_date,
  original_language
)
values ('movie', 550, 'Fight Club', '/test-poster.jpg', '1999-10-15', 'en');

create function pg_temp.attempt_cross_review_update()
returns bigint
language plpgsql
security invoker
set search_path = ''
as $$
declare
  affected_rows bigint;
begin
  update public.reviews
  set body = 'User B must never be able to overwrite the review that belongs to User A.'
  where user_id = '11111111-1111-4111-8111-111111111111';

  get diagnostics affected_rows = row_count;
  return affected_rows;
end;
$$;

select extensions.is(
  (select count(*) from public.profiles),
  2::bigint,
  'auth.users trigger creates one profile per user'
);

select extensions.is(
  (
    select display_name
    from public.profiles
    where id = '11111111-1111-4111-8111-111111111111'
  ),
  'Community A'::text,
  'safe display name metadata is copied into the profile'
);

select extensions.is(
  (
    select username::text
    from public.profiles
    where id = '11111111-1111-4111-8111-111111111111'
  ),
  'community_a'::text,
  'validated public username metadata is copied into the profile'
);

select extensions.ok(
  (
    select username::text ~ '^user_[0-9a-f]{19}$'
    from public.profiles
    where id = '22222222-2222-4222-8222-222222222222'
  ),
  'missing username metadata receives a constraint-safe fallback'
);

select extensions.is(
  public.is_username_available('community_a'),
  false,
  'username availability hides profile rows behind a boolean'
);

select extensions.is(
  public.is_username_available('new_viewer'),
  true,
  'an unused valid username is available'
);

select extensions.is(
  public.is_username_available('Not Valid!'),
  false,
  'invalid usernames are never reported as available'
);

select extensions.ok(
  has_function_privilege(
    'anon',
    'public.is_username_available(text)',
    'EXECUTE'
  ),
  'anonymous sign-up flow can check username availability'
);

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.is_username_available(text)',
    'EXECUTE'
  ),
  'authenticated profile flow can reuse username availability'
);

select extensions.is(
  (
    select prosecdef
    from pg_proc
    where oid = 'public.is_username_available(text)'::regprocedure
  ),
  false,
  'public username availability runs with the caller RLS privileges'
);

select extensions.is(
  has_function_privilege(
    'anon',
    'public.consume_ensure_media_quota(uuid)',
    'EXECUTE'
  ),
  false,
  'anonymous role cannot consume ensure-media quota'
);

select extensions.is(
  has_function_privilege(
    'authenticated',
    'public.consume_ensure_media_quota(uuid)',
    'EXECUTE'
  ),
  false,
  'authenticated role cannot bypass the Edge Function quota flow'
);

select extensions.ok(
  has_function_privilege(
    'service_role',
    'public.consume_ensure_media_quota(uuid)',
    'EXECUTE'
  ),
  'only the backend service role can consume ensure-media quota'
);

select extensions.is(
  has_table_privilege(
    'authenticated',
    'private.ensure_media_requests',
    'SELECT'
  ),
  false,
  'mobile role cannot inspect private quota rows'
);

select extensions.ok(
  has_table_privilege('service_role', 'public.media_items', 'SELECT'),
  'backend service role can read cached media'
);

select extensions.ok(
  has_column_privilege(
    'service_role',
    'public.media_items',
    'media_type',
    'INSERT'
  ),
  'backend service role can insert verified media metadata'
);

select extensions.ok(
  has_column_privilege(
    'service_role',
    'public.media_items',
    'title',
    'UPDATE'
  ),
  'backend service role can refresh verified media metadata'
);

select extensions.is(
  has_table_privilege('service_role', 'public.media_items', 'DELETE'),
  false,
  'backend service role cannot delete media through ensure-media'
);

select extensions.ok(
  has_sequence_privilege(
    'service_role',
    'public.media_items_id_seq',
    'USAGE'
  ),
  'backend service role can allocate a media identity'
);

set local role service_role;

insert into public.media_items (
  media_type,
  tmdb_id,
  title,
  poster_path,
  release_date,
  original_language
)
values ('tv', 1399, 'Game of Thrones', null, '2011-04-17', 'en')
on conflict (media_type, tmdb_id) do update
set title = excluded.title;

select extensions.ok(
  public.consume_ensure_media_quota('11111111-1111-4111-8111-111111111111'),
  'first ensure-media request is accepted'
);

do $$
begin
  for request_number in 2..30 loop
    if not public.consume_ensure_media_quota(
      '11111111-1111-4111-8111-111111111111'
    ) then
      raise exception 'ensure-media quota rejected request % too early', request_number;
    end if;
  end loop;
end;
$$;

select extensions.is(
  public.consume_ensure_media_quota('11111111-1111-4111-8111-111111111111'),
  false,
  'ensure-media rejects request 31 within ten minutes'
);

reset role;
set local role service_role;

select extensions.ok(
  public.consume_ensure_media_quota('22222222-2222-4222-8222-222222222222'),
  'ensure-media quota is isolated per user'
);

reset role;

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"11111111-1111-4111-8111-111111111111","role":"authenticated"}';

select public.set_user_rating(
  (select id from public.media_items where media_type = 'movie' and tmdb_id = 550),
  7::smallint
);

insert into public.reviews (
  user_id,
  media_id,
  headline,
  body,
  contains_spoilers
)
values (
  '11111111-1111-4111-8111-111111111111',
  (select id from public.media_items where media_type = 'movie' and tmdb_id = 550),
  'Sharp and memorable',
  'A sufficiently long review body used to validate the community backend flow.',
  false
);

insert into public.comments (review_id, user_id, body)
select
  id,
  '11111111-1111-4111-8111-111111111111',
  'Root comment'
from public.reviews
where user_id = '11111111-1111-4111-8111-111111111111';

select extensions.is(
  (
    select rating_count
    from public.media_rating_stats
    where media_id = (
      select id from public.media_items where media_type = 'movie' and tmdb_id = 550
    )
  ),
  1::bigint,
  'first rating creates the aggregate row'
);

select extensions.is(
  (
    select rating_sum
    from public.media_rating_stats
    where media_id = (
      select id from public.media_items where media_type = 'movie' and tmdb_id = 550
    )
  ),
  7::bigint,
  'first rating updates the aggregate sum'
);

reset role;
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"22222222-2222-4222-8222-222222222222","role":"authenticated"}';

select public.set_user_rating(
  (select id from public.media_items where media_type = 'movie' and tmdb_id = 550),
  9::smallint
);

select public.mark_review_helpful(
  (
    select id
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  )
);

select extensions.throws_ok(
  $$
    select public.set_review_reaction(
      (
        select id
        from public.reviews
        where user_id = '11111111-1111-4111-8111-111111111111'
      ),
      'arbitrary_emoji'
    )
  $$,
  '22023',
  'Unsupported review reaction',
  'reaction RPC rejects values outside the server allow-list'
);

select public.set_review_reaction(
  (
    select id
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  ),
  'love'
);

select public.set_review_reaction(
  (
    select id
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  ),
  'fire'
);

select extensions.is(
  (select count(*) from public.review_reactions),
  1::bigint,
  'one user keeps one reaction per review after an update'
);

select extensions.is(
  (select reaction from public.review_reactions limit 1),
  'fire'::text,
  'reaction upsert replaces the previous reaction atomically'
);

-- The mutation RPC is intentionally idempotent.
select public.mark_review_helpful(
  (
    select id
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  )
);

insert into public.comments (
  review_id,
  user_id,
  parent_comment_id,
  body
)
select
  root.review_id,
  '22222222-2222-4222-8222-222222222222',
  root.id,
  'One-level reply'
from public.comments root
where root.user_id = '11111111-1111-4111-8111-111111111111'
  and root.parent_comment_id is null;

select extensions.is(
  (
    select rating_count
    from public.media_rating_stats
    where media_id = (
      select id from public.media_items where media_type = 'movie' and tmdb_id = 550
    )
  ),
  2::bigint,
  'two users produce a rating count of two'
);

select extensions.is(
  (
    select rating_sum
    from public.media_rating_stats
    where media_id = (
      select id from public.media_items where media_type = 'movie' and tmdb_id = 550
    )
  ),
  16::bigint,
  'rating sum tracks both users atomically'
);

select extensions.is(
  (
    select helpful_count
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  ),
  1,
  'duplicate Helpful calls only increment the counter once'
);

select extensions.is(
  (
    select comment_count
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  ),
  2,
  'root comment and one reply increment the comment counter'
);

select extensions.is(
  (select count(*) from public.user_ratings),
  2::bigint,
  'unique rating key keeps one row per user and media'
);

select extensions.is(
  (select count(*) from public.review_helpful),
  1::bigint,
  'Helpful composite key remains unique'
);

select extensions.is(
  pg_temp.attempt_cross_review_update(),
  0::bigint,
  'RLS hides User A review from User B update operations'
);

select extensions.is(
  has_column_privilege('authenticated', 'public.reviews', 'helpful_count', 'UPDATE'),
  false,
  'the mobile role cannot update helpful_count directly'
);

select extensions.throws_ok(
  $$
    insert into public.user_ratings (user_id, media_id, rating)
    values (
      '11111111-1111-4111-8111-111111111111',
      (select id from public.media_items where media_type = 'movie' and tmdb_id = 550),
      10
    )
  $$,
  '42501',
  'new row violates row-level security policy for table "user_ratings"',
  'User B cannot create a rating owned by User A'
);

select extensions.throws_ok(
  $$
    insert into public.comments (review_id, user_id, parent_comment_id, body)
    select
      child.review_id,
      '22222222-2222-4222-8222-222222222222',
      child.id,
      'Forbidden third level'
    from public.comments child
    where child.user_id = '22222222-2222-4222-8222-222222222222'
  $$,
  'P0001',
  'Nested replies are limited to one level',
  'reply depth is limited by a database trigger'
);

select extensions.is(
  (select count(*) from public.get_community_feed(20, null, null)),
  1::bigint,
  'authenticated feed returns the published review'
);

select extensions.ok(
  (
    select viewer_has_marked_helpful
    from public.get_community_feed(20, null, null)
    limit 1
  ),
  'feed reports Helpful state for the authenticated viewer'
);

select extensions.is(
  (select count(*) from public.get_community_reviews_v2(20, null, null, null)),
  1::bigint,
  'v2 community feed returns the published review'
);

select extensions.is(
  (
    select viewer_reaction
    from public.get_community_reviews_v2(20, null, null, null)
    limit 1
  ),
  'fire'::text,
  'v2 feed reports the authenticated viewer reaction'
);

select extensions.is(
  (
    select fire_count
    from public.get_community_reviews_v2(20, null, null, null)
    limit 1
  ),
  1,
  'v2 feed reports allow-listed reaction aggregates'
);

select extensions.is(
  (
    select viewer_rating
    from public.get_community_reviews_v2(20, null, null, null)
    limit 1
  ),
  9::smallint,
  'v2 feed reports the viewer rating separately from the author rating'
);

select extensions.is(
  (
    select filmera_rating
    from public.get_community_reviews_v2(20, null, null, null)
    limit 1
  ),
  8.0::numeric,
  'v2 feed computes the real Filmera aggregate from rating stats'
);

select extensions.is(
  (
    select count(*)
    from public.get_review_detail_v2(
      (
        select id
        from public.reviews
        where user_id = '11111111-1111-4111-8111-111111111111'
      )
    )
  ),
  1::bigint,
  'review detail RPC resolves a published review by id'
);

select extensions.is(
  (
    select count(*)
    from public.get_community_feed(20, now(), null::uuid)
  ),
  0::bigint,
  'a partial feed cursor is rejected as an empty page'
);

select extensions.is(
  (select count(*) from public.get_review_comments(
    (select id from public.reviews where user_id = '11111111-1111-4111-8111-111111111111'),
    30,
    null,
    null
  )),
  1::bigint,
  'root comments RPC only returns root comments'
);

select extensions.is(
  (
    select reply_count
    from public.get_review_comments(
      (select id from public.reviews where user_id = '11111111-1111-4111-8111-111111111111'),
      30,
      null,
      null
    )
    limit 1
  ),
  1,
  'root comments RPC exposes the reply count'
);

select extensions.is(
  (
    select count(*)
    from public.get_comment_replies(
      (
        select id
        from public.comments
        where user_id = '11111111-1111-4111-8111-111111111111'
          and parent_comment_id is null
      ),
      30,
      null,
      null
    )
  ),
  1::bigint,
  'replies RPC returns only direct children'
);

select extensions.ok(
  public.create_review_comment(
    (
      select id
      from public.reviews
      where user_id = '11111111-1111-4111-8111-111111111111'
    ),
    'Created through the ownership-safe comment RPC',
    null
  ) is not null,
  'comment mutation RPC returns the inserted server id'
);

select extensions.is(
  (
    select count(*)
    from public.comments
    where user_id = '22222222-2222-4222-8222-222222222222'
      and body = 'Created through the ownership-safe comment RPC'
  ),
  1::bigint,
  'comment mutation RPC derives the owner from auth.uid'
);

select extensions.ok(
  (
    select is_owned_by_viewer
    from public.get_review_comment(
      (
        select id
        from public.comments
        where user_id = '22222222-2222-4222-8222-222222222222'
          and body = 'Created through the ownership-safe comment RPC'
      )
    )
  ),
  'single-comment RPC exposes ownership without trusting Android input'
);

select extensions.ok(
  public.publish_media_review(
    (select id from public.media_items where media_type = 'movie' and tmdb_id = 550),
    9::smallint,
    'A second perspective',
    'A second sufficiently long review published through the transaction-safe RPC.',
    false
  ) is not null,
  'publish review RPC returns the canonical server review id'
);

select extensions.is(
  (select count(*) from public.reviews where status = 'published'),
  2::bigint,
  'publish review RPC commits a review for the authenticated user'
);

delete from public.reviews
where user_id = '22222222-2222-4222-8222-222222222222';

reset role;
set local role anon;
set local request.jwt.claims = '{"role":"anon"}';

select extensions.is(
  public.is_username_available('community_a'),
  false,
  'anonymous sign-up sees an existing public username as unavailable'
);

select extensions.is(
  public.is_username_available('anon_new_viewer'),
  true,
  'anonymous sign-up can check an unused username through RLS'
);

select extensions.is(
  (select count(*) from public.get_community_feed(20, null, null)),
  1::bigint,
  'anonymous users can read the published feed'
);

select extensions.is(
  (
    select viewer_has_marked_helpful
    from public.get_community_feed(20, null, null)
    limit 1
  ),
  false,
  'anonymous feed safely reports false Helpful state'
);

select extensions.is(
  has_table_privilege('anon', 'public.user_ratings', 'INSERT'),
  false,
  'anonymous role has no rating write grant'
);

select extensions.is(
  has_function_privilege('anon', 'public.set_user_rating(bigint,smallint)', 'EXECUTE'),
  false,
  'anonymous role cannot execute the rating mutation RPC'
);

select extensions.is(
  has_function_privilege('anon', 'public.set_review_reaction(uuid,text)', 'EXECUTE'),
  false,
  'anonymous role cannot execute reaction mutations'
);

select extensions.is(
  has_function_privilege(
    'anon',
    'public.get_community_reviews_v2(integer,timestamp with time zone,uuid,uuid)',
    'EXECUTE'
  ),
  false,
  'anonymous role cannot access the authenticated Home engagement projection'
);

reset role;
set local role authenticated;
set local request.jwt.claims =
  '{"sub":"22222222-2222-4222-8222-222222222222","role":"authenticated"}';

select public.remove_user_rating(
  (select id from public.media_items where media_type = 'movie' and tmdb_id = 550)
);

select public.remove_review_helpful(
  (
    select id
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  )
);

select public.remove_review_reaction(
  (
    select id
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  )
);

delete from public.comments
where user_id = '22222222-2222-4222-8222-222222222222';

select extensions.is(
  (
    select rating_count
    from public.media_rating_stats
    where media_id = (
      select id from public.media_items where media_type = 'movie' and tmdb_id = 550
    )
  ),
  1::bigint,
  'rating delete decrements aggregate count'
);

select extensions.is(
  (
    select rating_sum
    from public.media_rating_stats
    where media_id = (
      select id from public.media_items where media_type = 'movie' and tmdb_id = 550
    )
  ),
  7::bigint,
  'rating delete decrements aggregate sum'
);

select extensions.is(
  (
    select helpful_count
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  ),
  0,
  'Helpful delete decrements the counter'
);

select extensions.is(
  (select count(*) from public.review_reactions),
  0::bigint,
  'reaction removal deletes only the viewer reaction'
);

select extensions.is(
  (
    select comment_count
    from public.reviews
    where user_id = '11111111-1111-4111-8111-111111111111'
  ),
  1,
  'comment delete decrements the counter'
);

reset role;
update public.profiles
set account_status = 'suspended'
where id = '22222222-2222-4222-8222-222222222222';

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"22222222-2222-4222-8222-222222222222","role":"authenticated"}';

select extensions.throws_ok(
  $$
    insert into public.reviews (user_id, media_id, body)
    values (
      '22222222-2222-4222-8222-222222222222',
      (select id from public.media_items where media_type = 'movie' and tmdb_id = 550),
      'A suspended account must not be able to create a new community review.'
    )
  $$,
  '42501',
  'Account cannot create reviews',
  'suspended users cannot create reviews'
);

select extensions.throws_ok(
  $$
    insert into public.comments (review_id, user_id, body)
    values (
      (select id from public.reviews where user_id = '11111111-1111-4111-8111-111111111111'),
      '22222222-2222-4222-8222-222222222222',
      'Suspended comment attempt'
    )
  $$,
  '42501',
  'Account cannot create comments',
  'suspended users cannot create comments'
);

reset role;

select extensions.is(
  (
    select count(*)
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relname in (
        'profiles',
        'media_items',
        'user_ratings',
        'media_rating_stats',
        'reviews',
        'comments',
        'review_helpful',
        'review_reactions'
      )
      and c.relrowsecurity
  ),
  8::bigint,
  'RLS is enabled on every Community Core public table'
);

select extensions.ok(
  has_function_privilege('authenticated', 'public.set_review_reaction(uuid,text)', 'EXECUTE'),
  'authenticated role can execute allow-listed reaction mutations'
);

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.create_review_comment(uuid,text,uuid)',
    'EXECUTE'
  ),
  'authenticated role can create comments through the ownership-safe RPC'
);

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.publish_media_review(bigint,smallint,text,text,boolean)',
    'EXECUTE'
  ),
  'authenticated role can publish an ensured media review through one transaction'
);

select extensions.is(
  has_table_privilege('anon', 'public.review_reactions', 'SELECT'),
  false,
  'anonymous role cannot inspect raw reaction rows'
);

select extensions.ok(
  has_table_privilege('authenticated', 'public.review_reactions', 'SELECT'),
  'authenticated feed role can aggregate public review reactions'
);

select extensions.ok(
  to_regclass('public.comments_parent_comment_fk_idx') is not null,
  'parent comment foreign key has a full supporting index'
);

select extensions.ok(
  has_function_privilege(
    'anon',
    'public.get_community_feed(integer,timestamp with time zone,uuid)',
    'EXECUTE'
  ),
  'anonymous role can execute the read-only feed RPC'
);

select * from extensions.finish();

rollback;
