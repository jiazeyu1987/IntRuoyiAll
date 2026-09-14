# Execution Log

## User Intent

- 用户要求在 ERP 表格自动同步表格中“增加一个手动同步的按钮”。
- 按当前表格结构理解为每个 ERP 表格行增加 `手动同步` 操作按钮，用于单独触发该表的正式增量同步。

## BDD

- BDD: ERP table sync supports row manual sync -> Given 用户查看 ERP 表格自动同步列表, When 用户点击某一行的 `手动同步`, Then 系统只提交该行 ERP 表格对应 handler 的正式增量同步任务。
- BDD: ERP table sync refreshes row result after manual sync -> Given 用户触发某个 ERP 表格手动同步, When 提交成功, Then 页面刷新最近同步时间、同步成功/失败和失败原因列。
- BDD: ERP table sync exposes manual sync failures -> Given 手动同步提交失败, When 后端或 Job API 返回错误, Then 页面通过 `ElMessage.error` 暴露错误，不显示默认成功。

## Evidence Reviewed

- `ProfileErpTableAutoSyncSetting.vue` 当前有底部 `立即执行一次` 批量按钮，但表格行内没有 `手动同步` 操作按钮。
- 组件已使用正式 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)`，可复用于单表手动同步。
- 静态合同已覆盖正式 ERP/Job 链路和禁止恢复旧 `kingdee-table-auto-sync` 接口。

## TDD Evidence

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, expected reason: 合同要求 `操作` 列和 `手动同步` 按钮后，组件尚未包含该用户可见操作列。
- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-manual-button` -> PASS。
- REGRESSION: `pnpm ts:check` in `IntRuoyiFronted` -> FAIL/BLOCKED, unrelated current workspace errors in `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`: missing `resolvePqcInspectionItemItems`, `resolvePqcEquipmentNumberItems`, `resolvePqcAcceptanceStandardItems`, `resolvePqcInspectionMethodItems`, `resolvePqcInspectionJudgementItems`, `resolvePqcPieceSampleItems`, `resolvePqcDefectDescriptionText`.

## Implementation

- 在 ERP 表格列表新增 `操作` 列，每行显示 `手动同步` 按钮。
- 新增 `handleRunSingle(row)`，使用 `ErpKingdeeSyncApi.runIncrementalSyncJob(row.handlerName)` 只提交当前 ERP 表格对应 handler。
- 新增 `manualSyncingType` 行级 loading，手动同步期间禁用其它行手动同步和底部批量 `立即执行一次`。
- 手动同步成功后刷新 `loadWatermarks()` 和 `loadLatestRuns()`，让最近同步时间、同步成功/失败和失败原因回读最新数据。
- 手动同步失败通过 `ElMessage.error(resolveErrorMessage(error, '手动同步失败'))` 暴露，不显示默认成功。

## Current Status

- blocked: 目标功能已实现并通过目标合同、NAS 回归和 scoped diff 检查；全量类型检查被无关 MES 页面当前改动阻塞。
