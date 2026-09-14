# Execution Log

## User Intent

- 用户基于 ERP 表格列表截图要求“这个表里面增加两列，同步成功/失败，失败原因”。
- 按当前页面结构理解：不恢复最近执行记录独立表，只在现有 ERP 表格选择列表中新增两列。

## BDD

- BDD: ERP table sync shows latest status -> Given 用户打开 Profile 配置页签的 ERP 表格自动同步列表, When 最近运行记录已加载, Then 每个 ERP 表格行显示最近一次同步是成功、失败、运行中或未执行。
- BDD: ERP table sync shows failure reason -> Given 某个 ERP 表格最近一次同步失败且返回失败原因, When 用户查看该行, Then `失败原因` 列展示该失败原因，不再需要展开最近执行记录表。
- BDD: ERP table sync preserves concise table -> Given 用户要求只保留 ERP 列表视图, When 页面渲染, Then 页面不恢复 `最近执行记录` 独立表格或 Job 调度表。

## Evidence Reviewed

- `IntRuoyiFronted/src/api/erp/sync/index.ts` 已提供 `ErpKingdeeSyncApi.getRunPage`，响应类型包含 `syncType`、`status`、`failureMessage`。
- `IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue` 当前列表仅包含 `ERP表格名称`、`本地页签名称`、`最近一次同步时间`。
- `IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js` 当前禁止恢复独立 `最近执行记录` 表，但还未覆盖列表级状态列。

## TDD Evidence

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, expected reason: 合同要求 `同步成功/失败` 列后，组件尚未包含该用户可见列。
- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-status-columns` -> PASS。
- REGRESSION: `pnpm ts:check` in `IntRuoyiFronted` -> FAIL/BLOCKED, unrelated current workspace errors in `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`: missing `resolvePqcInspectionItemItems`, `resolvePqcEquipmentNumberItems`, `resolvePqcAcceptanceStandardItems`, `resolvePqcInspectionMethodItems`, `resolvePqcInspectionJudgementItems`, `resolvePqcPieceSampleItems`, `resolvePqcDefectDescriptionText`.

## Implementation

- 在 ERP 表格列表新增 `同步成功/失败` 列，用 `el-tag` 展示 `成功`、`失败`、`运行中`、`未执行` 或未知状态。
- 在 ERP 表格列表新增 `失败原因` 列，只展示最近失败运行记录的正式 `failureMessage`。
- 使用 `ErpKingdeeSyncApi.getRunPage({ pageNo: 1, pageSize: 1, syncType })` 按每个 ERP 表格的 `syncType` 获取最近一次运行记录，避免全局分页漏掉某个表。
- 保留当前简洁列表结构，不恢复独立 `最近执行记录` 表或 `Job 调度` 表。

## Current Status

- blocked: 目标功能已实现并通过目标合同、NAS 回归和 scoped diff 检查；全量类型检查被无关 MES 页面当前改动阻塞。
