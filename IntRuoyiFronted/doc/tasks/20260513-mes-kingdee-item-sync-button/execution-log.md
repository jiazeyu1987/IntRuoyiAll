# Execution Log: MES item master Kingdee sync frontend

BDD: MES item page exposes Kingdee sync -> Given the MES item master page is open, When the user clicks `同步金蝶`, Then the page sends the MES item sync request and refreshes the visible data.

BDD: MES item page defaults to enabled items -> Given the MES item master page is opened or reset, When the list loads, Then the status filter defaults to enabled items.

RED: `Get-Content src/views/mes/md/item/index.vue -TotalCount 260` -> FAIL, the MES item master page had no `同步金蝶` action and its initial `status` filter was undefined instead of enabled.

GREEN: `pnpm exec eslint src/api/mes/md/item/index.ts src/views/mes/md/item/index.vue` -> PASS, the new MES sync API binding and page updates are lint-clean.

GREEN: fresh Playwright session `mes-item-sync-2` -> PASS, login with tenant `芋道源码`, username `admin`, password `admin123` reached the MES item master page and showed the new `同步金蝶` button with the default status filter displaying `开启`.

GREEN: first real page click on `同步金蝶` -> PASS, browser request `POST /admin-api/mes/md/item/sync-kingdee` returned `200` and the page toast showed `新增 2735 条，更新 0 条，停用 731 条，跳过 1 条`.

GREEN: second real page click on `同步金蝶` -> PASS, browser request rerun returned `createdCount=0`, `updatedCount=0`, `disabledCount=0`, `skippedCount=3467`.
