# Task: Merge batch-record preview worktree into int_main frontend

## Goal

Merge the committed frontend history from
`codex/yudao-ui-batch-record-preview` into `int_main` so the main frontend
branch includes the batch-record preview and comparison work delivered in that
worktree, while keeping temporary task artifacts out of `int_main`.

## Scope

- Frontend repository only
- Merge committed history from
  `D:\ProjectPackage\Int\IntRuoyi\worktrees\yudao-ui-admin-vue3-batch-record-preview`
  into `int_main`
- Clean merge-owned temporary task artifacts before merge
- Exclude one-off evidence files that should not land on `int_main`
- Run focused frontend verification on the merged result

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260518-four-batch-record-image-compare-post-viewer-cleanup/task.md`
- Status before this task: completed
- Impact: the comparison pass was complete, so the remaining work was branch
  hygiene plus integrating the committed frontend history into `int_main`

## Milestones

- [x] M1: Check the previous frontend task state before new merge work.
- [x] M2: Create the merge task package before Git history changes.
- [x] M3: Preview and classify merge-owned temporary task artifacts.
- [x] M4: Clean frontend task artifacts before merge.
- [x] M5: Merge the frontend feature branch into a clean `int_main`-based merge
  branch and resolve conflicts.
- [x] M6: Run focused frontend verification on the merged result.
- [x] M7: Record evidence and complete the verified merge branch for `int_main`
  fast-forward.

## Expected Verification

- `git merge --no-ff codex/yudao-ui-batch-record-preview`
- `pnpm exec vite build`

## Current Status

Completed on the verified merge branch
`codex/20260522-merge-batch-record-into-int-main`. The batch-record preview
frontend history has been merged on top of `int_main`, temporary compare
artifacts were cleaned before integration, the `vite.config.ts` conflict was
resolved by preserving the existing `/admin-api` proxy while adding the
batch-preview `/jmreport` proxy and runtime base override, and the merged
result passed a full Vite build.
