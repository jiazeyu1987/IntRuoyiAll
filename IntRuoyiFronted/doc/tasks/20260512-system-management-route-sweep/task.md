# Task: System management route sweep

## Goal

Route through every visible child page under the System Management menu in the frontend and verify that each real user path opens without frontend routing or runtime errors.

## Scope

- Use Playwright against the running frontend and backend.
- Log in as the configured admin user.
- Open the System Management menu and every visible child route under it.
- Record pass or exact blocker evidence for each route.
- Fix only frontend route/runtime issues found in this scope.
- Do not add fallback, mock success, or silent downgrade behavior.

## Milestones

- [x] M1: Previous frontend task checked and explicitly blocked because it was incomplete.
- [x] M2: Task documentation created before route sweep work.
- [x] M3: System Management child route inventory collected.
- [x] M4: Baseline route sweep executed through real UI paths.
- [x] M5: Frontend route/runtime defects fixed or exact blockers recorded.
- [x] M6: Full route sweep rerun completed.
- [x] M7: Evidence updated and task finalized.
- [x] M8: Current task changes committed separately after required verification passes.

## Expected Verification

- Playwright can log in to `http://127.0.0.1:8081`.
- The System Management menu is opened from the real sidebar.
- Every visible child route under System Management is clicked at least once.
- Each route result is recorded as `pass` or `blocked` with exact evidence.

## Route Inventory

- 租户管理 / 租户列表
- 租户管理 / 租户套餐
- 用户管理
- 角色管理
- 菜单管理
- 部门管理
- 岗位管理
- 字典管理
- 消息中心 / 短信管理 / 短信渠道
- 消息中心 / 短信管理 / 短信模板
- 消息中心 / 短信管理 / 短信日志
- 消息中心 / 邮箱管理 / 邮箱账号
- 消息中心 / 邮箱管理 / 邮件模版
- 消息中心 / 邮箱管理 / 邮件记录
- 消息中心 / 站内信管理 / 模板管理
- 消息中心 / 站内信管理 / 消息记录
- 消息中心 / 通知公告
- 审计日志 / 操作日志
- 审计日志 / 登录日志
- OAuth 2.0 / 应用管理
- OAuth 2.0 / 令牌管理
- 三方登录 / 三方应用
- 三方登录 / 三方用户
- 地区管理

## Current Status

Completed. Playwright routed through all 24 visible System Management leaf routes from a fresh admin login. Final result: 24 passed, 0 blocked.

## Final Verification

- Command: `npx --package @playwright/cli playwright-cli -s=system-route-sweep run-code --filename doc\tasks\20260512-system-management-route-sweep\scripts\audit-system-management-routes.mjs`
- Result: PASS, `total=24`, `passed=24`, `blocked=0`, console errors `0`.
