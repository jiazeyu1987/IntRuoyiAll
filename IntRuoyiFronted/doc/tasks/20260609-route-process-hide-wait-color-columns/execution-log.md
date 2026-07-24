# 执行日志

BDD: 组成工序主表不显示等待时间和颜色列 -> Given 用户打开工艺路线详情或编辑弹框 / When 查看组成工序表格 / Then 表头不包含 `等待时间` 和 `甘特图颜色`。

BDD: 编辑工序仍可维护等待时间和颜色 -> Given 用户点击组成工序行的编辑 / When 编辑工序弹框打开 / Then 弹框仍保留 `等待时间` 和 `甘特图颜色` 字段。

- RED: `node tests\e2e\mes-route-process-hide-wait-color-columns.spec.js` -> FAIL，主表仍显示 `等待时间` 列。
- GREEN: `node tests\e2e\mes-route-process-hide-wait-color-columns.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-process-shortage-inline-ratio.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-process-remove-today-columns.spec.js` -> PASS。
- GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8081 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，真实登录 `芋道源码/admin`，打开路线 `900026`，确认主表不显示 `等待时间` 和 `甘特图颜色`。
