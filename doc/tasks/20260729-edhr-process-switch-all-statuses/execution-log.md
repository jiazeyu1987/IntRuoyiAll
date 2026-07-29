# Execution Log

## Intent

User confirmed the design: auxiliary fill page process switch must list all current order/batch processes, distinguish statuses with the same visual state language as batch execution, and keep official backend gating for actual task opening.

## Preflight

- Read `docs/task-closeout-rules.md`.
- Read `docs/frontend-development.md`.
- Read `docs/e2e-rules.md`.
- Read `docs/powershell-encoding.md`.
- Read `docs/powershell-memory.md`.
- Read `docs/engineering/technology-stack-routing.md`.
- Used skill `frontend-feature-delivery`; read `SKILL.md` and `references/frontend-contract.md`.
- Created task directory `doc/tasks/20260729-edhr-process-switch-all-statuses/`.
- Read `docs/experience-index.md`; applicable gates copied into `task.md`.

## Git Baseline

- `git status --short --branch` showed branch `int_main...origin/int_main [ahead 3]` and existing dirty files.
- Baseline commit created before current task edits: `d432110f chore: baseline dirty worktree before process switch task`.
- Baseline file list recorded in `task.md`.

## BDD

- BDD: all-process switch list -> Given an auxiliary fill page is opened for a batch, When the user clicks the process switch, Then the dialog lists every ordinary process in the current batch instead of only openable/in-progress tasks.
- BDD: status visual distinction -> Given the process list includes waiting, draft, submitted, rejected, rework, completed, skipped and blocked tasks, When the dialog renders, Then each option exposes a status label/tag and uses the batch execution background color family for not-started, in-progress/started, and completed groups.
- BDD: no permission escalation -> Given a process is not openable but has an existing execution record, When the user selects it, Then the page opens read-only execution context; if neither work task nor execution exists, it fails fast instead of pretending the task is openable.

## RED

- RED: `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js` -> FAIL, expected reason: current process switch has no grouped `AssistProcessSwitchItem` model and still filters to openable batch tasks.

## GREEN

- GREEN: `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js` -> PASS, current implementation draft satisfies grouped all-process status contract.

## Regression

- REGRESSION: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> FAIL, expected conflict: concurrent task `20260729-edhr-fill-workspace-redbox-hide` removed `我的填写项` auxiliary topbar required by this process switch request.

## Blockers

- BLOCKED: concurrent task `doc/tasks/20260729-edhr-fill-workspace-redbox-hide/` requires hiding the auxiliary fill topbar, while this task requires the red-box process switch button in that same topbar to remain usable. Per project task ownership rules, do not continue mutating the conflicting DOM without user decision.
