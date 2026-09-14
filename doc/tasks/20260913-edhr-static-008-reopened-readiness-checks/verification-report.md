# Verification Report

## Scope

EDHR-STATIC-008 reopened readiness static logic only.

## Bug

Reopened EDHR-STATIC-008 readiness checks incorrectly rejected valid multi-project / multi-item PQC task sets and allowed CONFIRMED tasks to pass without formal inspection-result evidence.

## Expected Behavior

Readiness static checks accept valid multi-QA-item and dedicated/common regulation task sets, but PASS only when formal PQC submission, per-piece judgement, process-inspection aggregation, and required nonconformance disposition evidence are complete.

## Reproduction

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest#evaluateInspectionResultPassesWithMultipleQaItemsAndDedicatedCommonRegulations+evaluateInspectionResultBlocksWhenConfirmedTaskLacksFormalInspectionResultEvidence" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL as expected before implementation.

## Root Cause

- Duplicate task detection used a coarse identity that omitted QA process, QA item, regulation version, inspection rule key, and business date.
- Final-inspection applicability treated more than one regulation version for a production process as invalid, although dedicated plus common sources are valid.
- Inspection-result PASS was based on `CONFIRMED` task status and identity coverage only, without validating formal submitted PQC records, per-piece judgement, aggregation detail, or failed-result disposition closure.

## Results

- Targeted regression/static test RED: FAIL as expected before implementation.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest#evaluateInspectionResultPassesWithMultipleQaItemsAndDedicatedCommonRegulations+evaluateInspectionResultBlocksWhenConfirmedTaskLacksFormalInspectionResultEvidence" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 15, Failures: 0, Errors: 0, Skipped: 0.
- REGRESSION FINAL: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS after dead-code cleanup, Tests run: 15, Failures: 0, Errors: 0, Skipped: 0.
- REGRESSION MERGED BASE: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS after applying the fix on latest `origin/int_main`, Tests run: 15, Failures: 0, Errors: 0, Skipped: 0.
- `git diff --check`: PASS for final tracked changes.
- Branch runtime port guard: PASS for `codex/20260913-edhr-static-008-reopened-readiness-checks/int_main`, slot 7, frontend 8088, backend 48088.
- Implementation commit: `64bc19727b600f1d0a7ebcb6ac6e6c9985ab55b2`.
- Bug regression evidence validator after latest-base record update: PASS.
- Cleanup preview/apply after latest-base record update: PASS; kept `task.md`, `execution-log.md`, and `verification-report.md`; deleted none.
- Static changed-path review: PASS; changed production/test paths are limited to EDHR-STATIC-008 readiness implementation and regression test, with task records plus required project experience consolidation docs.
- E2E: not run by explicit user instruction.

## Verification

- Targeted non-E2E verification passed.
- Related service test class regression passed.
- Final related service test class regression after dead-code cleanup passed.
- Latest `origin/int_main` merged-base regression passed before Git closeout.
- Whitespace diff check passed.
- Bug regression evidence validator passed.
- Latest-base record update evidence validator passed.
- Cleanup preview/apply passed with worktree Git closeout disabled by task constraint; no files were deleted.
- Latest-base cleanup preview/apply passed with worktree Git closeout disabled; no files were deleted.

## Blockers

- None for implementation, non-E2E verification, and user-authorized implementation commit.
- E2E, service startup, and database writes were not run because the task remains scoped to static code logic checks.
