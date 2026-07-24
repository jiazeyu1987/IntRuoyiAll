# Execution Log: Kingdee Material Sync Page Until Empty

BDD: Material sync pages until the API returns empty -> Given `BD_MATERIAL` page 1 and page 2 both contain rows and page 3 is empty / When the Kingdee material client runs the current query flow / Then it keeps requesting later pages until the empty page / And all rows from earlier non-empty pages are included in the final sync result instead of being truncated by a fixed total cap

RED: mvn -pl yudao-module-erp -Dtest=ErpKingdeeMaterialClientImplTest test -> FAIL, `fetchMaterials_continuesPagingUntilEmptyPage` expected 2 materials but received 1 because the old implementation treated `product.query-limit` as the total sync cap and stopped after the first page.
GREEN: mvn -pl yudao-module-erp -Dtest=ErpKingdeeMaterialClientImplTest test -> PASS
GREEN: mvn -pl yudao-module-erp "-Dtest=ErpKingdeeMaterialClientImplTest,ErpKingdeeProductSyncServiceImplTest" test -> PASS
