# 执行日志

BDD: 组成工序表格不再显示今日列 -> Given 用户打开工艺路线详情或编辑弹框 / When 查看组成工序表格 / Then 表头不再包含 `今日可用` 和 `今日班次产能`。

BDD: 今日产能详情仍可查看 -> Given 工序存在设备或人工资源 / When 用户点击 `标准资源` / Then 仍打开资源产能详情弹框，并显示今日产能相关详情。

- CHANGE: `docs/changes/20260609-route-process-remove-today-columns.md` -> ACCEPTED，用户确认两列可以删除。
- RED: `node tests\e2e\mes-route-process-remove-today-columns.spec.js` -> FAIL，当前主表格仍显示 `今日可用` 列。
- GREEN: `node tests\e2e\mes-route-process-remove-today-columns.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-pro-route-process-machinery-column.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS。
- GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，真实登录 `芋道源码/admin`，打开路线 `900026`，确认主表不显示今日列且资源详情弹框仍可查看。
