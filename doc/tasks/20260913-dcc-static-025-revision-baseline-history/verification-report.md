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

## Local Fusion Verification

- `git merge int_main` on the task branch -> PASS, no conflicts, merge commit `a8eb02bb29c821133e1919b06ed2cb01f59ad89a`; latest local `int_main` before fusion was `a54464ac3c2053d58f9477f49623f0b7693e4d17`.
- `git merge-base --is-ancestor int_main HEAD` -> exit `0`; `git merge-base --is-ancestor 46139d4996d4ee09eeb3732249b9bf28782c2d9f HEAD` -> exit `0`.
- `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-025-revision-baseline-history-contract.spec.cjs` -> PASS, `DCC-STATIC-025 revision baseline history contract passed`.
- `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccLifecycleVisibilityAuditTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`, reactor `BUILD SUCCESS`.
- `git diff --check int_main..HEAD` -> PASS, no whitespace errors.
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, `Branch runtime port guard passed for codex/dcc-static-025-revision-baseline-history/int_main: frontend 8084, backend 48084.`
- Bug regression evidence validator -> PASS, `Bug regression evidence is valid.`

## Blockers

- `docs/bugs/20260912-dcc-90-step-static-audit.md` is not present in the clean `origin/int_main` worktree, so the shared bug file was not imported from the dirty main worktree and not edited.
- The user authorized local fusion into `int_main`; remote push remains outside the explicit request unless separately authorized.
