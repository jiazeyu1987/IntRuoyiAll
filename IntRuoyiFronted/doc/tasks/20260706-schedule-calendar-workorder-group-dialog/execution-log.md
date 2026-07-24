# 执行日志：排程明细弹框按工单合并

BDD: 任务类弹框按工单合并 -> Given 用户打开生产排程日历某天的任务详情、白班详情、夜班详情或锁定详情 / When 弹框渲染任务明细 / Then 左侧同一工单只显示一次，右侧展示当前选中工单对应的工序级任务行。

BDD: 点击工单切换明细 -> Given 弹框左侧存在多个工单 / When 用户点击其中一个工单 / Then 右侧表格只展示该工单对应的工序、产品、数量、已报工、待检、执行状态、锁定、排产冻结和产线。

BDD: 非任务类弹框保持原状 -> Given 用户打开工单详情或异常详情 / When 弹框渲染 / Then 原有工单聚合表和异常表保持独立，不被任务类工单分组视图替换。

RED: `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` -> FAIL，expected reason: 旧页面缺少 `day-summary-task-group-layout`、工单分组状态和选中工单明细表。

FIX: `apply_patch` -> 将 `src/views/mes/pro/task/calendar/index.vue` 任务类日汇总弹框改为左侧工单分组列表、右侧选中工单任务表；保留工序、产品、数量、已报工、待检、执行状态、锁定、排产冻结、产线列，并将工单产线分析入口迁入左侧工单卡。

GREEN: `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` -> PASS。

GREEN: `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js` -> PASS。

GREEN: `pnpm.cmd exec eslint src/views/mes/pro/task/calendar/index.vue tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js --format stylish` -> PASS。

GREEN: `pnpm.cmd run ts:check:schedule` -> PASS。

GREEN: `task-closeout-cleanup preview` -> PASS，预览仅删除本次额外 `frontend-feature-evidence.md`，保留任务核心记录。

FIX: `task-closeout-cleanup apply` -> BLOCKED，cleanup 脚本只识别 `## Current Status` 英文状态节，已补充 `completed` 状态节后重试。

GREEN: `task-closeout-cleanup apply` -> PASS，已删除本次额外 `frontend-feature-evidence.md`，保留 `task.md` 与 `execution-log.md`。
