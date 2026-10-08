# Depor CS HUB API

Java 21 + Spring Boot 3.5.7. Supabase provides PostgreSQL, Auth, and PostgREST. The Node.js implementation has been replaced. The companion UI is `DeporCS-HUB/Frontend-DeporCS-Hub`.

## Local startup

Install Java 21 and Maven 3.9+. Set environment values through your development/cloud environment; Spring does not automatically load `.env` files.

| Variable | Purpose |
| --- | --- |
| `SUPABASE_URL` | URL of a **development** Supabase project for local testing |
| `SUPABASE_ANON_KEY` | Project public API key, used by the Java adapter |
| `APP_ALLOWED_ORIGINS` | Comma-separated exact frontend origins; local default `http://localhost:3000` |
| `APP_COOKIE_SECURE` | `false` for local HTTP, `true` for HTTPS/cloud |
| `APP_COOKIE_SAME_SITE` | `Lax` by default; `None` requires HTTPS and secure cookies |
| `PORT` | HTTP port, default `8080` |
| `SUPABASE_SERVICE_ROLE_KEY` | Optional, backend-only; **not used by normal application requests** |
| `SUPABASE_CONNECT_TIMEOUT_MS` | Connection timeout; default 5000, permitted range 1–120000 ms |
| `SUPABASE_READ_TIMEOUT_MS` | Upstream read timeout; default 10000, permitted range 1–120000 ms |

No application `JWT_SECRET` is needed. The supplied public JWKS is not a JWT secret. Access tokens are verified by calling Supabase `/auth/v1/user`, so verification follows Supabase's current signing keys and token rules.

```sh
mvn verify
mvn spring-boot:run
# Production package:
java -jar target/depor-hub-1.0.0.jar
```

The Dockerfile builds/tests with Java 21 and runs the JAR as an unprivileged user. `render.yaml` configures a Docker/Java service with `/api/health`. It is deployment configuration, not evidence that a cloud deployment has been performed. Health reports JVM availability; it does not certify Supabase connectivity. A missing URL/key fails startup rather than falling back to dummy data.

## Database setup — manual, development first

`supabase/migrations/20261008154150_core.sql` creates `profiles`, `programs`, `tasks`, `finances`, `inventory`, constraints, indexes, RLS, an Auth profile trigger, and a database dashboard RPC. Supabase owns `auth.users`; there is no public password column. Existing Auth accounts are backfilled as **inactive members**. New Auth accounts are also inactive until approved by a trusted operator; public self-registration cannot grant department data access.

1. Use the user-authorized development project `dajpnhkutkhgxwkjzvpg` on the Free organization. The user explicitly reclassified this ref from protected main to development on 8 October 2026. Do not provision paid branching or upgrade the plan.
2. Inspect existing tables, foreign keys, grants, functions, policies, and Auth accounts. This migration intentionally fails when names conflict; it must not silently overwrite an existing schema. Reconcile any legacy public `users`/password schema manually and discontinue its grants after migrating real accounts through Supabase Auth.
3. Apply all migrations in filename order to development using Supabase SQL Editor, or a trusted SQL connection:

```sh
for migration in supabase/migrations/*.sql; do
  psql "$DEV_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$migration" || exit 1
done
```

4. Create development accounts through Supabase Auth's dashboard or trusted admin workflow. Use a trusted SQL operator to promote **only the intended development account**, for example:

```sql
update public.profiles set role = 'staff', active = true where id = '<development-auth-user-uuid>';
```

For approved member accounts, set `active = true` while keeping `role = 'member'`. User metadata cannot activate an account.

5. Optionally run `supabase/seed.sql` in development. It requires an active staff/admin profile, inserts clearly marked demo records, and is intentionally not rerunnable without duplicating data. It never seeds passwords or invents real department data.
6. Configure backend and frontend for that development project, then validate login → dashboard → CRUD → refresh → logout with member and staff accounts.

There is no automatic production migration, reset, seed, role promotion, or deployment. Production changes require a separate reviewed migration plan and authorization. Migration runs in a transaction; there is no destructive down migration.

## Authentication and authorization

- `POST /api/auth/login`: email/password exchanged directly with Supabase Auth; no plaintext password lookup or password logging.
- `POST /api/auth/refresh`: rotates the refresh token in an HttpOnly cookie; access tokens are returned to the browser for memory-only bearer use.
- `GET /api/auth/session`: verifies the token and returns the current trusted profile.
- `POST /api/auth/logout`: revokes the current refresh session through Supabase and clears the cookie.
- Auth mutations require an exact allowed `Origin`, including command-line requests. No cookie authenticates data endpoints; these require a bearer token. This separates refresh-cookie CSRF protection from bearer API authorization.
- User metadata, request-supplied roles, and client UI controls cannot grant privileges. Profiles' role/active flags have no authenticated update grant. `PUT /api/profiles/me` permits updating only the caller's display name. RLS and column grants prevent changes to other accounts, roles, activation, or identity. A trusted database operator manages roles; profile role administration is not exposed through the UI.
- All data requests use the user's JWT and the anon API key, **not** a privileged database bypass. RLS rechecks active membership and ownership in the database.

| Operation | member | staff/admin |
| --- | --- | --- |
| Read dashboard, programs, tasks, finances, inventory, profiles | Yes, active profile required | Yes |
| Program, finance, inventory, event create/update/delete | No | Yes |
| Create tasks | Self assignment only | Any assignee |
| Update/delete tasks | Creator or assignee; cannot reassign to others | All |
| Edit own display name | Yes | Yes |
| Change roles/active flags | No | Trusted SQL/admin provisioning only |

Supabase access JWTs are stateless: logout revokes refresh tokens, while an already copied access token can remain usable until its expiry. The UI removes its access token immediately, and inactive profiles are rejected on every request. Cross-site cookie restrictions may block refresh on unrelated domains; prefer a same-origin reverse proxy or same-site custom domains. Use HTTPS in cloud and exact CORS origins.

## API contract

All endpoints use `/api`. Success: `{ "data": ... }`; errors: `{ "error": { "code": "...", "message": "..." } }`. Create returns 201. Lists include `{ "meta": { "page": 0, "size": 50 } }`. Page is zero-based; size is 1–100, with stable ordering. Do not infer global totals from a single list page.

| Resource | List | Create | Full edit | Delete |
| --- | --- | --- | --- | --- |
| programs | GET `/programs` | POST `/programs` | PUT `/programs/{uuid}` | DELETE `/programs/{uuid}` |
| tasks | GET `/tasks` | POST `/tasks` | PUT `/tasks/{uuid}` | DELETE `/tasks/{uuid}` |
| finances | GET `/finances` | POST `/finances` | PUT `/finances/{uuid}` | DELETE `/finances/{uuid}` |
| inventory | GET `/inventory` | POST `/inventory` | PUT `/inventory/{uuid}` | DELETE `/inventory/{uuid}` |
| events | GET `/events` | POST `/events` | PUT `/events/{uuid}` | DELETE `/events/{uuid}` |
| profiles | GET `/profiles` | — | PUT `/profiles/me` (own name only) | — |

Request DTOs and limits are defined in `Inputs.java`. Unknown fields are rejected. UUIDs, date ranges, status enums, quantities, amounts, and string lengths are validated; creator IDs are injected server-side. Deleting a program with tasks/transactions returns a conflict. Failed or empty database writes never produce a success result.

`GET /dashboard` invokes `dashboard_stats()` as the authenticated caller. PostgreSQL computes budget totals, approved expenditure/revenue, budget utilization, task/program counts, asset quantities by status, current-year monthly finance totals, and current/upcoming programs. All currencies are IDR; unapproved/rejected transactions do not count toward realization. Remaining budget = sum of program budgets − approved expenses. There are no fabricated activity percentages, notifications, attendance, or event counts.

## Verification

```sh
mvn verify
```

- 31 Spring MockMvc/security tests use a mocked Supabase adapter.
- 11 adapter tests use a local HTTP server to verify Auth calls, trusted profile roles, inactive/missing profiles, and sanitized upstream errors.
- `supabase/tests/rls.sql` tests real SQL grants/RLS, ownership, constraints, and >1,000-row aggregation in a rollback transaction. Run only in a disposable development database.
- `supabase/tests/bootstrap.sql` is a **local PostgreSQL harness** that stubs Auth schema/roles. Never run it on a Supabase project. CI uses PostgreSQL 17 and this harness; it does not validate hosted Supabase Auth.

For a fresh local PostgreSQL test database:

```sh
psql "$LOCAL_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/bootstrap.sql
for migration in supabase/migrations/*.sql; do
  psql "$LOCAL_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$migration" || exit 1
done
for test in supabase/tests/rls*.sql; do
  psql "$LOCAL_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$test" || exit 1
done
```

Local implementation validation used PGlite's PostgreSQL runtime with the same harness, migration, and RLS tests. This verifies SQL semantics, not the hosted Supabase service. Live schema inspection, seed execution, login, and CRUD require installed environment settings and development accounts. The two reviewed application migrations were applied to the authorized development project on 8 October 2026. The hosted PostgreSQL RLS suites passed using temporary fixtures and simulated JWT claims, with every fixture rolled back. This is separate from successful hosted Auth login and authenticated HTTP CRUD, now also validated by 49 real API checks using the two user-provisioned staff/member accounts.

## Events and profile settings

`supabase/migrations/20261008154204_events_profiles.sql` adds event schedules, program relationships, venue and permit tracking, and an own-profile name update policy. It is additive and must be reviewed/applied after the core migration. The new nonblank profile-name constraint deliberately fails if existing names contain only spaces; inspect and reconcile those records before applying. Both migrations were applied to the authorized hosted development project and exercised by transactional RLS suites there, as well as local PGlite/previous CI. No production deployment or merge was performed.

Event date ranges and statuses are validated by Java and PostgreSQL. Active members can read events; staff/admin can create, edit, and delete them. Event lists sort by start date. Linked events prevent deletion of their program. Permit status is an internal record entered by staff after confirmation from the venue operator; there is no external application, notification, or approval delivery workflow.

Settings persists the caller's display name with a validated DTO. Name changes do not modify Supabase Auth credentials or grant any privileges. `supabase/tests/rls_events_profiles.sql` checks own/other/inactive profile edits, column privileges, event CRUD, dates, anonymous access, and foreign-key protection in a rollback transaction.

Account invitations, attendance/organization structure, role administration UI, external permits, language/theme/notification preferences, attachment uploads, and concurrent-edit conflict detection remain outside this implementation. Team remains a read-only profile directory.

## Hosted development checkpoint

Both migration filenames match the actual Supabase migration versions: `20261008154150_core.sql` and `20261008154204_events_profiles.sql`. Do not run a second copy under an older filename. Internal privileged helpers live in schema `private`; metadata cannot assign roles/activation, and whitespace-only display names receive a valid fallback.

See [development validation](docs/live-development-validation.md) for current evidence and remaining limits. Six application tables have RLS, the latest Auth advisor warns that leaked-password protection is disabled (a Pro feature), while Free remains in use, and public HTTP requests cannot read application data. Performance advisor recommendations remain documented; no extra paid resources or add-ons were enabled.

For a read-only test of hosted reachability and rejection paths, build the backend and set its environment securely, then run:

```sh
mvn verify
python scripts/verify-hosted-readiness.py
```

This script accepts only the authorized development URL, checks anonymous table/RPC denial, starts the backend on loopback, and checks invalid login/token/refresh and Origin handling. It creates no accounts or data and does not test successful login, session rotation, authenticated CRUD/persistence, or logout. Hosted email confirmation remains enabled. Create and verify staff/member Auth accounts through the trusted dashboard/admin flow before positive acceptance testing; activate only their verified UUIDs through a trusted operator.

### Positive hosted API acceptance

`scripts/integration-hosted.py` uses two existing, confirmed, active development accounts. Set `DEPOR_TEST_STAFF_EMAIL`, `DEPOR_TEST_STAFF_PASSWORD`, `DEPOR_TEST_STAFF_UUID`, and the matching `DEPOR_TEST_MEMBER_*` variables in secure process environment, alongside the backend URL/public key. Do not commit credentials or put them in command-line arguments or logs.

```sh
mvn verify
python scripts/integration-hosted.py
```

The script only permits the authorized development ref. It starts Java on loopback and configures a 15-second connect / 30-second upstream read timeout for the managed test runtime; application defaults remain 5 / 10 seconds. Diagnostics print only the transport exception class, without URLs, bodies, keys, or tokens.

It logs in through real Supabase Auth, creates/updates/deletes its own fixture records through Java, verifies persistence and RLS with user-token PostgREST, checks dashboard totals, member restrictions, ownership/profile grants, FK conflicts, refresh cookie rotation, and revoked refresh after logout. Cleanup restores the member's original display name and removes only tracked test records. Existing users/roles are not created, deleted, reset, or changed by this script. Browser React/reload behavior is outside the API acceptance suite.

The Supabase adapter uses Java HttpClient through Spring JdkClientHttpRequestFactory, which supports the PATCH requests used by PostgREST edits. A regression test checks the actual HTTP method, JSON body, and user-token header; the previous HttpURLConnection transport rejected PATCH with ProtocolException.

Current positive hosted acceptance passed 49 checks and mvn verify passed 42 Java tests. Both user-provisioned test accounts are confirmed/active; all fixture records and test sessions were removed. Real React browser acceptance remains separate. See docs/live-development-validation.md for the latest Auth/performance advisories and validation limits.
