-- Apply as database owner in a transaction; preserve installed trigger bindings.
-- Only direct EXECUTE grants change, not data, function bodies or RLS policies.
revoke execute on function public.messages_after_insert() from public, anon, authenticated;
revoke execute on function public.claims_before_insert() from public, anon, authenticated;
revoke execute on function public.claims_after_insert() from public, anon, authenticated;
revoke execute on function public.claims_after_status_change() from public, anon, authenticated;
