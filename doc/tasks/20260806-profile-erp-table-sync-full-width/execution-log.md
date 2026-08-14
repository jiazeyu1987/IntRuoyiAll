# Execution Log

## User Intent

- 用户基于截图要求：区域从黄框范围改成红框范围，列表区域跟着变大。
- 按当前 DOM/CSS 判断：黄框来自 `ProfileErpTableAutoSyncSetting.vue` 中 `.profile-erp-table-sync { max-width: 1080px; }`，需要移除该限制并设为全宽。

## BDD

- BDD: ERP sync card uses available config width -> Given 用户打开配置页签的 ERP 表格自动同步, When 页面渲染, Then ERP 同步卡片占满配置页签可用宽度，而不是停留在旧黄框宽度。
- BDD: ERP sync table expands with card -> Given ERP 同步卡片变宽, When 列表渲染, Then ERP 表格列表宽度跟随卡片变大。
- BDD: ERP sync layout preserves behavior -> Given 区域宽度调整, When 用户保存配置或点击手动同步, Then 原有同步配置和正式 Job 同步链路不变。

## Evidence Reviewed

- `ProfileErpTableAutoSyncSetting.vue` 当前 `.profile-erp-table-sync` 存在 `max-width: 1080px`，会让区域停留在截图黄框范围。
- `Profile/Index.vue` 的配置页签容器未额外设置小宽度，组件自身宽度限制是主要收窄点。

## TDD Evidence

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, expected reason: 合同要求 `.profile-erp-table-sync` 包含 `width: 100%` 和 `max-width: none` 后，组件仍保留旧 `max-width: 1080px`。
- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-full-width` -> PASS。
- REGRESSION: `pnpm ts:check` in `IntRuoyiFronted` -> FAIL/BLOCKED, unrelated current workspace error in `src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`: missing `productionStageStyle`.

## Implementation

- 将 `.profile-erp-table-sync` 从旧 `max-width: 1080px` 改为 `width: 100%; max-width: none;`。
- 保留 `.profile-erp-table-sync__select-table { width: 100%; }`，让列表区域跟随卡片宽度拉伸。
- 未改动 ERP/Job 同步接口、保存逻辑、手动同步逻辑或权限入口。

## Current Status

- blocked: 目标布局已实现并通过目标合同、NAS 回归和 scoped diff 检查；全量类型检查被无关前线模板页面当前改动阻塞。
