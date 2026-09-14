begin;

create extension if not exists pgtap with schema extensions;

select extensions.plan(13);

insert into auth.users (
  id, aud, role, email, raw_app_meta_data, raw_user_meta_data, created_at, updated_at
)
values
  (
    '33333333-3333-4333-8333-333333333333',
    'authenticated',
    'authenticated',
    'detail-author@filmera.test',
    '{"provider":"email","providers":["email"]}',
    '{"display_name":"Detail Author","username":"detail_author"}',
    now(),
    now()
  ),
  (
    '44444444-4444-4444-8444-444444444444',
    'authenticated',
    'authenticated',
    'detail-viewer@filmera.test',
    '{"provider":"email","providers":["email"]}',
    '{"display_name":"Detail Viewer","username":"detail_viewer"}',
    now(),
    now()
  );

insert into public.media_items (media_type, tmdb_id, title)
values
  ('movie', 991001, 'Scoped Movie'),
  ('tv', 991001, 'Scoped Series'),
  ('movie', 991002, 'Unrelated Movie');

insert into public.user_ratings (user_id, media_id, rating)
select '33333333-3333-4333-8333-333333333333', id, 8
from public.media_items where media_type = 'movie' and tmdb_id = 991001;

insert into public.user_ratings (user_id, media_id, rating)
select '44444444-4444-4444-8444-444444444444', id, 6
from public.media_items where media_type = 'movie' and tmdb_id = 991001;

insert into public.user_ratings (user_id, media_id, rating)
select '44444444-4444-4444-8444-444444444444', id, 9
from public.media_items where media_type = 'movie' and tmdb_id = 991002;

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"33333333-3333-4333-8333-333333333333","role":"authenticated"}';

insert into public.reviews (user_id, media_id, headline, body)
select
  '33333333-3333-4333-8333-333333333333',
  id,
  'Scoped review',
  'This review belongs only to the requested movie detail page.'
from public.media_items where media_type = 'movie' and tmdb_id = 991001;

set local request.jwt.claims =
  '{"sub":"44444444-4444-4444-8444-444444444444","role":"authenticated"}';

insert into public.reviews (user_id, media_id, headline, body)
select
  '44444444-4444-4444-8444-444444444444',
  id,
  'Series review',
  'This review belongs to the TV title and must stay isolated.'
from public.media_items where media_type = 'tv' and tmdb_id = 991001;

insert into public.review_helpful (review_id, user_id)
select
  review.id,
  '44444444-4444-4444-8444-444444444444'
from public.reviews as review
join public.media_items as media on media.id = review.media_id
where review.user_id = '33333333-3333-4333-8333-333333333333'
  and media.media_type = 'movie'
  and media.tmdb_id = 991001;

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.get_media_community_reviews(text,integer,integer)',
    'EXECUTE'
  ),
  'authenticated users can load media-scoped reviews'
);

select extensions.is(
  has_function_privilege(
    'anon',
    'public.get_media_community_reviews(text,integer,integer)',
    'EXECUTE'
  ),
  false,
  'anonymous users cannot load the authenticated community projection'
);

select extensions.ok(
  has_function_privilege(
    'authenticated',
    'public.get_media_rating_summary(text,integer)',
    'EXECUTE'
  ),
  'authenticated users can load the media rating summary'
);

select extensions.is(
  has_function_privilege(
    'anon',
    'public.get_media_rating_summary(text,integer)',
    'EXECUTE'
  ),
  false,
  'anonymous users cannot load personalized rating summaries'
);

set local role authenticated;
set local request.jwt.claims =
  '{"sub":"44444444-4444-4444-8444-444444444444","role":"authenticated"}';

select extensions.is(
  (select count(*) from public.get_media_community_reviews('movie', 991001, 20)),
  1::bigint,
  'the media RPC returns only reviews for the requested movie'
);

select extensions.is(
  (select media_type from public.get_media_community_reviews('movie', 991001, 20)),
  'movie'::text,
  'movie and TV records with the same TMDB id remain isolated'
);

select extensions.is(
  (select user_rating from public.get_media_community_reviews('movie', 991001, 20)),
  8::smallint,
  'the projection includes the review author rating'
);

select extensions.is(
  (select viewer_rating from public.get_media_community_reviews('movie', 991001, 20)),
  6::smallint,
  'the projection includes the current viewer rating'
);

select extensions.is(
  (select filmera_rating from public.get_media_community_reviews('movie', 991001, 20)),
  7.0::numeric,
  'the projection includes the aggregate Filmera rating'
);

select extensions.is(
  (select viewer_has_marked_helpful from public.get_media_community_reviews('movie', 991001, 20)),
  true,
  'Helpful state is personalized for the current viewer'
);

select extensions.is(
  (select count(*) from public.get_media_community_reviews('movie', 123456789, 20)),
  0::bigint,
  'an unknown media key returns an empty result without leaking other reviews'
);

select extensions.is(
  (
    select viewer_rating
    from public.get_media_rating_summary('movie', 991001)
  ),
  6::smallint,
  'the independent summary returns the current viewer rating'
);

select extensions.is(
  (
    select viewer_rating
    from public.get_media_rating_summary('movie', 991002)
  ),
  9::smallint,
  'rating summary remains available when a media title has no reviews'
);

select * from extensions.finish();
rollback;
