# Execution Log

BDD: 排产工单主操作显示在标题栏右侧 -> Given 用户打开排产工单列表，When 查看页面顶部，Then “同步工单 / 导出 / 手动重排 / 显示字段”应位于“排产工单”标题栏右侧红框区域，而不是占用筛选行右侧空间。

RED: `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> FAIL，旧契约仍要求主操作位于 `UnifiedListTemplate` 的 `actions` 插槽，即筛选行右侧。

实现记录：将主列表的页面级操作从 `UnifiedListTemplate` 的 `actions` 插槽移入 `ContentWrap` 的 `header` 插槽；标题栏右侧新增 `schedule-order-pool__header-actions` 容器，继续复用原权限、loading、disabled、批量操作与列配置保存逻辑。

GREEN: `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS。
GREEN: `node tests/e2e/unified-list-template-static.spec.js` -> PASS。
GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
