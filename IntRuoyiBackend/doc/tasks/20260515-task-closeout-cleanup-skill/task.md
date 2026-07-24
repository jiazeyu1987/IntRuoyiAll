# Task: Task Closeout Cleanup Skill

## Goal

Create a global `task-closeout-cleanup` skill that previews and then cleans task-specific intermediate artifacts, and when running inside an extra git worktree can optionally commit the cleanup, fast-forward merge to the main branch, and remove the worktree.

## Scope

- Check the latest backend task state before starting this task.
- Create this task document and execution log before editing files.
- Create the global skill under `C:\Users\BJB110\.codex\skills\task-closeout-cleanup`.
- Add the supporting cleanup script, references, and `agents/openai.yaml`.
- Update the global `C:\Users\BJB110\.codex\AGENTS.md` with the approved default baseline.
- Validate the skill script locally with representative preview/apply and worktree checks.

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-electronic-batch-record-image-codex-cli-import/task.md`
- Status before this task: blocked by user priority switch
- Impact: the paused image-import task does not block this global cleanup-skill work.

## Milestones

- [x] M1: Confirm the previous backend task status and create this task directory.
- [x] M2: Initialize the global skill scaffold and record BDD scenarios.
- [x] M3: Implement the cleanup script, skill instructions, and global baseline update.
- [x] M4: Run targeted validation for preview/apply and worktree logic.
- [x] M5: Update evidence and create a scoped repository commit for the task records.

## Expected Verification

- The global skill folder exists with `SKILL.md`, `agents/openai.yaml`, `scripts/task_closeout.py`, and `references/closeout-rules.md`.
- The script can preview cleanup candidates without mutating repo-tracked production files.
- The script can detect linked worktrees, enforce ff-only merge, and refuse unsafe cleanup/merge paths.
- The global `C:\Users\BJB110\.codex\AGENTS.md` contains the new default baseline line.

## Current Status

Completed. The global cleanup skill, support script, reference rules, and global AGENTS baseline are in place, and the skill passed representative preview/apply/worktree validation.

## Final Verification Result

- `python C:\Users\BJB110\.codex\skills\.system\skill-creator\scripts\quick_validate.py C:\Users\BJB110\.codex\skills\task-closeout-cleanup` -> PASS
- Temporary git repo validation -> PASS
  - preview keeps only `task.md` and `execution-log.md`
  - apply deletes tracked task artifacts and preserves core records
  - unfinished tasks are blocked
  - linked worktree closeout creates cleanup commit, fast-forward merges, and removes the worktree
  - dirty main worktree and non-fast-forward merge paths are blocked
- Real repo preview check:
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260515-task-closeout-cleanup-skill --mode preview` -> PASS
