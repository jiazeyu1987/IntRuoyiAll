# Frontend Feature Evidence

## Feature Goal

恢复 eDHR 执行页“只展示已保存单元格链接值”的前端合同：前端不再用 `/batch-record-cell-link/prefill` 结果冒充正式落库值。

## Non-Goals

- 不改后端自动落库服务。
- 不新增 UI 控件。
- 不用 mock 数据或 API-only 路径替代真实 E2E。

## Owned Files

- `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- `IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js`
- `IntRuoyiFronted/tests/e2e/edhr-batch-execution-real-flow.e2e.js`

## BDD

- BDD: Frontend uses persisted values only -> Given 执行详情没有保存目标单元格值 When 执行页 hydrate draft state Then 页面不得注入 `/prefill` 值伪装为已保存。
- BDD: Backend-persisted value displays like normal saved value -> Given 后端已把生产批号落库进 execution detail When 执行页加载详情 Then 目标格通过已保存 `detail.cellValues` 显示。

## RED

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL。

## GREEN

- 待补充。

## E2E Path

- `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8081 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48081 node tests/e2e/edhr-batch-execution-real-flow.e2e.js`

## Blockers

- 当前真实 E2E 缺少本地数据库可打开正式批记录任务夹具。
