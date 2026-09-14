begin;

create extension if not exists citext with schema extensions;

create schema if not exists private;

revoke all on schema private from public, anon, authenticated;

-- Keep future RPCs closed until a migration grants them explicitly.
alter default privileges in schema public
  revoke execute on functions from public;

create table public.profiles (
  id uuid primary key
    references auth.users(id)
    on delete cascade,
  username extensions.citext not null unique,
  display_name text not null,
  avatar_url text,
  bio text,
  account_status text not null default 'active'
    check (account_status in ('active', 'suspended', 'deleted')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint profiles_username_format_check
    check (
      username::text = lower(username::text)
      and username::text collate "C" ~ '^[a-z0-9_]{3,24}$'
    ),
  constraint profiles_display_name_length_check
    check (char_length(btrim(display_name)) between 1 and 60),
  constraint profiles_avatar_url_check
    check (
      avatar_url is null
      or (
        char_length(avatar_url) <= 2048
        and avatar_url ~ '^https://'
      )
    ),
  constraint profiles_bio_length_check
    check (bio is null or char_length(bio) <= 300)
);

create table public.media_items (
  id bigint generated always as identity primary key,
  media_type text not null
    check (media_type in ('movie', 'tv')),
  tmdb_id integer not null
    check (tmdb_id > 0),
  title text not null,
  poster_path text,
  release_date date,
  original_language text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint media_items_unique_tmdb unique (media_type, tmdb_id),
  constraint media_items_title_length_check
    check (char_length(btrim(title)) between 1 and 300),
  constraint media_items_poster_path_check
    check (
      poster_path is null
      or (
        char_length(poster_path) <= 500
        and poster_path ~ '^/'
      )
    ),
  constraint media_items_language_check
    check (
      original_language is null
      or original_language collate "C" ~ '^[a-z]{2,3}$'
    )
);

create table public.user_ratings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null
    references auth.users(id)
    on delete cascade,
  media_id bigint not null
    references public.media_items(id)
    on delete cascade,
  rating smallint not null
    check (rating between 1 and 10),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint user_ratings_one_per_media unique (user_id, media_id)
);

create table public.media_rating_stats (
  media_id bigint primary key
    references public.media_items(id)
    on delete cascade,
  rating_count bigint not null default 0
    check (rating_count >= 0),
  rating_sum bigint not null default 0
    check (rating_sum >= 0),
  updated_at timestamptz not null default now()
);

create table public.reviews (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null
    references auth.users(id)
    on delete cascade,
  media_id bigint not null
    references public.media_items(id)
    on delete cascade,
  headline text,
  body text not null,
  contains_spoilers boolean not null default false,
  status text not null default 'published'
    check (status in ('published', 'under_review', 'hidden', 'deleted')),
  helpful_count integer not null default 0
    check (helpful_count >= 0),
  comment_count integer not null default 0
    check (comment_count >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint reviews_one_per_user_media unique (user_id, media_id),
  constraint reviews_headline_length_check
    check (headline is null or char_length(headline) <= 120),
  constraint reviews_body_length_check
    check (char_length(btrim(body)) between 20 and 5000)
);

create table public.comments (
  id uuid primary key default gen_random_uuid(),
  review_id uuid not null
    references public.reviews(id)
    on delete cascade,
  user_id uuid not null
    references auth.users(id)
    on delete cascade,
  parent_comment_id uuid
    references public.comments(id)
    on delete cascade,
  body text not null,
  status text not null default 'published'
    check (status in ('published', 'under_review', 'hidden', 'deleted')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint comments_body_length_check
    check (char_length(btrim(body)) between 1 and 2000),
  constraint comments_not_self_parent_check
    check (parent_comment_id is null or parent_comment_id <> id)
);

create table public.review_helpful (
  review_id uuid not null
    references public.reviews(id)
    on delete cascade,
  user_id uuid not null
    references auth.users(id)
    on delete cascade,
  created_at timestamptz not null default now(),

  primary key (review_id, user_id)
);

create index profiles_account_status_idx
  on public.profiles(account_status);

create index user_ratings_media_idx
  on public.user_ratings(media_id);

create index user_ratings_user_updated_idx
  on public.user_ratings(user_id, updated_at desc);

create index reviews_feed_published_idx
  on public.reviews(created_at desc, id desc)
  where status = 'published';

create index reviews_media_recent_idx
  on public.reviews(media_id, created_at desc, id desc)
  where status = 'published';

create index reviews_media_helpful_idx
  on public.reviews(media_id, helpful_count desc, created_at desc, id desc)
  where status = 'published';

create index reviews_user_recent_idx
  on public.reviews(user_id, created_at desc);

create index comments_review_roots_idx
  on public.comments(review_id, created_at asc, id asc)
  where status = 'published' and parent_comment_id is null;

create index comments_parent_replies_idx
  on public.comments(parent_comment_id, created_at asc, id asc)
  where status = 'published';

-- The partial replies index cannot support every ON DELETE CASCADE lookup.
create index comments_parent_comment_fk_idx
  on public.comments(parent_comment_id);

create index comments_user_recent_idx
  on public.comments(user_id, created_at desc);

create index comments_review_all_idx
  on public.comments(review_id);

create index review_helpful_user_recent_idx
  on public.review_helpful(user_id, created_at desc);

create or replace function private.set_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

revoke all on function private.set_updated_at() from public;

create trigger profiles_set_updated_at
before update on public.profiles
for each row execute function private.set_updated_at();

create trigger media_items_set_updated_at
before update on public.media_items
for each row execute function private.set_updated_at();

create trigger user_ratings_set_updated_at
before update on public.user_ratings
for each row execute function private.set_updated_at();

create trigger reviews_set_updated_at
before update on public.reviews
for each row execute function private.set_updated_at();

create trigger comments_set_updated_at
before update on public.comments
for each row execute function private.set_updated_at();

create or replace function private.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  generated_username text;
  generated_display_name text;
begin
  generated_username := 'user_' || replace(new.id::text, '-', '');
  generated_username := left(generated_username, 24);

  generated_display_name := coalesce(
    nullif(btrim(new.raw_user_meta_data ->> 'display_name'), ''),
    'Filmera User'
  );

  insert into public.profiles (id, username, display_name)
  values (
    new.id,
    generated_username,
    left(generated_display_name, 60)
  );

  return new;
end;
$$;

revoke all on function private.handle_new_user() from public;

create trigger on_auth_user_created
after insert on auth.users
for each row execute function private.handle_new_user();

create or replace function private.sync_rating_stats()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    insert into public.media_rating_stats (
      media_id,
      rating_count,
      rating_sum,
      updated_at
    )
    values (new.media_id, 1, new.rating, now())
    on conflict (media_id)
    do update set
      rating_count = public.media_rating_stats.rating_count + 1,
      rating_sum = public.media_rating_stats.rating_sum + excluded.rating_sum,
      updated_at = now();
    return new;
  end if;

  if tg_op = 'UPDATE' then
    if old.media_id <> new.media_id then
      raise exception using
        errcode = 'P0001',
        message = 'Media ID cannot be changed';
    end if;

    if old.rating <> new.rating then
      update public.media_rating_stats
      set
        rating_sum = greatest(0, rating_sum - old.rating + new.rating),
        updated_at = now()
      where media_id = new.media_id;
    end if;
    return new;
  end if;

  if tg_op = 'DELETE' then
    update public.media_rating_stats
    set
      rating_count = greatest(0, rating_count - 1),
      rating_sum = greatest(0, rating_sum - old.rating),
      updated_at = now()
    where media_id = old.media_id;

    delete from public.media_rating_stats
    where media_id = old.media_id and rating_count = 0;
    return old;
  end if;

  return null;
end;
$$;

revoke all on function private.sync_rating_stats() from public;

create trigger user_ratings_sync_stats
after insert or update or delete on public.user_ratings
for each row execute function private.sync_rating_stats();

create or replace function private.sync_review_helpful_count()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    update public.reviews
    set helpful_count = helpful_count + 1
    where id = new.review_id;
    return new;
  end if;

  if tg_op = 'DELETE' then
    update public.reviews
    set helpful_count = greatest(0, helpful_count - 1)
    where id = old.review_id;
    return old;
  end if;

  return null;
end;
$$;

revoke all on function private.sync_review_helpful_count() from public;

create trigger review_helpful_sync_count
after insert or delete on public.review_helpful
for each row execute function private.sync_review_helpful_count();

create or replace function private.sync_review_comment_count()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    if new.status = 'published' then
      update public.reviews
      set comment_count = comment_count + 1
      where id = new.review_id;
    end if;
    return new;
  end if;

  if tg_op = 'UPDATE' then
    if old.review_id <> new.review_id then
      raise exception using
        errcode = 'P0001',
        message = 'Review ID cannot be changed';
    end if;

    if old.status <> 'published' and new.status = 'published' then
      update public.reviews
      set comment_count = comment_count + 1
      where id = new.review_id;
    elsif old.status = 'published' and new.status <> 'published' then
      update public.reviews
      set comment_count = greatest(0, comment_count - 1)
      where id = new.review_id;
    end if;
    return new;
  end if;

  if tg_op = 'DELETE' then
    if old.status = 'published' then
      update public.reviews
      set comment_count = greatest(0, comment_count - 1)
      where id = old.review_id;
    end if;
    return old;
  end if;

  return null;
end;
$$;

revoke all on function private.sync_review_comment_count() from public;

create trigger comments_sync_review_count
after insert or update or delete on public.comments
for each row execute function private.sync_review_comment_count();

create or replace function private.validate_comment_write()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  active_status text;
  recent_comment_count integer;
  parent_review_id uuid;
  parent_parent_id uuid;
  target_review_status text;
begin
  if new.user_id is distinct from (select auth.uid()) then
    raise exception using
      errcode = '42501',
      message = 'Invalid comment owner';
  end if;

  select account_status
  into active_status
  from public.profiles
  where id = new.user_id;

  if active_status is distinct from 'active' then
    raise exception using
      errcode = '42501',
      message = 'Account cannot create comments';
  end if;

  select status
  into target_review_status
  from public.reviews
  where id = new.review_id;

  if target_review_status is distinct from 'published' then
    raise exception using
      errcode = 'P0001',
      message = 'Review is not available';
  end if;

  select count(*)
  into recent_comment_count
  from public.comments
  where user_id = new.user_id
    and created_at >= now() - interval '10 minutes';

  if recent_comment_count >= 20 then
    raise exception using
      errcode = 'P0001',
      message = 'Comment rate limit exceeded';
  end if;

  if new.parent_comment_id is not null then
    select review_id, parent_comment_id
    into parent_review_id, parent_parent_id
    from public.comments
    where id = new.parent_comment_id
      and status = 'published';

    if not found then
      raise exception using
        errcode = 'P0001',
        message = 'Parent comment does not exist';
    end if;

    if parent_review_id <> new.review_id then
      raise exception using
        errcode = 'P0001',
        message = 'Parent comment belongs to another review';
    end if;

    if parent_parent_id is not null then
      raise exception using
        errcode = 'P0001',
        message = 'Nested replies are limited to one level';
    end if;
  end if;

  new.status = 'published';
  return new;
end;
$$;

revoke all on function private.validate_comment_write() from public;

create trigger comments_validate_insert
before insert on public.comments
for each row execute function private.validate_comment_write();

create or replace function private.validate_review_write()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  active_status text;
  recent_review_count integer;
begin
  if new.user_id is distinct from (select auth.uid()) then
    raise exception using
      errcode = '42501',
      message = 'Invalid review owner';
  end if;

  select account_status
  into active_status
  from public.profiles
  where id = new.user_id;

  if active_status is distinct from 'active' then
    raise exception using
      errcode = '42501',
      message = 'Account cannot create reviews';
  end if;

  select count(*)
  into recent_review_count
  from public.reviews
  where user_id = new.user_id
    and created_at >= now() - interval '1 hour';

  if recent_review_count >= 5 then
    raise exception using
      errcode = 'P0001',
      message = 'Review rate limit exceeded';
  end if;

  new.status = 'published';
  return new;
end;
$$;

revoke all on function private.validate_review_write() from public;

create trigger reviews_validate_insert
before insert on public.reviews
for each row execute function private.validate_review_write();

alter table public.profiles enable row level security;
alter table public.media_items enable row level security;
alter table public.user_ratings enable row level security;
alter table public.media_rating_stats enable row level security;
alter table public.reviews enable row level security;
alter table public.comments enable row level security;
alter table public.review_helpful enable row level security;

create policy "profiles are publicly readable"
on public.profiles
for select
to anon, authenticated
using (account_status <> 'deleted');

create policy "users update own active profile"
on public.profiles
for update
to authenticated
using (
  (select auth.uid()) = id
  and account_status = 'active'
)
with check (
  (select auth.uid()) = id
  and account_status = 'active'
);

create policy "media items are publicly readable"
on public.media_items
for select
to anon, authenticated
using (true);

create policy "rating stats are publicly readable"
on public.media_rating_stats
for select
to anon, authenticated
using (true);

create policy "ratings are publicly readable"
on public.user_ratings
for select
to anon, authenticated
using (true);

create policy "users insert own rating"
on public.user_ratings
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
);

create policy "users update own rating"
on public.user_ratings
for update
to authenticated
using (
  (select auth.uid()) = user_id
  and exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.account_status = 'active'
  )
)
with check (
  (select auth.uid()) = user_id
  and exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.account_status = 'active'
  )
);

create policy "users delete own rating"
on public.user_ratings
for delete
to authenticated
using ((select auth.uid()) = user_id);

create policy "published or owned reviews are readable"
on public.reviews
for select
to anon, authenticated
using (
  status = 'published'
  or (
    (select auth.uid()) is not null
    and (select auth.uid()) = user_id
  )
);

create policy "users insert own reviews"
on public.reviews
for insert
to authenticated
with check (
  (select auth.uid()) = user_id
  and status = 'published'
);

create policy "users update own reviews"
on public.reviews
for update
to authenticated
using (
  (select auth.uid()) = user_id
  and exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.account_status = 'active'
  )
)
with check (
  (select auth.uid()) = user_id
  and exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.account_status = 'active'
  )
);

create policy "users delete own reviews"
on public.reviews
for delete
to authenticated
using ((select auth.uid()) = user_id);

create policy "published or owned comments are readable"
on public.comments
for select
to anon, authenticated
using (
  status = 'published'
  or (
    (select auth.uid()) is not null
    and (select auth.uid()) = user_id
  )
);

create policy "users insert own comments"
on public.comments
for insert
to authenticated
with check (
  (select auth.uid()) = user_id
  and status = 'published'
);

create policy "users update own comments"
on public.comments
for update
to authenticated
using (
  (select auth.uid()) = user_id
  and exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.account_status = 'active'
  )
)
with check (
  (select auth.uid()) = user_id
  and exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.account_status = 'active'
  )
);

create policy "users delete own comments"
on public.comments
for delete
to authenticated
using ((select auth.uid()) = user_id);

create policy "users read own helpful rows"
on public.review_helpful
for select
to authenticated
using ((select auth.uid()) = user_id);

create policy "users mark reviews helpful"
on public.review_helpful
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

create policy "users remove own helpful"
on public.review_helpful
for delete
to authenticated
using ((select auth.uid()) = user_id);

revoke all
on public.profiles,
   public.media_items,
   public.user_ratings,
   public.media_rating_stats,
   public.reviews,
   public.comments,
   public.review_helpful
from anon, authenticated;

grant usage on schema public to anon, authenticated;

grant select
on public.profiles,
   public.media_items,
   public.user_ratings,
   public.media_rating_stats,
   public.reviews,
   public.comments,
   public.review_helpful
to anon, authenticated;

grant update (username, display_name, avatar_url, bio)
on public.profiles
to authenticated;

grant insert (user_id, media_id, rating)
on public.user_ratings
to authenticated;

grant update (rating)
on public.user_ratings
to authenticated;

grant delete on public.user_ratings to authenticated;

grant insert (user_id, media_id, headline, body, contains_spoilers)
on public.reviews
to authenticated;

grant update (headline, body, contains_spoilers)
on public.reviews
to authenticated;

grant delete on public.reviews to authenticated;

grant insert (review_id, user_id, parent_comment_id, body)
on public.comments
to authenticated;

grant update (body)
on public.comments
to authenticated;

grant delete on public.comments to authenticated;

grant insert (review_id, user_id)
on public.review_helpful
to authenticated;

grant delete on public.review_helpful to authenticated;

create or replace function public.set_user_rating(
  p_media_id bigint,
  p_rating smallint
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

  if p_rating not between 1 and 10 then
    raise exception using errcode = '22023', message = 'Rating must be between 1 and 10';
  end if;

  insert into public.user_ratings (user_id, media_id, rating)
  values ((select auth.uid()), p_media_id, p_rating)
  on conflict (user_id, media_id)
  do update set rating = excluded.rating;
end;
$$;

revoke all on function public.set_user_rating(bigint, smallint) from public;
grant execute on function public.set_user_rating(bigint, smallint) to authenticated;

create or replace function public.remove_user_rating(p_media_id bigint)
returns void
language sql
volatile
security invoker
set search_path = ''
as $$
  delete from public.user_ratings
  where user_id = (select auth.uid())
    and media_id = p_media_id;
$$;

revoke all on function public.remove_user_rating(bigint) from public;
grant execute on function public.remove_user_rating(bigint) to authenticated;

create or replace function public.mark_review_helpful(p_review_id uuid)
returns void
language sql
volatile
security invoker
set search_path = ''
as $$
  insert into public.review_helpful (review_id, user_id)
  values (p_review_id, (select auth.uid()))
  on conflict (review_id, user_id) do nothing;
$$;

revoke all on function public.mark_review_helpful(uuid) from public;
grant execute on function public.mark_review_helpful(uuid) to authenticated;

create or replace function public.remove_review_helpful(p_review_id uuid)
returns void
language sql
volatile
security invoker
set search_path = ''
as $$
  delete from public.review_helpful
  where review_id = p_review_id
    and user_id = (select auth.uid());
$$;

revoke all on function public.remove_review_helpful(uuid) from public;
grant execute on function public.remove_review_helpful(uuid) to authenticated;

create or replace function public.get_community_feed(
  p_limit integer default 20,
  p_cursor_created_at timestamptz default null,
  p_cursor_id uuid default null
)
returns table (
  review_id uuid,
  media_id bigint,
  media_type text,
  tmdb_id integer,
  media_title text,
  poster_path text,
  author_id uuid,
  author_username text,
  author_display_name text,
  author_avatar_url text,
  headline text,
  review_body text,
  contains_spoilers boolean,
  user_rating smallint,
  helpful_count integer,
  comment_count integer,
  viewer_has_marked_helpful boolean,
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
    p.id,
    p.username::text,
    p.display_name,
    p.avatar_url,
    r.headline,
    r.body,
    r.contains_spoilers,
    ur.rating,
    r.helpful_count,
    r.comment_count,
    coalesce(
      exists (
        select 1
        from public.review_helpful rh
        where rh.review_id = r.id
          and rh.user_id = (select auth.uid())
      ),
      false
    ),
    r.created_at,
    r.updated_at
  from public.reviews r
  join public.profiles p on p.id = r.user_id
  join public.media_items m on m.id = r.media_id
  left join public.user_ratings ur
    on ur.user_id = r.user_id
   and ur.media_id = r.media_id
  where r.status = 'published'
    and p.account_status = 'active'
    and (
      (
        p_cursor_created_at is null
        and p_cursor_id is null
      )
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
on function public.get_community_feed(integer, timestamptz, uuid)
from public;

grant execute
on function public.get_community_feed(integer, timestamptz, uuid)
to anon, authenticated;

create or replace function public.get_review_comments(
  p_review_id uuid,
  p_limit integer default 30,
  p_cursor_created_at timestamptz default null,
  p_cursor_id uuid default null
)
returns table (
  comment_id uuid,
  review_id uuid,
  author_id uuid,
  author_username text,
  author_display_name text,
  author_avatar_url text,
  body text,
  reply_count integer,
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
    c.created_at,
    c.updated_at
  from public.comments c
  join public.profiles p on p.id = c.user_id
  where c.review_id = p_review_id
    and c.parent_comment_id is null
    and c.status = 'published'
    and p.account_status = 'active'
    and (
      (
        p_cursor_created_at is null
        and p_cursor_id is null
      )
      or (
        p_cursor_created_at is not null
        and p_cursor_id is not null
        and (c.created_at, c.id) > (p_cursor_created_at, p_cursor_id)
      )
    )
  order by c.created_at asc, c.id asc
  limit least(greatest(coalesce(p_limit, 30), 1), 100);
$$;

revoke all
on function public.get_review_comments(uuid, integer, timestamptz, uuid)
from public;

grant execute
on function public.get_review_comments(uuid, integer, timestamptz, uuid)
to anon, authenticated;

create or replace function public.get_comment_replies(
  p_parent_comment_id uuid,
  p_limit integer default 30,
  p_cursor_created_at timestamptz default null,
  p_cursor_id uuid default null
)
returns table (
  comment_id uuid,
  review_id uuid,
  parent_comment_id uuid,
  author_id uuid,
  author_username text,
  author_display_name text,
  author_avatar_url text,
  body text,
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
    c.created_at,
    c.updated_at
  from public.comments c
  join public.profiles p on p.id = c.user_id
  where c.parent_comment_id = p_parent_comment_id
    and c.status = 'published'
    and p.account_status = 'active'
    and (
      (
        p_cursor_created_at is null
        and p_cursor_id is null
      )
      or (
        p_cursor_created_at is not null
        and p_cursor_id is not null
        and (c.created_at, c.id) > (p_cursor_created_at, p_cursor_id)
      )
    )
  order by c.created_at asc, c.id asc
  limit least(greatest(coalesce(p_limit, 30), 1), 100);
$$;

revoke all
on function public.get_comment_replies(uuid, integer, timestamptz, uuid)
from public;

grant execute
on function public.get_comment_replies(uuid, integer, timestamptz, uuid)
to anon, authenticated;

create or replace function private.broadcast_comment_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  target_review_id uuid;
  target_comment_id uuid;
  target_parent_id uuid;
  target_user_id uuid;
  target_updated_at timestamptz;
  event_name text;
begin
  if tg_op = 'DELETE' then
    target_review_id := old.review_id;
    target_comment_id := old.id;
    target_parent_id := old.parent_comment_id;
    target_user_id := old.user_id;
    target_updated_at := old.updated_at;
    event_name := 'comment_deleted';
  else
    target_review_id := new.review_id;
    target_comment_id := new.id;
    target_parent_id := new.parent_comment_id;
    target_user_id := new.user_id;
    target_updated_at := new.updated_at;
    event_name := case
      when tg_op = 'INSERT' then 'comment_created'
      else 'comment_updated'
    end;
  end if;

  perform realtime.send(
    jsonb_build_object(
      'comment_id', target_comment_id,
      'review_id', target_review_id,
      'parent_comment_id', target_parent_id,
      'user_id', target_user_id,
      'updated_at', target_updated_at
    ),
    event_name,
    'review:' || target_review_id::text || ':comments',
    true
  );

  return null;
end;
$$;

revoke all on function private.broadcast_comment_change() from public;

create trigger comments_broadcast_change
after insert or update or delete on public.comments
for each row execute function private.broadcast_comment_change();

create policy "authenticated users receive comment broadcasts"
on realtime.messages
for select
to authenticated
using (
  realtime.messages.extension = 'broadcast'
  and (select realtime.topic()) ~
    '^review:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}:comments$'
);

commit;
