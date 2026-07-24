# Execution Log: 生产工单列表隐藏工单类型和单位并增加完成时间

BDD: workorder_list_hides_type_and_unit -> Given a real user opens the MES production work-order list, When the table headers render, Then `工单类型` and `单位` should be absent.

BDD: workorder_list_shows_finish_time -> Given a real user opens the MES production work-order list, When the table headers render, Then `完成时间` should appear and display the work-order finish date field.

BDD: workorder_list_keeps_status_in_customer_code_slot -> Given a real user opens the MES production work-order list, When the table headers render, Then `工单状态` should still occupy the former customer-code slot.

RED: pre-change behavior -> FAIL, the live list still showed `工单类型` and `单位`, and there was no `完成时间` column.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-status-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-workorder-list-hide-type-unit-add-finish-time\scripts\verify-workorder-list-hide-type-unit-add-finish-time.mjs` -> PASS, headers became `工单编码, 工单名称, 产品编号, 产品名称, 规格型号, 工单数量, 已生产数量, 工单状态, 客户名称, 需求日期, 完成时间, 创建时间, 操作`.
