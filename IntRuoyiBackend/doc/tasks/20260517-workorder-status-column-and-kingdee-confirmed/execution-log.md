# Execution Log: 生产工单金蝶新增默认已确认后端

BDD: kingdee_new_workorders_default_to_confirmed -> Given a Kingdee production order does not already exist locally, When the sync service creates the MES work order, Then the new work order should be persisted as `已确认` regardless of the incoming ERP production status.

BDD: kingdee_existing_workorders_remain_skipped -> Given the same Kingdee source record or work-order code already exists locally, When the sync service runs, Then it should still skip creation and not create duplicate work orders.

RED: pre-change behavior -> FAIL, the previous sync implementation could map newly created work orders to statuses other than `已确认` depending on the Kingdee status mapping path.

GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, new sync work orders are persisted as `已确认`.
