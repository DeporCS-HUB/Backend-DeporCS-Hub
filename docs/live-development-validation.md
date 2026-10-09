# Supabase development integration validation

Checkpoint: 2026-10-08 23:55 WIB.

## Target and cost

**DeporCS HUB**, ref `dajpnhkutkhgxwkjzvpg`, organization `jvdgogmbphotxdzrvbvc`, region `ap-northeast-2`. The user explicitly designated this ref as development and authorized the application migrations. Organization plan remains **Free**. No project/branch provisioning, upgrade, paid compute, or paid add-on was used.

## Accounts

The user created two Auth users through the dashboard. Their supplied UUID/email pairs were matched against Auth, email confirmation was verified, and a trusted SQL operator activated exactly those profiles: one staff and one member. Role/active changes were limited to the verified UUIDs; user metadata was not trusted for authorization. Both accounts remain confirmed and active after testing. Personal account identifiers and passwords are intentionally kept out of this report and Git.

## Schema

| Hosted version | Migration | Result |
| --- | --- | --- |
| 20261008154150 | core | Applied |
| 20261008154204 | events_profiles | Applied |

Repository filenames match hosted history. All six application tables have RLS. Internal privileged functions are in `private`; dashboard RPC is SECURITY INVOKER. Profiles start inactive/member; role/activation metadata cannot grant access, and whitespace metadata names receive a valid fallback.

Both SQL RLS suites passed locally and on hosted PostgreSQL with rolled-back fixtures and simulated SQL JWT claims. They require an empty disposable database and must not be rerun on this now-populated Auth environment. Never run the local `bootstrap.sql` Auth harness on hosted Supabase.

## Current results

- **42 Java tests pass**: 31 mocked API/security plus 11 simulated HTTP adapter tests.
- **49 real Auth/Java/PostgREST checks pass**, using the actual hosted development service and user JWTs. No service-role bypass was used.
- Earlier read-only readiness also passed 15 live HTTP/API checks, including anonymous table/RPC denial and invalid login/token/refresh rejection.

The 49-check suite proves:

- Staff/member password login through Java and trusted-profile lookup; HttpOnly refresh cookie headers.
- Staff create, edit and delete on programs, tasks, finances, inventory and events.
- Edited records persist in hosted PostgreSQL and can be read by the member in separate direct PostgREST requests.
- Dashboard totals reflect real writes and return to their baseline after fixture removal.
- Member management restrictions; ownership of tasks; member task create/edit/delete; backend and direct RLS prevention of reassignment.
- Direct column grants reject role escalation; own profile name updates persist; RLS rejects changing another profile's name.
- A linked program cannot be deleted and returns 409.
- Both accounts rotate refresh cookies, authenticate with refreshed access tokens, clear cookies at logout, and cannot reuse their logged-out refresh tokens.

Cleanup is independently verified by SQL: Auth users 2, profiles 2, programs/tasks/finances/inventory/events all 0, and remaining sessions for the two test accounts 0. Original profile names were restored. The permanent test accounts and their requested roles remain available.

## Fixes found by live integration

The earlier adapter mapped Auth `403/bad_jwt` to permission denial. It now reports 401 so the frontend's refresh path can handle invalid sessions; genuine 403 permissions remain forbidden. See the [Auth error-code guide](https://supabase.com/docs/guides/auth/debugging/error-codes).

Live edit requests also exposed `ProtocolException`: the HttpURLConnection request factory rejected PATCH, which PostgREST requires for updates. The adapter now uses Java HttpClient through Spring JdkClientHttpRequestFactory. A regression test checks actual PATCH method, JSON payload and bearer header. All five hosted resource edits and own-profile edits now succeed. See [Spring request factory documentation](https://docs.spring.io/spring-framework/docs/6.1.6/javadoc-api/org/springframework/http/client/JdkClientHttpRequestFactory.html).

Connection/read timeouts are configurable and bounded to 1–120000 ms. Defaults remain 5000/10000 ms; the managed acceptance script uses 15000/30000 ms. Transport diagnostics log only exception class names at DEBUG, without request bodies, URLs, keys or tokens. TLS verification stays enabled.

## Advisors and remaining limits

The latest security advisor reports **one Auth warning**: leaked-password protection is disabled. This capability requires Pro or above; the authorized Free/$0 constraint is retained. This is not evidence that these specific passwords have leaked. [Password-strength/leaked-password remediation](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection).

Performance recommendations from the schema inspection remain:

- Four created_by foreign keys lack covering indexes. [FK-index remediation](https://supabase.com/docs/guides/database/database-linter?lint=0001_unindexed_foreign_keys).
- Nine RLS policies reevaluate auth.uid per row. [RLS initialization-plan remediation](https://supabase.com/docs/guides/database/database-linter?lint=0003_auth_rls_initplan).
- Unused-index INFO was observed on the fresh database; retain the indexes until workload evidence supports removal. [Unused-index advisory](https://supabase.com/docs/guides/database/database-linter?lint=0005_unused_index).

The suite exercises API/HTTP cookie handling, not a real React browser, browser reload, or browser third-party-cookie restrictions. Frontend's previous 10 unit and 7 mocked-API browser tests remain separate evidence. Invitations, attendance, role-administration UI, attachments, notifications, preferences, external permit delivery and edit-conflict resolution remain unimplemented.

Copied access JWTs may remain usable until expiry; the logout assertion concerns revoked refresh tokens. Profiles' role/active state is rechecked by application/database access. No production deployment or merge occurred.

## Reproduce

Set development URL/public key and existing test-user credentials only through secure process environment. Never commit credentials or put them in command-line arguments, reports or logs. The positive script expects DEPOR_TEST_STAFF_EMAIL/PASSWORD/UUID and DEPOR_TEST_MEMBER_EMAIL/PASSWORD/UUID.

```sh
mvn verify
python scripts/integration-hosted.py
```

The script only permits the authorized project, launches Java on loopback, creates/removes its own fixture rows, restores profile name, and logs out its sessions. It never provisions/deletes Auth users or changes roles/activation. `scripts/verify-hosted-readiness.py` provides the earlier read-only smoke checks.

Commits use `[skip ci]` to avoid new Actions runs under the user's zero-cost constraint. Current evidence comes from local Java tests and the real development service; draft PRs remain unmerged.
