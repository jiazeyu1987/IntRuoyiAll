# Task: Remove merged auto-schedule frontend worktree

## Goal

Remove the frontend feature worktree for `feature/auto-schedule-first-loop`
after its committed history has been integrated into `int_main`.

## Scope

- Frontend repository only
- Verify the target frontend worktree path and branch
- Ensure no required frontend dev server still depends on the feature worktree
- Remove the frontend worktree registration and filesystem path
- Keep the feature branch unless the user explicitly asks to delete it

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-frontend-vuetsc-cleanup/task.md`
- Status before this task: blocked by user priority switch
- Impact: deleting the merged worktree does not change the pending repository-wide
  `vue-tsc` cleanup scope

## Milestones

- [x] M1: Confirm the previous frontend task state.
- [x] M2: Create this task document before Git operations.
- [ ] M3: Verify the frontend feature worktree path and any process dependency.
- [ ] M4: Remove the frontend feature worktree safely.
- [ ] M5: Record verification evidence and final status.

## Expected Verification

- `git worktree list --porcelain`
- `git worktree remove D:/ProjectPackage/Int/IntRuoyi-worktrees/auto-schedule-first-loop/yudao-ui-admin-vue3`
- `git worktree list --porcelain`

## Current Status

Blocked by user priority switch. A new higher-priority runtime bug on the MES
route list status toggle must be handled first, so the worktree cleanup is
paused before dependency checks or removal commands run.

## Blocker And Impact

- Blocker: user priority switched to the MES route status-toggle runtime bug on
  2026-05-15.
- Impact: the merged frontend worktree still exists and the cleanup task cannot
  advance until the runtime bug is resolved or the user restores this cleanup
  as the active task.
