BDD: 点击人工资源打开编辑区 -> Given 用户在编辑工艺路线弹框查看人工工序 / When 点击 `5人` / Then 打开“人工产能”编辑区，显示人数、单人产能/h、班次小时和自动班次总产能。
BDD: 点击人工工序编辑优先维护人工产能 -> Given 用户在组成工序人工行点击 `编辑` / When 行资源类型是人工 / Then 打开“人工产能”编辑区而不是普通工序表单。
BDD: 保存人工产能调用资源接口 -> Given 用户修改人数、单人产能/h 和班次小时 / When 点击保存 / Then 前端调用 `/mes/pro/route-resource/save` 并刷新组成工序列表。
RED: `node tests\e2e\mes-route-process-worker-capacity-edit.spec.js` -> FAIL，缺少 `openWorkerCapacityEditor(scope.row)` 人工产能编辑入口。
GREEN: `node tests\e2e\mes-route-process-worker-capacity-edit.spec.js` -> PASS。
GREEN: `node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS。
GREEN: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
GREEN: `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js` -> PASS。
GREEN: `node tests\e2e\mes-route-process-hide-wait-color-columns.spec.js` -> PASS。
GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，只读验证 `芋道源码/admin` 路线 `900026`，未点击保存。
GREEN: `MES_ROUTE_PROCESS_SHIFT_CAPACITY_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-pro-route-process-shift-capacity-display-real-flow.e2e.js` -> PASS，只读验证 `芋道源码/admin` 路线 `900026`，未点击保存。
