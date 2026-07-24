# Execution Log: Fix missing tenant-id during login

BDD: default login tenant should complete real login -> Given the local frontend at `http://localhost:8081` and backend at `http://localhost:48081` are running with tenant mode enabled, When the user submits the default tenant, username, and password on the real login page, Then the frontend should resolve a valid tenant id before `/system/auth/login` and complete login without a missing-tenant-id backend error.

BDD: invalid tenant input should be blocked before login -> Given tenant mode is enabled on the login page, When the user enters a tenant name that does not exist, Then the frontend should stop before calling `/system/auth/login`.

RED: earlier real browser evidence -> FAIL, the login flow previously continued into backend complaints about a missing tenant identifier and later exposed an unsafe local loading cleanup path.

INFO: current branch code now resolves tenant id through `useLoginTenant()` and clears missing-tenant state through `removeTenantId()`.

GREEN: fresh Playwright session with default tenant `芋道源码`, username `admin`, password `admin123` -> PASS
- browser requests showed:
  - `GET /admin-api/system/tenant/get-id-by-name?name=芋道源码` => `200`
  - `POST /admin-api/system/auth/login` => `200`
- browser landed on `http://localhost:8081/index`

GREEN: normal visible `登录` button click after restarting the local `8081` frontend dev process -> PASS
- no Vite ESLint overlay remained on the page
- the browser moved from `http://localhost:8081/login` to `http://localhost:8081/index`

GREEN: fresh Playwright session with invalid tenant `不存在租户` -> PASS
- browser requests showed:
  - `GET /admin-api/system/tenant/get-id-by-name?name=不存在租户` => `200`
- browser requests did not show any `/admin-api/system/auth/login` call afterward

INFO: the earlier Vite ESLint overlay was stale frontend-process state. Current repository code plus a frontend dev restart no longer reproduces it on `8081`.
