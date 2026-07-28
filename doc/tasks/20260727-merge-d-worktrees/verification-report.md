# Verification Report

## Scope

- Requested scope after update: only process worktrees under `D:\IntRuoyiWorktree\` that are clean and verified mergeable into `int_main`.
- Processed:
  - `D:\IntRuoyiWorktree\20260727_pici` / `codex/20260727_pici`
  - `D:\IntRuoyiWorktree\edhr-latest-published-form` / `codex/edhr-latest-published-form`
- Retained:
  - `D:\IntRuoyiWorktree\202607727_yingshe` / `codex/202607727_yingshe`
  - `D:\IntRuoyiWorktree\codex-test-process-route` / `codex/codex-test-process-route`
  - `D:\IntRuoyiWorktree\20260727-todo-task-hidden-status` / `codex/20260727-todo-task-hidden-status`

## Merge Results

- `codex/20260727_pici`: already merged before this round; `git merge-base --is-ancestor codex/20260727_pici HEAD` returned PASS.
- `codex/edhr-latest-published-form`: merged into `int_main` via merge commit `b0914b54`.
- `codex/codex-test-process-route`: not merged; `git merge-tree --write-tree HEAD codex/codex-test-process-route` reports add/add conflicts in `doc/tasks/20260726-codex-test-process-route-case/execution-log.md` and `task.md`.
- `codex/202607727_yingshe`: not merged; worktree has dirty backend/frontend/SQL/E2E changes and its own task remains blocked.

## Deletion Results

- `D:\IntRuoyiWorktree\edhr-latest-published-form`: removed by `git worktree remove`; final physical path check returned `False`.
- `D:\IntRuoyiWorktree\20260727_pici`: Git registration removed; physical directory initially remained because pici-owned runtime processes held log files. Only processes whose command line pointed to `D:\IntRuoyiWorktree\20260727_pici` and ports `8084/48084` were stopped, then the residual directory was deleted; final physical path check returned `False`.
- Port registry `D:\IntRuoyiWorktree\.ports\worktree-ports.json` now marks both deleted targets inactive with cleanup task `20260727-merge-d-worktrees`.

## Verification Commands

- `git -C E:\IntRuoyi merge-base --is-ancestor codex/20260727_pici HEAD` -> PASS.
- `git -C E:\IntRuoyi merge-base --is-ancestor codex/edhr-latest-published-form HEAD` -> PASS.
- `powershell -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.
- `git -C E:\IntRuoyi diff --check` -> PASS.
- `git -C E:\IntRuoyi worktree list --porcelain` -> no entries for `20260727_pici` or `edhr-latest-published-form`.

## Remaining Blockers

- `codex/codex-test-process-route` requires manual conflict resolution before it can be verified mergeable.
- `codex/202607727_yingshe` requires its blocked real E2E prerequisites and dirty worktree handling before fusion can proceed.
