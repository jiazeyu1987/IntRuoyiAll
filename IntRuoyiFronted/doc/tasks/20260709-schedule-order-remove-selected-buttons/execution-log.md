# execution-log

BDD: 删除排产工单选中按钮 -> Given 用户打开排产工单页并停留在排产工单页签, When 查看筛选行右侧工具栏并勾选工单, Then 工具栏不再渲染 `同步工单`、`批量冻结`、`批量解冻`、`批量删除` 四个按钮, 仅保留未选中红框的导出、手动重排和显示字段入口。

RED: node tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js -> FAIL, 排产工单页签工具栏仍渲染 `同步工单`。

GREEN: node tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> PASS
GREEN: pnpm.cmd exec eslint src/views/mes/pro/scheduleorder/index.vue tests/e2e/mes-schedule-order-remove-selected-buttons-static.spec.js tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js --format stylish -> PASS
