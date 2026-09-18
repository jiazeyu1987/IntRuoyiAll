# EDHR-STATIC-022 Verification Report

## Scope

EDHR-STATIC-022 only. Validate that final release inventory trace dedup uses formal detail identity rather than source type alone.

## Bug

Legal inventory trace rows for the same active order were rejected as duplicate whenever more than one row shared `sourceType=TRANSFER`. This blocked normal multi-material, multi-batch, or multi-transfer final release paths.

## Expected

The final release inventory consistency check accepts distinct formal inventory trace details, even when they share a source type, and blocks only true duplicate persisted source identities.

## Reproduction

`mvn -pl yudao-module-mes -Dtest=MesOrderReleaseCompletenessServiceTest#evaluateInventoryConsistencyAllowsDistinctTransferDetails test`

Before the fix, this failed because the legal second `TRANSFER` detail returned `BLOCKER` instead of `PASS`.

## Root Cause

`MesOrderReleaseCompletenessServiceImpl.evaluateInventoryConsistency` grouped inventory traces by `sourceType` and treated any count greater than one as duplicate. The check ignored formal source document, source row/detail, material, batch, and inventory fact identity.

## Commands

- RED: `mvn -pl yudao-module-mes -Dtest=MesOrderReleaseCompletenessServiceTest#evaluateInventoryConsistencyAllowsDistinctTransferDetails test` -> FAIL, expected reason: `PASS` assertion received `BLOCKER`.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesOrderReleaseCompletenessServiceTest#evaluateInventoryConsistencyAllowsDistinctTransferDetails,MesOrderReleaseCompletenessServiceTest#evaluateInventoryConsistencyBlocksWhenTraceSourceIdentityDuplicate" test` -> PASS, 2 tests.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesOrderReleaseCompletenessServiceTest test` -> PASS, 16 tests.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderTransferTraceSchemaTest" test` -> PASS, 5 tests.
- Verification: `rg -n "duplicateSourceTypes|groupingBy\\(type -> type|map\\(MesProcessPoolActiveOrderTransferTraceDO::getSourceType\\).*counting" IntRuoyiBackend\\yudao-module-mes\\src\\main\\java\\cn\\iocoder\\yudao\\module\\mes\\service\\pro\\batchrecord\\MesOrderReleaseCompletenessServiceImpl.java` -> PASS, no matches.
- Verification: `rg -n "duplicateInventoryTraceIdentities|inventoryTraceIdentity|transferDetailId=|evaluateInventoryConsistencyAllowsDistinctTransferDetails|evaluateInventoryConsistencyBlocksWhenTraceSourceIdentityDuplicate" ...` -> PASS, identity helper and tests present.
- Verification: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-022-inventory-detail-dedup\verification-report.md` -> PASS.
- Verification: `git diff --check -- <task-owned paths>` -> PASS.
- Verification: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-022-inventory-detail-dedup --mode preview` -> BLOCKED; keep list contains the three required task records, delete list is empty, and blocked reason is `Current worktree branch could not be resolved`.
- Verification: `git check-ignore -v doc/tasks/20260914-edhr-static-022-inventory-detail-dedup/{task.md,execution-log.md,verification-report.md}` -> confirms task records are locally ignored by `.git/info/exclude` but present on disk.

## Results

- Legal distinct transfer details now pass when required source types are complete and stock is healthy.
- True duplicate transfer detail identity still returns `BLOCKER` and reports the duplicated `transferDetailId`.
- Transfer trace generation/schema tests still pass, covering the formal `transferLineId` / `transferDetailId` fields consumed by the new dedup identity.
- No E2E, database write, service start/stop/restart, remote server operation, git commit, or git push was performed.

## Static Contract

- Removed the `sourceType`-only duplicate grouping.
- Added duplicate identity using `sourceType`, `direction`, `sourceObjectType`, `sourceObjectId`, `transferId`, `transferLineId`, `transferDetailId`, `materialStockId`, `itemId`, and `batchId`.
- Kept missing required source-type behavior unchanged.
- Preserved stock-health validation after trace validity and duplicate identity checks.

## Risks And Blockers

- Blockers: final project closeout cannot be marked `completed` because this turn explicitly forbids git commit/push while project closeout rules require them.
- Blockers: cleanup preview is blocked by detached HEAD (`current_branch=None`) in this linked worktree; cleanup apply was not run.
- Blockers: task records are ignored by local `.git/info/exclude`, so they cannot be committed without explicit force-add policy and commit authorization.
- Risk: EDHR-STATIC-021 remains out of scope; new orders may still encounter missing inventory-source generation before this dedup path is observable.
- Risk: Static/non-E2E scope only; no runtime database or Playwright validation was authorized.
