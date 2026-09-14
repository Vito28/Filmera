begin;

-- Media Detail needs a media-scoped projection. Filtering in SQL keeps the
-- query indexed and avoids downloading an unrelated global community feed.
create or replace function public.get_media_community_reviews(
  p_media_type text,
  p_tmdb_id integer,
  p_limit integer default 20
)
returns table (
  review_id uuid,
  media_id bigint,
  media_type text,
  tmdb_id integer,
  media_title text,
  poster_path text,
  release_date date,
  author_id uuid,
  author_username text,
  author_display_name text,
  author_avatar_url text,
  headline text,
  review_body text,
  contains_spoilers boolean,
  user_rating smallint,
  viewer_rating smallint,
  filmera_rating numeric,
  rating_count bigint,
  helpful_count integer,
  comment_count integer,
  viewer_has_marked_helpful boolean,
  viewer_reaction text,
  love_count integer,
  fire_count integer,
  mind_blown_count integer,
  moving_count integer,
  created_at timestamptz,
  updated_at timestamptz
)
language sql
stable
security invoker
set search_path = ''
as $$
  select
    review.id,
    review.media_id,
    media.media_type,
    media.tmdb_id,
    media.title,
    media.poster_path,
    media.release_date,
    profile.id,
    profile.username::text,
    profile.display_name,
    profile.avatar_url,
    review.headline,
    review.body,
    review.contains_spoilers,
    author_rating.rating,
    viewer_rating.rating,
    case
      when stats.rating_count > 0
      then round(stats.rating_sum::numeric / stats.rating_count, 1)
      else null
    end,
    coalesce(stats.rating_count, 0),
    review.helpful_count,
    review.comment_count,
    exists (
      select 1
      from public.review_helpful as helpful
      where helpful.review_id = review.id
        and helpful.user_id = (select auth.uid())
    ),
    (
      select reaction.reaction
      from public.review_reactions as reaction
      where reaction.review_id = review.id
        and reaction.user_id = (select auth.uid())
    ),
    (
      select count(*)::integer
      from public.review_reactions as reaction
      where reaction.review_id = review.id and reaction.reaction = 'love'
    ),
    (
      select count(*)::integer
      from public.review_reactions as reaction
      where reaction.review_id = review.id and reaction.reaction = 'fire'
    ),
    (
      select count(*)::integer
      from public.review_reactions as reaction
      where reaction.review_id = review.id and reaction.reaction = 'mind_blown'
    ),
    (
      select count(*)::integer
      from public.review_reactions as reaction
      where reaction.review_id = review.id and reaction.reaction = 'moving'
    ),
    review.created_at,
    review.updated_at
  from public.reviews as review
  join public.profiles as profile on profile.id = review.user_id
  join public.media_items as media on media.id = review.media_id
  left join public.user_ratings as author_rating
    on author_rating.user_id = review.user_id
   and author_rating.media_id = review.media_id
  left join public.user_ratings as viewer_rating
    on viewer_rating.user_id = (select auth.uid())
   and viewer_rating.media_id = review.media_id
  left join public.media_rating_stats as stats on stats.media_id = review.media_id
  where review.status = 'published'
    and profile.account_status = 'active'
    and media.media_type = p_media_type
    and media.tmdb_id = p_tmdb_id
  order by review.created_at desc, review.id desc
  limit least(greatest(coalesce(p_limit, 20), 1), 50);
$$;

revoke all on function public.get_media_community_reviews(text, integer, integer)
from public, anon, authenticated;

grant execute on function public.get_media_community_reviews(text, integer, integer)
to authenticated;

-- Ratings are independent from reviews, so Detail must still receive a summary
-- when a title has ratings but nobody has published a review yet.
create or replace function public.get_media_rating_summary(
  p_media_type text,
  p_tmdb_id integer
)
returns table (
  media_id bigint,
  viewer_rating smallint,
  filmera_rating numeric,
  rating_count bigint
)
language sql
stable
security invoker
set search_path = ''
as $$
  select
    media.id,
    viewer.rating,
    case
      when stats.rating_count > 0
      then round(stats.rating_sum::numeric / stats.rating_count, 1)
      else null
    end,
    coalesce(stats.rating_count, 0)
  from public.media_items as media
  left join public.user_ratings as viewer
    on viewer.user_id = (select auth.uid())
   and viewer.media_id = media.id
  left join public.media_rating_stats as stats on stats.media_id = media.id
  where media.media_type = p_media_type
    and media.tmdb_id = p_tmdb_id
  limit 1;
$$;

revoke all on function public.get_media_rating_summary(text, integer)
from public, anon, authenticated;

grant execute on function public.get_media_rating_summary(text, integer)
to authenticated;

commit;
