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

## Acceptance

- 执行页不得调用 `BatchRecordCellLinkApi.getPrefill`。
- 执行页不得保留 `normalizeCellLinkPrefillDraftValue` 或 `prefills` 驱动的草稿 hydrate。
- 执行详情加载后必须通过 `hydrateDraftState(detail)` 只消费已保存详情。
- 相邻静态合同不得继续要求执行页使用旧 prefill API。
- 真实 E2E 不得用 mock、API-only 或直接 SQL 造数替代正式页面路径。

## BDD

- BDD: Frontend uses persisted values only -> Given 执行详情没有保存目标单元格值 When 执行页 hydrate draft state Then 页面不得注入 `/prefill` 值伪装为已保存。
- BDD: Backend-persisted value displays like normal saved value -> Given 后端已把生产批号落库进 execution detail When 执行页加载详情 Then 目标格通过已保存 `detail.cellValues` 显示。

## RED

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL。

## GREEN

- GREEN: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-pre-release-editable-submit-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-fill-workspace-worktask-permission-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-execution-real-flow.e2e.js` -> PASS，授权租户为 `测试租户`，账号为 `codexedhrcell01`，批次为 `BE-EDHR-CELL-20260728-104808`。
- BLOCKED: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> FAIL，失败点为并行新增的表单模板参数 API 合同断言，不属于本次执行页 `/prefill` 移除范围。

## E2E Path

- `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8081 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48081 node tests/e2e/edhr-batch-execution-real-flow.e2e.js`

## Verification

- Static verification passed for the focused auto-persist contract, pre-release editable submit contract, and work task permission contract.
- Broad batch-record cell-link static verification is currently blocked by an unrelated form-template API assertion.
- Type verification passed with `pnpm ts:check`.
- Runtime precheck confirmed frontend `8081` returned HTTP `200` and backend `48081` returned health `UP`; both ports belonged to `E:\IntRuoyi` main runtime.
- Real E2E produced `real-e2e-evidence.md` with PASS: Playwright opened the real batch detail, clicked `打开填写`, switched to `原表模式`, and asserted target cell `1:5` showed the persisted batch code `EDHR-CELL-20260728-104808`.

## Blockers

- 本次前端功能和真实 E2E 无剩余 blocker；并行宽合同仍阻塞在非本任务的表单模板 API 断言。
