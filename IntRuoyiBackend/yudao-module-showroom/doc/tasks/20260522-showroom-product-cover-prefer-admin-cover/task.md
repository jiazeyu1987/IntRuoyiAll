# Task: showroom product cover prefer admin cover

## Goal

Make the anonymous showroom display APIs return the admin product cover image as the primary product display image so Website product cards and product detail hero images match the cover shown in the IntRuoyi product editor.

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\doc\tasks\20260522-showroom-product-cover-prefer-admin-cover\**`

## Non-Scope

- Do not change hall or company display image behavior.
- Do not add fallback or compatibility branches.
- Do not change Website rendering logic unless the backend contract shape changes.

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\doc\tasks\20260522-showroom-hall-product-options-single-query\task.md`
- Status before this task: `Completed`
- Impact: The previous showroom module task is completed and does not block this product-display contract fix.

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: the repo contains unrelated local edits and generated task artifacts.
- Impact: this task must keep its write set limited to the anonymous showroom runtime, direct regression tests, and its own task records.

## Milestones

- [x] M1: Check previous task state and create this task record.
- [x] M2: Add a failing regression test proving product display should prefer the admin cover image.
- [x] M3: Implement the minimal runtime fix.
- [x] M4: Run targeted verification and record evidence.

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/showroom/display/website-config`
- `http://127.0.0.1:4173/`

## Current Status

- Status: Completed
- Completed work:
  - Confirmed the admin product editor stores the user-expected image in product field `cover_image`.
  - Confirmed the anonymous showroom runtime currently prioritizes product preview assets over `cover_image`.
  - Confirmed this priority causes Website product cards and detail hero images to diverge from the admin product cover.
  - Added regression test `websiteConfigAndHallDisplayShouldPreferAdminProductCoverImageOverPreviewAsset` and captured RED failure against the old priority.
  - Implemented the minimal showroom runtime fix so product display images now prefer admin `cover_image`, while products without a cover continue to use the existing preview asset path.
  - Updated the bilingual product contract test to assert the new image priority.
  - Passed targeted integration tests, rebuilt `yudao-server.jar`, restarted the local runtime, and verified the real Website root path now renders the admin cover for both the first product card and the product detail hero image.
- Remaining blockers:
  - None.
