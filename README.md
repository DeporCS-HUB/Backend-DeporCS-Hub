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

No application `JWT_SECRET` is needed. The supplied public JWKS is not a JWT secret. Access tokens are verified by calling Supabase `/auth/v1/user`, so verification follows Supabase's current signing keys and token rules.

```sh
mvn verify
mvn spring-boot:run
# Production package:
java -jar target/depor-hub-1.0.0.jar
```

The Dockerfile builds/tests with Java 21 and runs the JAR as an unprivileged user. `render.yaml` configures a Docker/Java service with `/api/health`. It is deployment configuration, not evidence that a cloud deployment has been performed. Health reports JVM availability; it does not certify Supabase connectivity. A missing URL/key fails startup rather than falling back to dummy data.

## Database setup — manual, development first

`supabase/migrations/202610080001_core.sql` creates `profiles`, `programs`, `tasks`, `finances`, `inventory`, constraints, indexes, RLS, an Auth profile trigger, and a database dashboard RPC. Supabase owns `auth.users`; there is no public password column. Existing Auth accounts are backfilled with the **member** role.

1. Create an isolated development Supabase project or branch.
2. Inspect existing tables, foreign keys, grants, functions, policies, and Auth accounts. This migration intentionally fails when names conflict; it must not silently overwrite an existing schema. Reconcile any legacy public `users`/password schema manually and discontinue its grants after migrating real accounts through Supabase Auth.
3. Apply the migration to development using Supabase SQL Editor, or a trusted SQL connection:

```sh
psql "$DEV_DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/migrations/202610080001_core.sql
```

4. Create development accounts through Supabase Auth's dashboard or trusted admin workflow. Use a trusted SQL operator to promote **only the intended development account**, for example:

```sql
update public.profiles set role = 'staff' where id = '<development-auth-user-uuid>';
```

5. Optionally run `supabase/seed.sql` in development. It requires an active staff/admin profile, inserts clearly marked demo records, and is intentionally not rerunnable without duplicating data. It never seeds passwords or invents real department data.
6. Configure backend and frontend for that development project, then validate login → dashboard → CRUD → refresh → logout with member and staff accounts.

There is no automatic production migration, reset, seed, role promotion, or deployment. Production changes require a separate reviewed migration plan and authorization. Migration runs in a transaction; there is no destructive down migration.

## Authentication and authorization

- `POST /api/auth/login`: email/password exchanged directly with Supabase Auth; no plaintext password lookup or password logging.
- `POST /api/auth/refresh`: rotates the refresh token in an HttpOnly cookie; access tokens are returned to the browser for memory-only bearer use.
- `GET /api/auth/session`: verifies the token and returns the current trusted profile.
- `POST /api/auth/logout`: revokes the current refresh session through Supabase and clears the cookie.
- Auth mutations require an exact allowed `Origin`, including command-line requests. No cookie authenticates data endpoints; these require a bearer token. This separates refresh-cookie CSRF protection from bearer API authorization.
- User metadata, request-supplied roles, and client UI controls cannot grant privileges. Profiles' role/active flags have no authenticated update grant. A trusted database operator manages roles; profile role administration is not exposed through the UI.
- All data requests use the user's JWT and the anon API key, **not** a privileged database bypass. RLS rechecks active membership and ownership in the database.

| Operation | member | staff/admin |
| --- | --- | --- |
| Read dashboard, programs, tasks, finances, inventory, profiles | Yes, active profile required | Yes |
| Program, finance, inventory create/update/delete | No | Yes |
| Create tasks | Self assignment only | Any assignee |
| Update/delete tasks | Creator or assignee; cannot reassign to others | All |
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
| profiles | GET `/profiles` | — | — | — |

Request DTOs and limits are defined in `Inputs.java`. Unknown fields are rejected. UUIDs, date ranges, status enums, quantities, amounts, and string lengths are validated; creator IDs are injected server-side. Deleting a program with tasks/transactions returns a conflict. Failed or empty database writes never produce a success result.

`GET /dashboard` invokes `dashboard_stats()` as the authenticated caller. PostgreSQL computes budget totals, approved expenditure/revenue, budget utilization, task/program counts, asset quantities by status, current-year monthly finance totals, and current/upcoming programs. All currencies are IDR; unapproved/rejected transactions do not count toward realization. Remaining budget = sum of program budgets − approved expenses. There are no fabricated activity percentages, notifications, attendance, or event counts.

## Verification

```sh
mvn verify
```

- 24 Spring MockMvc/security tests use a mocked Supabase adapter.
- 6 adapter tests use a local HTTP server to verify Auth calls, trusted profile roles, inactive/missing profiles, and sanitized upstream errors.
- `supabase/tests/rls.sql` tests real SQL grants/RLS, ownership, constraints, and >1,000-row aggregation in a rollback transaction. Run only in a disposable development database.
- `supabase/tests/bootstrap.sql` is a **local PostgreSQL harness** that stubs Auth schema/roles. Never run it on a Supabase project. CI uses PostgreSQL 17 and this harness; it does not validate hosted Supabase Auth.

For a fresh local PostgreSQL test database:

```sh
psql "$LOCAL_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/bootstrap.sql
psql "$LOCAL_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/migrations/202610080001_core.sql
psql "$LOCAL_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f supabase/tests/rls.sql
```

Local implementation validation used PGlite's PostgreSQL runtime with the same harness, migration, and RLS tests. This verifies SQL semantics, not the hosted Supabase service. Live schema inspection, seed execution, login, and CRUD require installed environment settings and development accounts. No hosted Supabase mutation was performed in this implementation session.

Events/permit workflows, account invitations, attendance/organization structure, stored settings, attachment uploads, and concurrent-edit conflict detection are outside this implementation. Team has a read-only live profile list; Events and Settings explicitly report their current limits.
