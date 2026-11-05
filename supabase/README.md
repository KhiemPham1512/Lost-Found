# Database operations

For a new project, run `schema.sql`. Existing projects run the two migrations in
`migrations/` in timestamp order, then `install_cron.sql` as the database owner.
The migration files supplement this repository's existing schema/patch workflow;
do not run `supabase db reset` or assume these are a complete baseline history.

`install_cron.sql` enables pg_cron and installs the named hourly `expire-posts`
job. Repeating it replaces the same named schedule. Inspect actual execution:

```sql
select jobid, jobname, schedule, command, active
from cron.job where jobname = 'expire-posts';
select status, start_time, end_time, return_message
from cron.job_run_details
where jobid = (select jobid from cron.job where jobname = 'expire-posts')
order by start_time desc limit 5;
```

The expiration function runs with invoker privileges; application roles cannot
call it. It expires only due ACTIVE posts, closes open claims, locks their chats
and notifies authors. Locked posts are skipped and revisited on the next run.

Read-only live verification on 2026-11-04 confirmed `expire-posts` is active
with schedule `0 * * * *` and command `select public.expire_posts()`. Its three
latest runs (14:00, 15:00, 16:00 UTC) succeeded. All 16 public tables have RLS
enabled, and `claims`, `messages`, `notifications` are in `supabase_realtime`.
This confirms configuration and cron execution, not complete policy behavior
or Realtime delivery on Android devices. No seed/reset or schema changes were
performed during this check.

Storage QA on 2026-11-05 used the existing dedicated QA users and one new PNG.
Upload into the owner's folder and public download passed; downloaded SHA-256
matched the uploaded bytes. Wrong-folder/root/anonymous uploads and another
user's overwrite attempt were denied with RLS `403`. Listing a known nonempty
folder returned no objects, consistent with the intentionally absent SELECT
policy. The image was attached to one new QA post and rendered by the live
admin UI; the post is archived and the image is retained. No object deletion,
seed/reset or policy changes were performed. Bucket MIME/size limits remain
unset; Android compression and Storage delete operations were not tested.

For demo data, from the repository root:

```powershell
backend/.venv/Scripts/python.exe dev/seed_demo.py
```

This provisions five dedicated Auth accounts and ten clearly labeled demo posts
without deleting existing data. Generated credentials stay in gitignored
`dev/demo-accounts.local.json`; repeat runs reuse them. It reads server settings
from `backend/.env`. Do not delete that local account file to rerun the same seed.
The SQL command `seed_demo.sql` accepts five existing Auth IDs through the
transaction-local `app.demo_user_ids` JSON array and preserves existing profiles.

`reset.sql` and `dev_clean_test_data.sql` are destructive development helpers;
neither is part of the test or seed workflow.
