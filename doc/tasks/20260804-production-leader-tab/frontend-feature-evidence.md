# Frontend Feature Evidence: eDHR 双组长页签

## Feature Goal And Non-Goals

- Goal: eDHR 顶部同时保留 `生产组长` 与 `PQC组长` 两个独立页签，并让 `组长工作台` 不再显示生产组长内容。
- Non-goal: 不改后端 API、权限 schema、数据库 schema，不用 process-pool 独立主导航替代 eDHR 内部页签。

## Requirements And Acceptance

- REQ-1: `生产组长` 页签路由为 `/mes/pro/feedback/edhr-batch-production-leader`，组件为 `BatchProductionLeaderWorkbenchPage.vue`。
- REQ-2: `PQC组长` 页签路由为 `/mes/pro/feedback/edhr-batch-pqc-leader`，组件为 `BatchPqcLeaderWorkbenchPage.vue`。
- REQ-3: `组长工作台` 包装页锁定 `leader-type="PQC"`，不得显示生产组长标题或 `PRODUCTION` 内容。
- REQ-4: process-pool 独立 `生产组长` 与 `PQC组长` 主导航入口可以保留，但不得替代 eDHR 顶部双页签。

## UI Entry Points And Owned Files

- Routes: `/mes/pro/feedback/edhr-batch-production-leader`, `/mes/pro/feedback/edhr-batch-pqc-leader`, `/mes/pro/feedback/edhr-batch-team-leader`, `/mes/pro/feedback/edhr-batch-page-graph`。
- Components: `EdhrBatchRecordTabs.vue`, `BatchProductionLeaderWorkbenchPage.vue`, `BatchPqcLeaderWorkbenchPage.vue`, `BatchTeamLeaderWorkbenchPage.vue`, `BatchPageGraphPage.vue`。
- Tests: `edhr-batch-record-leader-tabs-static.spec.js`, `edhr-batch-page-graph-tab-static.spec.js`, `mes-process-pool-team-leader-static.spec.js`。

## API Contracts And Data States

- Reuses existing `TeamLeaderWorkbenchPage.vue`; no new API contract.
- Production wrapper locks `leader-type="PRODUCTION"`; PQC and group leader wrappers lock `leader-type="PQC"`; all wrappers set `:show-leader-type-tabs="false"`。
- Backend errors and missing data behavior are unchanged; no fallback, mock data, or silent downgrade added.

## BDD Scenarios

- BDD: eDHR 双组长页签 -> Given 用户打开 eDHR 批记录顶部页签 / When 查看组长入口 / Then 同时存在 `生产组长` 与 `PQC组长` 独立页签并分别进入对应 eDHR route。
- BDD: 生产组长不在组长工作台 -> Given 用户进入 `组长工作台` / When 页面加载组长工作台包装页 / Then 页面不锁定 `PRODUCTION`，不显示生产组长标题或生产组长页签内容。
- BDD: PQC 正式链路保留 -> Given 用户进入 `PQC组长` / When 页面加载 / Then 复用正式 `TeamLeaderWorkbenchPage`，锁定 `leader-type="PQC"` 且不显示内部类型切换。

## RED / GREEN Evidence

- RED: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> FAIL，expected reason: `src/views/mes/pro/edhr-batch/BatchPqcLeaderWorkbenchPage.vue must exist.`
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> PASS。
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS。
- GREEN: `workdir=IntRuoyiFronted; node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS。
- GREEN: `workdir=IntRuoyiFronted; pnpm ts:check` -> PASS。
- POST-MERGE GREEN: reran all three static contracts and `pnpm ts:check` after resolving `origin/int_main` conflicts -> PASS。

## Responsive Accessibility Loading Empty Error Permission

- Responsive: wrapper pages retain the existing shared workbench layout; no new layout breakpoints.
- Accessibility: reuses Element Plus tabs and existing stable selectors; no custom keyboard behavior added.
- Loading / empty / error: inherited from `TeamLeaderWorkbenchPage.vue`; no fallback or exception swallowing added.
- Permission: eDHR leader routes keep `mes:pro-process-pool-team-leader:query`; page graph keeps `mes:pro-edhr-batch-execution:query`。

## E2E Or Component Verification Path

- Static source contracts verify route names, route paths, wrapper components, page graph route mapping, and negative checks against the wrong process-pool replacement口径。
- Real browser E2E was not run because this is a route/tab composition change covered by focused static contracts and TypeScript validation.

## Blockers And Follow-Up Skills

- Current implementation is verified in isolated worktree `D:\IntRuoyiWorktree\production-leader-tab-20260804`。
- Follow-up: commit the resolved merge, push the branch, then rerun closeout gate.
