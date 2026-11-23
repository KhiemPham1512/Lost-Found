-- Existing-project hardening; apply as database owner in a transaction.
-- No table data, RLS policies, function bodies or EXECUTE grants change.
revoke create on schema public from public, anon, authenticated;
alter function public.set_updated_at() set search_path = '';
alter function public.claims_before_update() set search_path = '';
alter function public.distance_km(double precision, double precision, double precision, double precision)
  set search_path = '';
