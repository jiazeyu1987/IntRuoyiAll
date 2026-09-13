# Verification Report

## Bug

DCC-STATIC-025: B/1 correctly stores `revisionBaseActiveControlledFileId`, but check-in-created B/2 dropped the immutable major-revision formal baseline, and version history did not project the stored field.

## Expected

Given B/1 is derived from A/2 while A/3 is the formal baseline at major-revision creation time, when B/1 is checked in to B/2 and history is queried, then B/1 and B/2 both return `revisionBaseActiveControlledFileId=A/3`, while B/2 still reports `predecessorControlledFileId=B/1`. The backend must use the stored row value and must not infer history from current ACTIVE state.

## Reproduction

`node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-025-revision-baseline-history-contract.spec.cjs` reproduced the defect before production changes by failing on the missing `copyForCheckin` baseline inheritance assertion.

## Root Cause

`copyForCheckin` rebuilt a `DccControlledFileDO` for the next minor version but copied only the direct predecessor and omitted `revisionBaseActiveControlledFileId`. `buildVersionHistory` manually populated `DccControlledFileVersionHistoryRespVO` fields and also omitted the stored `revisionBaseActiveControlledFileId`, so even rows that already had the baseline could not show it in history.

## Fix

- Added `.revisionBaseActiveControlledFileId(file.getRevisionBaseActiveControlledFileId())` to check-in copy creation.
- Added `respVO.setRevisionBaseActiveControlledFileId(history.getRevisionBaseActiveControlledFileId())` to version-history projection.
- Did not add fallback or current ACTIVE inference.

## RED/GREEN

- RED: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-025-revision-baseline-history-contract.spec.cjs` -> FAIL, `AssertionError [ERR_ASSERTION]: check-in copy must inherit the immutable major-revision formal baseline from the source version`.
- GREEN: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-025-revision-baseline-history-contract.spec.cjs` -> PASS, `DCC-STATIC-025 revision baseline history contract passed`.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccLifecycleVisibilityAuditTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`, reactor `BUILD SUCCESS`.

## Verification

- `git diff --check` -> PASS, no whitespace errors; Git emitted a line-ending warning for `DccControlledFileQueryServiceImpl.java`.
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-025-revision-baseline-history\verification-report.md` -> PASS, `Bug regression evidence is valid.`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-025-revision-baseline-history --mode preview` -> BLOCKED, with no delete candidates; blocked by dirty main worktree and uncommitted task implementation/test changes in the linked worktree.
- Static contract also checks the frontend detail page already consumes `version.revisionBaseActiveControlledFileId` only when present.
- No E2E, service start/restart, database write, remote operation, commit, or push was performed.

## Blockers

- `docs/bugs/20260912-dcc-90-step-static-audit.md` is not present in the clean `origin/int_main` worktree, so the shared bug file was not imported from the dirty main worktree and not edited.
- The user later authorized local fusion into `int_main`; remote push remains outside the explicit request unless separately authorized.
- Cleanup apply may still be blocked by unrelated untracked files in the main worktree `E:\IntRuoyi`; they must not be deleted or staged by this task.
