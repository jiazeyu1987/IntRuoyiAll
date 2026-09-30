# DCC Owner Readiness Live E2E Setup

## Goal

通过隔离测试环境真实前端，为已有 Owner A / Owner B 建立通过 DCC matrix review route readiness 所需的最小系统岗位、review 权限、电子签名授权和明确标记为合成测试的签名图片。唯一 DCC upload/readiness E2E 由父线程在这些前置配置完成后执行。

## BDD

- BDD-01: Given Owner A/B 为生产/质量部门负责人且缺岗位、review 权限、签名授权或有效图片；When 管理员通过正式 UI 配置最小必要 readiness 条件；Then 页面自然响应成功且重开后可见已保存值。
- BDD-02: Given 测试账号通过强制改密页面完成 ACTIVE 凭据；When 本人登录并通过电子签名管理页上传/启用 SYNTHETIC TEST 图片；Then 签名图片状态有效，素材清楚标示为测试合成而非真实签名。
- BDD-03: Given Owner A/B 处于 RESET_REQUIRED；When admin 经用户管理 UI 重置一次性密码且本人经强制改密 UI 设置新密码；Then 两人仅通过新密码正常登录，任何密码、哈希、令牌均不写入文件。
- BDD-04: Given 用户管理 UI 可编辑 Owner A/B；When admin 分别选择普通系统岗位“普通员工”；Then 页面自然保存成功，重开表单仍显示所选岗位。
- BDD-05: Given 当前测试角色不含 DCC review；When admin 从真实角色权限菜单树创建仅含 `dcc:controlled-file:review` 的非管理员角色并分配 A/B；Then 不授予 approve、admin、log 权限，保留原有两个角色，重开表单可见配置。
- BDD-06: Given A/B 的 DCC 电子签名授权行当前为已启用；When admin 从 UI 重新打开授权列表；Then 两行仍显示启用状态与明确测试变更原因（本轮不重复写授权）。
- BDD-07: Given A/B 以本人新密码登录且已获授权；When 检查本人可见菜单；Then 必须存在本人签名 UI 才能继续上传合成图片。当前两人菜单均无该入口，图片上传因此保持 BLOCKED，不得用管理员或深链绕过。

## Milestones

1. [x] Read repo rules and existing owner identity/readiness evidence.
2. [x] Generate explicit synthetic test signature PNG assets; visually verify the explicit synthetic/not-real labels.
3. [x] Through user management UI, confirm “普通员工” for A/B and create/assign non-admin review-only role from the visible permission tree.
4. [x] Verify through the reopened electronic-signature authorization UI that A/B are already enabled with an explicit test reason; no redundant authorization write was needed in this run.
5. [ ] A/B completed forced password change and new-session normal login; synthetic-image upload is BLOCKED because neither owner's visible menu exposes the personal signature UI.
6. [ ] Record configuration evidence; parent thread may run the unique-file DCC upload/readiness E2E only after the owner signature-image prerequisite is resolved. Do not alter the existing production/quality leader mapping.

## Expected Verification

- Playwright UI writes only; no fetch/API actions or direct database writes.
- For every saved system-user/role/authorization/signature step, observe only the natural UI request response (HTTP 200, business code 0) and reopen the relevant page to confirm persisted state.
- No DCC business file is submitted or approved in this setup task.
- Git diff check is not run because the current user explicitly prohibits Git operations.

## Design Constraints

- Separate worktree backend 48062, frontend 8062; do not touch shared int_main services.
- Departments must end in original state: production -> Owner A, quality -> Owner B.
- No administrator/log/approve privileges; only review and read access needed for this E2E.
- Synthetic signature imagery must contain visible SYNTHETIC TEST ONLY labeling and must never be represented as a real person's signature.
- No passwords, tokens, or cookies in task logs/evidence.
- No direct `fetch`, API client, shell HTTP calls, database access, or Git operations. All business writes use real Playwright interactions with visible frontend controls.
- Generate one-time passwords only in the live Node process. Print final Owner A/B passwords only to the parent thread after successful setup; do not write them to disk or screenshots.

## Current Status

blocked

## Given / When / Then

- Given: the isolated frontend/backend are assigned to 8062/48062, the tenant is 芋道源码, admin is the only setup operator, Owner A/B are existing users and department leaders, and both test images explicitly disclaim real signatures.
- When: the admin and each owner perform only the requested role, authorization, password and synthetic-image configuration through visible Playwright UI flows.
- Then: each visible role/password save succeeds and is confirmed; the existing electronic-signature authorization is verified enabled; Owner A/B can normally log in; owner-specific synthetic-image upload remains BLOCKED at the missing personal signature menu; department leader mappings remain unchanged; no DCC business file is created or approved in this task.

## Cleanup Keep
- doc/tasks/20260928-dcc-owner-readiness-live-e2e/task.md
- doc/tasks/20260928-dcc-owner-readiness-live-e2e/execution-log.md
- doc/tasks/20260928-dcc-owner-readiness-live-e2e/verification-report.md
- doc/tasks/20260928-dcc-owner-readiness-live-e2e/generate-synthetic-owner-signatures.cjs
