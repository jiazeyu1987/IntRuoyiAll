# Task: Merge remaining auto-schedule backend delta and remove feature branch

## Goal

Merge the remaining backend auto-schedule follow-up delta into `int_main`,
remove the obsolete `feature/auto-schedule-first-loop` backend worktree, and
delete the feature branch after the relevant backend content is present on
`int_main`.

## Scope

- Backend repository only
- Remove the backend feature worktree at `D:/wt/intsched-be`
- Delete the backend branch `feature/auto-schedule-first-loop`
- Record the verification boundary for the non-MySQL canonical bootstrap update

## Previous Task Check

- Previous backend task: `doc/tasks/20260514-backend-compile-check/task.md`
- Status before this task: completed

## Milestones

- [x] M1: Confirm the previous backend task state.
- [x] M2: Merge the remaining backend delta into `int_main`.
- [x] M3: Remove the backend feature worktree safely.
- [x] M4: Delete the backend feature branch after merge.
- [x] M5: Record final verification and status.

## Expected Verification

- `git cherry-pick f887a4d154`
- `git worktree remove D:/wt/intsched-be`
- `git branch -D feature/auto-schedule-first-loop`

## Current Status

Completed. The remaining backend delta was merged into `int_main` as
`dd500b0d54`. The backend feature worktree was removed and the backend feature
branch was deleted.

## Final Verification Result

- Remaining backend delta merged: yes
- Backend feature worktree removed: yes
- Backend feature branch deleted: yes
- Non-MySQL canonical bootstrap validation boundary: PostgreSQL appended block
  executed in isolation; OpenGauss / Kingbase / Oracle / SQL Server remain
  static-only validation, as already recorded in the merged follow-up evidence

