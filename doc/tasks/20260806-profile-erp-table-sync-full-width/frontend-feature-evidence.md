# Frontend Feature Evidence

## Feature

- Goal: 将 Profile 配置页签的 `ERP表格自动同步` 区域从旧黄框宽度扩展为红框范围，即占满配置页签可用宽度。
- Non-goal: 不改变 ERP/Job 同步接口、保存配置、手动同步、运行状态或权限逻辑。
- Entry point: `Profile` 页面配置页签下的 `ERP表格自动同步` 组件。
- Owned files: `IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue`、`IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js`。

## Acceptance

- AC1: `.profile-erp-table-sync` 使用 `width: 100%`。
- AC2: `.profile-erp-table-sync` 不再使用旧 `max-width: 1080px`。
- AC3: `.profile-erp-table-sync` 使用 `max-width: none`，跟随配置页签可用宽度。
- AC4: `.profile-erp-table-sync__select-table` 保持 `width: 100%`，列表区域跟随卡片拉宽。

## BDD

- BDD: ERP sync card uses available config width -> Given 用户打开配置页签的 ERP 表格自动同步, When 页面渲染, Then ERP 同步卡片占满配置页签可用宽度，而不是停留在旧黄框宽度。
- BDD: ERP sync table expands with card -> Given ERP 同步卡片变宽, When 列表渲染, Then ERP 表格列表宽度跟随卡片变大。
- BDD: ERP sync layout preserves behavior -> Given 区域宽度调整, When 用户保存配置或点击手动同步, Then 原有同步配置和正式 Job 同步链路不变。

## RED

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, expected reason: 合同要求 `.profile-erp-table-sync` 包含 `width: 100%` 和 `max-width: none` 后，组件仍保留旧 `max-width: 1080px`。

## GREEN

- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。

## Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-full-width` -> PASS。
- `pnpm ts:check` in `IntRuoyiFronted` -> BLOCKED, unrelated current workspace error in `src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`.
- Responsive/layout: 卡片和表格均为 `width: 100%`，卡片 `max-width: none`。
- Error/loading/permission states: 本次仅调整布局样式，未改变请求、错误、加载或权限逻辑。

## Blockers

- `pnpm ts:check` 被无关前线模板页面类型错误阻塞：`productionStageStyle` 模板绑定方法缺失。
