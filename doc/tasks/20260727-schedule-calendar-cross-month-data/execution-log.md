# Execution Log

## User Intent

用户确认问题不是复用甘特图接口，而是排程日历自身数据显示不真实：生产排产页签有任务，但排程日历某些日期显示任务 0。要求进行修复。

## BDD

BDD: 排程日历月视图跨月日期展示正式排产数据 -> Given 当前月视图的 42 天网格包含相邻月份日期，且相邻月份日期在排程日历正式月接口中存在任务 When 页面加载当前月排程日历 Then 页面必须加载可见网格涉及的所有月份并把相邻月份日期合并进单元格数据，跨月日期不得因未加载而显示任务 0 / 工单 0。

## Commands And Evidence

- INFO: experience-preflight -> PASS，命中 `docs/frontend-development.md#前端静态契约隔离门禁`，本任务使用聚焦静态契约。
- RED: `node tests/e2e/mes-schedule-calendar-visible-months-static.spec.js` -> FAIL，预期失败原因：排程日历缺少 `visibleMonthDays`，`calendarDayMap` 只读取当前月 `monthData.value.days`，跨月格子未加载正式日历数据。
- CHANGE: 更新 `IntRuoyiFronted/src/views/mes/pro/task/calendar/index.vue`，新增 42 天可见网格月份计算，使用排程日历月接口并发读取可见月份，当前月汇总仍使用当前月响应，单元格数据改为合并后的可见月份日数据。
- CHANGE: 新增 `IntRuoyiFronted/tests/e2e/mes-schedule-calendar-visible-months-static.spec.js` 并在 `package.json` 暴露 `e2e:mes:schedule-calendar-visible-months:static`。
- GREEN: `node tests/e2e/mes-schedule-calendar-visible-months-static.spec.js` -> PASS。
- GREEN: `pnpm e2e:mes:schedule-calendar-visible-months:static` -> PASS。
- GREEN: `pnpm ts:check:schedule` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/task/calendar/index.vue IntRuoyiFronted/tests/e2e/mes-schedule-calendar-visible-months-static.spec.js IntRuoyiFronted/package.json doc/tasks/20260727-schedule-calendar-cross-month-data/task.md doc/tasks/20260727-schedule-calendar-cross-month-data/execution-log.md` -> PASS。
- INFO: project-experience-consolidation -> CHECKED，检索 `docs/*memory*.md` 和排程日历关键词后没有合适长期经验归宿；未新建长期经验文档。
