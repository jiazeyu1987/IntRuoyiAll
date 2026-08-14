# Execution Log: OAuth 管理员权限检查 HTML 产物

## User Intent

用户要求将前面生产版 OAuth 管理员权限检查页面保存成可运行的 HTML 文件。

## BDD

BDD: production OAuth admin check page -> Given the production helper server exposes `/auth/login`, `/api/me`, and `/auth/logout`, When the user opens the HTML page and clicks authorize login, Then the browser starts the server-side OAuth authorization-code flow and later displays whether the current user has configured administrator roles.

## Execution

- Created task documentation before writing the HTML artifact.
- Wrote `output/oauth-admin-check-production.html`.
- Verification: UTF-8 marker check found `/auth/login`, `/api/me`, `/auth/logout`, `hasAdminPermission`, and `ADMIN_ROLE_CODES`.
- Verification: `git diff --check` passed for the generated HTML and task documents.
- Screenshot follow-up: user opened the HTML through `file://`, which cannot reach same-origin `/api/me`.
- Updated HTML to detect `file://` mode and show the helper service startup requirement instead of surfacing only `Failed to fetch`.
- Added zero-dependency Node helper service at `output/oauth-admin-check-helper-server.mjs`.
- Added environment example at `output/oauth-admin-check-helper.env.example`.
- Verification: `node --check output\oauth-admin-check-helper-server.mjs` -> PASS.
- Verification: HTML marker scan confirmed `/auth/login`, `/api/me`, `/auth/logout`, `file://`, `oauth-admin-check-helper-server.mjs`, and `hasAdminPermission`; it also confirmed the HTML does not contain OAuth client-secret markers.
- BDD: real OAuth admin permission E2E -> Given the helper service is started with a valid local OAuth authorization-code client and the default local admin identity is available, When Playwright opens the helper page, clicks authorize login, signs in through the real IntRuoyi frontend SSO path, and returns to the helper page, Then the page displays the current user and `管理员权限：有` without exposing OAuth tokens or client secrets in the browser-visible raw data.
- Verification setup: frontend `http://127.0.0.1:8081` returned HTTP 200 and backend `http://127.0.0.1:48081/actuator/health` returned `UP`; helper port `18080` was not listening before the task-owned helper process starts.
- Added task-owned Playwright script `doc/tasks/20260804-oauth-admin-check-html/oauth-admin-check-real-e2e.cjs`; it reads credentials/secrets only in memory, writes sanitized result JSON under `output/playwright/20260804-oauth-admin-check-html/`, and stops only its helper child process.
- RED: `node doc\tasks\20260804-oauth-admin-check-html\oauth-admin-check-real-e2e.cjs` with local authorization-code client reached `http://127.0.0.1:18080/oauth/callback?code=...`, then failed token exchange with “请求的租户标识未传递”; expected reason: helper did not send IntRuoyi `tenant-id` to `/system/oauth2/token`.
- Updated helper service to require `INT_TENANT_ID` and send `tenant-id` on OAuth token exchange, refresh-token exchange, and `/system/auth/get-permission-info`.
- Verification: local OAuth client check found `admin-check-prod=missing`; it found `yudao-sso-demo-by-code=present` with `authorization_code`, `refresh_token`, `user.read`, and a redirect that allows the helper callback.
- GREEN: `node --check output\oauth-admin-check-helper-server.mjs` -> PASS.
- GREEN: `node --check doc\tasks\20260804-oauth-admin-check-html\oauth-admin-check-real-e2e.cjs` -> PASS.
- GREEN: `node doc\tasks\20260804-oauth-admin-check-html\oauth-admin-check-real-e2e.cjs` -> PASS; result at `output/playwright/20260804-oauth-admin-check-html/result.json`, screenshots at `initial-not-logged-in.png` and `admin-permission-pass.png`.
- Verification: result JSON reports identity `芋道源码/admin`, user `admin`, `hasAdminPermission=true`, matched role `super_admin`, `permissionsCount=1282`, `consoleErrors=[]`, `pageErrors=[]`, and `requestFailures=[]`.
- Verification: sensitive marker scan over `result.json` and `oauth-admin-check-production.html` found no `access_token`, `refresh_token`, `client_secret`, `OAUTH_CLIENT_SECRET`, `Bearer`, or `Basic` markers.
- Verification: after E2E, port `18080` had no listening task-owned helper process.
