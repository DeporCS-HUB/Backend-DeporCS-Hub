# Supabase development validation — 8 October 2026

## Authorized target

**DeporCS HUB**, ref `dajpnhkutkhgxwkjzvpg`, organization `jvdgogmbphotxdzrvbvc`, region `ap-northeast-2`. The user explicitly designated this ref as development, authorized `core` and `events_profiles`, and revoked its earlier production restriction. Organization plan is **Free**, verified via the Supabase connector. No project/branch provisioning, paid compute, upgrade, or paid add-on was used.

## Applied schema

| Hosted version | Migration | Result |
| --- | --- | --- |
| 20261008154150 | core | Applied successfully |
| 20261008154204 | events_profiles | Applied successfully |

Repository filenames match these returned migration versions. Six application tables (profiles, programs, tasks, finances, inventory, events) have RLS. Privileged profile bootstrap and role lookup functions live in `private`; authenticated clients cannot execute bootstrap or promote roles/activation. Public dashboard RPC preserves caller RLS through SECURITY INVOKER. Auth profile bootstrap normalizes whitespace-only names and ignores role/active metadata for authorization.

## Proven hosted behavior

Both transactional SQL suites passed against the actual hosted PostgreSQL database, without using the local Auth bootstrap harness. Every Auth/profile/application fixture was rolled back. The suites require an empty development database and simulate JWT claims while using the authenticated/anon SQL roles; they do **not** establish real Supabase Auth sessions.

Covered: staff CRUD, member restrictions and task ownership, protection of role/active/identity/creator fields, inactive users, anonymous denial, profile name updates, event CRUD/permit status, invalid dates/quantity, foreign-key deletion restrictions, metadata role/activation spoofing, whitespace-name fallback, and dashboard aggregation over 1,002 pending tasks. Post-test counts confirmed no retained Auth or application fixtures.

Live HTTP checks showed hosted Auth settings reachable, email confirmation enabled, and anonymous PostgREST access to all six tables and dashboard RPC denied. The readiness script creates no user/data and does not disable email confirmation.

The integration test found a real adapter mismatch: hosted Auth returns `403` plus `error_code=bad_jwt` for a malformed JWT. Java previously reported FORBIDDEN, which prevents the frontend's 401 refresh handling. The adapter now maps this specific Auth error to 401 UNAUTHENTICATED; genuine Auth permissions and Data API 403 responses stay forbidden. Error details remain sanitized. See the [official Auth error-code guide](https://supabase.com/docs/guides/auth/debugging/error-codes).

Final results: `mvn verify` passed **41 tests** (31 mocked API/security + 10 simulated Auth/REST HTTP adapter tests), and `verify-hosted-readiness.py` passed **15 live HTTP/API checks**. Those include Java backend startup/health, authentication required, real hosted invalid-login/token/refresh rejection, missing-refresh cookie clearing, and untrusted-Origin denial, alongside hosted settings and anonymous table/RPC denial. Successful Auth login/refresh/CRUD remains outside these checks.

## Advisors

Security advisor returned no lints after both migrations. This is a point-in-time check, not a certification of every security property.

Performance advisor findings remain unmodified by these two migrations:

- Four `created_by` foreign keys on programs, finances, inventory, and events lack a covering index. [Remediation: unindexed foreign keys](https://supabase.com/docs/guides/database/database-linter?lint=0001_unindexed_foreign_keys).
- Nine policies reevaluate `auth.uid()` per row. Review a follow-up migration using scalar SELECT expressions while preserving authorization. [Remediation: Auth RLS initialization plan](https://supabase.com/docs/guides/database/database-linter?lint=0003_auth_rls_initplan).
- Nine indexes were reported unused on the new empty database. Retain the indexes required by ownership/FK/query access; no workload has established that they are redundant. [Unused-index advisory](https://supabase.com/docs/guides/database/database-linter?lint=0005_unused_index).

## Validation limits and next actions

No permanent Auth test accounts exist. Public email signup requires confirmation, and no trusted admin key or verified staff/member credentials were configured in the runtime. Successful Auth login, authenticated HTTP CRUD/persistence, positive refresh rotation, logout/revocation, and end-to-end React reload flows remain unverified. No seed, production deployment, or merge occurred.

Create/verify two test users through the official development Auth dashboard/admin workflow. Password entry belongs to the user. Verify their UUIDs before trusted SQL activation: staff with `role=staff, active=true`; member with `role=member, active=true`. Never take role/activation from user_metadata. Keep test credentials in secure process environment, never Git/chat/logs.

Then run the Java API and React UI and verify login → dashboard → role-aware CRUD → reload → refresh → logout. Local defaults: allowed origin `http://localhost:3000`, HTTP cookie secure false; use HTTPS/secure cookies in cloud.

Read-only readiness is reproducible after building the JAR:

```sh
mvn verify
python scripts/verify-hosted-readiness.py
```

Set SUPABASE_URL and SUPABASE_ANON_KEY securely first; JAVA_HOME is optional. The script permits only the authorized development ref and launches Java on loopback. With an environment HTTP proxy, it configures Java's HTTPS proxy; it does not disable TLS checks.

Commits use `[skip ci]` to avoid starting new GitHub Actions runs while the user's zero-cost requirement remains in force. Current validation comes from local Java tests, PGlite, actual hosted SQL RLS, and live read-only HTTP/API checks; old GitHub CI is historical evidence. Draft PRs remain unmerged.
