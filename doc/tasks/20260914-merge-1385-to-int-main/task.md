# Merge 1385 Dirty Worktree To int_main

## Task Goal

Commit the user-authorized full dirty state from `C:\Users\BJB110\.codex\worktrees\1385\IntRuoyi` as a traceable fusion batch, rebase it onto the current `int_main`, merge it into `int_main`, and push if allowed by repository state.

## Milestones

- [x] Read project closeout, worktree, branch-port, and task documentation rules.
- [x] Capture current detached worktree dirty baseline and user authorization.
- [ ] Create a task-owned integration worktree/branch from the detached baseline and preserve the authorized dirty state.
- [ ] Rebase/merge the integration branch onto `int_main`.
- [ ] Run required static and focused regression verification.
- [ ] Merge into `int_main`, push if possible, and record final evidence.
- [ ] Mark ready_for_closeout, run cleanup preview/apply when safe, then mark completed.

## Expected Verification

- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` passes in the integration branch and final `int_main`.
- `git diff --check` passes for staged and final integrated changes.
- Focused backend Maven tests for changed MES/DCC/server tests pass or an exact blocker is recorded.
- Focused frontend contract/static tests for changed DCC frontend files pass or an exact blocker is recorded.
- `git status --short --branch` confirms the final `int_main` state and push status.

## Current Status

in_progress

Authorized by user on 2026-09-14 to treat all current dirty and untracked changes in the detached `1385` worktree as the fusion batch for submission into `int_main`.

## Design Constraints Check

- No fallback, silent downgrade, mock success, or default PASS values.
- Do not reset, stash, restore, or discard task-owned dirty changes.
- Do not include unrelated worktrees or processes.
- Preserve task records and verification evidence.
- Use a named branch before commit because the source worktree is detached HEAD.
- Do not run E2E unless explicitly requested.

## Cleanup Candidates
