# BDD Scenarios

## Purpose and Scope

Define observable behavior for the six requested capabilities before implementation.

## Evidence Reviewed

- Prior terminal-batch personal-console fix and E2E gate.
- Existing batch void status, action lock, archive invalidation, work-task cancellation primitive, and workbench terminal-batch filtering.

## Feature Scenarios

### Scenario 1: Effective void makes batch terminal

Given an eDHR batch execution is voidable and has a valid void reason/signature/approval context  
When the void becomes effective  
Then the batch status is `VOIDED` and normal processing actions are locked as audit-only.

### Scenario 2: Effective void cancels active workbench tasks

Given an eDHR batch has work tasks in待处理、处理中、逾期、已完成、and已取消 states  
When the batch void becomes effective  
Then only待处理、处理中、and逾期 tasks are changed to已取消 with reason and completed time  
And `DONE` and existing `CANCELED` tasks remain unchanged.

### Scenario 3: Personal workbench excludes voided batch tasks

Given a user owns one normal active batch task and one task from a voided batch  
When the user opens personal workbench task list and statistics  
Then only the normal active batch task appears and is counted.

### Scenario 4: Old task links remain blocked

Given a user has an old link containing `workTaskId` from a voided batch  
When the user opens the link or calls the open-task API  
Then the backend fails fast with terminal batch status  
And the frontend does not treat the action as successful.

### Scenario 5: Void history is traceable

Given a batch is voided after tasks were issued  
When an auditor reviews change, signature, archive, and work-task records  
Then the void event, void reason, invalidated archive, canceled task status, cancellation reason, and original task ownership are retained.

### Scenario 6: Follow-up work uses controlled flow only

Given a batch is already voided  
When users need additional work for the same business context  
Then the old canceled work tasks cannot be reused  
And users must use an approved reopen, supplement, reexecute, or new-batch flow.

## Failure Scenarios

- Given task cancellation fails during effective void, when the void transaction executes, then the void must fail or roll back instead of leaving a voided batch with active tasks.
- Given the void request lacks required reason or approval process, when submitted, then existing fail-fast validation blocks it.
- Given frontend refresh fails, when workbench reloads, then the UI shows the real failure and does not clear tasks as if canceled.

## Boundary Scenarios

- Already voided batch remains invalid for another effective void unless the existing idempotency contract explicitly allows it.
- Batch with no active work tasks can still be voided and audited.
- Batch with only `DONE` tasks should not rewrite completed task history.
- Historical dirty data may still be hidden by terminal-batch query filtering, but new effective voids must cancel active tasks.

## Open Questions

- Whether a one-time data repair is required for historical voided batches that still have active task rows.

## Test Blockers

- Missing schema evidence for required task status/reason/completed fields.
- Missing real personal-console route, login, tenant, or target test data for E2E.
