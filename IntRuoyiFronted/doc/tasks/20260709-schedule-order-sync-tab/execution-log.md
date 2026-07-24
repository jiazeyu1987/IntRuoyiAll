# execution-log

BDD: 同步工单作为页签打开 -> Given 用户在排产工单页面, When 点击“同步工单”, Then 页面切换到“同步工单”页签并展示待同步差异列表, 不出现 Dialog 弹框。
BDD: 同步工单入池流程保留 -> Given 用户在同步工单页签选择可入池工单, When 点击“选中工单加入排产工单池”, Then 仍调用既有 createFromWorkOrders 接口并刷新同步列表和排产工单列表。

RED: node tests/e2e/mes-schedule-order-sync-tab-static.spec.js -> FAIL, 排产工单页面仍缺少 `scheduleOrderActiveTab` 页签承载同步工单。
GREEN: node tests/e2e/mes-schedule-order-sync-tab-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js -> PASS
GREEN: pnpm.cmd ts:check:schedule -> PASS