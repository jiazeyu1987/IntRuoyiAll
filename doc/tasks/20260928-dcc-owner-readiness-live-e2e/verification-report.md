# Verification Report

## Current Status

blocked

## Scope

This run configures existing DCC E2E Owner A/B accounts through the isolated frontend at `127.0.0.1:8062` with backend `127.0.0.1:48062`, tenant 芋道源码. All business writes described here were performed by visible Playwright UI interaction. No API/fetch/DB/Git write, DCC file submission, approval, or department-leader mapping change was performed.

## Passed

- User page baseline confirmed A `910326` in `DCC-E2E生产部-0922v1` and B `910327` in `DCC-E2E质量部-0923v1`; both already had system post `普通员工`, so no post mutation was needed.
- Created permission role `DCC E2E Owner Review 20260928`, role ID `991221`, through the visible role form. Natural `POST /admin-api/system/role/create`: HTTP 200, business code 0.
- Saved menu permissions through the visible role tree. Natural `POST /admin-api/system/permission/assign-role-menu`: HTTP 200, business code 0. Reopened role showed only leaf `DCC Review` checked; `DCC Approve`, audit/log/admin items and its `受控浏览` parent were unchecked. The checked `文控中心` root corresponds to menu `DccRoot`, whose permission identifier is empty.
- Assigned role `991221` to A and B through each visible user-row assignment form. Two natural `POST /admin-api/system/permission/assign-user-role` responses: HTTP 200, business code 0. Reopened rows retained `DCC Action View` and `审批中心入口`, and showed the new review role.
- Reopened admin UI `电子签名 -> 用户授权` shows A and B both `已启用`, signature `启用`, and the explicit test change reason `DCC 测试负责人审批 readiness 配置，仅用于测试环境。` No redundant authorization write was needed. Current-state screenshot: `evidence/readiness-setup-2026-09-28/signature-authorizations-a-b-current.png`.
- Admin UI reset both credentials; each natural `PUT /admin-api/system/user/update-password` returned HTTP 200, business code 0. Each owner submitted the visible forced-change page; the first natural `POST /admin-api/system/auth/change-password-before-login` returned HTTP 200, code 0. Both owners subsequently used their final passwords in a fresh browser session and reached their own `/user/profile` page (HTTP 200, login code 0). Owner A needed a second explicit visible login attempt before navigation; only that confirmed page transition is counted.
- The two generated PNG assets were visually checked: each is visibly labeled `SYNTHETIC TEST ONLY` and `NOT A REAL SIGNATURE`.

## Blocker

The Owner A/B authenticated sidebars expose `个人中心`, `审批中心`, and `文控中心`; expanding `文控中心` exposes only `受控浏览`. Neither owner has a visible `电子签名` or `我的签名` entry. To respect the requested role boundary, no deep-link URL, administrator impersonation, or hidden route was used. Owner-specific signature upload/enable was not attempted. Menu screenshots:

- `evidence/readiness-setup-2026-09-28/owner-a-visible-menus.png`
- `evidence/readiness-setup-2026-09-28/owner-b-visible-menus.png`

The single DCC upload/readiness E2E is therefore not ready to run: the synthetic images have not been uploaded as the corresponding owner signatures.

## Auth UI Anomaly

For each owner, after the successful forced-change request, the frontend issued a second request using the now-invalid old password. That duplicate request returned HTTP 200 with business code `1002003005` (`用户密码校验失败`). This response did not prevent later normal login, but it is a confirmed forced-change UI anomaly and is not treated as a successful second mutation.

## Restrictions And Data Safety

- Password values, hashes, tokens, and cookies are absent from task files and screenshots; values were held only in the active Node process and are returned to the parent thread separately.
- Production -> Owner A and quality -> Owner B mappings remain unchanged.
- No DCC business file was created or approved.
- No database, direct API/fetch, Git, shared service, or DCC business write was used.
