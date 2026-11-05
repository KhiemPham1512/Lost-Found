-- Preserve visibility and terminal lifecycle state across moderation removal.
alter table public.posts add column if not exists removed_from_status public.post_status;
update public.posts set removed_from_status='RESOLVED'
  where status='REMOVED' and resolved_at is not null and removed_from_status is null;
