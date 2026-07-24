# Execution Log: 修复 ERP 同步 BOM 后工单详情乱码

BDD: workorder_bom_sync_displays_normalized_item_text -> Given ERP product and MES item master data are synchronized from Kingdee materials, When the synced work-order BOM detail loads, Then item names and specifications display normalized readable Chinese text instead of mojibake.

BDD: kingdee_material_sync_normalizes_mojibake_text -> Given the Kingdee material query returns text that was UTF-8 bytes misread as Latin-1 and then persisted as JSON text, When the backend parses and syncs that material, Then the persisted ERP product and derived MES item use normalized UTF-8 Chinese text.

BDD: existing_garbled_master_data_can_be_repaired_without_bom_resync_logic_change -> Given work order `903245` already references local master-data rows with garbled text, When the targeted data repair runs after the normalization fix, Then the existing work-order BOM detail resolves the corrected Chinese fields without changing BOM quantities or work-order BOM row ownership.

RED: read-only SQL reproduction -> FAIL, `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT wob.id, wob.item_id, i.code, i.name, i.specification FROM mes_pro_work_order_bom wob LEFT JOIN mes_md_item i ON i.id = wob.item_id WHERE wob.work_order_id = 903245 AND wob.deleted = 0 ORDER BY wob.id DESC LIMIT 30;"` showed mojibake for local item codes `A002.09.001.000021` and `A002.11.001.000012`.

RED: read-only source-of-truth SQL -> FAIL, `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT bar_code, name, standard FROM erp_product WHERE bar_code IN ('A002.09.001.000021','A002.11.001.000012'); SELECT code, name, specification FROM mes_md_item WHERE code IN ('A002.09.001.000021','A002.11.001.000012');"` confirmed both ERP product rows and MES item rows are already stored with garbled text.

RED: `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeMaterialClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the new regression test expected `合格证（内贸INT）`, but the parsed material name still remained garbled before the normalization fix.

GREEN: `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeMaterialClientImplTest,ErpKingdeeProductSyncServiceImplTest,ErpKingdeeBomClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, targeted ERP module regression suite passed with the normalization fix in place.

GREEN: targeted live master-data repair -> PASS, SQL updates converted the reproduced `erp_product`, `mes_md_item`, `erp_product_unit`, and `mes_md_unit_measure` rows from mojibake back to readable Chinese for `A002.09.001.000021` and `A002.11.001.000012`.

GREEN: Playwright UI verification -> PASS, `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-bom-garbled-fix run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260518-workorder-erp-bom-garbled-item-fix\scripts\verify-workorder-bom-garbled-fix.mjs` opened work order `903245` and verified both detail tabs render the repaired Chinese names/specifications, with the `A002.09.001.000021` unit restored to `张`.
