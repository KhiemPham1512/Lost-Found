# Lost & Found Admin

Admin application: Supabase email/password login, rejection of non-admin or
banned accounts, dashboard, filtered reports/posts/users and audit history.
Report details support review and resolution, including post removal and account
bans. Post and user lists support independent moderation with required reasons.
Only directly reported conversations expose message inspection and locking;
image messages are identified as images without displaying attachments.

Current feature branch (not yet merged/deployed) completes existing admin data:
audit history gains All/Post/User/Conversation filtering through the existing
`target_type` parameter, combined with query/range and page reset. All omits the
parameter; Reset clears all conditions. Dashboard recent reports show the target
label and submission time, with the existing labeled record layout on smaller
screens. Reported posts expose their existing type/category/tags/event time,
posting date and the author's joining month. The stats response adds
`recent_reports[].target_label` using public post titles/account handles or both
conversation handles, matching report-list labels. No new endpoint, permission,
database schema or dependency; dedicated post/user details and chat image viewing follow.
Local verification on 2026-11-10: 35 browser tests passed, two pre-existing
diagnostic/abandoned-experiment probes skipped; TypeScript/Vite build and diff
check passed. The changed surfaces fit 375/768/1024/1440px without viewport or
internal table overflow. External Auth/API are controlled in these tests; this
feature is not yet verified live. Figma editing is deferred at the user's request.
Backend verification passed 87 tests on isolated PostgreSQL and mypy across 34
source files. An HTTP test verifies stats target labels for all three report
types; it initially failed because the field was missing from the real response.

Reported conversations show public sender handles, with sender IDs as a fallback
when a profile is unavailable. Messages run from oldest to newest across loaded
pages; the reporter's messages appear on the right and system notices in the
center. Public handles are read from `profiles` only after message inspection;
private message reads still go through the audited admin API. This change is
merged in PR #33 and verified on the hosted site with real QA Auth/API on
2026-11-10: explicit inspection, all seven QA messages with matching handles
and chronological content, and logout passed. All four viewport widths
375/768/1024/1440px fit without page runtime errors. Profile failure/fallback
and multiple message pages remain covered locally, not by this seven-message
live fixture.

The user now owns C's web scope; Android ownership is separate. Web design uses
`ui-ux-pro-max` when available and follows the functions in the
[team Figma](https://www.figma.com/design/oq27gMLy9XfXWTeerrGQqy/LTTBDD-L-F?node-id=0-1),
with the current API contract taking precedence for permissions and business rules.
Keep the existing teal/Geist tokens and shared shadcn/ui components.

Deployed admin: **https://lost-found-admin.onrender.com**. The FastAPI backend
has a separate URL: `https://lost-found-api-ph8v.onrender.com`.

Catalogs share a search/filter toolbar with a reset action and a result range.
On phones, table rows reflow into labeled records with visible action buttons;
touch controls are at least 44px high. Keyboard users can skip navigation and
focus moves to the destination heading. Canceling a moderation dialog restores
focus to its trigger. These interface changes are verified on the live release,
at `a5fdb02` during the 2026-11-05 check (PR #26). The backend then ran `37a0405`;
backend application code and SQL scripts are unchanged between these commits.

Additional Figma catalog fields are deployed and verified: report target type,
reporter handle and submission time; post category and posting date; user joining
month. They use the existing API response, including its masked phone and joining
month values. The labeled record layout now applies through 1180px so tablet
catalogs keep every field and action accessible; larger desktops retain tables.
Reference frames in the [team Figma](https://www.figma.com/design/oq27gMLy9XfXWTeerrGQqy/LTTBDD-L-F?node-id=0-1):
reports `297:11116`, posts `297:11482` and users `297:11740`. Design follows ui-ux-pro-max
with the existing teal/Geist tokens and shared shadcn/ui table. This is a scoped
catalog change and does not claim full Figma parity.
Local verification on 2026-11-05: all 23 regression/behavior tests and the
TypeScript/Vite build passed (one archived diagnostic probe is skipped). Long
content fits 375/768/1024/1440px without viewport or internal table overflow;
the responsive checks use reduced motion and report no console/page errors.
These UI checks control Auth/API and do not replace deployed integration QA.

Separate live smoke on 2026-11-05 (03:16:33–03:16:40 UTC) passed real Auth/CORS
login, filtered QA records on reports/posts/users and logout. New column headers
were present; reporter handle, category and joining month matched real API values.
Displayed timestamps and target type are covered locally, without automated
live value comparisons in this smoke. All three catalogs fit 375/768/1024/1440px without
viewport or internal table overflow, console/page errors or business writes;
catalog requests returned200. Render deploy `dep-dblv6im7bikc73ec7rq0` was live
at commit `a5fdb02` before this check. The browser blocked all business mutations.

Type filters from Figma are implemented and verified locally: All/Lost/Found on
posts and All/Post/User/Conversation on reports. They use the existing `type`
parameter together with search/status; changing type resets pagination, All
removes type and Reset clears the complete filter. Users omit this control;
audit history uses its separate target-type filter described above.
The shared shadcn ToggleGroup provides labeled keyboard controls with
44px touch targets and the existing semantic colors. Loading is exposed through
the catalog's busy state; error/retry and empty results retain the selected type.

Local verification on 2026-11-05 passed all 28 browser tests and TypeScript/Vite
build (one archived diagnostic is skipped), including type/query/status requests,
page reset, clearing, error recovery, keyboard/touch and 375/768/1024/1440px
without overflow or console/page errors in the normal responsive flow. These
tests control external Auth/API. A separate real Auth/API smoke on 2026-11-10
verified each type, combined search/status, returned item types/statuses, and
reset. With the user's authorized extra QA data, catalogs contain 27 posts and
21 reports: page two returns seven posts and one report without IDs overlapping
page one; changing type from page two sends offset zero. Both catalogs fit all
four viewport widths without page runtime errors; logout passed. Live outage
and retry behavior was not induced.
No endpoint, permission, database or dependency-manifest change is required.

Render confirmed admin merge SHA `45620acf63c5036d5667c4b2042f4ecb07773f1d`
live on 2026-11-10 (deploy `dep-dbpgoqu7bikc73djaigg`). Earlier SHA references
above describe the dated checks. The 11 new labeled QA posts/reports are retained
for verification and affect dashboard counts: posts are ACTIVE, reports PENDING.
Credentials, helper scripts, result manifests and screenshots remain local.

These checks do not verify Android device integration or full Figma parity.
Figma has admin post/user list frames and report details; dedicated admin
post/user detail frames and generic detail API endpoints are not specified.

```powershell
cd admin-web
npm ci
Copy-Item .env.example .env.local  # first setup only; fill publishable values
npm run dev
```

Open `http://localhost:5173`. Start FastAPI separately; set `VITE_API_BASE_URL`
to its URL and add the web origin to backend `ALLOWED_ORIGINS` (comma-separated).
Only use a **publishable key** in frontend configuration; never a server secret.
Changing `.env.local` requires restarting Vite.

```powershell
npm run typecheck
npm run build
```

`dist/` is the static build output. See the root [README](../README.md) for
backend setup, configuration and the two-terminal startup instructions.

Render Static Site deploys `develop` with repository-root build command
`cd admin-web && npm ci && npm run build` and publish directory `admin-web/dist`.
Set `SKIP_INSTALL_DEPS=true` and the three `VITE_*` variables from `.env.example`
in Render; use the deployed backend URL for `VITE_API_BASE_URL`. The backend's
`ALLOWED_ORIGINS` must include `https://lost-found-admin.onrender.com`.
Vite variable changes require a new build; backend CORS changes require deployment.

Live smoke checks on 2026-11-04 passed: health, unauthorized access rejection,
CORS, admin login, dashboard, reports/posts/users/audit lists, report detail and
logout. Search/empty results, search/status reset, keyboard navigation, skip
link and focus restoration after canceling moderation also passed. Dashboard
and all four catalogs fit 375/768/1024/1440px without horizontal overflow or
console/page errors. Live rows did not fill a second page, so pagination checks
covered the disabled last-page boundary; next/previous navigation passed in
the local UI tests. No business mutation was submitted during the live smoke.

Local regression: 19 Playwright tests and the TypeScript/Vite build passed.
An additional isolated integration test drives the rendered UI against real
FastAPI/PostgreSQL for post remove/restore, account ban/unban, report review/
resolution and audit history. External Auth is controlled; business endpoint
responses are real. This test uses a separate loopback CORS wrapper and dummy
identities, so it does not establish deployed Auth/CORS mutation behavior.
Browser plugin was unavailable; validation used local Playwright Chromium.
On 2026-11-05, real HTTP/SDK checks with Supabase Auth verified registration,
claim/PASS, Realtime messages/notifications, two-party DONE, report review/removal,
post restore, account ban/unban and audit on two new QA accounts/posts. Existing
accounts/posts were untouched; QA data is retained on the current project rather
than a separate staging environment. API ban rejected an existing JWT; Auth ban
rejected password login, and unban restored it.

Live browser checks also passed post remove/restore, account ban/unban and logout
with real Auth/CORS, no mocked responses and no console/page errors. The runner
allowed business writes only to the retained QA post/account URLs. The 375px
user list had no horizontal overflow.

Storage QA on 2026-11-05 added one PNG and one photo post using the existing QA
accounts. The uploaded bytes matched the public download; the real admin report
displayed the image at 375/768/1024/1440px without overflow or console/page errors.
The report was resolved with KEEP in the live UI; the QA post was archived and
its image retained. Storage rejected unauthorized uploads/overwrites, and the
API rejected foreign, missing, duplicate and excess image references. This does
not verify every report action, Android Photo Picker/compression or Storage
deletion. B/C integration is deferred while independent A work continues; see
[API verification status](../docs/api.md).

The UI uses Tailwind v4, shadcn/ui Base primitives and TanStack Query. Feature
modules own Auth and read screens; `lib/api.ts` obtains the current access token
for each request. The authenticated shell owns its query cache and discards it
on logout. Expired or rejected server authorization signs the user out.

Reported-chat QA on 2026-11-05 passed with the existing QA accounts and one new
labeled open conversation. Unreported, post-only and user-only reports did not
grant chat access (403); direct Supabase message reads by admin remained empty.
After a conversation report, cursor pagination returned exactly the retained
history and every successful API read wrote a VIEW_CONVERSATION audit entry
(five HTTP pages and two reads initiated by the browser).

The real admin UI opened the QA report/messages, canceled locking with restored
focus, then locked the conversation with a reason. Both participants' valid
sends were rejected with RLS42501 while their readable history stayed intact;
the lock state and one LOCK_CONVERSATION audit entry were verified. Repeated
locking returned409. The detail view fit 375/768/1024/1440px with no console/page
errors. The browser allowed writes only to the new QA conversation/report URLs,
resolved the report KEEP and logged out. Only this run's three QA reports were
resolved; its new post was archived and all history retained. This verifies
existing live behavior, not Android or full visual/functional Figma parity.
