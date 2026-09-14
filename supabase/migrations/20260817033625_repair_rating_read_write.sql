begin;

-- Accounts created before Community Core was installed do not pass the
-- user_ratings RLS policy because they have no public profile. Keep this
-- repair callable only by database owners so it can also be used after a
-- partial import without exposing auth.users through the Data API.
create or replace function private.repair_missing_profiles()
returns bigint
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
  auth_user record;
  requested_username text;
  candidate_username text;
  generated_display_name text;
  collision_attempt integer;
  repaired_count bigint := 0;
begin
  for auth_user in
    select users.id, users.raw_user_meta_data
    from auth.users as users
    left join public.profiles as profile on profile.id = users.id
    where profile.id is null
    order by users.created_at, users.id
  loop
    requested_username := lower(
      btrim(coalesce(auth_user.raw_user_meta_data ->> 'username', ''))
    );
    if requested_username collate "C" !~ '^[a-z0-9_]{3,24}$' then
      requested_username := left(
        'user_' || replace(auth_user.id::text, '-', ''),
        24
      );
    end if;

    generated_display_name := left(
      coalesce(
        nullif(btrim(auth_user.raw_user_meta_data ->> 'display_name'), ''),
        'Filmera User'
      ),
      60
    );

    collision_attempt := 0;
    loop
      candidate_username := case
        when collision_attempt = 0 then requested_username
        else 'user_' || left(
          md5(auth_user.id::text || ':' || collision_attempt::text),
          19
        )
      end;

      begin
        insert into public.profiles (id, username, display_name)
        values (auth_user.id, candidate_username, generated_display_name);
        repaired_count := repaired_count + 1;
        exit;
      exception
        when unique_violation then
          -- A concurrent auth trigger may already have repaired this ID.
          if exists (
            select 1 from public.profiles where id = auth_user.id
          ) then
            exit;
          end if;
          collision_attempt := collision_attempt + 1;
          if collision_attempt > 10 then
            raise;
          end if;
      end;
    end loop;
  end loop;

  return repaired_count;
end;
$$;

revoke all on function private.repair_missing_profiles()
from public, anon, authenticated, service_role;

select private.repair_missing_profiles();

-- These RPCs preserve the ownership-safe mutations from Community Core and
-- return the authoritative viewer/aggregate state after the trigger has run.
-- Android can therefore reject a write that returned no readable result.
create or replace function public.set_user_rating_with_summary(
  p_media_id bigint,
  p_rating smallint
)
returns table (
  media_id bigint,
  viewer_rating smallint,
  filmera_rating numeric,
  rating_count bigint
)
language plpgsql
volatile
security invoker
set search_path = ''
as $$
begin
  perform public.set_user_rating(p_media_id, p_rating);

  return query
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
  where media.id = p_media_id;
end;
$$;

revoke all on function public.set_user_rating_with_summary(bigint, smallint)
from public, anon, authenticated, service_role;
grant execute on function public.set_user_rating_with_summary(bigint, smallint)
to authenticated;

create or replace function public.remove_user_rating_with_summary(
  p_media_id bigint
)
returns table (
  media_id bigint,
  viewer_rating smallint,
  filmera_rating numeric,
  rating_count bigint
)
language plpgsql
volatile
security invoker
set search_path = ''
as $$
begin
  if (select auth.uid()) is null then
    raise exception using errcode = '42501', message = 'Authentication required';
  end if;

  perform public.remove_user_rating(p_media_id);

  return query
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
  where media.id = p_media_id;
end;
$$;

revoke all on function public.remove_user_rating_with_summary(bigint)
from public, anon, authenticated, service_role;
grant execute on function public.remove_user_rating_with_summary(bigint)
to authenticated;

commit;
