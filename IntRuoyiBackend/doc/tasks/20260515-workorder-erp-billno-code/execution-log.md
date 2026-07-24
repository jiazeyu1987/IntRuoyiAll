BDD: sync_workorder_code_uses_erp_bill_no -> Given a Kingdee production order with bill number `881MO091049`, When the MES sync service creates a work order from that payload, Then the persisted MES work-order code should equal `881MO091049`.

BDD: sync_preserves_source_doc_code -> Given the same Kingdee payload, When the MES sync service builds the create request, Then `orderSourceCode` remains the ERP bill number so existing source tracking stays intact.

RED: `mvn --% -pl yudao-module-mes -am -Dtest=MesKingdeeProductionOrderSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `MesKingdeeProductionOrderSyncServiceImplTest.syncWorkOrders_autoCreatesItemUnitTypeAndWorkOrder` asserted `expected: <881MO091049> but was: <KDMO-310119-1558165540>`.

GREEN: `mvn --% -pl yudao-module-mes -am -Dtest=MesKingdeeProductionOrderSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, target sync test suite completed with `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.
