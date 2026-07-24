# Task: showroom hall product options single query

## Goal

Add a dedicated lightweight admin API for hall product candidate loading so the frontend can fetch all candidate products for `维护产品` in one request instead of paging through `/showroom/product/page`. The endpoint must return real product ids, names, revision info, and related hall ids without changing the existing hall mapping write contract.

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\service\ShowroomContentOperations.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\service\ShowroomPersistentContentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\service\ShowroomContentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\dal\mysql\content\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\doc\tasks\20260522-showroom-hall-product-options-single-query\**`

## Non-Scope

- Do not change `/showroom/hall/update-product-mapping`.
- Do not modify database schema.
- Do not add fallback or compatibility shims.

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\doc\tasks\20260522-batch-cover-ok-showroom-cover\task.md`
- Status before this task: `Completed`
- Impact: The latest same-repo showroom task is completed and does not block this API slice.

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: the repo contains many unrelated in-progress changes, including existing modifications under `yudao-module-showroom`
- Impact: this task must keep its write set narrowly scoped to the dedicated hall product options API and direct tests/docs

## Milestones

- [x] M1: Check the previous task state and create the backend task document
- [x] M2: Add a failing backend regression/contract test for the dedicated hall product options API
- [x] M3: Implement the minimal lightweight API behavior
- [x] M4: Run targeted backend verification and record evidence

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#hallProductOptionsShouldReturnCandidateRowsWithHallIdsInSingleCall" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Current Status

Completed on 2026-05-22.

## Verification Summary

- PASS: `mvn -pl yudao-module-showroom clean "-Dtest=ShowroomHttpApiIntegrationTest#hallProductOptionsShouldReturnCandidateRowsWithHallIdsInSingleCall" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: live `http://127.0.0.1:48081/admin-api/showroom/hall/product-options` runtime verification against the restarted local process
