# 执行日志：排程明细任务表产品列拆分

BDD: 隐藏红框任务列 -> Given 用户打开生产排程日历任务类明细弹框 / When 查看右侧所选工单任务表 / Then 不再展示 `待检` 和 `执行状态` 列。

BDD: 产品拆分为编码和名称 -> Given 用户打开生产排程日历任务类明细弹框 / When 查看右侧所选工单任务表 / Then 产品信息拆分显示为 `产品编码` 与 `产品名称` 两列，不再合并为 `产品` 一列。

BDD: 工单分组保持不变 -> Given 用户切换左侧工单 / When 右侧任务表刷新 / Then 仍只展示当前工单对应的工序级任务行，工单详情和异常详情不受影响。

RED: `node tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js` -> FAIL，expected reason: 右侧任务表仍显示合并 `产品` 列，缺少 `产品编码` 和 `产品名称`。

FIX: `apply_patch` -> 将右侧任务表 `产品` 列拆成 `产品编码` / `产品名称`，删除 `待检` 和 `执行状态` 列，并同步既有排程日历静态契约。

GREEN: `node tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js` -> PASS。

GREEN: `node tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js` -> PASS。

GREEN: `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js` -> PASS。

GREEN: `pnpm.cmd exec eslint src/views/mes/pro/task/calendar/index.vue tests/e2e/mes-pro-schedule-calendar-task-product-columns-static.spec.js tests/e2e/mes-pro-schedule-calendar-workorder-group-dialog-static.spec.js tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js --format stylish` -> PASS。

GREEN: `pnpm.cmd run ts:check:schedule` -> PASS。
