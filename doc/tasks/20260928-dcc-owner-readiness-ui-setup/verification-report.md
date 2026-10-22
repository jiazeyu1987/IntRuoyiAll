# Verification Report

## Current Status

blocked

## Scope

- Tenant: `芋道源码`.
- Isolated runtime: frontend `http://127.0.0.1:8062`, backend `48062`.
- Users: Owner A `dccE2EOwnerA09286704` / `910326`; Owner B `dccE2EOwnerB09284620` / `910327`.
- All in-scope writes were driven by visible Playwright pages. No `fetch`, APIRequest, direct HTTP, database, Git, service restart, shared `int_main` backend access, DCC business-file action, or impersonation was used.

## Passed Configuration

### Ordinary system position

- Owner A already had the least-privileged visible position `普通员工`; reopening the user form confirmed it, so no redundant update was issued.
- Owner B initially had no position. The visible choices were `车间主任`, `项目经理`, and `普通员工`; only `普通员工` was selected.
- Natural `PUT /admin-api/system/user/update` for B returned HTTP 200 / business code 0. The refreshed row and reopened form showed `普通员工`.
- Both users retained their existing roles `DCC Action View` and `审批中心入口`. Production and quality department assignments remained unchanged.
- The isolated role `DCC E2E Owner Review 20260928` was assigned to both A and B through the real user-role page. Both refreshed rows now show this third, dedicated role while retaining the two preexisting roles and ordinary position `普通员工`. A's user-role POST returned HTTP 200 / business code 0; the final page visibly confirms the role on both rows.

Evidence: `evidence/post-assignment-ownerB-2026-09-27T23-47-08-189Z.json`, `evidence/user-post-saved-ownerB-2026-09-27T23-47-08-189Z.png`.

### DCC signature authorization

- Real sidebar path: `电子签名 -> 用户授权`.
- A and B initially showed `未授权`, signature disabled, and unlocked.
- Admin enabled only each user's DCC signature authorization using the visible switch and reason dialog. Reason: `DCC 测试负责人审批 readiness 配置，仅用于测试环境。`
- Natural writes:
  - A: `PUT /admin-api/dcc/electronic-signature-authorizations/910326`, HTTP 200 / code 0.
  - B: `PUT /admin-api/dcc/electronic-signature-authorizations/910327`, HTTP 200 / code 0.
- Reopened rows showed `已启用`, `未锁定`, and the audit reason. This did not grant signature-management privileges.

Evidence: `evidence/signature-authorization-ownerA-2026-09-28T00-03-11-658Z.json`, `evidence/signature-authorization-ownerB-2026-09-28T00-03-11-658Z.json`, and same-run screenshots.

## Remaining Blocker

### Owner signature image

- The supplied PNG was inspected and is explicitly generated test material labeled `DCC TEST SIGNATURE - ADMIN`.
- The official `电子签名 -> 我的签名` page uploads/enables the currently authenticated user's image. Owner A/B current passwords were not supplied in this task turn, so no owner-only upload/enable was attempted. Admin's own signature image was not reused.
- No password reset, impersonation, direct endpoint, or database operation was used. No `VALID` owner-image status is claimed.

## DCC REVIEW Configuration: PASS

- The visible system menu form was switched to `按钮`, which hid route-path and showed the permission identifier field. A hidden button item named `DCC 测试 REVIEW 权限` with exact permission `dcc:controlled-file:review` was created under the DCC root.
- Natural `POST /admin-api/system/menu/create` returned HTTP 200 / code 0. Searching/reopening the menu page showed the item and exact permission code.
- The dedicated role's natural `POST /admin-api/system/permission/assign-role-menu` returned HTTP 200 / code 0. Reopening the menu-permission dialog showed only `DCC 测试 REVIEW 权限` under DCC; no `受控浏览`, `APPROVE`, DCC admin, or DCC log item was selected.
- A's user-role assignment was performed through the visible user-role dialog; natural `POST /admin-api/system/permission/assign-user-role` returned HTTP 200 / code 0.
- A and B's final visible user rows both show the dedicated role alongside their existing roles and `普通员工`. B's final row was read-only during the last verification run; no duplicate role save was issued.
- No shared existing role definition was modified.

Evidence: `evidence/review-permission-create-2026-09-28T06-00-06-882Z.json`, `evidence/review-role-menu-save-2026-09-28T08-34-17-829Z.json`, `evidence/review-role-menu-reopened-2026-09-28T08-34-17-829Z.png`, A's natural assignment evidence `evidence/review-role-user-ownerA-2026-09-28T08-37-25-369Z.json`, and final A/B user-row evidence/screenshots from `evidence/review-role-user-ownerA-2026-09-28T08-50-40-359Z.*` and `evidence/review-role-user-ownerB-2026-09-28T08-50-40-359Z.*`.

## Untouched Constraints

- No controlled file was created, uploaded, approved, revised, distributed, or obsoleted.
- Department leaders remain production -> Owner A and quality -> Owner B; approval candidates remain unchanged.
- No shared role/menu definition was modified. The dedicated REVIEW-only test role is assigned to A and B.
- Ports `8062/48062` were not restarted; shared `int_main` was not touched.
- No password, token, cookie, credential request body, or browser storage value was written into evidence.
- The Playwright script requires runtime environment injection via `DCC_READINESS_ADMIN_PASSWORD`; it contains no literal administrator password.

## Resume Conditions

1. Have Owner A and Owner B authenticate through their own real frontend sessions, upload and enable the supplied labeled test image from each `我的签名` page, then reopen to verify the official image state. Do not impersonate or reset passwords for them. If either state does not reach `VALID`, record the exact official status/blocker and stop.
