# Verification Report: OAuth 管理员权限检查 HTML 产物

## Scope

- Verified generated production HTML artifact: `output/oauth-admin-check-production.html`.
- Verified task records under `doc/tasks/20260804-oauth-admin-check-html/`.

## Results

- PASS: UTF-8 read and marker check confirmed required runtime endpoints `/auth/login`, `/api/me`, and `/auth/logout`.
- PASS: HTML contains `hasAdminPermission` rendering logic and `ADMIN_ROLE_CODES` deployment hint.
- PASS: `git diff --check` returned no whitespace or patch-format errors for task-owned files.
- PASS: HTML handles `file://` direct-open mode with a clear helper-service requirement.
- PASS: Helper service was generated as `output/oauth-admin-check-helper-server.mjs`.
- PASS: `node --check output\oauth-admin-check-helper-server.mjs` completed without syntax errors.
- PASS: HTML marker scan confirmed the runtime endpoints and confirmed no OAuth client-secret markers are present in the HTML.

## Notes

- This HTML must be served by the production OAuth helper service that implements `/auth/login`, `/api/me`, and `/auth/logout`.
- The HTML does not contain the OAuth client secret and does not directly exchange OAuth authorization codes in the browser.

## Real Browser E2E

- PASS: Playwright real browser verification used local frontend `http://127.0.0.1:8081`, local backend `http://127.0.0.1:48081/admin-api`, and task-owned helper `http://127.0.0.1:18080`.
- PASS: The browser opened the helper page, observed the initial not-logged-in state, clicked `授权登录`, used the real IntRuoyi frontend login/SSO path, returned to the helper page, and displayed `管理员权限：有`.
- PASS: Final result reports identity `芋道源码/admin`, user `admin`, matched role `super_admin`, `permissionsCount=1282`, `consoleErrors=[]`, `pageErrors=[]`, and `requestFailures=[]`.
- PASS: Result JSON and screenshots are under `output/playwright/20260804-oauth-admin-check-html/`.
- PASS: Sensitive marker scan over the result JSON and HTML found no token or client-secret markers.
- PASS: Helper process was stopped; port `18080` had no listening task-owned process after E2E.
- RED: Real browser OAuth callback produced an authorization code, but helper token exchange failed because `tenant-id` was missing from the server-side token request.
- GREEN: Helper now requires `INT_TENANT_ID` and sends `tenant-id` on token exchange, refresh-token exchange, and permission-info requests.

## Client Note

- Local runtime does not currently contain `admin-check-prod`; the E2E explicitly used existing seed client `yudao-sso-demo-by-code`, which supports `authorization_code`, `refresh_token`, `user.read`, and the helper callback redirect.
