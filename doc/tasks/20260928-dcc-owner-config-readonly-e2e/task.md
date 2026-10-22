# DCC Owner Configuration Read-Only UI E2E

## Goal
Use real Playwright UI navigation to determine whether the `芋道源码/admin` tenant has at least two distinct enabled users suitable for production and quality department leaders, and whether the department-leader configuration page is reachable. Assess readiness for BDD-09/10 owner-change E2E and ordinary-permission boundary E2E.

## Milestones
- Inspect the applicable E2E, runtime, login, and closeout rules.
- Log in through the UI and inspect the system user list and department-management UI.
- Inspect the DCC route/review-matrix UI entry as visible to the same identity.
- Save read-only page evidence and review whether the identity/data supports the follow-up E2E cases.

## Expected Verification
- Real Playwright page navigation only; passive observation of naturally generated browser responses is allowed.
- Record visible display name, username, and enabled state for candidate users; do not record credentials.
- Record the visible department-leader configuration entry and whether its form opens.
- No direct API/fetch calls, database writes, user/configuration changes, or Git operations.

## Current Status
blocked

管理员只读探查已完成：用户列表显示总数 2149，首屏 20 个用户均为启用；系统管理菜单可打开用户管理和部门管理，生产/质量 DCC 专用部门的负责人编辑表单均可打开且未提交；DCC 上传审批页可由真实前端路由打开。BDD-09/10 的完整多身份 E2E 仍因当前仅有 `admin` 登录凭据而阻塞，未修改任何用户或配置。

## Design Constraints
- Use only `http://127.0.0.1:8062`, tenant `芋道源码`, account `admin`.
- All findings must be based on visible UI or passive network observation caused by UI navigation.
- Never click Save/Submit or mutate users, departments, or DCC configuration.
