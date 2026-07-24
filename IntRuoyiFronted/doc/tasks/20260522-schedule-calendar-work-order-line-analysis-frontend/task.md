# Task: 排程日历工单产线分析前端展示

## Goal

在排程日历页为预览态和正式排程态补充“工单产线分析”详情面板，支持点击工单查看归属产线、工序产量、资源产能与瓶颈工序。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\task\calendar\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\mes\pro\task\autoSchedule\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\mes\pro\scheduleCalendar\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\schedule-calendar-*.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-work-order-line-analysis-frontend\**`

## Non-Scope

- 不调整日历页班次编辑交互。
- 不增加新的全局导航入口。
- 不重构工单主数据页。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-inline-editor-popup-regression\task.md`
- Status before this task: `Completed`
- Impact: 不存在未完成前置任务阻塞，可继续在同一页面叠加工单分析入口。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的并行改动和未追踪文件。
- Impact: 本任务只允许修改排程日历页、直接相关 API 类型、定向测试和本任务文档，提交时单独暂存。

## Milestones

- [x] M1: 建立任务文档并补 BDD 场景。
- [x] M2: 先写 RED 测试，锁定预览/正式态的工单分析入口和面板结构。
- [x] M3: 扩展前端 API 类型并接入预览态工单分析面板。
- [x] M4: 接入正式排程工单分析查询与冲突展示。
- [x] M5: 跑定向前端回归、回写证据、执行 closeout preview，并按任务单独提交。

## Expected Verification

- `node --test scripts/schedule-calendar-work-order-line-analysis.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs scripts/schedule-calendar-inline-shift-editor.test.mjs`
- `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue src/api/mes/pro/task/autoSchedule/index.ts src/api/mes/pro/scheduleCalendar/index.ts scripts/schedule-calendar-work-order-line-analysis.test.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-work-order-line-analysis-frontend\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-schedule-calendar-work-order-line-analysis-frontend --mode preview`

## Current Status

Completed.

## Completed Work

- 扩展了自动排产预览响应类型与正式排程工单分析接口类型。
- 排程日历页新增“工单产线分析”详情面板，入口统一改为点击工单编码。
- 预览态下点击工单会直接读取 `autoSchedulePreview.workOrderAnalyses`，不离开当前页。
- 正式排程态下点击工单会调用 `/mes/pro/schedule-calendar/work-order-analysis` 加载分析结果。
- 面板展示了工单、产品、数量、归属产线、起止时间、瓶颈工序以及各工序资源产能表。
- 保留了“查看工单主数据”按钮，二级跳转工单详情页。
- 支持正式排程跨线冲突展示：返回 `conflict=true` 时直接展示警告文案。

## Verification Result

- PASS: `node --test scripts/schedule-calendar-work-order-line-analysis.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs scripts/schedule-calendar-inline-shift-editor.test.mjs`
- PASS: `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue src/api/mes/pro/task/autoSchedule/index.ts src/api/mes/pro/scheduleCalendar/index.ts scripts/schedule-calendar-work-order-line-analysis.test.mjs`
- PASS: `git diff --check -- src/views/mes/pro/task/calendar/index.vue src/api/mes/pro/task/autoSchedule/index.ts src/api/mes/pro/scheduleCalendar/index.ts scripts/schedule-calendar-work-order-line-analysis.test.mjs doc/tasks/20260522-schedule-calendar-work-order-line-analysis-frontend/task.md doc/tasks/20260522-schedule-calendar-work-order-line-analysis-frontend/execution-log.md`
- PASS: 真实页面入口复验 `http://127.0.0.1:8081/mes/pro/schedule-calendar`
  - 可以进入排程日历页
  - 当前测试租户下页面显示 `范围 0 个已确认自制工单`、`当前正式排程为空`
  - 因真实测试数据为空，无法在浏览器里完整点击到工单分析面板；该项作为真实数据阻塞记录
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-work-order-line-analysis-frontend\frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-schedule-calendar-work-order-line-analysis-frontend --mode preview`

## Notes

- 当前测试租户没有真实待排工单和正式排程数据，所以浏览器端无法完成“点击真实工单打开分析面板”的最后一步验收；静态回归和后端定向测试已覆盖功能链路。
- `frontend-feature-evidence.md` 已通过校验，并按 closeout preview 规则作为附属证据文件清理，不纳入提交。
