# Task: 生产排程日历直达路由恢复与 E2E

## Goal

恢复生产排程日历页面的前端直达路由，使 `http://localhost:8081/mes/pro/schedule-calendar` 不再返回 404，并补上对应的真实数据 E2E 验证。

## Scope

- 检查上一条前端任务状态，确认临时冻结真实数据 E2E 任务已完成。
- 创建当前任务文档、执行日志与前端证据文件。
- 为生产排程日历页面增加前端隐藏路由，保持 `activeMenu` 仍归属 `生产排产`。
- 补一条真实 Playwright E2E，用于验证日历页面可直达打开且仍通过工单分页请求携带 `temporaryFrozen=false`。
- 不修改后端接口，不新增测试专用控件，不改动临时冻结业务规则。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-pro-workorder-temporary-freeze-real-e2e/task.md`
- Status before this task: completed.
- Impact: 临时冻结 E2E 资产已落地，本任务可以聚焦恢复排程日历真实入口并补齐对应验证。

## Milestones

- [x] M1: 检查前序任务状态并创建当前任务目录、文档和证据文件。
- [x] M2: 记录 RED 证据，确认当前直达路由为 404。
- [x] M3: 补齐生产排程日历前端隐藏路由。
- [x] M4: 运行真实 E2E，记录 GREEN 证据并提交当前任务相关改动。

## Expected Verification

- `http://127.0.0.1:8081/mes/pro/schedule-calendar` 不再显示 404。
- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-direct-route run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-schedule-calendar-direct-route-e2e\scripts\verify-schedule-calendar-direct-route.mjs`
- E2E 结果需证明页面标题/主体为排程日历页面，并抓到 `temporaryFrozen=false` 的工单分页请求。

## Current Status

Completed. 已完成直达路由修复、真实 Playwright 验证与前端仓库提交。

## Blocker And Impact

- Blocker: none remaining for the route fix itself.
- Impact:
  - `http://127.0.0.1:8081/mes/pro/schedule-calendar` 现在可以直达打开排程日历。
  - 该直达页面已通过真实 E2E 证明仍沿用 `temporaryFrozen=false` 的工单范围过滤。

## Final Verification Result

- `npx.cmd eslint --ext .ts src/router/modules/remaining.ts`
  - PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar-direct-route run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-schedule-calendar-direct-route-e2e\scripts\verify-schedule-calendar-direct-route.mjs`
  - PASS，返回：
    - `url = http://127.0.0.1:8081/mes/pro/schedule-calendar`
    - `title = 瑛泰管理系统 - 排程日历`
    - `requestUrl = http://localhost:48081/admin-api/mes/pro/work-order/page?status=1&type=1&temporaryFrozen=false&pageNo=1&pageSize=200`
    - `httpCode = 200`
    - `apiCode = 0`

## Commit Result

- Frontend repo `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
  - Commit: `15d3eceb`
  - Message: `任务: 恢复排程日历直达路由`
