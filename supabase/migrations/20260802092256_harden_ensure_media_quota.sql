begin;

alter table private.ensure_media_requests
  add column id bigint generated always as identity;

alter table private.ensure_media_requests
  add constraint ensure_media_requests_pkey primary key (id);

create or replace function public.consume_ensure_media_quota(p_user_id uuid)
returns boolean
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
  recent_request_count integer;
begin
  if p_user_id is null then
    raise exception using
      errcode = '22023',
      message = 'User ID is required';
  end if;

  perform pg_catalog.pg_advisory_xact_lock(
    pg_catalog.hashtextextended('ensure-media:' || p_user_id::text, 0)
  );

  delete from private.ensure_media_requests
  where user_id = p_user_id
    and requested_at < now() - interval '1 day';

  select count(*)
  into recent_request_count
  from private.ensure_media_requests
  where user_id = p_user_id
    and requested_at >= now() - interval '10 minutes';

  if recent_request_count >= 30 then
    return false;
  end if;

  insert into private.ensure_media_requests (user_id)
  values (p_user_id);

  return true;
end;
$$;

revoke all on function public.consume_ensure_media_quota(uuid)
from public, anon, authenticated, service_role;

grant execute on function public.consume_ensure_media_quota(uuid)
to service_role;

drop function public.consume_ensure_media_quota();

commit;
