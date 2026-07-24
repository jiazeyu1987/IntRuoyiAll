# Execution Log: Task Closeout Cleanup Skill

BDD: preview keeps core task records and lists only removable artifacts -> Given a completed task directory and related temporary outputs / When the cleanup skill runs in preview mode / Then it keeps `task.md` and `execution-log.md` and lists only task-specific artifacts, temporary files, and temporary test products for deletion

BDD: apply blocks unfinished tasks -> Given a task directory whose `task.md` is not completed / When the cleanup skill runs in apply mode / Then it fails fast without deleting files

BDD: linked worktree closeout is ff-only -> Given the current repository is a linked worktree and the task cleanup commit exists / When the cleanup skill performs worktree closeout / Then it merges to the detected main branch with `--ff-only` and removes the worktree only after the merge succeeds

RED: `python C:\Users\BJB110\.codex\skills\.system\skill-creator\scripts\init_skill.py task-closeout-cleanup --path C:\Users\BJB110\.codex\skills --resources scripts,references --interface display_name="任务收尾清理" --interface short_description="预览并清理任务产物，支持 worktree 收尾" --interface default_prompt="Use $task-closeout-cleanup ..."` -> FAIL, the initial `short_description` was shorter than the skill-creator UI constraint.

RED: `python C:\Users\BJB110\.codex\skills\.system\skill-creator\scripts\quick_validate.py C:\Users\BJB110\.codex\skills\task-closeout-cleanup` -> FAIL, the initial `SKILL.md` description still contained angle brackets.

GREEN: `python C:\Users\BJB110\.codex\skills\.system\skill-creator\scripts\quick_validate.py C:\Users\BJB110\.codex\skills\task-closeout-cleanup` -> PASS

GREEN: temporary git repo validation script -> PASS
- preview/apply normal repo scenario passed
- unfinished task apply block passed
- linked worktree cleanup commit + ff-only merge + remove passed
- dirty main worktree block passed
- non-fast-forward block passed

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260515-task-closeout-cleanup-skill --mode preview` on the real repository -> PASS
