# Test Data

## Purpose and Scope

Define required data for backend tests and real-path E2E of batch void work-task closure.

## Evidence Reviewed

- Existing backend tests can insert batch execution and work task rows in test context.
- Prior real E2E used a specific voided batch/task to prove personal-console filtering.

## Required Test Data

Backend unit/integration tests:

- One voidable batch execution with active work tasks:
  - 待处理 fill task.
  - `DOING` fill task.
  - `OVERDUE` review or fill task.
  - `DONE` task for history preservation.
  - Existing `CANCELED` task for idempotent no-op boundary.
- Valid void change event, approval/direct policy context, and signature context matching existing test helpers.
- Optional archive row to verify archive invalidation remains intact.

Real E2E:

- Authorized non-production tenant.
- Responsible user account that owns or can view a task for the target batch.
- Task-owned batch/test data identifiable by this task ID.
- If using existing data, only read it unless the user explicitly authorizes write setup.

## Reset Procedure

Backend tests should use transactional test isolation or test database cleanup already provided by the module.

E2E write setup, if later approved, must record:

- Created batch code and task code.
- Original state for any adjusted row.
- Cleanup or rollback procedure.
- Confirmation that no production/admin baseline data is modified.

## Data Ownership

All newly created E2E data must be task-owned and traceable to `20260727-edhr-batch-void-work-task-closure`.

Existing unrelated data must not be modified for convenience.

## Test Blockers

- Missing current schema for work-task cancellation fields.
- Missing tenant/account credentials.
- Missing current runtime that serves the modified backend.
- Existing historical dirty rows are not sufficient as write verification unless explicitly selected and approved.
