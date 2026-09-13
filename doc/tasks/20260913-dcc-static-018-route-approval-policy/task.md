# DCC-STATIC-018 Route Approval Policy Alignment

## Task Goal

Fix DCC-STATIC-018: DCC approval routes must not allow editable approval method, approval ratio, or required-switch values that are not consumed by the fixed four-stage BPMN execution model.

## Milestones

- [x] M1: Record BDD and planned RED for the fixed route approval policy.
- [x] M2: Add focused regression/static contracts proving unsupported route approval policy values are rejected or not editable.
- [x] M3: Apply the minimal backend and frontend fix for the fixed four-stage policy.
- [x] M4: Run targeted verification without E2E, service start/restart, database writes, remote operations, or Git commit/push.
- [x] M5: Update evidence, closeout status, and cleanup records.

## Expected Verification

- Backend targeted unit/static contract for route approval policy.
- Frontend targeted static contract for route form fixed policy UI and payload.
- Branch runtime port guard before commit/push.
- Task branch commit/push and closeout integration after 2026-09-13 user authorization.
- `git diff --check`.
- No E2E, no local service start/restart, no database writes.

## Current Status

ready_for_closeout

Implementation and required verification are complete. User authorized Git closeout on 2026-09-13, and task-owned changes were migrated from the detached Codex worktree to the registered `D:\IntRuoyiWorktree\20260913-dcc-static-018-route-approval-policy` worktree for compliant commit, push, and integration.

## Design Constraints Check

- Fixed DCC BPMN is authoritative for this task scope.
- Backend must reject unsupported `approveMethod`, `approveRatio`, and `required` values instead of saving misleading evidence.
- Frontend must present fixed approval rules as read-only or non-editable and submit only supported values.
- Do not change other DCC-STATIC items.
- Do not modify shared bug inventory unless only updating DCC-STATIC-018 status/evidence.

## Cleanup Keep

- doc/tasks/20260913-dcc-static-018-route-approval-policy/task.md
- doc/tasks/20260913-dcc-static-018-route-approval-policy/execution-log.md
- doc/tasks/20260913-dcc-static-018-route-approval-policy/verification-report.md
