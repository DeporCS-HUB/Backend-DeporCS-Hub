# Depor CS HUB — development checkpoint

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
