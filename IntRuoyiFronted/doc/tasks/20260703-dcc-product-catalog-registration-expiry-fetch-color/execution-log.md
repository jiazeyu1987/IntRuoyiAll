# 执行日志

BDD: 注册证有效期真实不一致才显示红色 -> Given 比对状态为 `MISMATCH` / When 列表渲染有效期单元格 / Then 有效期文字显示红色并提示当前值与外站值不一致。

BDD: 外站抓取失败不应显示红色 -> Given 比对状态为 `FETCH_FAILED` / When 列表渲染有效期单元格 / Then 有效期文字不显示红色，只通过 tooltip 展示抓取失败原因。

INFO: root-cause -> 真实 NMPA 链接服务端抓取返回 `HTTP 412`，当前前端把 `FETCH_FAILED` 和 `MISMATCH` 共用红色类名，造成“实际未完成比对但显示红色不一致”的误报。

GREEN: `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js` -> PASS
