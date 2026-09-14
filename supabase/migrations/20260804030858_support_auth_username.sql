begin;

-- Usernames are public profile data only. Never use raw_user_meta_data for
-- authorization; auth.users.id remains the ownership boundary.
create or replace function private.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  generated_username text;
  generated_display_name text;
  requested_username text;
begin
  generated_username := left(
    'user_' || replace(new.id::text, '-', ''),
    24
  );

  requested_username := lower(
    btrim(coalesce(new.raw_user_meta_data ->> 'username', ''))
  );

  if requested_username collate "C" !~ '^[a-z0-9_]{3,24}$' then
    requested_username := generated_username;
  end if;

  generated_display_name := coalesce(
    nullif(btrim(new.raw_user_meta_data ->> 'display_name'), ''),
    'Filmera User'
  );

  insert into public.profiles (id, username, display_name)
  values (
    new.id,
    requested_username,
    left(generated_display_name, 60)
  );

  return new;
end;
$$;

revoke all on function private.handle_new_user() from public;

-- This deliberately returns one boolean instead of profile rows. SECURITY
-- DEFINER is required so usernames reserved by soft-deleted accounts remain
-- unavailable even though those profiles are hidden by RLS.
create or replace function public.is_username_available(p_username text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select case
    when p_username is null
      or lower(btrim(p_username)) collate "C" !~ '^[a-z0-9_]{3,24}$'
      then false
    else not exists (
      select 1
      from public.profiles
      where username = lower(btrim(p_username))::extensions.citext
    )
  end;
$$;

revoke all on function public.is_username_available(text)
from public, anon, authenticated, service_role;

grant execute on function public.is_username_available(text)
to anon, authenticated;

commit;
