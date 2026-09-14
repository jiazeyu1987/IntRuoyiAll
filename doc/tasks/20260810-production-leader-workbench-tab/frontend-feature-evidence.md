# Frontend Feature Evidence：生产组长工作台 tab

## Feature Goal And Non-goals

- Goal: 将现有生产组长工作台作为“生产组长”页面内部功能模块 tab 暴露，生产组长进入该页即可看到“生产组长工作台”。
- Non-goal: 不新增后端接口、不改动态菜单 SQL、不改变 PQC 组长页面的专属 tab。

## Requirements And Acceptance

- R1: `ProductionLeaderWorkbenchPage.vue` 必须以 `leader-type="PRODUCTION"` 和 `:show-production-module-tabs="true"` 进入共享工作台。
- R2: 所有生产组长 module tab 条必须同步出现 `生产组长工作台`，不能只改某一个当前可见块。
- R3: 新 tab 使用独立 `workbench` key 和 `showProductionWorkbenchModule` gate，复用正式报工工作台内容。
- R4: 切换到 `workbench` 时按 `PRODUCTION` 上下文设置查询并调用正式 `getSubmissionList()`。
- R5: PQC 组长包装页不得暴露生产组长工作台 tab。

## UI Entry Points And Owned Files

- Route: `/mes/pro/process-pool/production-leader`。
- Components: `IntRuoyiFronted/src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue`, `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- Tests: `IntRuoyiFronted/tests/e2e/production-leader-workbench-tab-static.spec.cjs`, `IntRuoyiFronted/tests/e2e/edhr-batch-record-leader-tabs-static.spec.js`。

## API Contracts And Data States

- No new API contract.
- Workbench tab reuses the existing team-leader submission page request and keeps `queryParams.leaderType = 'PRODUCTION'` before loading data.
- PQC filter gate remains anchored on the current shared component state and still exposes `PQC_SIMPLIFIED`.

## BDD Scenarios

- BDD: 生产组长看到工作台 tab -> Given 用户进入生产组长页签且具备生产组长身份 / When 页面渲染顶部 tab / Then 能看到“生产组长工作台”tab 并可切换进入现有工作台内容。
- BDD: 非当前工作台逻辑不变 -> Given 用户使用人员管理、报工管理、活跃订单池、工序配置等既有 tab / When 新增工作台 tab 后切换其它 tab / Then 既有 tab key、列表和操作入口不被重命名或替换。

## RED / GREEN Evidence

- RED: `node tests\\e2e\\edhr-batch-record-leader-tabs-static.spec.js` -> FAIL, old adjacent assertion followed obsolete `leaderType === 'PQC'` text instead of the current formal PQC filter gate.
- GREEN: `node tests\\e2e\\production-leader-workbench-tab-static.spec.cjs` -> PASS.
- GREEN: `node tests\\e2e\\production-leader-function-tabs-static.spec.js` -> PASS.
- GREEN: `node tests\\e2e\\production-leader-tabs-flat-style-static.spec.js` -> PASS.
- GREEN: `node tests\\e2e\\production-leader-remove-team-config-tab-static.spec.cjs` -> PASS.
- GREEN: `node tests\\e2e\\production-leader-active-order-pool-tab-static.spec.js` -> PASS.
- GREEN: `node tests\\e2e\\edhr-batch-record-leader-tabs-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check -- <task-owned paths>` -> PASS, only CRLF conversion warnings.

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Responsive: Reuses existing flat production module tab styles and existing workbench layout.
- Accessibility: Keeps visible text label `生产组长工作台` and stable data marker `data-production-leader-module-tab-workbench`.
- Loading/empty/error: Reuses existing `getSubmissionList()`, `loading`, `loadError`, and table empty states.
- Permission: Uses existing production leader route permission `mes:pro-process-pool-team-leader:query`; no broader role/menu grant added.

## Blockers And Follow-up Skills

- Blockers: None.
- Follow-up skills: No new long-term experience file created; existing frontend role-tab gate already covers this pattern.
