# Frontend Feature Evidence

## Feature Goal

- Make QA 规程配置 route scope loading continue when the selected product route lacks unique `checkFlag` but has one deterministic formal BATCH batch-record process.
- Preserve fail-fast behavior for ambiguous process configuration.

## Non-Goals

- Do not change backend API contracts.
- Do not use `formBindings` or form slots to infer the QA batch-record process.
- Do not make the yellow-box route scope fields manually editable.

## Requirements And Acceptance IDs

- R1: Selecting `ID / 球囊扩张压力泵 / 112` must not be blocked only because `checkFlag` is missing when the route has one enabled formal batch-record process.
- R2: Multiple `checkFlag=true` processes must still show an explicit configuration error.
- R3: Multiple formal BATCH batch-record process candidates must fail-fast instead of selecting one implicitly.

## UI Entry Points And Owned Files

- Entry point: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`
- Tests:
- `IntRuoyiFronted/tests/e2e/qa-regulation-route-checkflag-fallback-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`

## API Contracts And Data States

- Existing API wrappers remain unchanged:
- `ProRouteProcessApi.getRouteProcessListByRoute(routeId)`
- `ProRouteFlowConfigApi.getProcessConfigList(routeId, 'SCHEDULE', routeVersionId)`
- `ProRouteFlowConfigApi.getProcessConfigList(routeId, 'BATCH', routeVersionId)`
- Data state rule: `checkFlag` is preferred; if absent, only an enabled BATCH config with formal `batchRecordReports` can identify the process.

## BDD Scenarios

- BDD: QA 路线缺少 checkFlag 但有唯一正式批记录绑定工序 -> Given QA 规程配置页已从产品读取到正式绑定路线和 ACTIVE 版本 When 该路线工序列表没有唯一 `checkFlag=true` 但 BATCH 配置存在唯一启用的 `batchRecordReports` 工序 Then 页面不应显示 `工艺路线范围加载失败`，应继续展示路线版本和适用工序。
- BDD: QA 路线存在多个 checkFlag -> Given 路线存在多个 `checkFlag=true` 工序 When 加载 QA 适用范围 Then 仍应 fail-fast 提示多个质检工序，避免错误选工序。

## RED

RED: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> FAIL
- Expected reason: missing formal BATCH batch-record binding resolver.

## GREEN

GREEN: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\qa-regulation-pressure-pump-screenshot-pages-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs` -> PASS
GREEN: `pnpm ts:check` -> PASS

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Responsive/UI layout unchanged.
- Accessibility labels unchanged.
- Loading state remains `qaRouteScopeLoading`.
- Error state remains visible through `data-qa-regulation-route-scope-error`.
- Permission and menu behavior unchanged.

## E2E Or Component Verification Path

- Static contract verification was used because the reported bug is a deterministic route scope parsing contract.
- No real browser E2E was run in this pass.

## Verification

- Target route checkFlag contract, adjacent QA contracts, and `pnpm ts:check` passed.

## Blockers And Follow-Up Skills

- No implementation blocker remains.
- Commit/push closeout is pending user confirmation because the shared `int_main` working tree contains many unrelated dirty changes.
