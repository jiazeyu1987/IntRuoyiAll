# Execution Log

BDD: Deleted product mapping is absent from release -> Given a showroom hall no longer has an active mapping to `product_002`, When manual publish materializes `website-index`, Then `product_002` must be absent from that release package.

BDD: Current published product rename is exported -> Given `product_001` current published revision name is `三通旋塞`, When manual publish materializes `website-index`, Then the product card must expose `三通旋塞`.

BDD: Public Website readback remains mandatory -> Given release materialization succeeds, When Website public scoped current cannot return the same JSON release, Then manual publish must fail instead of reporting success.

START: Created task record before code changes.

INSPECT: `ShowroomReleaseAssembler.resolveSourceSnapshot` materializes hall product cards from `contentService.listHalls()` and `ShowroomHall.productMappings()`, then resolves current product revisions. Therefore stale `showroom_hall_product` rows are exported into the release package.

INSPECT: `ShowroomApiRuntime.importProductExcel` previously updated product revisions from the Excel rows but did not replace hall product mappings from the imported product list. Therefore an Excel sheet that omits `product_002` could still publish `product_002` if the hall mapping table still contained it.

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceHallMappingsFromReplacementProductList" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: old import behavior left the hall mapped to both `product_001` and `product_002` instead of replacing the mapping with only `product_001`.

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceHallMappingsFromReplacementProductList" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests.

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomReleaseWebsiteIndexAssemblyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests. Regression asserts `website-index` contains `product_001`, name `三通旋塞`, and no `product_002`.

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomReleaseWebsiteIndexAssemblyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.

GREEN: `python -X utf8 "C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py" --evidence "doc/tasks/20260529-showroom-publish-source-alignment/backend-api-evidence.md"` -> PASS.

GREEN: `git diff --check -- "doc/tasks/20260529-showroom-publish-source-alignment" "yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/ShowroomApiRuntime.java" "yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/integration/ShowroomProductExcelImportExportIntegrationTest.java" "yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/release/ShowroomReleaseWebsiteIndexAssemblyTest.java"` -> PASS.

CLEANUP: `python -X utf8 "C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py" --task-id 20260529-showroom-publish-source-alignment --mode preview` -> PASS, keep task docs and backend evidence, delete `<none>`.
