# Execution Log

## BDD

- BDD: company current contract enrichment -> Given company content has live or latest draft metadata, When `/showroom/company/current` is queried, Then the response must expose the company metadata and current revision details instead of a bare revision snapshot.
- BDD: product content query detail contract -> Given products have ownership, lifecycle, approval, and revision metadata, When `/showroom/product/page` and `/showroom/product/get` are queried, Then the response must include the richer content view needed by admin and not only a simplified snapshot list.
- BDD: display fields use human labels -> Given display payload fields are rendered for company and product pages, When `/showroom/display/company`, `/showroom/display/hall/{hallId}`, or `/showroom/display/product/{productId}` is queried, Then field labels must be human-readable and must not echo raw field codes.
- BDD: incomplete product remains displayable -> Given a product has no approved live revision yet but has a draft revision, When `/showroom/display/product/{productId}` is queried, Then the product must still render with an incomplete indicator instead of failing fast on missing live content.
- BDD: preview image url follows file contract -> Given a live preview asset exists for a display target, When a display payload is built, Then `previewImageUrl` must resolve through the real file access contract rather than remain blank.

## RED

- RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `adminCompanyCurrentShouldExposeCompanyMetadataAlongsideRevision` reported missing accessor `companyType()` on `CompanyCurrentRespVO`.
- RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `adminProductPageShouldExposeDetailedRowsAndHonorContentFilters` reported missing extended `PageQueryReqVO(String, Integer, Integer, String, String, String, String, String)` content-filter contract.
- RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `displayShouldKeepIncompleteProductsVisibleAndUseHumanReadableLabels` threw `SHOWROOM_TARGET_NOT_FOUND: live product revision not found` for an incomplete draft-only product.
- RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `displayShouldBuildPreviewUrlsFromLiveFileContractAndHideRawFieldCodes` reported missing runtime preview-asset seeding hook and still lacked human-readable display labels.
- RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `adminProductHistoryShouldExposeGroupedRevisionDiffs` still returned flat audit rows (`expected: <2> but was: <6>`) instead of revision-grouped diff metadata.

## GREEN

- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest,ShowroomCompanyContentTest,ShowroomProductContentTest,ShowroomHallContentTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

## Blockers

- No functional blocker remains.
- Closeout blocker: the shared showroom worktree already contains concurrent non-B2 edits in overlapping controller/workflow files, so a task-only Git commit was not created in this turn to avoid mixing unrelated changes.
