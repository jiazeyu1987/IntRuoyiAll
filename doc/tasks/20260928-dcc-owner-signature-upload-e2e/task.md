# Owner Synthetic Signature Upload E2E

## Goal

通过真实 Playwright UI 为专用测试角色仅增加 `signature-governance:policy:query` 菜单权限，再由 Owner A/B 分别上传并启用各自明确标记为合成测试、非真实签名的 PNG。

## BDD

- BDD-01: Given role ID `991221` 当前仅有 DCC Review 且无 Approve/admin/audit/log 子权限；When 管理员在系统角色菜单树只勾选 `signature-governance:policy:query` 并保存；Then 自然页面响应成功，重新打开后只新增该 permission，任何禁选子项仍未勾选。
- BDD-02: Given Owner A/B 使用本人账号登录并从可见菜单进入“我的签名”；When 各自上传指定任务 PNG、填写测试材料理由并启用；Then 页面自然响应成功，重开页面仍显示本人对应的 SYNTHETIC TEST ONLY / NOT A REAL SIGNATURE 图片有效。
- BDD-03: Given 本人签名图片上传和启用需要留下用途理由；When 当前页面提供可见、必填的变更原因并由本人填写测试专用说明；Then 上传与启用自然请求复用该非空理由，留空时不得发起任一写请求。

## Milestones

1. [x] Read repository E2E, login, runtime, worktree and closeout rules; verify 8062/48062 listeners and task-owned PNGs.
2. [x] Through real admin UI add only the policy-query menu permission and reopen to verify exact role permissions.
3. [ ] Verify through the owner UI that the visible required reason is reused by both upload and enable actions.
4. [ ] Owner A admin-UI reset, forced password change, and normal login in a fresh Playwright context; credentials remain in memory only.
5. [ ] Owner B self-login, menu navigation, upload and enable synthetic PNG; verify persisted active/valid state.
6. [ ] Review natural UI responses, screenshots and evidence; write verification report.

### Owner A Debug Final Continuation

- Scope: Owner A only (`910326`, `dccE2EOwnerA09286704`) on the registered 8062/48062 runtime.
- Through the visible admin UI, reset Owner A with a random temporary password held only in the Node process; then use a fresh browser context for the owner's forced password change and normal login.
- Submit the required-change form exactly once. Record visible password-field count, submit-button role/type/outerHTML, visible validation errors, and natural request count/status/code without reading or persisting input values.
- If the change request is not emitted, clear every password input before saving the diagnostic screenshot and sanitized JSON. Never retry the submit blindly.
- If forced change and normal login both succeed, navigate by the visible `电子签名 -> 我的签名` menu, upload only the task-owned Owner A synthetic PNG via the file chooser, enter a `SYNTHETIC TEST ONLY` reason, and verify natural upload/enable responses plus refreshed visible status, filename and synthetic label.
- One Playwright/Node process only. No direct fetch/API, database, Git, service restart, trace, password/token/cookie persistence, password-bearing screenshot, Owner B action, or DCC workflow action.
- Evidence destination: `evidence/owner-a-debug-final/`.
- Final one-click diagnostic: Owner A's required-change page rendered three visible password inputs and an enabled native submit button; the single submit emitted no password-change POST and showed required-field validation. Inputs were cleared before `required-change-no-submit.png` was saved. Signature upload/enable did not start; see `evidence/owner-a-debug-final/result.json`.

## Expected Verification

- All role and signature writes are performed through visible frontend controls with Playwright.
- Observe only page-natural responses for HTTP status and business code; do not issue direct API/fetch/DB/Git operations.
- Reopen role menu and each owner's own signature page to verify saved state.
- No DCC file approval or signature is performed.

## Design Constraints

- Runtime: frontend `127.0.0.1:8062`, backend `127.0.0.1:48062`, tenant `芋道源码`.
- Role `DCC E2E Owner Review 20260928`, ID `991221`, code `dcc_e2e_owner_review_20260928`.
- Only add menu permission `signature-governance:policy:query`; never select manage/admin/records/retention/csv children.
- Owner A `910326` uploads only `evidence/synthetic-signatures/owner-a-synthetic-test.png`; Owner B `910327` uploads only `owner-b-synthetic-test.png`.
- Reason must identify test material and be supplied through the visible page; upload/enable must use the same reason. Passwords are never written to files.
- Cancel without saving if the tree auto-selects any prohibited child permission.

## Current Status

blocked

Historical run `evidence/owner-a-final/result.json` stopped at forced password change. The authorized continuation is now running under `evidence/910326-final/`; only sanitized response metadata and screenshots without credential-bearing UI may be retained.

Owner B run authorized on 2026-09-28: reset user `910327` through the real admin UI, read the generated temporary password only from the visible success toast into Node memory, complete required password change and normal login in a fresh context, then navigate via the visible `电子签名 -> 我的签名` menu and upload/enable only Owner B's task-owned synthetic PNG. Evidence destination: `evidence/910327-final/`. Owner A evidence remains separate and is not overwritten.

## Cleanup Keep

- doc/tasks/20260928-dcc-owner-signature-upload-e2e/task.md
- doc/tasks/20260928-dcc-owner-signature-upload-e2e/execution-log.md
- doc/tasks/20260928-dcc-owner-signature-upload-e2e/verification-report.md
- doc/tasks/20260928-dcc-owner-signature-upload-e2e/evidence/owner-a-debug-final/run-owner-a-debug-final.cjs
