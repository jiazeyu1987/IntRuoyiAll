# Task: 排程日历内联班次编辑弹框回归修复

## Goal

修复排程日历月视图中点击日历格子后未按预期弹出内联班次编辑面板的问题，并保持“仅允许修改系统今天及未来日期”的既有业务约束不变。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\task\calendar\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\schedule-calendar-inline-shift-editor.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-inline-editor-popup-regression\**`

## Non-Scope

- 不修改后端接口、数据库和排程规则持久化契约。
- 不回退“仅今天及未来日期可编辑”的限制。
- 不新增 fallback、mock 数据或临时调试入口。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-inline-shift-editor\task.md`
- Status before this task: `Completed`
- Impact: 可以在其基础上继续定位当前“点击无弹框”回归，不存在未完成前置任务阻塞。

## Milestones

- [x] M1: 建立任务文档，确认回归现象与期望行为。
- [x] M2: 真实页面复现问题，并锁定根因。
- [x] M3: 先补 RED 回归测试，再做最小修复。
- [x] M4: 跑定向回归、lint 和必要的真实路径复验。
- [x] M5: 回写证据、执行 closeout preview，并按任务单独提交。

## Expected Verification

- `node --test scripts/schedule-calendar-inline-shift-editor.test.mjs`
- `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue scripts/schedule-calendar-inline-shift-editor.test.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-inline-editor-popup-regression\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-schedule-calendar-inline-editor-popup-regression --mode preview`

## Current Status

Completed.

## Completed Work

- 用真实页面复现了两个边界：
  - 点击 `2026-05-14` 这类早于系统今天 `2026-05-22` 的过去日期时，只切换右下角日详情，不打开班次编辑面板。
  - 点击 `2026-05-22` 这类带 `可编辑` 标识的日期时，会打开内联班次编辑面板并展示 `白班 / 夜班 / 双班 / 休息`。
- 锁定根因：`selectCalendarDate(...)` 原先是在 `await loadDayDetail(nextDate)` 之后才设置 `calendarShiftEditorDate`，导致可编辑日期要等日详情请求返回后才显示面板，体感上像“点了没弹”。
- 已将打开顺序调整为：
  - 先计算是否可编辑
  - 必要时刷新月份格子
  - 立即设置 `calendarShiftEditorDate`
  - 再加载对应日期的日详情
- 补充了回归测试，锁定“面板必须在等待日详情前就打开”的顺序语义。

## Verification Result

- PASS: `node --test scripts/schedule-calendar-inline-shift-editor.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs`
- PASS: `pnpm exec eslint src/views/mes/pro/task/calendar/index.vue scripts/schedule-calendar-inline-shift-editor.test.mjs scripts/schedule-calendar-preview-overlay.test.mjs scripts/schedule-calendar-issue-tabs-and-jumps.test.mjs`
- PASS: `git diff --check -- src/views/mes/pro/task/calendar/index.vue scripts/schedule-calendar-inline-shift-editor.test.mjs doc/tasks/20260522-schedule-calendar-inline-editor-popup-regression/task.md doc/tasks/20260522-schedule-calendar-inline-editor-popup-regression/execution-log.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-schedule-calendar-inline-editor-popup-regression\bug-regression-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-schedule-calendar-inline-editor-popup-regression --mode preview`
- PASS: 真实页面复验 `http://127.0.0.1:8081/mes/pro/schedule-calendar`
  - 点击 `2026-05-22`：出现 `白班 / 夜班 / 双班 / 休息`
  - 点击 `2026-05-14`：仅切换为 `2026-05-14 日详情`，不弹框

## Notes

- 用户截图中的高亮日期是 `2026-05-14`，该日期按既定规则属于只读过去日期，不会弹出班次编辑面板。
- `bug-regression-evidence.md` 已通过校验，并按 closeout preview 规则作为附属证据文件清理，不纳入提交。
