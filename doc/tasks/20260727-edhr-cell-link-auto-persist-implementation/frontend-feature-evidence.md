# Frontend Feature Evidence

## Feature

前端配合后端自动落库新语义：eDHR 执行页不再调用 `/mes/pro/batch-record-cell-link/prefill` 把未保存的链接值注入本地草稿；执行页只从执行详情返回的已保存 `detail.cellValues` / `cellValuesJson` hydrate 草稿状态。

Owned frontend files include:

- `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- `IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js`
- `IntRuoyiFronted/tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- `IntRuoyiFronted/tests/e2e/edhr-batch-execution-filler-entry-static.spec.js`

## Acceptance

- 执行页不调用 `BatchRecordCellLinkApi.getPrefill` 作为正式值来源。
- 删除 `normalizeCellLinkPrefillDraftValue` 等把预填值转为草稿值的路径。
- `hydrateDraftState` 不再接受 `prefills` 参数。
- 执行详情加载后以 `hydrateDraftState(detail)` 为入口，只消费已保存详情。
- 后端落库失败时不由前端写空值、默认值或静默兜底。
- 真实 E2E 覆盖 worktree 时必须同时提供 `EDHR_BATCH_E2E_BASE_URL` 和 `EDHR_BATCH_E2E_BACKEND_URL`，并校验属于同一 `int_main` runtime slot。
- E2E 启动浏览器前必须验证前端 HTTP 200 和后端 health `UP`。
- 页面内只读执行详情核验必须复用浏览器 `ACCESS_TOKEN`、`tenant-id` 和可选 `visit-tenant-id`，不得把未认证响应误判为业务数据缺失。

## BDD

- BDD: Frontend uses persisted values only -> Given 执行详情没有保存目标单元格值，When 执行页 hydrate draft state，Then 页面不得注入 `/prefill` 值伪装为已保存。
- BDD: Backend-persisted value displays like normal saved value -> Given 后端已经把生产批号落库进 execution detail，When 执行页加载详情，Then 目标格通过已保存 `detail.cellValues` 显示。
- BDD: Prefill endpoint is not a save substitute -> Given 单元格链接规则存在但自动落库失败，When 前端打开执行页，Then 不调用 `/prefill` 兜底展示成功态。
- BDD: Paired isolated runtime is explicit -> Given 真实 E2E 使用附加 worktree，When 收集运行参数，Then 前后端 URL 必须同时提供且端口按同一 slot 配对。
- BDD: Detail readback reuses browser auth -> Given 用户已通过真实登录页进入系统，When 页面上下文读取执行详情作为最终核验，Then 请求携带浏览器登录态 token 和租户头。

## RED

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL expected before frontend adjustment because `ExecutionPage.vue` still contained `BatchRecordCellLinkApi.getPrefill`, `normalizeCellLinkPrefillDraftValue`, and prefill-driven draft hydration.

## GREEN

- GREEN: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> PASS: `PASS: eDHR cell link auto-persist frontend static contract`。
- GREEN: `node tests/e2e/edhr-batch-execution-filler-entry-static.spec.js` -> PASS: paired runtime URL and authenticated readback static contract.
- GREEN: `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8086 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48086 node tests/e2e/edhr-batch-execution-real-flow.e2e.js` -> PASS through the real batch-detail open-task page path.

## Verification

- Static contract verifies the removed API call, removed draft conversion helper, removed `prefills` hydrate signature, and retained `hydrateDraftState(detail)` detail-only flow.
- Runtime contract verifies paired local URLs, frontend/backend reachability, and reuse of authenticated browser storage for detail readback.
- Real Playwright opened batch `EDHRB-1785116357526`, task `6666`, execution `1571`; the execution detail and visible input both contained target `3:3=34126020001`.
- Responsive/accessibility visual changes are not applicable because this slice removes a hidden data hydration path and does not add UI controls.
- Loading, empty, error, and permission behavior remain owned by existing execution-detail and global request handling; no new frontend fallback or mock data was introduced.

## Blockers

- No frontend blocker remains for the owned static contract.
- No frontend blocker remains for the real browser path; it passed on the authorized paired runtime, and API-only verification was not used as a substitute.
