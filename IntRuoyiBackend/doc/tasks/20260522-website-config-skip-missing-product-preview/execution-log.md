# Execution Log: 20260522-website-config-skip-missing-product-preview

BDD: website-config should continue returning valid products when one mapped product lacks a live preview -> Given a hall contains multiple mapped public products and one of them has no published live PRODUCT preview / When anonymous website-config aggregation builds showroom products / Then the missing-preview product should be skipped and the remaining valid products should still be returned
BDD: website-config should only skip the user-approved missing-preview case -> Given a mapped product fails for a reason other than missing live PRODUCT preview / When the aggregate builds products / Then the aggregate should still fail fast instead of silently hiding unrelated data errors

RED: current runtime probe `GET http://127.0.0.1:48081/showroom/display/website-config` -> FAIL, returns `SHOWROOM_TARGET_NOT_FOUND: live product preview asset is required`
INFO: current mapped dataset -> `165` active mapped products, `164` without published live PRODUCT preview, first failing product is `product_002(id=2)`
RED: `mvn -pl yudao-module-showroom clean "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigShouldSkipProductsWhoseLivePreviewAssetIsMissingInsteadOfFailingWholeAggregate+publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, before the change the aggregate stopped on the first missing-preview product instead of returning the later valid product
GREEN: `mvn -pl yudao-module-showroom clean "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigShouldSkipProductsWhoseLivePreviewAssetIsMissingInsteadOfFailingWholeAggregate+publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, missing-preview product is skipped, valid product remains, and direct-publish preview sync regression still passes
GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS
GREEN: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS
GREEN: real runtime probe `GET http://127.0.0.1:48081/showroom/display/website-config` -> PASS, returned `code=0 / showrooms=8 / total_products=1`
