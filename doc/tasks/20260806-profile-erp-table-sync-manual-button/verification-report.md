# Verification Report

## Scope

- 在 `ERP表格自动同步` 表格中新增每行 `手动同步` 操作。
- 手动同步仅触发当前行 ERP 表格对应 handler，不影响其它表格选择配置。

## Result

- `操作` 列已加入 ERP 表格列表。
- 每行 `手动同步` 按钮已接入正式 `ErpKingdeeSyncApi.runIncrementalSyncJob(row.handlerName)`。
- 手动同步成功后刷新水位和最近运行记录。
- 手动同步失败通过 `ElMessage.error` 显示，不吞异常、不默认成功。

## Verification Evidence

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-manual-button` -> PASS。

## Blockers

- `pnpm ts:check` in `IntRuoyiFronted` -> FAIL/BLOCKED；失败点在无关共享改动 `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 本任务不修改该 MES 页面，以避免混入并行任务范围。
