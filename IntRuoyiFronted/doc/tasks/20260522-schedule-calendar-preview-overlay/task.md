# Task: 排程日历预览月历与日详情切换到预览任务视图

## Goal

修复 `生产排程日历` 页签在生成自动排产预览后，右侧预览汇总显示 `工单=2 / 新任务=48`，但月历格子与 `日详情` 仍然只显示正式排程数据的问题，使页面在存在预览结果时优先展示预览任务视图。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\task\calendar\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\mes\pro\workorder\bom\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\schedule-calendar-preview-overlay.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-preview-overlay\**`

## Non-Scope

- 不修改自动排产后端接口。
- 不修改排程日历正式排程接口契约。
- 不顺带重做预览问题弹框、工单详情页或产线/工作站设置页。
- 不引入 fallback、mock 数据或静默降级。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-hall-product-candidate-page-contract-fix\task.md`
- Status before this task: `Completed`
- Impact: 最近同仓前端任务已完成，不阻塞本次排程日历预览视图修复。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的在途改动与任务目录。
- Impact: 本任务只允许修改排程日历页、直接相关 API 类型与本任务测试/文档；提交时单独暂存本任务文件。

## Milestones

- [x] M1: 检查前置任务状态并创建当前任务文档。
- [x] M2: 先补 RED 测试，锁定“生成预览后月历/日详情读预览任务”的行为。
- [x] M3: 在排程日历页增加预览任务 overlay 计算与月历/日详情切换。
- [x] M4: 运行定向 GREEN 验证并记录证据。
- [x] M5: 更新任务状态并执行 closeout preview。

## Expected Verification

- `node --test scripts/schedule-calendar-preview-overlay.test.mjs`
- 可选真实复验：本地登录后生成预览，确认 2026-05-13 当日 `工单=2`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-schedule-calendar-preview-overlay --mode preview`

## Current Status

Completed.

## Reproduced Finding

- 后端预览已生成 `48` 条预览任务，且两张工单都排在 `2026-05-13`。
- 当前页面右侧预览汇总使用了 `autoSchedulePreview.summary`，能显示 `工单=2 / 新任务=48`。
- 但月历格子、`日详情` summary、下方任务卡片仍然从 `loadMonthCalendar()` / `loadDayDetail()` 的正式排程接口取数，因此还显示正式排程的 `工单=1 / 任务=1`。

## Completed Work

- 在 `calendar/index.vue` 中增加了预览 overlay 计算：
  - 从 `autoSchedulePreview.tasks` 提取预览任务
  - 按日期构建预览月历统计
  - 在 `selectedDayTaskRows`、`selectedDayMaterialRows`、月历格子和顶部统计中优先使用预览数据
- 引入 `ProWorkOrderBomApi.getWorkOrderBomItemListByWorkOrderId()`，在生成预览后加载当前工单物料需求，用于预览当日物料汇总的按工单归集。
- 调整预览浏览行为：
  - 有预览时，切换日期/月份不再立刻清空预览
  - 预览任务卡片不再暴露锁定/解锁动作，避免对 preview 临时任务误操作
- 新增回归测试 `scripts/schedule-calendar-preview-overlay.test.mjs`，锁定：
  - 预览存在时月历格子切到 overlay
  - 日详情任务视图切到 overlay
  - 生成预览后会加载工单物料需求 map

## Verification Result

- PASS: `node --test scripts/schedule-calendar-preview-overlay.test.mjs`
- PASS: `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue scripts/schedule-calendar-preview-overlay.test.mjs src/api/mes/pro/workorder/bom/index.ts`
- PASS: `git diff --check -- src/views/mes/pro/task/calendar/index.vue src/api/mes/pro/workorder/bom/index.ts scripts/schedule-calendar-preview-overlay.test.mjs`
- PASS: 真实后端复现证据
  - `POST /admin-api/mes/pro/auto-schedule/preview` 返回的 48 条 type=303 预览任务中，`301_903200` 与 `301_903245` 两张工单的全部任务起始时间都落在 `2026-05-13`

## Remaining Notes

- 本地浏览器默认登录的是 `测试租户 / aoteman`，与当前复现预览数据所用的 `tenant-id=1 / admin` 不是同一套数据，因此未把浏览器截图作为主验证证据。
