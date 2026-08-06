# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 生产组长报工管理表删除红框列，并展示生产报工的完成数量、损耗数量、损耗明细、选用设备和设备参数。
- Goal: 生产组长报工表和显示字段设置不展示 PQC 专属提交内容、检验任务和过程检验汇集列。
- Goal: 一线正式报工提交结构化 `lossDetails`、`selectedDevice`、`deviceParameterReadings`。
- Goal: 参数超上下限允许提交，但填写页与组长报工表使用红色异常 marker 提醒。
- Non-goal: 不改 PQC 组长专属审核列，不新增真实 E2E fixture，不引入 mock 或 API-only 验收。

## Requirements And Acceptance IDs

- AC-FE-1: 生产报工表不显示 `生产工单`、`PQC`、`提交内容` 三列。
- AC-FE-2: 报工表显示结构化损耗、设备和参数字段。
- AC-FE-3: 提交 payload 包含每个损耗原因及数量、选用设备和设备参数读数。
- AC-FE-4: 参数异常用 `is-parameter-out-of-range` / `data-parameter-status` 标记，且不阻断提交。
- AC-FE-5: 生产组长报工表列池与 PQC 组长列池隔离，生产列池不包含 PQC 专属字段。

## UI Entry Points And Owned Files

- Entry: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- Entry: `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- API types: `IntRuoyiFronted/src/api/mes/pro/feedback/index.ts`
- API types: `IntRuoyiFronted/src/api/mes/pro/processpool/index.ts`
- Static contracts: `IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs`
- Static contracts: `IntRuoyiFronted/tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs`
- Static contracts: `IntRuoyiFronted/tests/e2e/team-leader-production-report-abnormal-parameter-static.spec.cjs`

## API Contracts And Data States

- Submit request sends `lossDetails[]`, `selectedDevice`, and `deviceParameterReadings[]`.
- Timeline/read model accepts `outputQuantity`, `lossQuantity`, `lossDetails`, `selectedDevice`, and `deviceParameterReadings`.
- Parameter status values are `NORMAL`, `BELOW_LOWER`, and `ABOVE_UPPER`.
- Abnormal status is display-only; submit remains allowed.

## BDD Scenarios

- BDD: 生产报工表结构化展示 -> Given 班组长打开报工管理, When 查看生产报工记录, Then 红框列被删除且结构化字段可见。
- BDD: 一线提交结构化参数 -> Given 当前工序存在损耗、设备和参数配置, When 员工提交报工, Then payload 包含明细数组和设备参数快照。
- BDD: 参数超限允许提交并标红 -> Given 参数值超出上下限, When 员工提交并班组长查看, Then 提交允许且异常数值红色提示。
- BDD: 生产组长不显示 PQC 内容 -> Given 生产组长打开报工管理和显示字段设置, When 查看可见列, Then 不出现 PQC提交内容、检验类型/轮次、过程检验汇集等 PQC 专属列。

## RED Command And Expected Failure

- RED: `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs` -> FAIL, 旧生产报工表仍保留红框列且缺少结构化设备/参数列。
- RED: `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs` -> FAIL, 旧提交 payload 只能表达单个损耗原因且缺少结构化设备参数。
- RED: `node tests/e2e/team-leader-production-report-abnormal-parameter-static.spec.cjs` -> FAIL, 旧页面缺少参数异常红色 marker。
- RED: `pnpm ts:check` -> FAIL, `ProFrontlineDeviceParameterReadingReqVO` 类型谓词把可选字段误收窄成必填字段。
- RED: `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs` -> FAIL, 旧实现没有生产/PQC 独立列池，生产列配置仍可能暴露 PQC 专属字段。

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/team-leader-production-report-abnormal-parameter-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/team-leader-report-allocation-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS after worktree dependency install and type predicate fix.
- GREEN: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS after生产/PQC列池隔离.

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Accessibility: abnormal parameters expose status through stable class/data markers and ARIA abnormal cue.
- Empty state: no selected production device returns empty parameter readings instead of fabricated values.
- Error state: parameter status is display-only; backend/API errors are not hidden by frontend fallback.
- Permission: no route or menu permission changes were introduced.
- Responsive: no layout redesign or new breakpoint behavior was introduced.

## E2E Or Component Verification Path

- Current verification uses task-owned static contracts plus `pnpm ts:check`.
- Real write-type E2E was not run because this task did not establish a running frontend/backend pair, test tenant/account, signature, production order, and process configuration fixture; no API-only or mock substitute was used.

## Blockers And Follow-Up Skills

- No frontend blocker remains for the documented static/type verification scope.
- If a future release requires real write E2E, prepare task-owned tenant/account/data fixtures and run Playwright against the registered worktree URLs `8086/48086`.
