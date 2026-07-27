# Frontend Feature Evidence

## Feature

前端配合后端自动落库新语义：eDHR 执行页不再调用 `/mes/pro/batch-record-cell-link/prefill` 把未保存的链接值注入本地草稿；执行页只从执行详情返回的已保存 `detail.cellValues` / `cellValuesJson` hydrate 草稿状态。

Owned frontend files include:

- `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- `IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js`

## Acceptance

- 执行页不调用 `BatchRecordCellLinkApi.getPrefill` 作为正式值来源。
- 删除 `normalizeCellLinkPrefillDraftValue` 等把预填值转为草稿值的路径。
- `hydrateDraftState` 不再接受 `prefills` 参数。
- 执行详情加载后以 `hydrateDraftState(detail)` 为入口，只消费已保存详情。
- 后端落库失败时不由前端写空值、默认值或静默兜底。

## BDD

- BDD: Frontend uses persisted values only -> Given 执行详情没有保存目标单元格值，When 执行页 hydrate draft state，Then 页面不得注入 `/prefill` 值伪装为已保存。
- BDD: Backend-persisted value displays like normal saved value -> Given 后端已经把生产批号落库进 execution detail，When 执行页加载详情，Then 目标格通过已保存 `detail.cellValues` 显示。
- BDD: Prefill endpoint is not a save substitute -> Given 单元格链接规则存在但自动落库失败，When 前端打开执行页，Then 不调用 `/prefill` 兜底展示成功态。

## RED

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL expected before frontend adjustment because `ExecutionPage.vue` still contained `BatchRecordCellLinkApi.getPrefill`, `normalizeCellLinkPrefillDraftValue`, and prefill-driven draft hydration.

## GREEN

- GREEN: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> PASS: `PASS: eDHR cell link auto-persist frontend static contract`。

## Verification

- Static contract verifies the removed API call, removed draft conversion helper, removed `prefills` hydrate signature, and retained `hydrateDraftState(detail)` detail-only flow.
- Responsive/accessibility visual changes are not applicable because this slice removes a hidden data hydration path and does not add UI controls.
- Loading, empty, error, and permission behavior remain owned by existing execution-detail and global request handling; no new frontend fallback or mock data was introduced.

## Blockers

- No frontend blocker remains for the owned static contract.
- Real browser eDHR verification remains conditional on local runtime, login, tenant, and writable test-data prerequisites; API-only verification is not used as a substitute.
