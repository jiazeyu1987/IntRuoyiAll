# DCC-STATIC-025 Revision Baseline History

## Task Goal

Fix only DCC-STATIC-025: subsequent minor versions created by check-in must inherit the immutable major-revision formal baseline (`revisionBaseActiveControlledFileId`), and version-history responses must project that stored field instead of deriving it from the current ACTIVE file.

## Milestones

- [x] Read required project rules and bug-regression-fix-loop skill.
- [x] Create clean worktree from `origin/int_main` without inheriting `E:\IntRuoyi` uncommitted diff.
- [x] Record BDD scenario and RED target before production code changes.
- [x] Add focused regression/static contract that fails on the current code.
- [x] Apply minimal backend fix for check-in copy and history projection.
- [x] Run targeted verification only; no E2E, service restart, database write, remote operation, commit, or push.
- [x] Record DCC-STATIC-025 evidence in task report; shared `docs/bugs/20260912-dcc-90-step-static-audit.md` is absent from clean `origin/int_main`, so it was not imported from the dirty main worktree.
- [x] Commit task-owned code/test/evidence and fast-forward merge into `int_main` per user request on 2026-09-13.

## Expected Verification

- `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-025-revision-baseline-history-contract.spec.cjs`
- Targeted Maven compile/test command if required by Java changes.
- `git diff --check`
- Bug regression evidence validator for this task evidence.

## Design Constraints Check

- No fallback, downgrade, mock success, or current ACTIVE inference.
- Preserve immutable historical baseline stored on each `DccControlledFileDO`.
- Keep direct source (`predecessorControlledFileId`) distinct from major-revision baseline.
- Do not modify other DCC-STATIC items.
- Do not run E2E, start/restart services, write database, operate remote, commit, or push without new explicit authorization.

## Current Status

ready_for_closeout

- Clean worktree: `D:\IntRuoyiWorktree\20260913-dcc-static-025-revision-baseline-history`.
- Branch: `codex/dcc-static-025-revision-baseline-history`.
- Base: `origin/int_main` at `6c6487c9f151244910b9ff80454c397bc303e330`.
- User authorized local fusion into `int_main` on 2026-09-13; remote push remains outside the explicit request.
- Latest local `int_main` `a54464ac3c2053d58f9477f49623f0b7693e4d17` was merged into the task branch as `a8eb02bb29c821133e1919b06ed2cb01f59ad89a`; DCC-STATIC-025 implementation commit `46139d4996d4ee09eeb3732249b9bf28782c2d9f` remains an ancestor.
- Post-merge local fusion verification passed: DCC-STATIC-025 static contract, targeted DCC Maven test, `git diff --check int_main..HEAD`, bug-regression evidence validator, and branch runtime port guard.
