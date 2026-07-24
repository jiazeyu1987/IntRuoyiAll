# Execution Log: Enable live Kingdee purchase-order sync

BDD: Use existing local K3Cloud secrets without duplicate repository config -> Given the local IntPP backend `.env` already contains the working K3Cloud endpoint, account, username, password, LCID, and purchase-order query settings, When IntRuoyi starts with the local backend profile, Then the `同步金蝶` sync path reads those values and does not fail immediately on missing `yudao.erp.kingdee.base-url`.

BDD: Auto-provision supplier from Kingdee source data -> Given a Kingdee purchase order references a supplier number that has no explicit static mapping and no local ERP supplier yet, When synchronization runs, Then IntRuoyi creates one enabled local supplier and records the Kingdee supplier-number mapping before creating the purchase order.

BDD: Auto-provision product master data from Kingdee material metadata -> Given a Kingdee purchase-order line references a material that has no explicit static mapping and no local ERP product/unit/category yet, When synchronization runs, Then IntRuoyi fetches the material metadata from Kingdee, creates the required unit/category/product records from real source fields, and uses them to create the purchase-order line.

BDD: Accept host-style Kingdee base URLs -> Given the runtime K3Cloud base URL is stored as `http://host` without a trailing `/K3Cloud`, When the sync client builds service URLs, Then it sends requests to `/K3Cloud/<service>.kdsvc` and not to the host root.

## Evidence

- M1: Completed. Previous backend task `20260512-ai-model-route-codex-cli` was checked and already marked completed before this task started.
- M2: Completed. This task directory and task document were created before backend production code changes.
- M3: Completed. Live backend logs, local database counts, local IntPP `.env`, and direct K3Cloud responses were inspected to capture the full blocker chain.
- RED: `python doc\tasks\20260513-kingdee-purchase-order-sync-live-enable\verify_local_kingdee_config.py` -> FAIL, expected reason: `application-local.yaml` did not yet import the IntPP `.env` file or bridge the required `yudao.erp.kingdee.*` properties.
- RED: `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeePurchaseOrderSyncServiceImplTest,ErpKingdeePurchaseOrderClientImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: the live-enable regression tests referenced new supplier sync mapping and material-detail support that production code did not yet provide.
- GREEN: `python doc\tasks\20260513-kingdee-purchase-order-sync-live-enable\verify_local_kingdee_config.py` -> PASS.
- GREEN: `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeePurchaseOrderSyncServiceImplTest,ErpKingdeePurchaseOrderClientImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests run, 0 failures, 0 errors.
- GREEN: `curl -X POST http://localhost:48081/admin-api/erp/purchase-order/sync-kingdee` (with real tenant and bearer token) -> PASS, first live run returned `createdCount=297` and `skippedCount=0`.
- GREEN: `curl -X POST http://localhost:48081/admin-api/erp/purchase-order/sync-kingdee` (with real tenant and bearer token) -> PASS, second live run returned `createdCount=0`, `skippedCount=297` in about 4.5 seconds.
- GREEN: Playwright real-user verification on `http://localhost:8081/erp/purchase/order` -> PASS, login succeeded, the `同步金蝶` button was present and clickable, and the purchase-order page remained stable with 297 synced records visible after the regression run.
