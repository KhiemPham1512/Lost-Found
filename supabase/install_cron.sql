-- Run as the project database owner after installing the expiration migration.
-- A named schedule is replaced on repeat execution, not duplicated.
create extension if not exists pg_cron with schema pg_catalog;
select cron.schedule('expire-posts', '0 * * * *', 'select public.expire_posts()');
select jobid, jobname, schedule, command, active from cron.job where jobname='expire-posts';
