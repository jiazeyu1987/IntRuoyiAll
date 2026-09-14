# Verification Report

## Scope

- Target worktree: `D:\IntRuoyiWorktree\20260727-todo-task-hidden-status`
- Target worktree: `D:\IntRuoyiWorktree\codex-test-process-route`
- Main branch: `int_main`

## Results

- PASS: `codex/20260727-todo-task-hidden-status` is an ancestor of `int_main`.
- PASS: `codex/codex-test-process-route` is an ancestor of `int_main`.
- PASS: Merge commits recorded in `int_main`: `84a91ccc`, `7975f05c`, `6e45bab6`.
- PASS: Target directories are absent after deletion: `D:\IntRuoyiWorktree\20260727-todo-task-hidden-status`, `D:\IntRuoyiWorktree\codex-test-process-route`.
- PASS: `D:\IntRuoyiWorktree\.ports\worktree-ports.json` marks both target entries inactive with `cleanupTask=20260727-merge-remaining-worktrees`.
- PASS: Remaining registered worktrees are `E:\IntRuoyi` on `int_main` and `D:\IntRuoyiWorktree\202607727_yingshe` on `codex/202607727_yingshe`.

## Verification Commands

- PASS: `python -X utf8 -m pytest script\tests\test_system_profile_workbench_task_visibility_sql.py -q`
- PASS: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q`
- PASS: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `node tests\e2e\system-codex-test-management-static.spec.js`
- PASS: `node --check doc\tasks\20260726-codex-test-process-route-case\ensure-process-route-codex-test-items.e2e.cjs`
- PASS: `pnpm ts:check`
- PASS: `scripts\preflight\branch-runtime-port-guard.ps1`
- PASS: `git merge-base --is-ancestor codex/20260727-todo-task-hidden-status HEAD`
- PASS: `git merge-base --is-ancestor codex/codex-test-process-route HEAD`

## Final Gate

- PASS: cleanup preview/apply kept only core task records and deleted no files.
- PASS: project experience consolidation updated `docs\worktree-memory.md`.
- PASS: closeout status is `completed`; final branch guard and push verification are performed with the closeout commit.
