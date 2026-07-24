# Execution Log: MES item master Kingdee sync backend

BDD: MES item sync creates and updates mirrored product rows -> Given Kingdee returns material master rows, When MES item sync runs, Then MES product items are created or updated by material code and shared fields mirror Kingdee.

BDD: MES item sync disables missing local rows -> Given a MES item exists whose code is absent from the current Kingdee material list, When MES item sync runs, Then the MES item is disabled and retained.

BDD: MES item sync preserves MES-specific fields -> Given a MES item already has local inventory-control settings, When Kingdee sync updates that item, Then MES-specific fields remain unchanged while shared fields are refreshed.

RED: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeItemSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `MesKingdeeItemSyncServiceImpl` did not exist yet, so the MES Kingdee item sync feature could not compile.

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeItemSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, the sync service now creates missing rows, updates mirrored rows, preserves MES-only fields, and disables missing rows.

GREEN: real page-triggered `POST /admin-api/mes/md/item/sync-kingdee` -> PASS, first run returned `createdCount=2735`, `updatedCount=0`, `disabledCount=731`, `skippedCount=1`.

GREEN: real page-triggered rerun `POST /admin-api/mes/md/item/sync-kingdee` -> PASS, second run returned `createdCount=0`, `updatedCount=0`, `disabledCount=0`, `skippedCount=3467`.
