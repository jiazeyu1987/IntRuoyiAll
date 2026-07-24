# Execution Log: Kingdee product master sync action

BDD: Sync Kingdee product master data -> Given an operator is on the ERP product page and Kingdee access is configured, When the operator clicks `同步金蝶`, Then IntRuoyi should sync approved Kingdee material master data into ERP product master records and return created/updated/skipped counts.

BDD: Preserve invalid upstream filtering -> Given Kingdee material master rows may contain blank codes or unapproved records, When the sync queries Kingdee, Then the backend should only process rows with non-blank material codes and approved document status.

INFO: Real Kingdee `BD_MATERIAL` sampling confirmed usable field keys:
- `FNumber`
- `FName`
- `FSpecification`
- `FCategoryID.FNumber`
- `FCategoryID.FName`
- `FBaseUnitId.FName`
- `FForbidStatus`
- `FDocumentStatus`

INFO: Real Kingdee material-master sampling with filter `(FNumber <> '') and (FDocumentStatus = 'C')` returned approved rows and current status distribution in the sampled pages:
- `FForbidStatus='A', FDocumentStatus='C'` => `4928`
- `FForbidStatus='B', FDocumentStatus='C'` => `72`

GREEN: `mvn -pl yudao-module-erp -am "-Dtest=ErpKingdeeMaterialClientImplTest,ErpKingdeeProductSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS

GREEN: live `POST /admin-api/erp/product/sync-kingdee` with local `tenant-id: 1` and `admin/admin123` token -> PASS, `createdCount=2169`, `updatedCount=451`, `skippedCount=116`, elapsed about `5.02s`
