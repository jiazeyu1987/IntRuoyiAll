# Task: 登录 XButton 点击修复

## Goal

修复当前登录页点击主登录按钮时有时不触发真实 `/system/auth/login` 请求的问题，恢复基于 `XButton` 的点击事件透传，确保依赖真实登录路径的前端 E2E 可以稳定继续。

## Scope

- 仅修改前端仓库中的通用 `XButton` 组件和与本次验证直接相关的任务证据。
- 使用真实登录页路径验证点击主按钮后会发出登录请求。
- 不修改后端登录契约，不改 DCC 业务页面，不引入 mock。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-batch-record-image-compare-phase2/task.md`
- Status before this task: blocked by login interaction instability.
- Impact: the phase2 image-compare task is paused specifically so this shared login-click blocker can be repaired first.

## BDD Scenarios

- BDD: clicking XButton should trigger parent click handlers -> Given a page uses `XButton` with a parent `@click` binding, When the operator clicks the rendered button, Then the parent click handler should run exactly once.
- BDD: login button click should issue a real login request -> Given the login form contains valid tenant, username, and password values with captcha disabled, When the operator clicks the main login button, Then the frontend should send `/admin-api/system/auth/login`.

## Milestones

1. [ ] M1: Create task package and record BDD scenarios.
2. [ ] M2: Capture RED evidence showing the login button click does not send the login request.
3. [ ] M3: Implement the minimal `XButton` click forwarding fix.
4. [ ] M4: Re-run the login request verification and update evidence.
5. [ ] M5: Update task docs and commit only task-scoped frontend files.

## Expected Verification

- Real browser verification shows `/admin-api/system/auth/login` is requested after clicking the login button.

## Current Status

Blocked pending explicit resume. A higher-priority DCC approval-route display defect
is being isolated in a separate task package before any more shared-button work
continues.

## Blocker And Impact

- Blocker: task intentionally paused for an unrelated, higher-priority DCC route-display fix.
- Impact: no additional login-button behavior changes are made in this turn, and this
  task must be resumed explicitly after the DCC fix lands.
