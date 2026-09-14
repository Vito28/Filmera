begin;

-- Supabase can add explicit API-role EXECUTE grants when a public function is
-- created. Revoke mutation RPCs from anon explicitly; revoking PUBLIC alone is
-- not sufficient when an explicit role ACL already exists.
revoke execute
on function public.set_user_rating(bigint, smallint),
            public.remove_user_rating(bigint),
            public.mark_review_helpful(uuid),
            public.remove_review_helpful(uuid)
from public, anon;

grant execute
on function public.set_user_rating(bigint, smallint),
            public.remove_user_rating(bigint),
            public.mark_review_helpful(uuid),
            public.remove_review_helpful(uuid)
to authenticated;

commit;
