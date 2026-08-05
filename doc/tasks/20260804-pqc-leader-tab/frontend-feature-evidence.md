# Feature

生产组长与 PQC组长拆成 eDHR 父菜单下的独立主导航页面，类似 `批次执行`，不再显示在 eDHR 批次页内部 tabs 或单一组长工作台中。

## Acceptance

- `生产组长` 使用 `/mes/pro/process-pool/production-leader` 与 `ProductionLeaderWorkbenchPage.vue`，包装共享工作台并锁定 `leader-type="PRODUCTION"`。
- `PQC组长` 使用 `/mes/pro/process-pool/pqc-leader` 与 `PqcLeaderWorkbenchPage.vue`，包装共享工作台并锁定 `leader-type="PQC"`。
- `EdhrBatchRecordTabs.vue` 不包含 `组长工作台`、`生产组长`、`PQC组长` 及对应 active-tab key。
- 页面关系图只指向 process-pool 独立主导航路由，不再使用旧 eDHR leader routes。

## UI Entry Points And Owned Files

- Routes: `/mes/pro/process-pool/production-leader`, `/mes/pro/process-pool/pqc-leader`.
- Components: `IntRuoyiFronted/src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue`, `IntRuoyiFronted/src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue`, `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`.
- Updated: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue`, `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue`.

## BDD

- BDD: 两类组长独立主导航页签 -> Given 用户展开 eDHR 主导航, When 查看 QA 与批次执行之间的菜单, Then 依次显示 `生产组长` 和 `PQC组长` 两个独立入口。
- BDD: 生产组长独立页面 -> Given 用户进入 `生产组长`, When 页面加载, Then 使用 `leaderType=PRODUCTION` 且不显示内部类型切换。
- BDD: PQC组长独立页面 -> Given 用户进入 `PQC组长`, When 页面加载, Then 使用 `leaderType=PQC` 且不显示内部类型切换。
- BDD: eDHR 内部页签清理 -> Given 用户打开 eDHR 批次内部页签, When 查看顶部 tabs, Then 不出现 `组长工作台`、`生产组长` 或 `PQC组长`。

## RED

- RED: `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> FAIL, expected reason: corrected contract required standalone production/PQC leader pages and no eDHR internal leader tabs.
- RED: `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> FAIL, expected reason: page graph still referenced old eDHR leader route semantics.
- RED: `node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> FAIL, expected reason: menu contract lacked production leader id `900436` and corrected ordering.

## GREEN

- GREEN: `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- `rg -n 'edhr-batch-(team-leader|production-leader|pqc-leader)|Batch(Team|Production|Pqc)LeaderWorkbenchPage|active-tab="(teamLeader|productionLeader|pqcLeader)"' IntRuoyiFronted\src IntRuoyiFronted\tests\e2e -g '!*node_modules*'` -> only negative test assertions remained.
- `git diff --check -- <task-owned paths>` -> PASS with CRLF warnings only.

## Blockers

- Real Playwright E2E was not run because runtime/login/test-tenant preconditions were not established.
- Commit/push closeout is blocked by unrelated dirty/ahead workspace state.
