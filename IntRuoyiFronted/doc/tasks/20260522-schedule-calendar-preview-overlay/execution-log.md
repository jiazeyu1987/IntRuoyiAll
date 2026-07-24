# Execution Log: 排程日历预览月历与日详情切换到预览任务视图

- 2026-05-22 14:31: 已建立任务文档。
- BDD: 生成预览后月历任务/工单数切换到预览结果 -> Given 自动排产预览已返回 2 个工单和 48 条新任务 / When 页面展示当前月份日历格子 / Then 预览涉及日期应显示预览任务与工单计数，而不是继续显示正式排程计数。
- BDD: 生成预览后当日日详情切换到预览任务 -> Given 当前选中日期属于预览任务实际排入日期 / When 页面展示右侧 `日详情` / Then `任务/工单/任务卡片` 应来自预览任务视图，而不是正式排程详情接口。
- RED: `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\schedule-calendar-preview-overlay.test.mjs` -> FAIL，初始源码中不存在 `ProWorkOrderBomApi / isPreviewCalendarOverlayActive / activeCalendarDayMap / previewTaskRowsByDate` 等 overlay 逻辑。
- GREEN: `node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\schedule-calendar-preview-overlay.test.mjs` -> PASS，4 个定向断言全部通过。
- GREEN: `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue scripts/schedule-calendar-preview-overlay.test.mjs src/api/mes/pro/workorder/bom/index.ts` -> PASS。
- GREEN: `git diff --check -- src/views/mes/pro/task/calendar/index.vue src/api/mes/pro/workorder/bom/index.ts scripts/schedule-calendar-preview-overlay.test.mjs` -> PASS。
- GREEN: backend preview evidence -> PASS，当前 `POST /admin-api/mes/pro/auto-schedule/preview` 返回的 48 条预览任务中，`301_903200` 与 `301_903245` 两张工单的子任务都落在 `2026-05-13`，证明“只有 1 个工单排下去”是前端视图口径问题，不是后端预览结果问题。
