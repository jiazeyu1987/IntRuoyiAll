# 20260529-showroom-publish-source-alignment

## Task Goal

Fix the showroom manual publish source alignment so a publish cannot silently export hall-product mappings or product names that do not match the operator's effective published showroom data, especially `product_002` deletion and `product_001` rename expectations.

## Milestones

- [x] Create task record before code changes.
- [x] Inspect admin save/delete/publish APIs and release source assembly.
- [x] Add failing regression coverage for deleted product mapping and product rename publish behavior.
- [x] Implement the smallest backend/API fix.
- [x] Verify scoped release package and deployment/loading guards.
- [x] Record RED/GREEN evidence and run closeout cleanup preview.

## Expected Verification

- A deleted or inactive hall-product mapping is not materialized into `website-index`.
- A product rename that is the current published revision is materialized into `website-index`.
- Manual publish fails fast if the operator-facing source and release source cannot be reconciled.
- Website public scoped current still must return JSON for the latest release before publish success.

## Current Status

Completed. Local verification passed.

## Completed Work

- Confirmed release assembly reads `showroom_hall_product` mappings and current product revisions.
- Changed product Excel import so `展柜名称` is an authoritative existing hall name for successful imports.
- On a fully successful product Excel import, replaced each imported hall's product mappings with exactly the products present in the sheet, in sheet order.
- Added fail-fast validation for unknown hall names, duplicate hall names, and duplicate product rows within one hall.
- Added import regression coverage proving an import without `product_002` removes it from the hall mapping and renames `product_001` to `三通旋塞`.
- Added release regression coverage proving `website-index` contains only `product_001`, uses `三通旋塞`, and does not contain `product_002`.

## Cleanup Keep

- doc/tasks/20260529-showroom-publish-source-alignment/backend-api-evidence.md

## Constraints

- No fallback success, silent skip, mock publish success, or compatibility shim.
- Do not mutate live IntRuoyi business data unless explicitly needed and recorded.
- Preserve unrelated working tree changes.

## Final Verification

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomReleaseWebsiteIndexAssemblyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests.
- `python -X utf8 "C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py" --evidence "doc/tasks/20260529-showroom-publish-source-alignment/backend-api-evidence.md"` -> PASS.
- `git diff --check -- "doc/tasks/20260529-showroom-publish-source-alignment" "yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/ShowroomApiRuntime.java" "yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/integration/ShowroomProductExcelImportExportIntegrationTest.java" "yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/release/ShowroomReleaseWebsiteIndexAssemblyTest.java"` -> PASS.
- `python -X utf8 "C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py" --task-id 20260529-showroom-publish-source-alignment --mode preview` -> PASS, delete `<none>`.
