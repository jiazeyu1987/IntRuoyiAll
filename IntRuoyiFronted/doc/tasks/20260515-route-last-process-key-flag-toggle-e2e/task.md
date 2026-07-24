# Task: 两条工艺路线末道工序设为关键工序并验证开关

## Goal

将 `ROUTE-XLSX-00001` 和 `ROUTE-XLSX-00002` 两条工艺路线的最后一道工序设置为关键工序，并基于真实前端入口 `http://127.0.0.1:8081` 做 E2E 验证，确认这两条工艺路线都可以成功开启和关闭。

## Scope

- 先检查同仓库上一条前端任务状态；若未完成，则显式阻塞后再启动本任务。
- 在执行真实数据操作前创建任务目录、任务文档与执行日志。
- 通过真实登录、真实页面路径进入工艺路线列表和工艺路线编辑页。
- 对 `ROUTE-XLSX-00001` 与 `ROUTE-XLSX-00002` 的最后一道工序执行最小数据变更：将 `keyFlag` 设为 `true`。
- 通过真实页面操作验证这两条路线都能从关闭切到开启，再从开启切回关闭。
- 若启用/停用仍存在业务前置条件阻塞，必须记录精确阻塞项并停止，不得用 fallback 或伪造成功掩盖。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-dcc-file-category-list-e2e-verification/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused DCC verification task does not block this MES route configuration and E2E task.

## Milestones

- [x] M1: Check the previous frontend task state and block it explicitly.
- [x] M2: Create this task document and execution log before real data changes.
- [x] M3: Reproduce the current route data state and identify the last process rows for both routes.
- [x] M4: Set the last process of both routes to `keyFlag = true` through the real UI path.
- [ ] M5: Run RED/GREEN-style real E2E verification for enable and disable on both routes.
- [x] M6: Record verification evidence and stop on the exact blocking prerequisite.

## Expected Verification

- A real browser path logs into `http://127.0.0.1:8081`.
- Both `ROUTE-XLSX-00001` and `ROUTE-XLSX-00002` show the last process as key process after configuration.
- Both routes can be enabled successfully from the list page.
- Both routes can be disabled successfully from the list page after enable succeeds.
- The verification record captures the exact route codes, page path, and observed toggle results.

## Current Status

Blocked. Both routes now have the last process marked as key process, and
`ROUTE-XLSX-00002` has been verified to enable and disable through the real UI.
`ROUTE-XLSX-00001` is still blocked by a missing product BOM master-data
prerequisite, so the requested full two-route toggle verification cannot be
completed yet without additional real data input.

## Blocker And Impact

- Blocker: `ROUTE-XLSX-00001` fails enable with `产品 PTCA球囊扩张导管 未配置工序的 BOM 消耗`, and the real
  `BOM 物料` selector for that route product returned no selectable BOM
  candidates.
- Impact: the route cannot be enabled through the real system until product BOM
  master data for `PTCA球囊扩张导管` exists and at least one route-product BOM row
  can be configured.

## Partial Verification Result

- RED before data change:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session route-key-flag-toggle-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-last-process-key-flag-toggle-e2e\scripts\verify-two-routes-toggle-e2e.mjs`
  - Result: FAIL on `ROUTE-XLSX-00001` with `工艺路线必须要有关键工序`
- Data change:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session route-key-flag-toggle-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-last-process-key-flag-toggle-e2e\scripts\set-last-process-key-flag-via-ui.mjs`
  - Result: PASS, both routes saved the last-process `keyFlag=true`
- RED after key-process fix:
  - Same two-route toggle command
  - Result: FAIL on `ROUTE-XLSX-00001` with `产品 PTCA球囊扩张导管 未配置工序的 BOM 消耗`
- Prerequisite check:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session route-key-flag-toggle-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-last-process-key-flag-toggle-e2e\scripts\ensure-route-product-bom-via-ui.mjs`
  - Result: FAIL, no selectable BOM candidates were available for the route product
- Real UI verification for `ROUTE-XLSX-00002`:
  - Enabled successfully: route row switched to checked state and row actions became disabled on the live page
  - Disabled successfully: route row returned to unchecked state and row `编辑` became enabled again on the live page
