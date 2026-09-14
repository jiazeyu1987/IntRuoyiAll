# Merge 1385 Dirty Worktree To int_main

## Task Goal

Commit the user-authorized full dirty state from `C:\Users\BJB110\.codex\worktrees\1385\IntRuoyi` as a traceable fusion batch, rebase it onto the current `int_main`, merge it into `int_main`, and push if allowed by repository state.

## Milestones

- [x] Read project closeout, worktree, branch-port, and task documentation rules.
- [x] Capture current detached worktree dirty baseline and user authorization.
- [x] Create a task-owned integration worktree/branch from the detached baseline and preserve the authorized dirty state.
- [x] Rebase/merge the integration branch onto `int_main` (verified baseline 90adf7d6e; main has since advanced).
- [x] Run required static and focused regression verification.
- [x] Merge into `int_main`, push if possible, and record final evidence.
- [ ] Finish physical dependency-residue cleanup and release slot 24, then mark completed.

## Expected Verification

- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` passes in the integration branch and final `int_main`.
- `git diff --check` passes for staged and final integrated changes.
- Focused backend Maven tests for changed MES/DCC/server tests pass or an exact blocker is recorded.
- Focused frontend contract/static tests for changed DCC frontend files pass or an exact blocker is recorded.
- `git status --short --branch` confirms the final `int_main` state and push status.

## Current Status

ready_for_closeout

All required checks pass: DCC 350, MES 42, server 4, Python 155, frontend contracts and type checking. Integration ad2e3a1d4 was fast-forwarded into int_main and pushed. Task artifact cleanup passed and Git worktree registration is removed. Physical node_modules residue remains under the former task worktree; automatic approval rejected its scoped recursive removal with "blocked by policy". Slot 24 remains active until the directory is removed. Three unrelated main documentation files retain their original hashes.

Authorized by user on 2026-09-14 to treat all current dirty and untracked changes in the detached `1385` worktree as the fusion batch for submission into `int_main`.

## Design Constraints Check

- No fallback, silent downgrade, mock success, or default PASS values.
- Do not reset, stash, restore, or discard task-owned dirty changes.
- Do not include unrelated worktrees or processes.
- Preserve task records and verification evidence.
- Use a named branch before commit because the source worktree is detached HEAD.
- Do not run E2E unless explicitly requested.

## Cleanup Candidates

- doc/tasks/20260914-merge-1385-to-int-main/pytest-temp-01/
