# Verification Report

## Scope

- 将 `ERP表格自动同步` 卡片从旧 `1080px` 宽度限制改为配置页签可用宽度。
- 让 ERP 表格列表跟随卡片变宽。

## Result

- `.profile-erp-table-sync` 已改为 `width: 100%; max-width: none;`。
- `.profile-erp-table-sync__select-table` 保持 `width: 100%;`。
- ERP/Job 同步接口、保存配置、手动同步、状态列和权限入口均未改动。

## Verification Evidence

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-full-width` -> PASS。

## Blockers

- `pnpm ts:check` in `IntRuoyiFronted` -> FAIL/BLOCKED；失败点在无关共享改动 `src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`。
- 本任务不修改该前线模板页面，以避免混入并行任务范围。
