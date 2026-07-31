# E2E Plan

## Purpose and Scope

Plan real user-path verification after backend implementation. E2E is required for user-visible workbench behavior, while API/DB checks are supporting evidence only.

## Evidence Reviewed

- `docs/e2e-rules.md#eDHR 终态批次个人待办门禁`
- Prior real E2E used personal console route `http://localhost:8081/user/profile` and verified target voided task was absent from API/page.

## User Paths

- Responsible user opens personal console after a batch is voided.
- Responsible user refreshes personal task statistics.
- Responsible user attempts to open an old work-task link for the voided batch.
- Auditor or authorized user opens batch/detail/change history to confirm traceability, if a current UI entry exists.

## Browser or Client Steps

1. Confirm frontend/backend runtime URLs from current branch and runtime rules before E2E.
2. Login with an authorized test tenant/account, not production baseline data.
3. Navigate to personal console.
4. Search or observe task list for the task code belonging to the voided batch.
5. Assert the canceled/voided task is not visible as actionable待办.
6. Attempt stale link/open action for the old `workTaskId`.
7. Assert terminal-state error is shown and no submit/fill request succeeds.

## API Verification

Allowed supporting checks after UI path:

- `edhr-work-task/my-page` and `edhr-work-task/stats` do not include the voided batch task.
- Read-only DB/API check confirms batch status `VOIDED`.
- Read-only DB/API check confirms active tasks were changed to `CANCELED` with reason.
- Read-only DB/API check confirms completed tasks remain `DONE`.

## Console and Log Checks

- Browser console must not show unhandled frontend exceptions during refresh or stale-link handling.
- Backend should not log swallowed cancellation failures.
- No password, token, or signature secret may be recorded.

## Test Blockers

- Missing authorized test account, tenant, current batch/work task fixture, or local runtime blocks E2E.
- If write-type setup is needed to create a voided batch, test data ownership and cleanup must be documented before execution.
- API-only verification cannot replace the personal-console browser path.
