# Bug Regression Evidence: showroom product cover prefer admin cover

## Bug Summary

Website product cards and product detail hero images did not match the product cover image shown in the IntRuoyi product editor. The public showroom runtime returned the published preview asset image even when the product revision already had a user-approved `cover_image`.

## Expected Behavior

When a product has a saved admin `cover_image`, the anonymous showroom display APIs should return that admin cover as the primary product display image so Website matches the back-office product cover.

## Reproduction

- Real user path: `http://127.0.0.1:4173/`
- Contract probe: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config`

## Root Cause

`ShowroomApiRuntime` prioritized the live product preview asset for both `website-config` products and `display/hall` product cards. The admin cover image in `revision.fields().get("cover_image")` was only used as a secondary path in one runtime branch, so Website truthfully rendered the wrong image for this requirement.

## Regression Test

- Added `ShowroomHttpApiIntegrationTest#websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset`

## RED:

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL, `previewImageUrl` returned `/admin-api/infra/file/28/get/showroom/preview/product-cover-priority-preview.png` instead of admin `cover_image`.

## GREEN:

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#narrationGetAndDisplayPayloadShouldExposePersistedLiveAssets+websiteConfigProductShouldExposeBilingualBasicFieldsAndKeepAdvancedFieldsExcluded+websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am -DskipTests package`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- Real verification: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config` now returns `/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png` for the first public product, and real Playwright verification against `http://127.0.0.1:4173/` shows the first product card and detail hero image both use that same path.

## Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#narrationGetAndDisplayPayloadShouldExposePersistedLiveAssets+websiteConfigProductShouldExposeBilingualBasicFieldsAndKeepAdvancedFieldsExcluded+websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am -DskipTests package`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- Real Website check: first product card and detail hero image on `http://127.0.0.1:4173/` both match `/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png`.

## Risk And Regression Scope

- Scope is limited to product display image selection in the anonymous showroom runtime.
- Hall and company image selection behavior is unchanged.
- Products without `cover_image` still use the existing preview asset path.

## Blockers And Follow-up

- None.
