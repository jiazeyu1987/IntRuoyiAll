# Verification Report

## Scope

- EDHR-STATIC-025 only: production release PROCESS_INSPECTION dynamic FormCenter backfill when multiple confirmed PQC inspection task sources share one target dynamic form instance.
- Changed service scope: `MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl`.
- Test scope: writer unit regression, dynamic FormCenter port replay guard, and process-inspection dynamic template static contract.

## Bug

- Same route process can have multiple confirmed PQC inspection tasks mapped to one PROCESS_INSPECTION dynamic FormCenter binding.
- Previous writer loop called `dynamicFormPort.write(...)` for each prepared inspection, so the first item submitted the FormCenter instance to EFFECTIVE and the second item was rejected as a different formal source.

## Expected

- All confirmed inspection task sources for the same target dynamic FormCenter instance are aggregated into one write command and submitted/effected once.
- Existing EFFECTIVE instances remain locked against unrelated or partial formal source writes.

## Contract

- Dynamic writer groups by current eDHR batch task and FormCenter instance before calling the dynamic port.
- Combined fields keep one target field per template rule and include all item summaries, signatures, source value hashes, and a grouped evidence hash.
- Traditional batch-record execution backfill continues to use the existing per-inspection field-audit path.

## Validation

- Success behavior: `dynamicFormBackfillAggregatesMultipleInspectionTasksBeforeEffectiveSubmit` verifies two PQC task sources sharing one FormCenter instance produce exactly one dynamic port write, include both `PRESSURE` and `FLOW`, carry four signature evidence rows, and do not open traditional execution.
- Lock behavior: `effectiveInstanceRejectsDifferentFormalSourceWithoutDraftOrSubmit` verifies an EFFECTIVE instance with mismatched audit source fails before `saveDraft` or `submitInstance`.
- Static behavior: `mes-process-inspection-dynamic-template-static.spec.cjs` verifies the writer collects dynamic groups and no longer calls the legacy per-inspection dynamic write overload.

## BDD:

- Given one active batch has two confirmed PQC inspection task sources for the same route process and the same PROCESS_INSPECTION dynamic FormCenter instance, When production release backfill writes process inspection evidence, Then the writer aggregates both inspection sources into the target instance before submit/effective and the final command contains both source values.
- Given a PROCESS_INSPECTION FormCenter instance is already EFFECTIVE for a completed backfill source set, When another unrelated or partial source tries to write the same instance, Then the dynamic form port rejects the write and does not modify the locked instance.

## Reproduction

- RED command: `mvn -pl yudao-module-mes -Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest#dynamicFormBackfillAggregatesMultipleInspectionTasksBeforeEffectiveSubmit test`
- RED result: FAIL, `dynamicFormPort.write(...)` wanted 1 time but was 2 times from `MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.write`.

## Root Cause

- The writer planned one `PreparedInspection` per PQC task source and immediately submitted the shared dynamic FormCenter instance for each prepared inspection.
- The dynamic port correctly treats an EFFECTIVE instance as locked and only permits exact replay by matching audit head/source snapshot, so the second task source could not be applied after the first task had already made the instance EFFECTIVE.

## RED:

- `mvn -pl yudao-module-mes -Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest#dynamicFormBackfillAggregatesMultipleInspectionTasksBeforeEffectiveSubmit test` -> FAIL, expected reason: per-inspection dynamic write was invoked twice for one shared FormCenter instance.

## GREEN:

- `mvn -pl yudao-module-mes -Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest#dynamicFormBackfillAggregatesMultipleInspectionTasksBeforeEffectiveSubmit test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest" test` -> PASS, Tests run: 24, Failures: 0, Errors: 0, Skipped: 0.
- `node yudao-module-mes\src\test\js\mes-process-inspection-dynamic-template-static.spec.cjs` -> PASS.
- `git diff --check -- <task-owned changed paths>` -> PASS, no whitespace errors; CRLF normalization warnings only.
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-025-multi-inspection-backfill\verification-report.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260914-edhr-static-025-multi-inspection-backfill\verification-report.md` -> PASS.

## Verification

- Non-E2E targeted verification completed.
- No Playwright/E2E, database write, service start/stop/restart, remote server operation, or git push was performed.
- Changed paths:
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionServiceTest.java` (testCompile unblocker only)
  - `IntRuoyiBackend/yudao-module-mes/src/test/js/mes-process-inspection-dynamic-template-static.spec.cjs`
  - `doc/tasks/20260914-edhr-static-025-multi-inspection-backfill/task.md`
  - `doc/tasks/20260914-edhr-static-025-multi-inspection-backfill/execution-log.md`
  - `doc/tasks/20260914-edhr-static-025-multi-inspection-backfill/verification-report.md`

## Blockers

- Implementation and verification are complete.
- 2026-09-14 user authorized local commit and fusion into `int_main`.
- Task branch was committed and rebased over the latest stable local `int_main` snapshot reached during this turn; implementation commit after the last successful rebase is `98e18bc83`.
- Local fusion into `int_main` is blocked because `E:\IntRuoyi` is currently `ahead 8, behind 1` and has dirty parallel task record `doc/tasks/20260914-edhr-static-012-route-rename-archive/task.md`. The current task must not submit, pull over, or rewrite that parallel state.
- Final project closeout still cannot be marked `completed` until the main worktree is clean, local fast-forward fusion succeeds, and git push is explicitly authorized and succeeds, because `docs/task-closeout-rules.md` requires push before completed status.
