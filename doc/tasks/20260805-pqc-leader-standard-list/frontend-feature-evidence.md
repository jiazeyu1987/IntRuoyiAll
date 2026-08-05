# Frontend Feature Evidence

## Feature Goal

将 PQC 管理标准列表的多条件筛选与“显示字段”调整为桌面端同一行：筛选在左侧主区域，列设置在右侧固定区域。

## Non-Goals

- 不改变多条件筛选状态、查询参数或后端接口。
- 不改变其它标准列表的默认工具栏布局。
- 不取消窄屏响应式换行。

## Acceptance

- AC4: PQC 管理列表显式启用标准模板单行工具栏布局。
- AC5: `UnifiedListTemplate` 单行模式使用左侧弹性筛选列和右侧自动宽度工具列。
- AC6: 窄屏下恢复可换行布局，控件不得被压缩为 0 宽。

## UI Entry Points

- Route: `/mes/pro/process-pool/pqc-leader`
- Page: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- Shared component: `IntRuoyiFronted/src/components/UnifiedListTemplate/index.vue`

## API Contracts

- 无 API 契约变更。

## BDD

- BDD: 桌面端筛选与显示字段同一行 -> Given PQC 管理列表处于桌面宽度 When 工具栏渲染 Then 筛选位于左侧，“显示字段”位于右侧且同一行。
- BDD: 窄屏保持可操作 -> Given 页面宽度不足 When 响应式布局生效 Then 筛选与列设置允许换行且保持可见。

## RED And GREEN

- RED: Pending。
- GREEN: Pending。

## Verification

- `node tests/e2e/pqc-leader-standard-list-template-static.spec.js`
- `node tests/e2e/unified-list-template-multi-filter-static.spec.js`
- `node tests/e2e/team-leader-multifilter-render-state-static.spec.js`
- `pnpm ts:check`

## Blockers

- 真实 Playwright 页面验证尚未运行。
- 共享工作区存在其它并发任务改动，本任务不执行混合提交。
