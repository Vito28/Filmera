begin;

create table private.ensure_media_requests (
  user_id uuid not null
    references auth.users(id)
    on delete cascade,
  requested_at timestamptz not null default now()
);

create index ensure_media_requests_user_recent_idx
  on private.ensure_media_requests(user_id, requested_at desc);

revoke all on table private.ensure_media_requests
from public, anon, authenticated;

create or replace function public.consume_ensure_media_quota()
returns boolean
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
  caller_id uuid := (select auth.uid());
  recent_request_count integer;
begin
  if caller_id is null then
    raise exception using
      errcode = '42501',
      message = 'Authentication required';
  end if;

  -- Serialize quota checks per user so concurrent requests cannot bypass the
  -- limit between COUNT and INSERT.
  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtextextended('ensure-media:' || caller_id::text, 0)
  );

  delete from private.ensure_media_requests
  where user_id = caller_id
    and requested_at < now() - interval '1 day';

  select count(*)
  into recent_request_count
  from private.ensure_media_requests
  where user_id = caller_id
    and requested_at >= now() - interval '10 minutes';

  if recent_request_count >= 30 then
    return false;
  end if;

  insert into private.ensure_media_requests (user_id)
  values (caller_id);

  return true;
end;
$$;

revoke all on function public.consume_ensure_media_quota()
from public, anon, authenticated;

grant execute on function public.consume_ensure_media_quota()
to authenticated;

commit;
