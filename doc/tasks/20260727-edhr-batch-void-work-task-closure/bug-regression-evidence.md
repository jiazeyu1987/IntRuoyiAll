# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: eDHR batch execution void paths could mark a batch `VOIDED` while leaving previously issued workbench tasks active, relying only on workbench query filtering as defense in depth.
- Expected behavior: once batch void becomes effective, active workbench tasks for the batch must be canceled in the same controlled backend flow, while historical done/canceled tasks remain traceable.

## Reproduction Command Or Path

- Reproduction command: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- RED result: FAIL, Mockito verified that both effective void paths had zero interactions with `MesProEdhrWorkTaskService.cancelActiveTasksByBatch`.

## Root Cause

- Root cause: `approveVoidBatchExecutionByBpm` in the batch void effect service and the legacy record-change service updated batch/archive/change-event state but did not call the existing work-task cancellation service boundary.
- Secondary compile blocker found during RED setup: `MesProEdhrWorkTaskMapper.applyParticipantFilter` returned the result of `wrapper.and(...)`, which did not preserve the method's `LambdaQueryWrapperX` return type.

## Regression Test Added Or Updated

- Added `MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks`.
- Updated `MesProEdhrRecordChangeServiceTest` with `voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks`.
- Added `@MockitoBean MesProEdhrWorkTaskService` to verify the effective void side effect without duplicating work-task persistence logic.

## RED

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected active work-task cancellation call was missing.

## GREEN

- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests, 0 failures, 0 errors.

## Verification

- VERIFICATION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, terminal-batch workbench filtering remains intact.
- VERIFICATION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest,MesProEdhrRecordChangeServiceTest#voidBatchExecution_directPlatformExecutionVoidsBatchWithoutBpmProcess+voidBatchExecution_approvedBpmCallbackMarksBatchVoidedAndArchiveInvalid" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, direct, BPM, and golden-finger batch void behavior remains intact.
- VERIFICATION: `node --check IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-submit.e2e.cjs` -> PASS.
- VERIFICATION: `node IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-static.spec.js` -> PASS.
- VERIFICATION: real E2E on `http://127.0.0.1:8084` / `http://127.0.0.1:48084` -> PASS, batch `900000000855`, change `121`, artifact `doc/tasks/20260727-edhr-batch-void-work-task-closure/e2e-artifacts/edhr-batch-void-work-task-20260726174912.json`.

## Risk And Regression Scope

- Regression scope covered: terminal-batch workbench filtering, BPM batch void, direct platform batch void, and golden-finger bulk void delegation.
- No fallback or catch-and-continue path was added; if task cancellation fails, the effective void transaction fails rather than producing an inconsistent state.
- Remaining risk: post-merge E2E still needs to confirm the user-visible workbench path on `int_main`.

## Blockers And Follow-Up Actions

- No backend unit/regression or branch runtime E2E blocker remains.
- Follow-up: merge/fuse into `int_main` and run post-merge E2E.
