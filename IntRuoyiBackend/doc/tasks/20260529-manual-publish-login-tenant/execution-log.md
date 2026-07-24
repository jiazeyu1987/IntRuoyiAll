# Execution Log

BDD: manual publish uses the current login tenant as the source tenant -> Given the admin user is logged into tenant A and the public site binding currently points to tenant B / When the admin triggers manual showroom publish for the same `siteKey + stage` / Then the published release must be built from tenant A data.

BDD: manual publish updates the public site binding to the current login tenant -> Given the admin user is logged into tenant A and publishes `siteKey + stage` / When the publish succeeds / Then the public site binding for that `siteKey + stage` must point to tenant A so the Website current-release route resolves the same data that was just published.

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` in the main repository -> FAIL, unrelated dirty-repo compile blockers in `yudao-module-infra` and legacy showroom test imports prevented the new publish-scope tests from reaching execution.

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` in clean worktree `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260529-showroom-release-truth-implementation\ruoyi-vue-pro` -> PASS, `9` tests passed including:
- `publishReleaseShouldUseCurrentLoginTenantInsteadOfExistingBindingTenant`
- `publishReleaseShouldCreateBindingForCurrentLoginTenantWhenMissing`

GREEN: real local backend publish with `芋道源码/admin` -> PASS, after deploying the same source changes into the main repository runtime and publishing `siteKey=yingtai-showroom, stage=TEST`, the binding row changed from tenant `122` to tenant `1`, and both backend and Website current-release routes resolved release `20260529T143353Z-9968d48ea3c2`.

GREEN: real published Website payload check -> PASS, `http://127.0.0.1:4173/showroom/sites/yingtai-showroom/stages/TEST/release/current` returned the latest release, and its `website-index` for `hall_01` contained only `product_001 / 三通旋塞`, with no `product_002`.
