# Execution Log: 金蝶生产工单同步遇到重复编码时跳过继续

BDD: 重复工单编码应被跳过 -> Given 金蝶生产订单中存在本地已存在编码的订单, When 同步金蝶生产工单, Then 该订单应被跳过且同步流程继续处理后续订单。

RED: `POST /admin-api/mes/pro/work-order/sync-kingdee` -> FAIL, 当前实现会抛出 `生产工单编码已存在` 并中断整批同步。

RED: `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest#syncWorkOrders_skipsLaterRowsWithSameBillNoInSingleBatch test` -> FAIL, 同批次重复 `billNo` 仍会创建第二条工单并触发断言失败。

GREEN: `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest test` -> PASS, 5 个回归用例全部通过。

GREEN: `POST /admin-api/mes/pro/work-order/sync-kingdee` -> PASS, 返回 `code=0`，`createdCount=806`，`skippedCount=194`。

GREEN: live DB check -> PASS, `mes_kingdee_production_order_sync_record=806` 且 `mes_pro_work_order=808`。
