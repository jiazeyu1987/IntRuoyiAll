# Frontend Design

## Purpose and Scope

Ensure the user-facing workbench behavior matches the backend void invariant: voided batches are audit-only and cannot be processed from old or newly refreshed workbench surfaces.

This design intentionally avoids adding a new fallback UI. The backend remains source of truth.

## Evidence Reviewed

- Prior verified issue in `doc/tasks/20260726-edhr-personal-console-open-task-status/`: a待处理 work task from a `VOIDED(60)` batch appeared in personal console and caused a terminal-status toast when opened.
- Existing backend mapper now filters terminal batch statuses out of actionable workbench queries.
- `openTask` continues to block closed, archived, rejected, and voided batches.

## Pages and Routes

Affected surfaces:

- Personal console / user profile task area.
- eDHR personal task list backed by `edhr-work-task/my-page`.
- Work task statistics backed by `edhr-work-task/stats`.
- Approval center待办 and candidate signature待办 surfaces.
- Direct old task links that include `workTaskId`.

No new route is required for this feature.

## Components

Expected UI behavior:

- Actionable待办/逾期 lists should not render tasks from voided batches after refresh.
- Existing completed/history views may continue to show completed historical tasks if their API already supports history.
- If a stale browser tab tries an old action, display the backend terminal-state error clearly.
- Do not hide backend failures behind success toasts or local optimistic removal.

## State and Data Flow

The frontend should rely on backend responses:

- Refreshing the workbench should call the normal task list/stat APIs.
- Since backend cancels active tasks and filters terminal batches, the default task lists should naturally exclude voided work.
- If an already-open page receives a terminal-state error, local state can refresh after showing the error, but it must not claim the action succeeded.

## Error States

- Terminal old-link open: show clear error from backend and keep user on current page or return them to task list.
- Refresh failure: show the real request failure; do not clear the list as if all tasks were resolved.
- Permission failure: show permission error; do not infer that the task was canceled.

## Accessibility and Responsive Behavior

No new visible component is required. Existing error message and list-refresh behavior should remain keyboard and screen-reader compatible according to current project patterns.

## Open Questions

- Whether the product wants a separate read-only trace shortcut from canceled work tasks. Current design says no new route; trace remains through batch/detail/audit surfaces.

## Design Blockers

- If real personal-console route or task APIs are unavailable, E2E must be blocked rather than replaced with API-only success.
