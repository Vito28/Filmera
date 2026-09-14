begin;

-- A small, allow-listed reaction layer complements Helpful without turning
-- review engagement into arbitrary user-controlled labels or payloads.
create table public.review_reactions (
  review_id uuid not null
    references public.reviews(id)
    on delete cascade,
  user_id uuid not null
    references auth.users(id)
    on delete cascade,
  reaction text not null
    check (reaction in ('love', 'fire', 'mind_blown', 'moving')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  primary key (review_id, user_id)
);

create index review_reactions_user_recent_idx
  on public.review_reactions(user_id, created_at desc);

create trigger review_reactions_set_updated_at
before update on public.review_reactions
for each row execute function private.set_updated_at();

alter table public.review_reactions enable row level security;

create policy "published review reactions are readable"
on public.review_reactions
for select
to authenticated
using (
  exists (
    select 1
    from public.reviews r
    join public.profiles p on p.id = r.user_id
    where r.id = review_id
      and r.status = 'published'
      and p.account_status = 'active'
  )
);

create policy "users add own review reaction"
on public.review_reactions
for insert
to authenticated
with check (
  (select auth.uid()) = user_id
  and exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.account_status = 'active'
  )
  and exists (
    select 1
    from public.reviews r
    where r.id = review_id
      and r.status = 'published'
  )
);

create policy "users update own review reaction"
on public.review_reactions
for update
to authenticated
using ((select auth.uid()) = user_id)
with check (
  (select auth.uid()) = user_id
  and exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.account_status = 'active'
  )
);

create policy "users remove own review reaction"
on public.review_reactions
for delete
to authenticated
using ((select auth.uid()) = user_id);

revoke all on public.review_reactions from anon, authenticated;
grant select on public.review_reactions to authenticated;
grant insert (review_id, user_id, reaction)
on public.review_reactions to authenticated;
grant update (reaction)
on public.review_reactions to authenticated;
grant delete on public.review_reactions to authenticated;

create or replace function public.set_review_reaction(
  p_review_id uuid,
  p_reaction text
)
returns void
language plpgsql
volatile
security invoker
set search_path = ''
as $$
begin
  if (select auth.uid()) is null then
    raise exception using errcode = '42501', message = 'Authentication required';
  end if;

  if p_reaction is null
    or p_reaction not in ('love', 'fire', 'mind_blown', 'moving') then
    raise exception using errcode = '22023', message = 'Unsupported review reaction';
  end if;

  insert into public.review_reactions (review_id, user_id, reaction)
  values (p_review_id, (select auth.uid()), p_reaction)
  on conflict (review_id, user_id)
  do update set reaction = excluded.reaction;
end;
$$;

revoke all on function public.set_review_reaction(uuid, text)
from public, anon, authenticated;
grant execute on function public.set_review_reaction(uuid, text)
to authenticated;

create or replace function public.remove_review_reaction(p_review_id uuid)
returns void
language sql
volatile
security invoker
set search_path = ''
as $$
  delete from public.review_reactions
  where review_id = p_review_id
    and user_id = (select auth.uid());
$$;

revoke all on function public.remove_review_reaction(uuid)
from public, anon, authenticated;
grant execute on function public.remove_review_reaction(uuid)
to authenticated;

-- This is the one read model shared by Home, Community, and Review Detail.
-- Keeping the aggregate in one RPC prevents an N+1 query for every card.
create or replace function public.get_community_reviews_v2(
  p_limit integer default 20,
  p_cursor_created_at timestamptz default null,
  p_cursor_id uuid default null,
  p_review_id uuid default null
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
    r.id,
    r.media_id,
    m.media_type,
    m.tmdb_id,
    m.title,
    m.poster_path,
    m.release_date,
    p.id,
    p.username::text,
    p.display_name,
    p.avatar_url,
    r.headline,
    r.body,
    r.contains_spoilers,
    author_rating.rating,
    viewer_rating.rating,
    case
      when stats.rating_count > 0
      then round(stats.rating_sum::numeric / stats.rating_count, 1)
      else null
    end,
    coalesce(stats.rating_count, 0),
    r.helpful_count,
    r.comment_count,
    exists (
      select 1
      from public.review_helpful rh
      where rh.review_id = r.id
        and rh.user_id = (select auth.uid())
    ),
    (
      select rr.reaction
      from public.review_reactions rr
      where rr.review_id = r.id
        and rr.user_id = (select auth.uid())
    ),
    (
      select count(*)::integer
      from public.review_reactions rr
      where rr.review_id = r.id and rr.reaction = 'love'
    ),
    (
      select count(*)::integer
      from public.review_reactions rr
      where rr.review_id = r.id and rr.reaction = 'fire'
    ),
    (
      select count(*)::integer
      from public.review_reactions rr
      where rr.review_id = r.id and rr.reaction = 'mind_blown'
    ),
    (
      select count(*)::integer
      from public.review_reactions rr
      where rr.review_id = r.id and rr.reaction = 'moving'
    ),
    r.created_at,
    r.updated_at
  from public.reviews r
  join public.profiles p on p.id = r.user_id
  join public.media_items m on m.id = r.media_id
  left join public.user_ratings author_rating
    on author_rating.user_id = r.user_id
   and author_rating.media_id = r.media_id
  left join public.user_ratings viewer_rating
    on viewer_rating.user_id = (select auth.uid())
   and viewer_rating.media_id = r.media_id
  left join public.media_rating_stats stats on stats.media_id = r.media_id
  where r.status = 'published'
    and p.account_status = 'active'
    and (p_review_id is null or r.id = p_review_id)
    and (
      (p_cursor_created_at is null and p_cursor_id is null)
      or (
        p_cursor_created_at is not null
        and p_cursor_id is not null
        and (r.created_at, r.id) < (p_cursor_created_at, p_cursor_id)
      )
    )
  order by r.created_at desc, r.id desc
  limit least(greatest(coalesce(p_limit, 20), 1), 50);
$$;

revoke all
on function public.get_community_reviews_v2(integer, timestamptz, uuid, uuid)
from public, anon, authenticated;
grant execute
on function public.get_community_reviews_v2(integer, timestamptz, uuid, uuid)
to authenticated;

create or replace function public.get_review_detail_v2(p_review_id uuid)
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
  select feed.*
  from public.get_community_reviews_v2(1, null, null, p_review_id) feed
  limit 1;
$$;

revoke all on function public.get_review_detail_v2(uuid)
from public, anon, authenticated;
grant execute on function public.get_review_detail_v2(uuid)
to authenticated;

-- The server derives the owner from auth.uid(); Android never submits user_id.
create or replace function public.create_review_comment(
  p_review_id uuid,
  p_body text,
  p_parent_comment_id uuid default null
)
returns uuid
language plpgsql
volatile
security invoker
set search_path = ''
as $$
declare
  inserted_id uuid;
begin
  if (select auth.uid()) is null then
    raise exception using errcode = '42501', message = 'Authentication required';
  end if;

  insert into public.comments (
    review_id,
    user_id,
    parent_comment_id,
    body
  )
  values (
    p_review_id,
    (select auth.uid()),
    p_parent_comment_id,
    p_body
  )
  returning id into inserted_id;

  return inserted_id;
end;
$$;

revoke all on function public.create_review_comment(uuid, text, uuid)
from public, anon, authenticated;
grant execute on function public.create_review_comment(uuid, text, uuid)
to authenticated;

create or replace function public.get_review_comment(p_comment_id uuid)
returns table (
  comment_id uuid,
  review_id uuid,
  parent_comment_id uuid,
  author_id uuid,
  author_username text,
  author_display_name text,
  author_avatar_url text,
  body text,
  reply_count integer,
  is_owned_by_viewer boolean,
  created_at timestamptz,
  updated_at timestamptz
)
language sql
stable
security invoker
set search_path = ''
as $$
  select
    c.id,
    c.review_id,
    c.parent_comment_id,
    p.id,
    p.username::text,
    p.display_name,
    p.avatar_url,
    c.body,
    (
      select count(*)::integer
      from public.comments reply
      where reply.parent_comment_id = c.id
        and reply.status = 'published'
    ),
    c.user_id = (select auth.uid()),
    c.created_at,
    c.updated_at
  from public.comments c
  join public.profiles p on p.id = c.user_id
  where c.id = p_comment_id
    and c.status = 'published'
    and p.account_status = 'active';
$$;

revoke all on function public.get_review_comment(uuid)
from public, anon, authenticated;
grant execute on function public.get_review_comment(uuid)
to authenticated;

-- The verified media id comes from the authenticated ensure-media Edge
-- Function. Rating and review are then committed in one database transaction.
create or replace function public.publish_media_review(
  p_media_id bigint,
  p_rating smallint,
  p_headline text,
  p_body text,
  p_contains_spoilers boolean default false
)
returns uuid
language plpgsql
volatile
security invoker
set search_path = ''
as $$
declare
  published_review_id uuid;
begin
  if (select auth.uid()) is null then
    raise exception using errcode = '42501', message = 'Authentication required';
  end if;

  if p_rating not between 1 and 10 then
    raise exception using errcode = '22023', message = 'Rating must be between 1 and 10';
  end if;

  perform public.set_user_rating(p_media_id, p_rating);

  insert into public.reviews (
    user_id,
    media_id,
    headline,
    body,
    contains_spoilers
  )
  values (
    (select auth.uid()),
    p_media_id,
    nullif(btrim(p_headline), ''),
    p_body,
    coalesce(p_contains_spoilers, false)
  )
  on conflict (user_id, media_id)
  do update set
    headline = excluded.headline,
    body = excluded.body,
    contains_spoilers = excluded.contains_spoilers
  returning id into published_review_id;

  return published_review_id;
end;
$$;

revoke all
on function public.publish_media_review(bigint, smallint, text, text, boolean)
from public, anon, authenticated;
grant execute
on function public.publish_media_review(bigint, smallint, text, text, boolean)
to authenticated;

commit;
