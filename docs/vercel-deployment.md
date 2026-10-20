# Vercel deployment — Free/$0 only

Current target: Vercel, selected by the user on 9 October 2026. Both DeporCS-HUB repositories are verified public and PR #1 in each is merged. Existing Render files are an unused alternative; do not provision both providers.

## Access and cost

Install/connect the Vercel plugin to the intended account. Before creating projects, inspect existing matching projects and reuse them to avoid duplicates. Verify the selected team is on Hobby, Container Images is available, included compute/registry/build/network usage remains, and no paid feature or upgrade is selected. Do not enable a Pro trial, paid add-on, paid build machine, paid cold-start prevention, custom-domain purchase or Marketplace datastore. Database/Auth remains the existing Supabase Free development ref `dajpnhkutkhgxwkjzvpg`.

The [Hobby guide](https://vercel.com/docs/plans/hobby) documents free-plan usage limits and its personal/non-commercial scope. Check actual account eligibility and usage rather than assuming the Hobby plan or a $0 bill from the repository alone. The [Container Images guide](https://vercel.com/docs/functions/container-images) states the feature is Beta on all plans and uses Functions pricing/limits. Review actual account billing and [pricing](https://vercel.com/pricing) for compute and registry allowances before deployment. No hosting costs have been verified in an authenticated Vercel account yet.

## Backend

Import `DeporCS-HUB/Backend-DeporCS-Hub` from main at the repository root. Vercel detects the root `Dockerfile.vercel` and builds the Java container. It is intentionally identical to the existing Dockerfile: Java 21, Maven verify, then a JRE image running as an unprivileged user. Do not replace the backend with Node or deploy the local SQL harness.

Set these runtime environment variables in the Vercel project before deploying:

| Variable | Value |
| --- | --- |
| PORT | 8080 |
| SUPABASE_URL | Authorized development URL, https://dajpnhkutkhgxwkjzvpg.supabase.co |
| SUPABASE_ANON_KEY | The development project's public API key, configured only in the hosting environment |
| APP_ALLOWED_ORIGINS | Exact actual frontend HTTPS origin; no wildcard |
| APP_COOKIE_SECURE | true |
| APP_COOKIE_SAME_SITE | None for separate frontend/backend domains |

PORT must be set in the **Vercel project settings**, not just Docker EXPOSE: Vercel's container router defaults to port 80, while Spring defaults to 8080. Setting project PORT=8080 makes the router and application agree. No service-role key or staff/member passwords belong in the production runtime.

Reuse the project's real assigned HTTPS URLs; do not guess a subdomain from a project name. The backend's /api/health should return 200. Its health endpoint certifies JVM availability only. If the frontend origin is not assigned yet, create/link that project to obtain its stable production origin before completing backend configuration. Do not allow arbitrary preview origins; add only specific origins needed for acceptance.

## Frontend

Import `DeporCS-HUB/Frontend-DeporCS-Hub` as Vite. The committed build command explicitly uses VITE_API_URL=/api, overriding the old absolute public value if present. The API rewrite /api/:path* forwards to the verified existing https://depor-cs-hub-api.vercel.app/api/:path* before the SPA fallback. No frontend secret/key or additional Function is needed. If the real backend target changes, update this explicit destination in the repository; do not guess it.

API responses use private/no-store and CDN no-store; only hashed /assets resources are immutable. Keep APP_ALLOWED_ORIGINS set to the exact frontend HTTPS origin, APP_COOKIE_SECURE=true, and host-only HttpOnly cookies scoped to /api/auth. The existing SameSite=None remains compatible with the first-party proxy; Lax is suitable when all clients use same-site routing. Do not change cookies or weaken Origin checks based on an untrusted forwarded host. A refresh cookie set previously on the backend host cannot be copied to the frontend host: users may need one new login after this release.

## Deploy and verify

Deploy the exact merged main commits only after account/Free verification, then inspect both deployments until they reach a terminal successful state. Record the actual frontend/backend URLs and deployed commit SHAs.

Verify HTTPS, frontend deep-link reload, backend health, exact-origin CORS, login for both existing confirmed accounts, role-aware CRUD, persistence after browser reload, own-profile name, refresh rotation and logout. Remove only unique acceptance fixtures, restore original profile names and end only acceptance sessions. Credentials, cookies and tokens must not appear in Git, screenshots or logs.

Previously validated: 42 Java and 49 real hosted Auth/Java/PostgREST checks, plus historical 10 frontend unit/7 mocked-browser flows. The local runtime is offline, so no new Docker image build or live Vercel/browser test has run for this hosting configuration. The Docker build retains mvn verify; a successful remote build and hosted acceptance still need to be observed. A merged configuration or public repository is not evidence of deployment.

If deployment requires a paid capability, stop that action under the user's $0 constraint and report the actual requirement. Keep Supabase migration history intact; no new migration/reset/seed is part of hosting deployment.
