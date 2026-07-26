# Execution Log

## User Intent

- 用户询问“批次执行已给负责人工作台发任务后，如果批次执行被作废，正确流程是什么”，确认可以做到后，要求“帮我实现上面的 6 条功能，先进行文档设计”。

## Skill And Rule Preflight

- Used skill: `system-design-docs`，用于先形成后端/API、前端、数据模型、配置安全部署设计。
- Used skill: `product-requirements-docs`，用于补齐 6 项能力的 PRD、用户流程与产品验收标准。
- Used skill: `bdd-tdd-acceptance-planner`，用于先形成 BDD、严格 TDD、E2E 和测试数据计划。
- Read: `docs/task-closeout-rules.md`
- Read: `docs/backend-development.md`
- Read: `docs/frontend-development.md`
- Read: `docs/database-rules.md`
- Read: `docs/e2e-rules.md`
- Read: `docs/powershell-memory.md`
- Read: `docs/experience-index.md` relevant route for `eDHR 终态批次个人待办`
- Read: `C:\Users\BJB110\.codex\skills\product-requirements-docs\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\product-requirements-docs\references\prd-structure.md`
- Read: `C:\Users\BJB110\.codex\skills\system-design-docs\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\system-design-docs\references\system-design-structure.md`
- Read: `C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\SKILL.md`
- Read: `C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\references\acceptance-structure.md`

## Existing Workspace Note

- `git status --short --branch` showed branch `int_main` ahead of `origin/int_main` by 1 commit and an existing unrelated untracked task directory `doc/tasks/20260727-personal-workbench-task-hide-restore/`.
- That existing directory is about personal workbench hide/restore and was not modified by this design task.

## Evidence Reviewed

- `MesProEdhrBatchExecutionServiceImpl` defines terminal `BATCH_STATUS_VOIDED = 60` and action lock reason `批次已作废，只能追溯审计`.
- `MesProEdhrBatchVoidEffectServiceImpl#approveVoidBatchExecutionByBpm` currently marks the batch `VOIDED`, clears active context, and invalidates archive.
- `MesProEdhrWorkTaskServiceImpl#cancelActiveTasksByBatch` already cancels待处理、处理中、逾期中的 tasks, stamps reason/remark/completedAt, and revokes runtime task entitlement.
- `MesProEdhrWorkTaskMapper` already filters terminal batch statuses `30/40/50/60` out of actionable personal待办/逾期 surfaces.
- `doc/tasks/20260726-edhr-personal-console-open-task-status/` documents the prior bug where a待处理 task from a `VOIDED(60)` batch was incorrectly displayed on the personal console.

## BDD

- BDD: Void batch cancels active work tasks -> Given a batch has active workbench tasks / When the batch void becomes effective / Then active tasks are canceled with reason and no longer actionable.
- BDD: Voided batch remains blocked from old task links -> Given a user opens an old work task URL for a voided batch / When `openTask` is called / Then the backend fails fast and preserves audit-only access.
- BDD: Voided task history is traceable -> Given a batch is voided after work tasks were issued / When audit history is reviewed / Then original tasks, cancellation reason, signature, archive invalidation, and change event remain traceable.
- BDD: Follow-up execution uses controlled flow -> Given a batch is already voided / When business needs more work / Then users must use controlled reopen/supplement/reexecute/new-batch flow instead of reusing canceled tasks.

## RED / GREEN Evidence

- DESIGN: `python -X utf8 C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root D:\IntRuoyiWorktree\20260727_pici\doc\tasks\20260727-edhr-batch-void-work-task-closure` -> PASS.
- DESIGN: `python -X utf8 C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root D:\IntRuoyiWorktree\20260727_pici\doc\tasks\20260727-edhr-batch-void-work-task-closure` -> PASS.
- DESIGN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root D:\IntRuoyiWorktree\20260727_pici\doc\tasks\20260727-edhr-batch-void-work-task-closure` -> PASS.
- DESIGN: `python -X utf8 -c "...read_text(encoding='utf-8')..."` over task design docs -> PASS, 11 markdown files readable as UTF-8.
- BLOCKED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, existing compile blocker in `MesProEdhrWorkTaskMapper.applyParticipantFilter` returned `LambdaQueryWrapper` from `wrapper.and(...)` where method contract requires `LambdaQueryWrapperX`.
- FIXED: changed `MesProEdhrWorkTaskMapper.applyParticipantFilter` to mutate `wrapper.and(...)` and return the original `LambdaQueryWrapperX` wrapper.
- BLOCKED: RED fixture compile failed because new `MesProEdhrBatchVoidEffectServiceImplTest` used non-existent `MesProEdhrBatchExecutionArchiveDO` builder fields `archiveCode` and `fileId`.
- FIXED: aligned the new test fixture with the actual `MesProEdhrBatchExecutionArchiveDO` fields: `artifactType`, `archiveVersion`, `archiveStatus`, `fileName`, `filePath`, `contentHash`, `sourceManifestJson`, `sealedSignatureId`, and valid flags.
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected Mockito verification failure because both effective void paths did not call `MesProEdhrWorkTaskService.cancelActiveTasksByBatch`.
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests, 0 failures, 0 errors.
- REGRESSION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test, 0 failures, 0 errors.
- REGRESSION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest,MesProEdhrRecordChangeServiceTest#voidBatchExecution_directPlatformExecutionVoidsBatchWithoutBpmProcess+voidBatchExecution_approvedBpmCallbackMarksBatchVoidedAndArchiveInvalid" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests, 0 failures, 0 errors.

## Milestone Updates

- completed: Created task directory `doc/tasks/20260727-edhr-batch-void-work-task-closure/`.
- completed: Wrote product requirements docs for 6 capabilities: PRD, user flows, and acceptance criteria.
- completed: Wrote system design and acceptance/TDD design documents.
- completed: Added RED coverage for BPM-approved batch void and direct platform void effect paths.
- completed: Implemented effective-void work-task cancellation through `MesProEdhrWorkTaskService.cancelActiveTasksByBatch`.
- completed: Verified GREEN and focused backend regressions.
- completed: Ran real frontend E2E on branch runtime slot 3 (`8084/48084`), including BPM-required approval center review and post-approval workbench exclusion.
- pending: Merge/fuse into `int_main` and run post-merge E2E.

## Implementation Notes

- `MesProEdhrBatchVoidEffectServiceImpl#approveVoidBatchExecutionByBpm` now cancels active work tasks after batch status/archive invalidation and before marking the change event effective.
- `MesProEdhrRecordChangeServiceImpl#approveVoidBatchExecutionByBpm` keeps the same invariant for the legacy/direct path that can still set a batch to `VOIDED`.
- Cancellation reason is deterministic: `批次已作废：<reasonText>` with `remark` only as a documented secondary source; if neither value exists, the flow fails with the existing reason-required error.
- No fallback, catch-and-continue, mock success, physical deletion, or frontend-only hiding was introduced.

## Real E2E Evidence

- STATIC: `node --check IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-submit.e2e.cjs` -> PASS.
- STATIC: `node IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-static.spec.js` -> PASS.
- RUNTIME: `Invoke-WebRequest http://127.0.0.1:8084/` -> HTTP 200; `Invoke-RestMethod http://127.0.0.1:48084/actuator/health` -> `{"status":"UP"}`.
- BLOCKED: real E2E first reached both void submit endpoints, but timed out waiting for `approval-resolution` because the UI requests approval policy when the void dialog opens, before submit. Artifact: `doc/tasks/20260727-edhr-batch-void-work-task-closure/e2e-artifacts/edhr-batch-void-work-task-20260726174516.json`.
- FIXED: moved `approval-resolution` response wait before the row `作废` click and added BPM-required approval-center completion using the actual `act_ru_task` assignee mapped to `system_users`.
- GREEN: `EDHR_BATCH_VOID_E2E_BASE_URL=http://127.0.0.1:8084; EDHR_BATCH_VOID_E2E_BACKEND_URL=http://127.0.0.1:48084; node IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-submit.e2e.cjs` -> PASS, batch `900000000855`, change `121`, artifact `doc/tasks/20260727-edhr-batch-void-work-task-closure/e2e-artifacts/edhr-batch-void-work-task-20260726174912.json`.
- REGRESSION: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrRecordChangeServiceTest#voidBatchExecution_approvedBpmCallbackCancelsActiveWorkTasks,MesProEdhrBatchVoidEffectServiceImplTest#executeDirectPlatformVoidBatchExecution_cancelsActiveWorkTasks,MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches,MesProEdhrBatchExecutionGoldenFingerBulkVoidServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests, 0 failures, 0 errors.

## Experience Consolidation

- GREEN: experience-preflight -> PASS, read `project-experience-consolidation` and merged reusable E2E lessons into existing `docs/e2e-rules.md` plus `docs/experience-index.md`; no new long-term document was needed.
- EXPERIENCE: added `eDHR 作废 BPM 审批真实 E2E 门禁` covering early `approval-resolution` wait timing, actual BPM assignee mapping, approval center review path, and post-void workbench verification.

## int_main Merge And Post-Merge Verification

- BASELINE: `b585f4b4 chore: preserve int_main dirty baseline before pici merge` saved pre-existing `int_main` dirty tracked/untracked changes before fusion.
- BASELINE: `423c89b3 chore: preserve residual int_main task artifact` saved a residual empty patch artifact that appeared after the first baseline commit.
- MERGE: `f5979a45 merge: integrate pici batch void task closure` merged `codex/20260727_pici` into `int_main`; conflicts were limited to task documentation and were resolved by keeping the completed implementation/E2E evidence.
- BUILD: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS, produced merged `yudao-server-exec.jar`.
- RUNTIME: `int_main` frontend `http://127.0.0.1:8081/` -> HTTP 200; backend `http://127.0.0.1:48081/actuator/health` -> `UP`, PID `44480`, command line points to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- GREEN: post-merge real E2E `EDHR_BATCH_VOID_E2E_BASE_URL=http://127.0.0.1:8081; EDHR_BATCH_VOID_E2E_BACKEND_URL=http://127.0.0.1:48081; node IntRuoyiFronted\tests\e2e\edhr-batch-void-form-center-real-submit.e2e.cjs` -> PASS, batch `900000000859`, change `122`, artifact `doc/tasks/20260727-edhr-batch-void-work-task-closure/e2e-artifacts/edhr-batch-void-work-task-20260726180948.json`.
- STATUS: implementation, branch E2E, `int_main` merge, post-merge E2E, cleanup, and final push are complete.

## Closeout

- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-edhr-batch-void-work-task-closure --mode preview` -> ready, no blocked paths, kept design/evidence/artifact directories, planned deletion only for `int-main-backend-48081.err.log` and `int-main-backend-48081.out.log`.
- CLEANUP APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-edhr-batch-void-work-task-closure --mode apply` -> applied, deleted only the two task-owned runtime log files.
- FINAL STATUS: completed; closeout, final guard, and `int_main` push are complete.

## Final Push Reconciliation

- BASELINE: `978fcaf0 chore: preserve residual process advance e2e changes` saved the unrelated residual dirty change in `IntRuoyiFronted/tests/e2e/edhr-work-task-process-advance-real.e2e.js` before final `int_main` push, per dirty-worktree baseline policy.
- BASELINE: `590879d8 chore: preserve residual process advance fixture changes` saved the later unrelated fixture-column dirty change in the same process-advance E2E file before final `int_main` push.
- BASELINE: `a07369af chore: preserve process advance closeout docs` saved unrelated dirty closeout records under `doc/tasks/20260727-edhr-process-fill-advance-optimization/` that appeared during final audit.
- FINAL GUARD: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, `int_main/int_main` frontend `8081`, backend `48081`.
- PUSH: `git push origin int_main` -> PASS; final audit verified local `int_main` synchronized with `origin/int_main` after push.
- FINAL STATUS: `git status --short --branch` -> `## int_main...origin/int_main`; no local ahead/behind delta remains.
