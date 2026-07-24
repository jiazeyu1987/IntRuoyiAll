# Task: Preview blocking data analysis

## Goal

Analyze the current local MES scheduling data to determine why clicking auto-schedule preview still returns blocking issues, and identify the exact work orders, processes, and missing workstation/production-line prerequisites behind the message `工序缺少可用工作站或产线绑定`.

## Scope

- Explicitly block the current same-repository backend delivery task `doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/task.md` before starting this analysis.
- Create this backend analysis task package before running the investigation.
- Use read-only inspection only: backend API calls, local browser evidence, source inspection, and database queries as needed.
- Reconstruct the current preview scope, identify the exact work orders included in the request, and inspect the corresponding route-process and workstation/production-line data.
- Explain whether the blocking issue is caused by missing workstations, missing production-line binding, disabled production lines, or other current data conditions.
- Do not change production code or mutate repo-tracked files beyond this task record.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused schedule-calendar delivery task remains isolated and does not block this data-analysis slice.

## Milestones

- [x] M1: Block the previous same-repository backend task and create this analysis task package first.
- [x] M2: Reproduce the current preview request and capture the blocking issue payload.
- [x] M3: Trace the blocking work orders/processes back to current workstation and production-line data.
- [x] M4: Record the exact blocker cause and user-facing impact.

## Expected Verification

- Read-only backend/API inspection against the current local environment
- Optional read-only database queries against the local MySQL container

## Current Status

Completed for analysis. The current local preview-blocking cause is identified from live API and database data.

## Final Verification Result

PASS: live preview scope and database inspection identify the exact blocking work orders, route process, and missing production-line binding.

## Blocker And Impact

- Blocker: route `900025` first process `900331 / 吹球囊成型` has workstation rows, but every matching workstation currently has `production_line_id = NULL`.
- Impact: both scoped work orders fail preview at the same first process and return `LINE` blocking issues before capacity allocation can continue.
