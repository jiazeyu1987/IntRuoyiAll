# Verification Report

## Scope

EDHR-STATIC-005 reopened single-item PQC task backfill only.

## Results

- Targeted regression: PASS. `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest#readsDedicatedAndCommonQaTasksByEachTaskFrozenRegulationVersion,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest#itemScopedTaskDoesNotRequireSiblingQaItemsInSameFrozenVersion+commonQaTaskPlansWithItsOwnFrozenVersionAndItemScope" "-Dsurefire.failIfNoSpecifiedTests=false" test` completed with 3 tests, 0 failures, 0 errors, 0 skipped.
- Static contract: PASS. `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-005-single-item-task-static.spec.cjs`.
- Bug regression evidence validator: PASS. `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260913-edhr-static-005-reopened-single-item-task/bug-regression-evidence.md`.
- `git diff --check`: PASS.
- Changed-scope static review: PASS. Changed implementation scope is process-inspection QA provenance, reader task-frozen QA resolution, writer task-scoped QA item validation/evidence, targeted regression tests, task records, and the durable backend rule entry.
- Post-fusion full reader/writer regression: PASS. `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` completed with 25 tests, 0 failures, 0 errors, 0 skipped.
- Post-fusion static contracts: PASS. `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-005-single-item-task-static.spec.cjs` and `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-findings-fix-static.spec.cjs`.
- Post-fusion branch runtime guard: PASS. `scripts\preflight\branch-runtime-port-guard.ps1` passed in both the registered task worktree and `E:\IntRuoyi`.

## Root Cause

The reader selected QA regulation/version from the active-order locked product QA identity instead of each PQC task's own frozen version. The writer validated all QA items for an inspection type, so a single item-scoped task was forced to cover sibling items that belong to separate PQC tasks; product-only owner validation also rejected common `MES_QA_COMMON` tasks.

## Fix Summary

- Reader now resolves QA by each task's frozen `regulationVersionId`.
- Product `MES_QA` still requires active-order frozen DCC/QA identity.
- Common `MES_QA_COMMON` versions are accepted through verified provenance evidence.
- Writer item validation and QA item evidence now use the task's `regulationVersionId + qaProcessId + qaItemCode + inspectionType` scope.

## Closeout

- Project experience consolidation: PASS. Durable rule merged into `docs/backend-development.md`.
- Cleanup auto preview: BLOCKED by detached linked worktree branch resolution; no files deleted.
- Cleanup scoped preview/apply with `--worktree-closeout off`: PASS. Kept `task.md`, `execution-log.md`, and `verification-report.md`; deleted only task-local temporary `bug-regression-evidence.md`.
- Local task branch commit: `7a05d8bf3 fix: scope PQC process inspection backfill by task`.
- Local `int_main` fusion: `37053c4d2 Merge branch 'codex/20260913-edhr-static-005-reopened-single-item-task' into int_main`.
- Local `origin/int_main` sync merge: `e3288631c Merge remote-tracking branch 'origin/int_main' into int_main`.
- Final task status: `completed`.

## Not Run

- E2E / Playwright: not run by explicit user instruction.
- Service start: not run by explicit user instruction.
- Database write: not run by explicit user instruction.
- Remote push: not run because the current user request authorized local commit and local `int_main` fusion, but did not explicitly authorize push.
