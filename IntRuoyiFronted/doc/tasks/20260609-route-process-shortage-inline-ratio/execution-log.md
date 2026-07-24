# 执行日志

BDD: 资源短缺时标准资源显示红色比值 -> Given 工序标准资源为 5 且今日可用为 4 / When 用户查看组成工序表格 / Then `标准资源` 显示红色 `4/5`。

BDD: 产能短缺时标准班次产能显示红色比值 -> Given 工序标准班次产能为 500 且今日班次产能为 400 / When 用户查看组成工序表格 / Then `标准班次产能` 显示红色 `400/500`。

BDD: 无短缺时保持标准值展示 -> Given 今日可用资源和今日班次产能不小于标准值 / When 用户查看组成工序表格 / Then `标准资源` 和 `标准班次产能` 仍显示标准值。

- RED: `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js` -> FAIL，标准资源列缺少今日资源短缺红色样式判断。
- GREEN: `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-process-remove-today-columns.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-pro-route-process-machinery-column.spec.js` -> PASS。
- GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，真实登录 `芋道源码/admin`，打开路线 `900026`，验证短缺比值展示逻辑不影响资源详情弹框。
