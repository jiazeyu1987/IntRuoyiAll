# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 同时保留 eDHR `生产组长` 与 `PQC组长` 两个独立页签、隐藏路由和页面关系图节点。
- Non-goal: 不改变后端接口、权限编码、组长工作台数据源或真实 E2E 范围。

## Requirements And Acceptance

- REQ-1: `生产组长` 必须是独立页签，路由到 `BatchProductionLeaderWorkbenchPage.vue`。
- REQ-2: `PQC组长` 必须是独立页签，路由到 `BatchPqcLeaderWorkbenchPage.vue`。
- REQ-3: 页面关系图必须同时展示 `组长工作台`、`生产组长` 和 `PQC组长` 三个入口。

## UI Entry Points And Owned Files

- Routes: `/mes/pro/feedback/edhr-batch-production-leader`、`/mes/pro/feedback/edhr-batch-pqc-leader`。
- Components: `EdhrBatchRecordTabs.vue`、`BatchProductionLeaderWorkbenchPage.vue`、`BatchPqcLeaderWorkbenchPage.vue`、`BatchPageGraphPage.vue`。
- Tests: `edhr-batch-record-leader-tabs-static.spec.js`、`edhr-batch-page-graph-tab-static.spec.js`、`mes-process-pool-team-leader-static.spec.js`。

## API Contracts And Data States

- No backend API contract changed.
- Both wrapper pages continue using formal `TeamLeaderWorkbenchPage.vue` and `mes:pro-process-pool-team-leader:query` permission.
- `生产组长` locks `leader-type="PRODUCTION"`; `PQC组长` locks `leader-type="PQC"`; internal leader-type tabs remain hidden.

## BDD Scenarios

- BDD: dual leader tabs -> Given 用户打开 eDHR 批记录页签栏, When 查看组长相关入口, Then `生产组长` 与 `PQC组长` 都是独立页签。
- BDD: page graph leader nodes -> Given 用户打开页面关系图, When 查看复核类节点, Then `组长工作台`、`生产组长`、`PQC组长` 都作为独立入口显示。

## RED Command And Expected Failure

- RED: `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> FAIL, expected reason: 生产组长 route 缺失。
- RED: `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> FAIL, expected reason: 页面关系图缺少 `组长工作台`/`生产组长` 口径。

## GREEN Command And Passing Result

- GREEN: `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive Accessibility Loading Empty Error Permission Checks

- Static contracts verify stable route names, visible tab labels, wrapper selectors, and formal permission arrays.
- No loading, empty, error, or API request handling was changed.
- Permission contract remains `mes:pro-process-pool-team-leader:query` for both dedicated leader routes.

## E2E Or Component Verification Path

- This turn uses focused static contracts and TypeScript verification.
- Full real E2E was not rerun per earlier user instruction.

## Blockers And Follow-Up Skills

- No blocker remains for the dual independent leader tab scope.
- Git push still requires final staged diff, branch guard, object scan, push, and post-push status verification.
