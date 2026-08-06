# Execution Log

## User Intent

- 用户反馈：全选同步后，生产用料清单看起来没有同步，时间不对。
- 用户补充：继续修正，避免使用用户难理解的内部术语。

## BDD Scenarios

- BDD: 主表显示最近执行时间 -> Given 用户在 ERP 表格自动同步页面查看表格，When 某个 ERP 表格存在最近运行记录，Then 主表时间列显示该运行记录的完成时间或开始时间，而不是内部增量位置。
- BDD: 文案避免内部术语 -> Given 用户查看 ERP 表格自动同步页面，When 页面展示时间列、错误提示或任务说明，Then 不出现用户难理解的内部术语。
- BDD: 运行中任务仍可识别 -> Given 用户点击立即执行一次后任务仍在运行，When 页面刷新运行记录，Then 主表可以使用运行记录开始时间表达当前任务已启动，并在运行中 Job 列表展示正在进行的任务。

## Evidence

- 2026-08-06：截图显示生产用料清单 `新增行数=992` 且 `同步成功/失败=成功`，说明最新运行记录成功；旧时间来自主表 `lastSuccessTime` 展示，不代表最近执行完成时间。
- 2026-08-06：前端组件旧时间列使用 `formatDateTimeValue(row.lastSuccessTime, '-')`，不符合“最近执行时间”的用户理解。

## TDD Log

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, 旧合同缺少 `最近执行时间` 并仍允许旧时间口径。
- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS，主表时间来自 `latestRun.endedAt || latestRun.startedAt`。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS，NAS 页签删除合同未回退。
- REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-execution-time-copy` -> PASS。
- REGRESSION-BLOCKED: `pnpm ts:check`（工作目录：`IntRuoyiFronted`）-> FAIL，阻塞于无关文件 `src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue(490,15)` 缺少 `productionStageStyle`。

## Blockers

- 当前工作区存在大量无关脏改动和已暂存文件；本任务只修改 ERP 同步组件、对应静态合同和本任务文档。
- 全量类型检查被无关的一线生产组件错误阻塞；本任务未修改该文件，按并行任务边界不擅自修复。
