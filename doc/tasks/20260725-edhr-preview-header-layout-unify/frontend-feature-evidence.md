# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 统一 eDHR 批次详情顶部栏，使带批记录切换和不带批记录切换时 `工艺流程`、`同步状态` 位置一致。
- Non-goal: 不改变工艺流程跳转、同步状态 API、版本号来源或批记录/记录本切换行为。

## Requirements And Acceptance

- Requirement: 顶部栏结构为左侧上下文、中间操作组、右侧附加组。
- Acceptance: `工艺流程` 和 `同步状态` 始终在 `.edhr-batch-detail__preview-actions` 中间操作组内。

## UI Entry Points And Owned Files

- Route/Page: eDHR 批次详情页 `BatchExecutionDetailPage.vue`。
- Component file: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`。
- Static contract: `IntRuoyiFronted/tests/e2e/edhr-preview-header-layout-static.spec.js`。

## API Contracts And Data States

- No API contract changes.
- Data states covered: 当前节点有版本号/载体切换，以及特殊节点无载体切换。

## BDD Scenarios

- BDD: 顶部栏操作位置统一 -> Given 当前批次详情选中带批记录切换或不带批记录切换的节点, When 顶部栏渲染上下文、工艺流程、同步状态、版本和批记录/记录本切换, Then 工艺流程链接和同步状态按钮始终位于同一中间操作组，右侧附加控件不改变其位置。

## RED

- RED: `node tests\e2e\edhr-batch-context-carrier-header-static.spec.js` -> FAIL, expected reason: 缺少 `.edhr-batch-detail__preview-actions` 三段式操作组。

## GREEN

- GREEN: `node tests\e2e\edhr-preview-header-layout-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive Accessibility Loading Empty Error Permission

- Responsive: 窄屏媒体查询将三段式 grid 改为单列，并让中间操作组和右侧附加组左对齐换行。
- Accessibility: 保留顶部上下文、工艺流程按钮、同步按钮、版本号和填写载体的现有可访问标签。
- Loading/Error/Permission: 未改变同步按钮 loading、工艺流程禁用状态或权限判断。

## E2E Or Component Verification Path

- Static verification only: 本次为布局结构调整，不涉及真实写入路径。

## Blockers And Follow-Up Skills

- Blocker: 当前工作区存在大量并发未提交改动，未执行全量提交/推送。