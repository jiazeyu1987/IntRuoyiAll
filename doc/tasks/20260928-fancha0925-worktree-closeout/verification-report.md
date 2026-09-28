# Verification Report

## Result

The integrated `fancha0925` managed worktree was archived and removed. Its feature branch remains in the local repository and is an ancestor of `int_main`.

## Evidence

- `git merge-base --is-ancestor fancha0925 int_main` -> PASS.
- `git rev-list --left-right --count fancha0925...int_main` -> `0 7` before archive.
- Codex archive artifact `01a0e84f-2200-7020-babe-52e9481eddd7` -> created for the exact managed worktree.
- `Test-Path C:\Users\BJB110\.codex\worktrees\fancha202609\IntRuoyi` -> `False`.
- `git worktree list --porcelain` -> no target path or `fancha0925` worktree association.
- Six ignored task-document directories (53 files) and six archive-tool residue files (one tracked-source copy, four logs, one marker) were SHA-256 verified into `archived-task-records/`.
- Runtime reservation for `int_main` slot 37 / ports 8212 and 48212 -> `active=false`, with `deletedAt` and `cleanupTask=20260928-fancha0925-worktree-closeout`.
- Registry validator and `scripts/preflight/branch-runtime-port-guard.ps1` -> PASS.
- Cleanup preview -> ready, no blockers/warnings; cleanup apply -> applied and removed only the temporary slot-release helper.

## Preserved Scope and Blocker

- The parent directory `C:\Users\BJB110\.codex\worktrees\fancha202609` remains because the archive tool left task-specific runtime logs and a source copy there. All six files were copied and hash-verified; the unknown Java process on port 48094 was not stopped. This residue is outside the Git checkout and was not recursively deleted.
- User-authorized Git closeout is limited to this task's three core records. Existing unrelated or parallel `int_main` changes remain untouched and excluded.

## Final Closeout

- Cleanup preview and apply passed with no deletions, blockers, or warnings.
- The branch runtime guard passed. Only this task's core records are committed and pushed; unrelated staged, unstaged, deleted, and untracked workspace changes are excluded.
- Closeout commit `b5f9ee6aca7f5e9d18eb54e52fffd8e7eadc41b0` pushed successfully to `origin/int_main`; post-push ahead/behind count is `0 0`.
