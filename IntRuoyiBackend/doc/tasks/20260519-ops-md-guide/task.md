# Task: Add OPS.md For IntRuoyi Operations Toolkit

## Goal

Create a repository-root `OPS.md` guide that documents the current IntRuoyi operations toolkit, including publish, restart, status, help, and safety usage for both test and production.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this documentation task.
- Record BDD for the operations guide before writing the document.
- Add a concise but complete operator guide covering the unified launcher, direct wrapper commands, environments, and common usage paths.
- Keep the guide aligned to the currently verified scripts and live endpoints.

## Previous Task Check

- Previous backend task: `doc/tasks/20260519-ops-bat-help/task.md`
- Status before this task: completed.
- Impact: the launcher help task is already closed, so this documentation task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this documentation task package.
- [x] M2: Record BDD and operator-guide scope.
- [x] M3: Add the `OPS.md` guide.
- [x] M4: Verify closeout preview and prepare a task-scoped commit.

## Expected Verification

- Manual doc review against the currently verified wrappers and endpoints
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-ops-md-guide --mode preview`

## Current Status

Completed on 2026-05-19. The repository root now includes `OPS.md`, documenting the currently verified publish, restart, status, help, and safety flows for both test and production.

## Final Verification Result

- PASS: manual review of `OPS.md` against the currently verified launchers and live endpoints
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-ops-md-guide --mode preview`

## Blocker And Impact

- Blocker: none.
- Impact: none.
