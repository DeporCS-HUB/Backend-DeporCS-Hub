# Depor CS HUB — development checkpoint

## Iterasi implementasi — 19 Oktober 2026

Filter server dan pagination exact-next untuk enam resource; dashboard global recorded/status counts; assignment Staff membawa status/progress langsung; KPI dominan dan donut; scroll chart dan halaman list pendek. Migration `20261019042816_dashboard_coverage.sql` sudah diterapkan melalui apply_migration; nama file mengikuti versi yang tercatat di hosted. Jangan mengulang atau seed.

Verifikasi lokal: Java 62, frontend 23 unit + lint/build, empat suite SQL rollback dan 22 browser mock termasuk 375/768/1440 px. Hosted RPC dibaca dengan simulated JWT, bukan login Auth nyata. QA-01 menunggu kredensial uji aman; tidak reset user. Security advisor masih mencatat leaked-password protection disabled; tidak mengaktifkan fitur yang dapat mengubah biaya. Tidak ada provisioning/upgrade.


## Checkpoint dokumentasi — 19 Oktober 2026

Sembilan dokumen proyek tersedia pada root: [AGENTS.md](AGENTS.md), [PRD.md](PRD.md), [DESIGN.md](DESIGN.md), [ARCHITECTURE.md](ARCHITECTURE.md), [DATA.md](DATA.md), [API.md](API.md), [TASKS.md](TASKS.md), [CHANGELOG.md](CHANGELOG.md), dan [README.md](README.md). README/AGENTS disesuaikan dengan repository ini; aturan development, Free/$0 dan perlindungan kredensial tetap berlaku.

Baseline kode yang didokumentasikan: frontend `cae6348`, backend `445813b`. Dokumentasi membedakan fitur yang sudah diimplementasikan dari rencana visual berdasarkan referensi baru. Tidak ada perubahan aplikasi, schema, data, akun, atau konfigurasi. Paragraf bertanggal lama di bawah merupakan riwayat; jangan memakai pernyataan lama tentang data kosong atau dashboard lama sebagai keadaan terkini.

## Program workspace — 10 October 2026

The development database now contains the user-supplied program roster. Migration `20261009173515_program_workspace.sql` adds Proker/UKOR categories, multiple account-linked PJ assignments, optional progress/dates/budget, next milestones, and append-only progress history. Actual department records are stored only in development, not in this public repository. See [program workspace](docs/program-workspace.md).

This checkpoint supersedes historical statements below about empty business tables and read-only Staff program access. Original Auth accounts and stored legacy roles remain unchanged. No paid resources were added.


Checkpoint: 2026-10-08 23:55 WIB. Read AGENTS.md and README.md before continuing.

## Project and authorization

Development is **DeporCS HUB**, ref `dajpnhkutkhgxwkjzvpg`, URL `https://dajpnhkutkhgxwkjzvpg.supabase.co`, organization **DeporCS Hub** `jvdgogmbphotxdzrvbvc`. Plan Free is verified. Only Free/$0 features are authorized; no upgrades, paid branching, compute/add-ons, or new provisioning.

The user explicitly reclassified this formerly protected ref as development, authorized migrations core/events_profiles, and revoked its former production restriction. Do not modify other production projects. Implementation work is on `codex/supabase-main-flows`. The user authorized merge and Free/$0 deployment on 9 October 2026; see the current authorization below.

| Repository | Merged PR #1 |
| --- | --- |
| https://github.com/DeporCS-HUB/Backend-DeporCS-Hub | https://github.com/DeporCS-HUB/Backend-DeporCS-Hub/pull/1 |
| https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub | https://github.com/DeporCS-HUB/Frontend-DeporCS-Hub/pull/1 |

Never keep credentials in Git, reports, logs, frontend, or command-line arguments. Use secure process environment. The frontend requires no Supabase key. Do not copy passwords from chat into files.

## Hosted schema and users

Applied migrations and matching filenames:

1. `supabase/migrations/20261008154150_core.sql` — version 20261008154150, name core.
2. `supabase/migrations/20261008154204_events_profiles.sql` — version 20261008154204, name events_profiles.

Six tables (profiles/programs/tasks/finances/inventory/events), all RLS. Internal SECURITY DEFINER bootstrap/role lookup is in private schema; dashboard RPC is SECURITY INVOKER. Profile bootstrap is inactive/member, ignores role/active metadata, and normalizes whitespace names. Do not rerun migrations under old filenames.

The user created two confirmed development Auth accounts. Their supplied UUID/email mappings were verified before trusted SQL promotion: staff+active and member+active. Both now remain active/confirmed with original names. Credentials and personal identifiers are not stored here; request secure environment configuration for future tests, never passwords in chat. No service-role/admin key was needed for the application tests.

## Current evidence

- `mvn verify`: **42 passing Java tests**, 31 mock API/security plus 11 simulated HTTP adapter tests.
- `scripts/integration-hosted.py`: **49 real Auth/Java/PostgREST checks passed**, using actual hosted services and user JWTs.
- Staff/member login and trusted-profile lookup; HttpOnly cookie headers; all five resource CRUD and cross-request PostgREST persistence; dashboard totals; member ownership/management restrictions; direct RLS and role-grant protections; profile self-edit; FK conflict; real refresh rotation; logout and revoked refresh reuse.
- Cleanup SQL: Auth users/profiles 2/2, all five resource tables empty, test-account sessions 0, original profile names restored. No seed or permanent fixture data remains.
- Earlier local+hosted transactional SQL RLS suites passed with simulated JWT claims, and 15 read-only HTTP/API checks passed. SQL suites require an empty disposable DB; do not rerun them on this now-populated Auth project. Never run `supabase/tests/bootstrap.sql` on hosted Supabase.
- Previous frontend tests: 10 unit and 7 mocked-API browser flows. Real React/browser reload and browser cookie-policy behavior are still untested.

Live integration found two adapter issues, now fixed: Auth 403/bad_jwt maps to 401 for session handling; HttpURLConnection rejected PATCH and is replaced with Java HttpClient/JdkClientHttpRequestFactory. PATCH regression verifies method/body/user token. Configurable connect/read timeouts default 5000/10000 ms, acceptance harness uses 15000/30000 ms, range 1–120000. Debug transport logs contain only exception class names.

## Advisors and limits

Latest security advisor: one warning, leaked-password protection disabled. Supabase documents this as Pro+; keep Free/$0, do not upgrade. Remediation: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection . No evidence establishes that the chosen test passwords leaked.

Performance inspection: four created_by FKs lack indexes and nine RLS policies reevaluate auth.uid per row. Follow-up optimization has not been applied. Unused-index INFO on the fresh DB did not justify removal. See backend docs/live-development-validation.md for links/evidence/limits.

Logout proof concerns refresh revocation; copied access JWTs may last until expiry. Roles/activation are rechecked on data access. At the API-test checkpoint, no deployment or merge had occurred. Commits skip new Actions runs for the zero-cost constraint; actual evidence is local tests and hosted API/SQL.

## Implementation and next work

Backend Java 21/Spring Boot 3.5.7; React 19/Vite. Protected Auth/session cookies, role-aware CRUD, server/SQL validation, dashboard RPC, events with manual permit records, own-profile settings, read-only Team. Missing: invitations, attendance, role admin UI, attachments, notifications, preferences, external permit delivery, concurrent-edit conflict handling.

Future API acceptance: configure SUPABASE_URL/SUPABASE_ANON_KEY plus DEPOR_TEST_STAFF_EMAIL/PASSWORD/UUID and DEPOR_TEST_MEMBER_EMAIL/PASSWORD/UUID in secure environment; run mvn verify then python scripts/integration-hosted.py. The script only accepts the authorized development ref, creates/removes its own fixtures, restores profile name, and logs out. It never creates/deletes users or changes roles. Read-only smoke: python scripts/verify-hosted-readiness.py.

Next useful validation is real React/browser acceptance (login → CRUD → reload → refresh → logout) against this development backend, without deploying production. Backend defaults 8080, frontend 3000, allowed origin http://localhost:3000 and secure-cookie false for HTTP local. Preserve HTTPS/secure cookies for cloud. Review performance with a new CLI-generated migration when authorized; keep existing migration history intact.

History/browser sessions do not transfer via this document. Chrome login does not automatically authenticate cloud browser. Supabase connector works; earlier cloud dashboard login was blocked by a Google network 502. User provisioned Auth accounts through their own authenticated dashboard.

## Merge/deployment authorization — 9 October 2026

The user explicitly requested merge through deployment. This supersedes the earlier requirement to wait for new merge/deploy instructions. Both PR #1s may be merged after checks; deployment remains limited to Free/$0 services and the already authorized development database. Render hosting is configured but no service or live URL is verified yet. The Render plugin must be installed and connected before hosted service inspection/provisioning can continue; the local execution environment is offline.

The backend Blueprint now explicitly sets plan: free; omitting plan would select paid compute for a new web service. Both Blueprints target main with autoDeployTrigger: off to avoid uncontrolled builds. Merge/commit messages use [skip ci] and [skip render] to avoid automatic runs until hosting/account cost checks are complete. Neither setting blocks an explicitly requested manual deployment.

Before provisioning, verify a no-cost Render workspace, remaining free usage and a billing setup that cannot charge overages. Free compute alone does not guarantee a $0 bill: Render can charge excess bandwidth/build usage when a payment method exists. Do not add a payment method or upgrade. Review backend docs/deployment.md for the deployment sequence and acceptance checks. The previous 42 Java/49 hosted API results remain valid; real hosted browser acceptance is still pending.

## Current hosting checkpoint — 9 October 2026

Both repository PR #1s are merged into main and both repositories are verified public after the user changed visibility. The user selected Vercel for the Java backend. Vercel Container Images now support Java/Spring Boot through root Dockerfile.vercel; use the project environment PORT=8080 to match Spring and container routing. The frontend has Vercel Vite/SPA configuration and requires its actual VITE_API_URL at build time.

Vercel is the current hosting target. Earlier Render configuration is an unused alternative; do not provision both. Merge/deployment is authorized only within Free/$0. Install/connect the Vercel plugin, verify the actual Hobby team, feature availability and usage/billing before creating/deploying projects. No Vercel deployment or live URL is verified yet. See backend docs/vercel-deployment.md for runtime environment and acceptance steps.

These final hosting changes retain the existing Java Docker build and frontend application code. Configuration structure and persisted file contents are checked; no new Docker/build/browser run is available while the execution environment is offline. Previous 42 Java/49 hosted API checks remain separate evidence.

## UI and BPH/Staff adjustment — 9 October 2026

User requested feature adjustment, removal of unattractive stickers, two product roles (BPH and Staff as executing members), and explicitly deferred database population.

This iteration derives a trusted API departmentRole from existing profile.role: staff/admin -> bph (BPH), member -> staff (Staff). Legacy stored roles and RLS are unchanged; no migration, account promotion, seed, or business-data write is part of this release. Product labels and controls use the same mapping, including an old-backend fallback. Self-promotion remains forbidden. Team remains read-only.

Dashboard/inventory emoji stickers are removed, inventory editing preserves existing hidden emoji values, dashboard links reflect the role, and Team has name/role filters. Staff task assignment remains self-only.

The user reported successful Vercel deployment and login at https://depor-cs-hub-web.vercel.app and backend https://depor-cs-hub-api.vercel.app. The current Vercel plugin cannot inspect those projects/deployments; do not confuse user-reported success with automated hosted acceptance. Exact backend allowed origin is https://depor-cs-hub-web.vercel.app. Free/$0 remains required.

CI validation for this iteration is recorded in the associated PRs. Local execution is offline. Backend docs/role-adjustment.md explains the compatibility contract, permissions, deployment order and test limits.

## Loading optimization — 9 October 2026

The user reported very slow loading. Frontend route/chart splitting, removal of blocking external font CSS, a visible session-checking login form, and a bounded 30-second in-memory GET cache address startup and repeated navigation. Successful mutations and account/role changes invalidate cached reads; old-account pending reads are rejected. Backend only caches CORS preflight permissions for one hour; trusted Auth/profile checks and RLS are preserved. No hosted business-data changes or paid features.

See frontend docs/loading-performance.md for behavior and validation limits. This iteration uses public-repository GitHub CI because local execution is offline. Actual CI measurements and deployment status are recorded in the PRs.


## Program workspace checkpoint — 19 October 2026

User requested separate program pages with timeline, todos, account assignments, and BPH/PJ-only access plus better BPH task tracking. Implemented /programs/:id with Overview/Tasks/Timeline/Agenda, real append-only notes and editable milestones. Program and related-row RLS restricts active BPH/PJ; unlinked tasks are private to creator/assignee and BPH. No changes to stored accounts/assignments/business records. A program with no PJ remains BPH-only until accounts are selected; do not infer assignments from legacy PIC text.

Applied development migration 20261019074232_program_hub_access on authorized ref dajpnhkutkhgxwkjzvpg. Supabase organization Free verified. Before/after counts unchanged; hosted read-only simulated JWT confirms BPH department scope and Staff assigned scope, denied unrelated timeline and no Staff workload. Security advisor still reports the pre-existing Auth leaked-password protection warning; performance reports historical FK/initplan recommendations and unused indexes (new empty tables). No paid settings enabled.

Local Java21 verify:70 tests; disposable Postgres-compatible PGlite:5 rollback suites. Frontend lint/unit/build and browser API-mock flows validated; no real hosted Auth/browser login credentials available and no resets performed. Full browser suite plus targeted bounded-board check recorded in release PR. Existing GitHub/Vercel pipeline remains the release path; connector cannot inspect actual Vercel projects, so verify commit statuses and live HTTP assets. See current PRs for final merge/deployment IDs.


## Status and self-service account checkpoint — 19 October 2026

User requested Planning / On Going / Finished program statuses and own photo/password settings. UI uses three main choices mapped to existing Planning/Ongoing/Completed; Active reads as On Going and the Ongoing server filter includes Active. Legacy unknown/cancelled states are preserved, no business backfill. AvatarController owns image validation, private versioned Storage paths, signed 900-second URLs, CAS profile pointer and post-commit cleanup. Auth password endpoint re-verifies current password using server-owned email and adopts a fresh own-user session; no role/userId request input or service role.

Development migration 20261019100116_profile_avatars applied after verifying project/Free organization. Bucket depor-avatars is private/JPEG/512 KiB; four authenticated ownership policies verified. Existing users' photos/passwords were not changed. New Storage tests use local bootstrap stand-in, never hosted metadata fixtures. Supabase advisor only retains the existing leaked-password warning and historical performance recommendations; no paid toggle enabled.

Validation: frontend lint/build, 28 unit and 36 browser mock cases; Java21 verify 84; six disposable SQL suites. Actual signed-URL byte upload/password change through a real hosted account was not performed. No password reset or credential extraction to obtain tests. Source contract/docs updated; release commit/status evidence belongs to associated PRs.


## Team photo visibility — 20 October 2026

User requested team-visible profile photos in Tim Departemen, program PJ, and task assignment. AccountIdentity renders a real account photo with name/initials. AvatarDirectory batches 100 IDs, deduplicates repeated identities, refreshes 900-second signed URLs every 12 minutes/on tab visibility, and never blocks business content on Storage errors. Directory read cache follows user/role/revision and retrieves all pages, so external photo/name edits appear on manual refresh. Programs show account-backed assignees rather than legacy PIC text.

GET /profiles includes avatar_path; GET /profiles/avatars?ids=... validates canonical UUIDs, selects current active profile pointers via caller JWT/RLS, batch-signs through Storage, and omits missing/untrusted entries. No client-selected path, role bypass, public bucket, cross-user write endpoint, service-role secret or dependency added. Current owner-only photo/password flows remain intact.

Applied development migration 20261020025834_team_avatar_read on verified dajpnhkutkhgxwkjzvpg (organization plan Free). It adds only a SELECT policy for active reader/current active-member photo. Existing private bucket and three write policies unchanged; no accounts, passwords, photos, assignments or business rows edited. Before/after selected-photo count remained one. Hosted read-only simulated-JWT queries show Staff and BPH see that current team photo; unregistered identity sees zero. Advisor findings unchanged: existing leaked-password warning; 4 unindexed FKs, 6 initplan, 6 unused-index recommendations. No paid setting enabled.

Validation: Java21 mvn verify 92 passing tests; 7 disposable PGlite SQL/RLS suites passed (Storage is a stand-in schema, not byte-level hosted service). Frontend lint/unit/build pass; 28 unit tests; browser fixtures cover shared photos, both viewport sizes, Staff, errors, replacement/removal and URL refresh. Final browser/CI/deployment evidence belongs to release PRs. No real hosted account password/photo mutation or browser login was performed. Supabase MCP used for migration history; earlier telemetry restriction on CLI remains respected.


## Faster-load checkpoint — 20 October 2026

User requested optimizing until faster-load marks pass. Baselines use f831074 frontend and 4f5c7ee backend. Added compact invoker RPCs workspace_directory/task_board/dashboard_overview, applied as development migration 20261020032751_performance_reads after verifying project/Free org. New endpoints GET /directory, /tasks/board, /dashboard/overview and /tasks/:id. Board has independently paginated columns plus global summary in one query, excludes descriptions; edit force-loads full task so saving does not erase omitted text. Existing dashboard_stats financial contract stays intact. JWT/Auth/profile/RLS remain fresh per API call, no service-role or principal cache. No user/account/photo/password/business rows changed.

Frontend starts session and static route preloads before React mount; lazy Layout and program Tasks tab reduce startup dependencies. Compact shared directory stays in existing per-account read cache; writes/refresh invalidate. Java gzip is verified over a real embedded local HTTP server and preserves Cache-Control no-store. All performance tooling is dev-only, fixtures are fictitious and never use hosted credentials. No paid resources enabled.

99 Java tests and 8 rollback SQL suites pass; hosted read-only simulated-JWT comparisons show board summary and dashboard overview match old endpoints for BPH/Staff, and Staff workload stays empty. Hosted BPH compact dashboard JSON text was 5753 bytes versus 12394 (size, not wire/time measurement). Performance budget checks use production build and controlled API/network/CPU; they are not live authenticated latency measurements. Final before/after evidence, browser cases, CI/merge/deploy IDs are recorded in the performance documentation and release PRs.


## Small program tasks checkpoint — 20 October 2026

User clarified tasks are small execution work items inside Proker/UKOR. No extra hierarchy/table was invented. Global board aggregates program tasks; embedded create locks parent, board filter preselects parent for every create control, and cards/list link to the parent with its kind. Create/full-edit DTO requires program_id. A new invoker trigger rejects new NULL parents and detachment; pre-existing NULL rows remain editable by partial status until manually classified. Directory adds kind; task_summary/task_board add filtered unlinked count and independent program_scope linked/unlinked. RLS/BPH/PJ/self-assignment and current-user JWT reads stay intact.

Hosted read-only preflight found 12 tasks, 2 legacy NULL parents, on the verified Free development project. No parent was guessed or business/account row rewritten. Disposable suites explicitly simulate legacy fixtures via privileged local setup; never run their bootstrap/fixtures on hosted data. Tests cover required parent, both kinds, relink without losing valid assignment/description, partial legacy status, detachment denial, BPH/PJ/outsider/removal, board/list filter parity and desktop/mobile browser flows. Latest CI/deployment evidence belongs to this release PR.

Applied development migration 20261020111952_task_program_required through Supabase MCP after local validation and verifying project/Free org. Read-only simulated-JWT checks: BPH board/list summary parity true, legacy filter count 2; sampled assigned Staff parity true, no workload groups. These are SQL/RLS checks, not a real hosted Auth browser login. Task count 12/unlinked 2 and whole-table fingerprint remained unchanged across migration. Security advisor remains the single pre-existing leaked-password warning; no paid setting enabled. Java21 verify: 101 tests; disposable migration/RLS suite: 9 passing suites. Browser/CI/performance release evidence is recorded on PRs.
