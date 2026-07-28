# Verification Report

## Summary

Status: completed.

The requested business behavior is implemented and both direct and standard targeted Maven tests pass. T4/T5/T6 regression clusters have been repaired and verified. The former Sheet1 authoritative Excel fixture blocker was resolved by the user-approved scope change that real Sheet1 Excel sample coverage is not required. The final complete MES module run passed with 2530 tests, 0 failures, 0 errors, and 18 skipped.

## Verified Behavior

- Fill task notifications are sent to every user in `candidateUserSnapshot`.
- Review task notifications are sent per work task and use that task's own candidate snapshot.
- Repeated candidate IDs inside one snapshot are de-duplicated before sending station messages.
- Existing notification template code resolution and template parameters remain unchanged.
- No fallback to `assigneeUserId`, current login user, role ID, department ID, or empty success was introduced.

## Commands

RED:

`mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: FAIL. Expected multiple notification sends, actual implementation sent only to single assignee.

GREEN:

`mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=true"`

Result: PASS. 3 tests run, 0 failures, 0 errors.

Standard targeted lifecycle:

`mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: PASS. 3 tests run, 0 failures, 0 errors.

Stale-blocker revalidation on the current shared branch:

`mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result at 2026-07-27 20:40:03: PASS. 3 tests run, 0 failures, 0 errors, 0 skipped; all 24 reactor modules reported `BUILD SUCCESS`.

Regression:

`mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=true"`

Result: PASS. 66 tests run, 0 failures, 0 errors.

Compile:

`mvn -pl yudao-module-mes -am "-DskipTests" compile`

Result: PASS.

Diff check:

`git diff --check -- <task-owned files>`

Result: PASS, with Git line-ending conversion warnings only.

## Historical Blocker

Earlier `mvn -pl yudao-module-mes -am test` failed in upstream `yudao-module-infra` with 415 tests, 38 failures, 1 error, and 10 skipped; Maven skipped MES.

`mvn -pl yudao-module-mes test` then ran for 15 minutes and timed out without producing a fresh complete Surefire report. The task-owned Maven process was stopped after verifying its command line and PID.

The same command was rerun with a longer finite timeout and completed after 38:34 at 2026-07-27 20:17:20. Result: FAIL, 2509 tests run, 58 failures, 78 errors, 31 skipped. Failures included scheduling contracts, missing local Word/Excel fixtures, database test context failures, and other module tests. In that same full run, `MesProEdhrWorkTaskServiceImplTest` passed all 66 tests with 0 failures and 0 errors.

After T4/T5/T6 remediation, `mvn -pl yudao-module-mes test` was rerun and completed at 2026-07-28 01:15:20 +08:00. Result: FAIL, 2511 tests run, 0 failures, 4 errors, 18 skipped. All four errors were the missing Sheet1 authoritative Excel fixture in `Sheet1RouteExcelParserTest`, `Sheet1RouteExcelImportServiceImplTest`, and `Sheet1RouteExcelImportServiceImplDbTest`.

Workspace search `rg --files -g "*球囊扩张导管工序*"` under `E:\IntRuoyi` found no project-owned authoritative Excel fixture. A desktop candidate with SHA-256 `A7ACF4ADE2E09A00B68D80701B1FB86BC79B6F3CCDA55504B7C838AB85240354` remains unconfirmed, so it must not be copied, renamed, substituted, synthesized, or used to skip tests.

The three Sheet1 tests now read `fixtures/sheet1-route-balloon-catheter.xlsx` through `Sheet1RouteExcelTestFixtures` instead of the prior `D:\ocr2` personal path. Targeted rerun at 2026-07-28 01:23:39 +08:00 compiled 306 test sources and failed only with `NoSuchFileException: src/test/resources/fixtures/sheet1-route-balloon-catheter.xlsx` across the same four fixture-backed methods.

The latest full MES rerun completed at 2026-07-28 01:30:40 +08:00 with `2511 tests`, `0 failures`, `4 errors`, and `18 skipped`. All four errors are the same missing project fixture `src/test/resources/fixtures/sheet1-route-balloon-catheter.xlsx`; no other suite currently reports a failure or error.

## Scope Change Resolution

User decision: Sheet1 Excel real fixture coverage is not required for this task. The prior missing fixture blocker is therefore removed from the acceptance gate by explicit scope change, not by fallback.

Implementation adjustment:

- Removed `Sheet1RouteExcelImportServiceImplTest`, `Sheet1RouteExcelImportServiceImplDbTest`, and `Sheet1RouteExcelTestFixtures`, which depended on the missing real Excel fixture.
- Removed only `Sheet1RouteExcelParserTest.parseFixture_returnsTwoRoutesWithFirstAppearanceDeduplicatedSteps`.
- Preserved the four synthetic parser fail-fast tests in `Sheet1RouteExcelParserTest`.

Guardrail review: no `@Disabled`, Maven excludes, assumptions, empty fixture, desktop candidate fixture, or synthetic workbook was used to masquerade as the removed real fixture coverage.

## Final Verification

Targeted Sheet1 parser check:

`mvn -pl yudao-module-mes "-Dtest=Sheet1RouteExcelParserTest" test`

Result: PASS. 4 tests run, 0 failures, 0 errors, 0 skipped.

Complete MES regression:

`mvn -pl yudao-module-mes test`

Result at 2026-07-28 08:53:25 +08:00: PASS. 2530 tests run, 0 failures, 0 errors, 18 skipped, `BUILD SUCCESS`.

## Completion Gate

Implementation, required verification, cleanup preview/apply, final commit, and push are complete.

## Integration State

Concurrent baseline commit `f18927b9e3682a8a66d44d535b24c75b824b40e2` already contains the Java implementation, regression tests, and the initial task documents. Final closeout evidence is recorded in this task directory; `HEAD` and `origin/int_main` are aligned and the working tree is clean.
