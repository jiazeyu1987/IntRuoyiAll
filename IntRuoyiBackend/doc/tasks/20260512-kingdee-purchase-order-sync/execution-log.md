# Execution Log: Kingdee purchase order synchronization

BDD: Sync new Kingdee purchase order -> Given a configured Kingdee K3Cloud source returns a purchase order with source `FormId + FID` and all referenced supplier/product master data mappings exist in IntRuoyi, When an administrator triggers synchronization, Then IntRuoyi creates one purchase order and records the source mapping.

BDD: Skip already synchronized purchase order -> Given a sync record already exists for the same source `FormId + FID`, When synchronization runs again, Then IntRuoyi does not create a duplicate purchase order and reports the skipped source record.

BDD: Fail fast on missing Kingdee configuration -> Given the Kingdee endpoint or credentials are missing, When synchronization is requested, Then the backend fails with a clear business error and does not call Kingdee or create a purchase order.

BDD: Persist idempotency constraint -> Given the sync-record table exists, When two records use the same source `FormId + FID`, Then the database rejects the duplicate source key.

## Evidence

- M1/M2: Completed. Backend task document and BDD scenarios were created in the backend repository before production code changes.
- RED: `mvn "-Dtest=ErpKingdeePurchaseOrderSyncServiceImplTest,ErpKingdeePurchaseOrderClientImplTest,ErpKingdeePurchaseOrderSyncRecordMapperTest" test` -> FAIL, expected reason: production sync service, Kingdee client, sync record DO, and sync record mapper do not exist yet.
- GREEN: `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeePurchaseOrderSyncServiceImplTest,ErpKingdeePurchaseOrderClientImplTest,ErpKingdeePurchaseOrderSyncRecordMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests run, 0 failures, 0 errors.
- GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true" package` -> PASS, reactor build success through `yudao-server`.
