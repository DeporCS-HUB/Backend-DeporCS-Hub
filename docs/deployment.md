# Free deployment runbook

Authorization: 9 October 2026. Merge and hosting deployment are authorized; only Free/$0 resources and Supabase development ref `dajpnhkutkhgxwkjzvpg` are in scope. No new database migration, reset, seed or Auth provisioning is needed.

## Cost and access gate

Install/connect the Render plugin to the intended account and grant Render's GitHub integration access to the two private DeporCS-HUB repositories. Inspect existing services before creating any to avoid duplicates. Verify Hobby/no workspace subscription fee, available instance hours/build minutes/bandwidth and no payment method capable of charging overages. Do not add a payment method or select paid resources. If a payment method exists and overage billing cannot be blocked and verified, do not provision under the strict $0 instruction; report the blocker.

Render's [Free guide](https://render.com/docs/free) and [FAQ](https://render.com/docs/faq) document usage limits and possible bandwidth/build charges even for Free services when a payment method is present. Exhaustion without a payment method suspends services/builds. Free web services sleep after inactivity; initial requests can be slow. No paid uptime upgrade is authorized.

## Configuration

| Component | Repository | Render type | Compute |
| --- | --- | --- | --- |
| Java API | DeporCS-HUB/Backend-DeporCS-Hub | Docker web service | Explicit `plan: free` |
| React UI | DeporCS-HUB/Frontend-DeporCS-Hub | Static site | No compute plan |
| Database/Auth | Existing Supabase development project | Existing Free project | Unchanged |

Both Blueprints use branch main and autoDeployTrigger: off. This controls deploy triggers; it does not disable a Blueprint's Auto Sync setting. Do not connect an automatically syncing Blueprint until billing checks pass. Merge messages include [skip ci] and [skip render] to avoid unintended build/deploy runs. Use a manual deployment once the account and environment are verified. The [Blueprint reference](https://render.com/docs/blueprint-spec) documents that omitted web-service plan defaults to paid compute; do not remove plan: free.

1. Reuse a matching existing Free API service or create depor-hub-api from backend main with the Dockerfile and /api/health check. Confirm the platform-reported plan is Free before deployment. Do not create Render Postgres, disks, cache or background services.
2. Reuse/create depor-hub-web as a static site from frontend main. Build: npm ci && npm run build; output: dist; SPA rewrite: /* to /index.html. Use the actual platform-assigned URLs, not names guessed from service labels.
3. Set backend SUPABASE_URL to the authorized development URL and SUPABASE_ANON_KEY to its public API key in hosting environment. Normal requests need no service-role key. Set APP_ALLOWED_ORIGINS to the exact actual frontend HTTPS origin, APP_COOKIE_SECURE=true and APP_COOKIE_SAME_SITE=None for separate domains. Never store credentials in Git or build logs.
4. Set frontend VITE_API_URL to the actual API HTTPS URL plus /api and rebuild. No Supabase key belongs in the frontend. For a same-origin proxy, use relative /api and configure a verified proxy before the SPA fallback; verify it preserves request methods, Authorization, cookies and query strings. Do not guess an API URL or ship a placeholder.
5. Trigger manual deployments of the merged commits and inspect terminal deployment status/build logs. Success requires a real frontend URL and an API URL with a healthy response. /api/health checks JVM availability only; it is not database readiness.

## Hosted acceptance

Check HTTPS, SPA deep-link reload, exact-origin CORS and API JSON responses. With the existing confirmed staff/member test accounts, verify login, staff CRUD, member restrictions/own tasks, persistence after browser reload, own-profile name restore, refresh and logout. Use uniquely named temporary records, delete only those fixtures, restore profile names and close only test sessions. Keep passwords/tokens out of screenshots, Git and logs.

Separate-domain HttpOnly refresh cookies can be blocked by browser policy even with SameSite=None. Do not report browser acceptance until verified; if blocked, configure a same-origin proxy or same-site routing and repeat session checks. A static site's SPA rewrite alone is not an API proxy.

Previous evidence: 42 Java and 49 real Auth/Java/PostgREST tests passed; frontend's 10 unit/7 mocked-browser tests are separate. Local runtime is currently offline, so no new Docker/browser build has run for these configuration/documentation changes. Real deployment/browser acceptance remains pending connection and cost verification. See [development validation](live-development-validation.md).

On deployment failure, retain the last working version, inspect the actual error and fix/redeploy within Free. Do not upgrade automatically. Deployment rollback must not reset the Supabase database or undo its existing migrations.
