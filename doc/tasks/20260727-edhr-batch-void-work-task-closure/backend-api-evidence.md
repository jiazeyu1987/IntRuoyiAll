# Backend API Evidence

## Scope

- Service boundary: eDHR batch execution effective void paths in `MesProEdhrBatchVoidEffectServiceImpl` and `MesProEdhrRecordChangeServiceImpl`.
- Handler behavior: when a batch void becomes effective, the backend marks the batch `VOIDED`, invalidates the latest batch archive, cancels active workbench-facing tasks, and then marks the change event `EFFECTIVE`.
- No new public endpoint or response contract was added.

## API Contract And Data Contract

- Existing void request contracts remain unchanged: `EdhrRecordChangeRequestReqVO` still carries `batchExecutionId`, `reasonCategory`, `reasonText`, `password`, and optional `comment`.
- Existing void response contract remains unchanged: `EdhrRecordChangeRespVO` returns the persisted change event state.
- Data side effect: `MesProEdhrWorkTaskService.cancelActiveTasksByBatch(batchExecutionId, reason)` is invoked for active work tasks only; completed and already canceled task history remains owned by the existing work-task service.
- Cancellation reason format: `批次已作废：<reasonText>`; if `reasonText` is unavailable, `remark` is the secondary source; if both are blank, the existing reason-required service exception is raised.

## Auth, Permission, Validation, And Error Behavior

- Existing password, BPM, golden-finger, release-lock, duplicate-change, and status validation paths are preserved.
- Work-task cancellation is inside the same transactional effective-void flow; failures are not swallowed and roll back the transaction.
- Old work-task links remain protected by the existing terminal batch guard; this change does not loosen `openTask`.
- No fallback, graceful degradation, compatibility shim, or default-success value was introduced.

## Required Config, Services, Fixtures, And Migrations

- Required service: `MesProEdhrWorkTaskService`.
- Required fixture fields: batch execution with voidable terminal-predecessor status, optional sealed batch archive, and valid void reason/password context.
- Database migration: not required; existing work-task cancellation fields and batch archive fields are reused.
- Runtime config: unchanged.

## BDD Scenarios

- BDD: Effective void makes batch terminal -> Given a voidable batch / When void becomes effective / Then batch status is `VOIDED` and actions are audit-only.
- BDD: Effective void cancels active workbench tasks -> Given workbench tasks exist / When batch void becomes effective / Then active tasks are canceled through the work-task service.
- BDD: Old task links remain blocked -> Given an old task link for a voided batch / When opened / Then backend fails fast instead of allowing reuse.
- BDD: Follow-up work uses controlled flow only -> Given a batch is voided / When more work is needed / Then users must use approved reopen, supplement, reexecute, or new-batch flow.

## RED

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, Mockito expected `cancelActiveTasksByBatch` calls but observed zero interactions in both effective void paths.

## GREEN

- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests, 0 failures, 0 errors.

## Contract And Regression Verification

- REGRESSION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, workbench terminal filtering remains intact.
- REGRESSION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest,MesProEdhrRecordChangeServiceTest#voidBatchExecution_directPlatformExecutionVoidsBatchWithoutBpmProcess+voidBatchExecution_approvedBpmCallbackMarksBatchVoidedAndArchiveInvalid" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, direct, BPM, and golden-finger batch void behavior remains intact.

## Observability Touchpoints

- Existing change event status, reason, approval actor/time, effective time, archive invalidation, and work-task cancellation reason remain auditable.
- No additional logs were added because the existing persisted audit trail carries the required business evidence.

## Blockers And Downstream Skill Needs

- Branch runtime real frontend E2E on slot 3 (`8084/48084`) passed; pending downstream verification is post-merge E2E on `int_main`.
- No schema, migration, or config blocker was found for the backend implementation.
