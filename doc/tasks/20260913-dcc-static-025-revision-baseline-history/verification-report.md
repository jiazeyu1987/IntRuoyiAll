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
- `git merge origin/int_main` after remote drift -> PASS, no conflicts, merge commit `f9d761c5261481c7802f92315b7b60260cf33015`; latest absorbed `origin/int_main` was `f117a3275491b6c5c83b41b18fe4386cf5a6bef6`.
- After absorbing latest `origin/int_main`, DCC-STATIC-025 static contract -> PASS; targeted DCC Maven test -> PASS with `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`; `git diff --check origin/int_main..HEAD` -> PASS; branch runtime port guard -> PASS; bug-regression evidence validator -> PASS.
- `git merge int_main` after local/remote main advanced to `fdaae3de2a7695d326ed462bad71719c3a7c84bc` -> PASS, no conflicts, merge commit `0df657daa9686ff8e6a89f380d8726c483135261`.
- After absorbing `fdaae3de2a7695d326ed462bad71719c3a7c84bc`, DCC-STATIC-025 static contract -> PASS; targeted DCC Maven test -> PASS with `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`; `git diff --check int_main..HEAD` -> PASS; branch runtime port guard -> PASS; bug-regression evidence validator -> PASS.
- Current task branch is fast-forward-ready relative to `int_main` and `origin/int_main`: both ancestry checks returned exit `0`.
- Cleanup preview after final verification -> BLOCKED only by dirty main worktree `E:\IntRuoyi`; keep list contains the three core task records, delete `<none>`, warnings `<none>`.
- After user re-authorized fusion, `origin/int_main` at `5eb62893a19f7d81382ad113e425d8b2ce4a62a4` was merged into the task branch as `db1b58109264224c8ab73f446cc138073878dec8` with no conflicts.
- After absorbing `5eb62893a19f7d81382ad113e425d8b2ce4a62a4`, DCC-STATIC-025 static contract -> PASS; targeted DCC Maven test -> PASS with `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`; `git diff --check origin/int_main..HEAD` -> PASS; branch runtime port guard -> PASS; bug-regression evidence validator -> PASS.
- Current task branch is fast-forward-ready relative to latest `origin/int_main`: ancestry check returned exit `0`.
- Remote fusion completed: `git push origin HEAD:int_main` -> PASS, advancing remote `int_main` from `5eb62893a19f7d81382ad113e425d8b2ce4a62a4` to `c8f860a8293d3d124bdda317ef607fe71db624ef`.
- Post-push verification confirmed `HEAD` equals `origin/int_main` at `c8f860a8293d3d124bdda317ef607fe71db624ef`.

## Blockers

- `docs/bugs/20260912-dcc-90-step-static-audit.md` is not present in the clean `origin/int_main` worktree, so the shared bug file was not imported from the dirty main worktree and not edited.
- Remote `int_main` fusion is complete; local worktree cleanup is still blocked because `E:\IntRuoyi` is dirty and behind the remote.
- Earlier during fusion, local `E:\IntRuoyi` temporarily had unrelated MES process-pool conflict/staged EDHR task state; this task did not stage, clean, reset, or resolve those unrelated files.
- On reauthorization, local `E:\IntRuoyi` still showed unrelated EDHR-STATIC-013 dirty files including `UU` state; remote fast-forward was used to avoid overwriting or staging unrelated main-worktree changes.
