# Execution Log: showroom product cover prefer admin cover

BDD: anonymous showroom product display should prefer the admin product cover image -> Given a product has both an approved preview asset and a saved admin `cover_image` / When Website reads the anonymous showroom display APIs for gallery cards and product detail hero images / Then the returned product display image should match the admin `cover_image` rather than the preview asset image

INFO: Product editor evidence shows the user-expected product cover is stored in the product revision `cover_image` field.
INFO: Current anonymous showroom runtime returns `previewImageUrl` from the published product preview asset when it exists, so Website truthfully renders the wrong image for this requirement.

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `previewImageUrl` still returned `/admin-api/infra/file/28/get/showroom/preview/product-cover-priority-preview.png` instead of admin `cover_image`.
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#narrationGetAndDisplayPayloadShouldExposePersistedLiveAssets+websiteConfigProductShouldExposeBilingualBasicFieldsAndKeepAdvancedFieldsExcluded+websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server.jar` with the showroom image-priority change.
GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS, local runtime restarted onto the rebuilt jar.
GREEN: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config` -> PASS, first public product now returns `/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png`.
GREEN: real Playwright root-path verification -> PASS, `http://127.0.0.1:4173/` first product card and product detail hero image both resolve to `/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png`.
