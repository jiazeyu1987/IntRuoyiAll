# 执行日志：20260703-schedule-order-trace-dialog-readability

BDD: 追溯弹框先展示排产工单上下文 -> Given 用户点击排产工单行内追溯 / When 弹框打开 / Then 弹框顶部展示排产编码、日志数量、最近操作和最近时间，用户无需先读表格即可识别当前对象。

BDD: 操作日志按时间线阅读 -> Given 追溯接口返回多条操作日志 / When 弹框渲染 / Then 每条日志以时间线卡片展示操作类型、操作人、原因和排产编码，字段差异放在当前操作卡片内。

BDD: 字段差异值长文本可读 -> Given 字段差异包含长备注、JSON 或日期 / When 用户展开追溯记录 / Then 新旧值使用可换行的等宽值块展示，不依赖单行 tooltip。

RED: `node tests/e2e/mes-schedule-order-trace-dialog-readability-static.spec.js` -> FAIL，当前追溯弹框仍为 900px 单表格布局，缺少摘要区、时间线卡片、空状态和可换行字段差异值块。

CHANGE: `src/views/mes/pro/scheduleorder/index.vue` 将“排产工单追溯”弹框改为 `min(1120px, calc(100vw - 24px))` 响应式宽度；新增顶部摘要区、空状态、时间线卡片、操作原因区和字段差异值块；保留 `MesProScheduleOrderApi.getOperationLog(row.id)` API 调用和原字段差异解析逻辑。

GREEN: `node tests/e2e/mes-schedule-order-trace-dialog-readability-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-schedule-order-route-progress-dialog-width-static.spec.js; node tests/e2e/mes-schedule-order-trace-dialog-readability-static.spec.js` -> PASS
