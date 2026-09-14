# Verification Report

## Status

- Backend implementation status: verified.
- Branch runtime E2E status: verified on slot 3 (`8084/48084`).
- Overall task status: completed; implementation, `int_main` merge/fusion, post-merge E2E, and cleanup are verified.

## Verified Behavior

- Product design docs validate the 6 requested capabilities at PRD, user-flow, and acceptance-criteria level.
- System design docs validate backend/API, frontend, data-model, and config/security/deployment boundaries.
- Acceptance design docs validate BDD, strict TDD, real E2E, and test-data planning.
- BPM-approved batch void calls `MesProEdhrWorkTaskService.cancelActiveTasksByBatch` with deterministic reason `批次已作废：<reasonText>`.
- Direct platform batch void calls the same work-task cancellation boundary.
- Batch status, archive invalidation, and change-event effective state remain on the existing transaction path.
- Terminal-batch workbench filtering remains intact.
- Direct, BPM, and golden-finger bulk void regressions remain passing.
- Real frontend E2E confirms BPM-required batch void is approved through the approval center, then active workbench tasks are canceled, old task links fail fast, DONE history remains DONE, and the assignee workbench no longer lists the voided batch task.

## Commands

- DESIGN: `python -X utf8 C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root D:\IntRuoyiWorktree\20260727_pici\doc\tasks\20260727-edhr-batch-void-work-task-closure` -> PASS.
- DESIGN: `python -X utf8 C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root D:\IntRuoyiWorktree\20260727_pici\doc\tasks\20260727-edhr-batch-void-work-task-closure` -> PASS.
- DESIGN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\IntRuoyiWorktree\20260727_pici\doc\tasks\20260727-edhr-batch-void-work-task-closure` -> PASS.
- DESIGN: UTF-8 read check over task design docs -> PASS, 11 markdown files.
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL as expected before implementation.
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- REGRESSION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.
- REGRESSION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest,MesProEdhrRecordChangeServiceTest#voidBatchExecution_directPlatformExecutionVoidsBatchWithoutBpmProcess+voidBatchExecution_approvedBpmCallbackMarksBatchVoidedAndArchiveInvalid" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests.
- STATIC: `node --check IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-submit.e2e.cjs` -> PASS.
- STATIC: `node IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-static.spec.js` -> PASS.
- RUNTIME: `Invoke-WebRequest http://127.0.0.1:8084/` -> HTTP 200; `Invoke-RestMethod http://127.0.0.1:48084/actuator/health` -> `{"status":"UP"}`.
- E2E: `EDHR_BATCH_VOID_E2E_BASE_URL=http://127.0.0.1:8084; EDHR_BATCH_VOID_E2E_BACKEND_URL=http://127.0.0.1:48084; node IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-submit.e2e.cjs` -> PASS, batch `900000000855`, change `121`, artifact `doc/tasks/20260727-edhr-batch-void-work-task-closure/e2e-artifacts/edhr-batch-void-work-task-20260726174912.json`.
- PRE-COMMIT REGRESSION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks,MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches,MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests, 0 failures, 0 errors.
- MERGE: `f5979a45 merge: integrate pici batch void task closure` integrated the worktree branch into `int_main` after preserving pre-existing dirty changes in baseline commits `b585f4b4` and `423c89b3`.
- BUILD: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS for merged `int_main`.
- POST-MERGE RUNTIME: frontend `http://127.0.0.1:8081/` -> HTTP 200; backend `http://127.0.0.1:48081/actuator/health` -> `UP`.
- POST-MERGE E2E: `EDHR_BATCH_VOID_E2E_BASE_URL=http://127.0.0.1:8081; EDHR_BATCH_VOID_E2E_BACKEND_URL=http://127.0.0.1:48081; node IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-submit.e2e.cjs` -> PASS, batch `900000000859`, change `122`, artifact `doc/tasks/20260727-edhr-batch-void-work-task-closure/e2e-artifacts/edhr-batch-void-work-task-20260726180948.json`.

## Pending Verification

- No functional verification remains.

## Closeout Verification

- CLEANUP PREVIEW: `task_closeout.py --task-id 20260727-edhr-batch-void-work-task-closure --mode preview` -> ready, no blocked paths or warnings.
- CLEANUP APPLY: `task_closeout.py --task-id 20260727-edhr-batch-void-work-task-closure --mode apply` -> applied, deleted only task-owned runtime logs and preserved task records, design docs, evidence docs, and E2E artifacts.
- FINAL GUARD: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `int_main/int_main`, frontend `8081`, backend `48081`.
- FINAL PUSH: `git push origin int_main` -> PASS; final audit verified local `int_main` synchronized with `origin/int_main` after push.

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，作废生效点调用既有工作任务取消服务边界。
- `是否存在临时补丁或绕过`：否。
