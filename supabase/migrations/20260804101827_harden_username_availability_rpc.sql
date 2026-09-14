begin;

-- Availability only needs rows visible through the existing profile RLS.
-- Keeping caller privileges avoids an anonymous SECURITY DEFINER endpoint.
alter function public.is_username_available(text) security invoker;

commit;
