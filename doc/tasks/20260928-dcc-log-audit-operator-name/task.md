# DCC Access Audit Operator Name Resolution

## Goal

Fix targeted controlled-file audit log name resolution when an access log has no `user_id` but its linked, file-scoped access event has a user.

## BDD

- BDD: event user resolves the operator name -> Given a targeted query for a 19-digit controlled-file ID, an access log with `user_id = null`, and its linked access event with a known user ID; When the audit log page is queried; Then `operatorUserId` and `operatorName` both come from the linked event user.
- BDD: access-log user retains precedence -> Given a linked access event and access log with different non-null user IDs; When the targeted audit log page is queried; Then the access-log user remains the displayed operator.
- BDD: event scope remains file-bound -> Given unrelated events outside the requested controlled-file ID; When the targeted audit log page is queried; Then user-name resolution only includes the already-scoped event set and no unrelated event/log row is returned.

## Milestones

1. Read backend, database-test, and task-closeout rules; inspect the current implementation and `BaseDbUnitTest` patterns.
2. Add regression coverage and run the focused test to record RED.
3. Add event users from the existing scoped event collection to the user-name lookup set; preserve access-log precedence and event filtering.
4. Run the focused test and `git diff --check`; record verification and closeout readiness.

## Expected Verification

- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileLogQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `git diff --check`

## Design Constraints

- Write set: `DccControlledFileLogQueryServiceImpl.java`, `DccControlledFileLogQueryServiceTest.java`, and this task's `doc/tasks/20260928-dcc-log-audit-operator-name/` files only.
- Do not widen access-event or access-log queries; only use the existing event collection already scoped to `controlledFileId`.
- Keep access-log `user_id` as the first-choice identity, falling back to the linked event user only when absent.
- Use `BaseDbUnitTest` fixtures; do not modify external database state, Git history, or running services.
- Preserve unrelated pre-existing worktree changes.

## Current Status

ready_for_closeout
