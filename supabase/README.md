# Database operations

## Public schema hardening (applied to live)

PR #53 merged and the separately approved patch was applied on November 23,
2026 as `public_schema_hardening` (`20261123063736`). Live checks confirmed
client CREATE is denied, USAGE/RLS/EXECUTE and function bodies are preserved,
the three search-path advisories are gone, and normal member chat still works.
Scheduled expiration also succeeded after deployment, including the three
latest runs checked on November 27 (11:00, 12:00 and 13:00 UTC).

`patches/20261123_public_schema_hardening.sql` is the existing-project upgrade;
`schema.sql` includes the same settings for new projects. Run the patch as the
database owner inside a transaction after reviewing the live definitions of
`set_updated_at()`, `claims_before_update()` and `distance_km(float8,float8,float8,float8)`.
Their bodies must still use only trigger records and built-in operations, since
the patch pins their search path to an empty string. It preserves their bodies,
owners, invoker/definer mode and EXECUTE grants.

The patch revokes CREATE on schema `public` from PUBLIC, `anon` and
`authenticated`. It preserves existing USAGE and table/RLS permissions, so
creating a post, claim or message through the application is unaffected. The
database owner can still install objects. No rows, indexes, buckets, Auth
settings or RLS policies are changed. Applying the patch twice is safe; merging
the source does not apply it to the hosted database.

After deployment, verify `has_schema_privilege('anon','public','CREATE')` and the
same check for `authenticated` are false, all three functions have an empty
configured search path, and normal application reads/writes still work. Re-run
security advisors; the three mutable-search-path findings should be gone.

The November 23 read-only review also found EXECUTE advisories on four trigger
functions and `is_admin()`. Trigger functions cannot be called as ordinary
functions; `is_admin()` checks the current `auth.uid()` and is used by RLS.
This patch deliberately leaves their execution permissions unchanged. These
findings are not proof of a privilege bypass, nor is this a complete security
audit. Leaked-password protection is disabled; Supabase makes that feature
available on [Pro and above](https://supabase.com/docs/guides/auth/password-security).
It requires a separate Auth/plan decision. Performance findings (12 uncovered
foreign keys, 13 per-row Auth/RLS expressions and three unused indexes) remain
separate measured optimization work; do not drop indexes based only on low use.

## Private trigger helpers (not applied to live)

`patches/20261123_private_trigger_functions.sql` removes direct EXECUTE from
PUBLIC, `anon` and `authenticated` for the four internal claim/message trigger
helpers. The same restrictions are included in `schema.sql` after the owner
installs each canonical trigger. Review and apply this separate patch as owner
inside a transaction; the approval for PR #53 does not cover it.

The patch does not recreate triggers or change their functions, data or RLS.
Existing triggers continue handling claim notifications, conversations, message
timestamps and handover completion. Explicit owner/service-role grants remain.
`is_admin()` remains available for its existing RLS callers and checks the
current user's role. Client roles do not need direct access to internal helpers
for normal application operations. See PostgreSQL's
[trigger permission requirements](https://www.postgresql.org/docs/17/sql-createtrigger.html).

After deployment, verify direct EXECUTE is false for both client roles on all
four helpers, normal workflows still succeed and the corresponding eight
definer-EXECUTE advisory records are gone. This does not resolve the separate
`is_admin` advisories, leaked-password plan decision or performance notices.

## Chat image storage

The reported-chat image feature adds a **private** chat-images bucket. For an
existing project apply patches/20261111_private_chat_images.sql as owner inside
a transaction before using the new admin image endpoint. It does not delete or
move existing data or change post-images. Verify that the chat-images bucket
has public=false, a 5 MiB limit and JPEG/PNG/WebP MIME
types. The patch preserves configuration if the bucket name already exists.
The API fails closed if that bucket is absent/public. This storage patch was
approved separately and applied to the live project on November 22, 2026;
actual private-member upload/read checks are recorded in the API handoff.

Authenticated active members may upload only into their own sender folder in
an unlocked conversation, and read the conversation's history after locking.
Nonmember admins obtain temporary image access only through audited FastAPI.
No UPDATE/DELETE policy is added. Path and client integration contract are in
docs/api.md. Policy tests run locally; actual member uploads/private reads and
locked-history access were also verified against live Storage on November 22.

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
