# Execution Log: DCC Training-Rules Real-Content E2E

BDD: training-rules page shows the real selected category -> Given an
administrator logs in through the real frontend path and the live category list
returns at least one active category, When the user opens the DCC training page
and selects a category with training rules, Then the page category selector
shows that real category and the requirement-warning state matches the live
`trainingRequired` flag from `/admin-api/dcc/file-categories`.

BDD: training-rules page persists and shows real training-rule rows -> Given an
active category and at least one live department are available, When the user
opens the DCC training page and adds a training department through the real
page for a category that currently has no rules, Then the page persists the new
rule, reloads the same row count as
`/admin-api/dcc/file-categories/{id}/training-rules`, shows the real
department name and active flag using `/admin-api/system/dept/simple-list`, and
can restore the original empty state afterward.

BDD: missing runtime prerequisites fail loudly -> Given the runtime lacks an
active category, a department choice, or required department lookup data, When
the verification runs, Then it fails with the exact missing prerequisite and
does not mark the page content as covered.

RED: `Test-Path D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-rules-real-content-e2e\scripts\verify-dcc-training-rules-real-content-e2e.mjs`
-> FAIL, returned `False`, proving the repository did not yet contain a
dedicated DCC training-rules real-content Playwright verification.

RED: direct runtime probe with fresh real admin login -> FAIL for preexisting
row-level content, because `tenant-id=1` currently returns
`{"activeCategoryCount":48,"deptCount":12,"matchingCategoryCount":0,"trainingRequiredTrueCount":0}`.
The live runtime has active categories and departments, but zero categories
with training-rule rows and zero categories with `trainingRequired=true`, so
the new E2E must establish minimal real page content before row-level
assertions can be truthful.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-rules-real-content-e2e\scripts\verify-dcc-training-rules-real-content-e2e.mjs`
-> PASS, the real frontend path reached `/dcc/controlled-file/training`,
selected live category `产品技术要求`, confirmed the warning state for
`trainingRequired=false`, and matched one real persisted training-rule row for
department `瑛泰源码` with `active=true`.

RED: direct cleanup attempt to restore category `1` training rules to an empty
array -> FAIL, `PUT /admin-api/dcc/file-categories/1/training-rules` with `[]`
returned `code=500` and `NoClassDefFoundError:
org/hibernate/validator/internal/engine/ValidatorImpl$CascadingValueReceiver`.
This is a backend cleanup gap, not a read-side verification failure.
