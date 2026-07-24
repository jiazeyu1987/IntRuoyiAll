# Frontend Design: Current-System-First Auto Scheduling

## Purpose and Scope

This document defines the frontend design for adding automatic scheduling to the existing IntRuoyi production scheduling experience. The first version must reuse the current production scheduling entry and display one current effective schedule only. It must not introduce persisted schedule versions, drafts, snapshots, version comparison, or load-saved-version flows.

## Evidence Reviewed

- `docs/product/prd.md`
- `docs/product/user-flows.md`
- `docs/product/acceptance-criteria.md`
- `doc/tasks/20260512-intpp-auto-schedule-migration-assessment/task.md`
- Current frontend evidence:
  - `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/src/views/mes/pro/task/index.vue`
  - `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/src/views/mes/pro/task/ProTaskList.vue`
  - `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/src/views/mes/pro/task/components/GanttChart.vue`
  - `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/src/api/mes/pro/task`
- Current Gantt evidence: `GanttChart.vue` disables `auto_scheduling`, disables `drag_links`, and sends `links: []`.

## Pages and Routes

- Reuse the existing production scheduling route and menu entry for MES production scheduling.
- Add auto scheduling controls inside the current production scheduling page, not a new version-management page.
- Keep the existing task list and Gantt views as the primary result surface.
- Add no route for schedule version list, saved snapshots, or version comparison.

Recommended route-level behavior:

- Existing production scheduling page:
  - Shows current production tasks.
  - Allows manual task edits where current permissions allow.
  - Adds automatic scheduling actions when the user has new auto-scheduling permissions.
- Optional detail drawer/dialog:
  - Shows schedule conflicts and material shortages for the current request or current applied schedule.
  - Does not persist as a separate schedule version UI.

## Components

Add or extend the following components:

- `AutoScheduleActionBar`
  - Placement: current production scheduling page toolbar.
  - Actions: generate/apply current schedule, preview, replan, view conflicts, view shortages.
  - Must hide or disable actions based on permissions.

- `AutoScheduleDialog`
  - Inputs: work order scope, start date/time, capacity mode, replan strategy, conflict policy, material shortage policy.
  - Shows fail-fast validation results before apply when returned by backend.
  - Must not expose version name, version list, snapshot, or publish-version fields.

- `ScheduleResultSummary`
  - Shows affected work orders, generated task count, changed task count, protected task count, shortage count, conflict count, start/end range.
  - If preview is used, it is in-memory only and must clearly indicate that no task has been written.

- `ScheduleIssueDrawer`
  - Shows conflicts and material shortages.
  - Fields: issue type, severity, work order, process, workstation/resource, material, shortage quantity, date/shift, message.

- `TaskLockControl`
  - Allows marking tasks as locked/manual-preserved if the backend model supports it.
  - Requires explicit permission.

- `GanttChart`
  - Must accept and render backend task dependency links.
  - Current `links: []` must be replaced by backend-provided links or a computed dependency list from route/process data.
  - `drag_links` should remain disabled unless a separate manual dependency-editing requirement is approved.
  - Gantt must visibly distinguish auto-generated tasks, manual tasks, locked tasks, shortage-risk tasks, and conflict tasks.

## State and Data Flow

Primary generate/apply flow:

1. User opens existing scheduling page.
2. Frontend loads current work orders/tasks through existing list and Gantt APIs.
3. User opens `AutoScheduleDialog`.
4. Frontend submits scheduling request to backend.
5. Backend returns either:
   - applied result summary,
   - non-persisted preview result,
   - or fail-fast validation error.
6. On successful apply, frontend refreshes:
   - current production tasks,
   - Gantt data,
   - work order `quantityScheduled`,
   - current issue summaries.

Replan flow:

1. User selects replan scope and preservation strategy.
2. Frontend submits replan request.
3. Backend returns protected-task impacts and result summary.
4. Frontend refreshes current tasks and Gantt after successful apply.

Data ownership:

- The frontend must treat backend production tasks as the source of truth after apply.
- Preview data is disposable and must not be treated as a saved version.
- Existing task create/update/delete flows remain available subject to permissions.

## Error States

The UI must handle these fail-fast errors without changing current task data:

- Missing route.
- Missing route process.
- Route dependency cycle.
- Missing workstation/resource.
- Missing calendar or no working shift.
- Missing capacity plan.
- Actual capacity required but missing.
- Blocking material shortage.
- Quantity exceeds work order quantity.
- Attempt to overwrite finished task.
- Permission denied.
- Request scope is empty.

Error display requirements:

- Show a short summary toast.
- Show a detailed issue list in the issue drawer/dialog.
- Preserve user input so the user can correct parameters.
- Do not show success when backend rejects the request.

## Accessibility and Responsive Behavior

- Controls must be reachable by keyboard.
- Dialogs and drawers must have clear focus return behavior.
- Issue tables must support horizontal overflow on smaller screens.
- Gantt dependency, risk, and lock indicators must not rely on color alone; use icon/text/tooltip combinations.
- Existing desktop-heavy scheduling workflow is acceptable for first version, but controls must not overlap on common laptop widths.

## Open Questions

- Should generate/apply be a single confirmation flow, or should preview be mandatory before apply?
- Which task states should be considered "manual preserved" in the UI?
- Where should task locking live: Gantt row action, task detail dialog, or task list action?
- Should material risk be shown at work order level, task level, or both?

## Design Blockers

- Final backend API response shape is not yet approved.
- Task lock/manual-preserve model is not yet approved.
- Material shortage policy is not yet approved.
- Capacity mode labels and defaults are not yet approved.
