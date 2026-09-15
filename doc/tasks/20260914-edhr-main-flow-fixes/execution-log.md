# Execution Log

## Persistent Main-flow Goal

- Previous goal turn classification: progress; code audit identified ordinary-path blockers that determine this implementation slice. The full goal remains active, not reduced to the prior 133-test scope.
- BDD: production before ERP pick list -> Given an active order has valid frozen process/material configuration but its pick list has not synced, When production staff open the process and submit signed production, Then configuration and production entry are available without inventing input batches; completion must resolve actual pick-list evidence and fail if still absent.
- BDD: automatic multi-process evidence -> Given production and inspection are confirmed for multiple processes, When PQC approves and backfills their controlled inspection/loss forms, Then automatic evidence can become effective without downstream manual fill todos or assigning the production responsibility to the PQC approver.
- BDD: manual responsibility remains enforced -> Given a manual route form assigned to production staff, When another user submits through the public form endpoint, Then the actual submitter is checked and rejected; automatic-backfill authority cannot be requested via frontend form data.

## Follow-up audit fixes

- User authorized fixing the two remaining findings directly in int_main. Resume this task instead of creating duplicate records.
- BDD: cross-order completion -> Given A produces 100 and a reviewed allocation assigns 100 to B, When B completes and prepares production release, Then all source readers follow B's allocation to A's signed event, retain source identity A, and use B's allocated quantity; wrong route, missing event or mismatched feedback must fail.
- BDD: dynamic loss release -> Given a reviewed replenishment creates positive formal loss and LOSS_REPORT uses a dynamic form, When PQC approves production release, Then the effective form instance and audit evidence are retained under their own types and release continues; missing or incomplete evidence must fail.
- BDD: mixed loss evidence -> Given traditional and dynamic loss records are both present, When release receipts and signing evidence are generated, Then neither source set is dropped or mislabeled as the other.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderCompletionProgressPortImplTest#allocatedSourceOrderEventCanCompleteTheTargetOrder,MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest#dynamicLossEvidenceMustSurviveFormalDossierWrite" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, missing allocated source event during completion and dynamic loss receipt rejected by the dossier gate.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#activeOrderListProgressIncludesAllocatedSourceFromAnotherOrder,MesTeamLeaderOrderProcessCompletionServiceTest#recalculationKeepsLastEventWithinTheRemainingAllocatedSources" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, list progress could not find the source event and completion lastEventId retained removed event 1001 instead of remaining event 1002.
- Implementation: list/progress/loss/dossier readers now follow current target allocations to source event IDs; feedback still must belong to its source event, and target allocation/route/process/signature checks remain. Batch writer accepts the allocated source work order without rewriting it. Recalculation sets lastEventId from the surviving allocation.
- Implementation: dynamic loss instance IDs, audit IDs and audit hashes propagate through dossier receipt, PQC decision, stored JSON/replay, controller and frontend types. Traditional execution IDs remain separate. Missing dynamic audit or inconsistent no-loss evidence blocks release.
- REGRESSION: 9-class follow-up run -> 158 tests, 128 passed, 30 failed; all failures belonged to existing MesTeamLeaderActiveOrderServiceTest cases, not the new cross-order case.
- BASELINE: original ActiveOrderServiceImpl extracted from HEAD into task-owned followup-baseline/classes; same JUnit Launcher/classpath executed 73 tests -> 42 passed, 31 failed. Failure-set comparison proved the same 30 old failures plus the new cross-order list test; that new test passes only on the fixed code. Shared source/classes and main backend were not replaced.
- GREEN: final 14-class/method selection in verification-report.md -> PASS, 133 tests, zero failures/errors/skips, completed 2026-09-14 23:06:47.
- GREEN: `pnpm ts:check` -> PASS, exit 0 after adding typed dynamic-loss fields.
- GREEN: `git -c core.safecrlf=false diff --check` -> PASS.
- GREEN: follow-up consolidated verification-report.md passed backend-api-delivery and bug-regression-fix-loop evidence validators.
- Follow-up experience consolidation merged cross-order source discovery and typed-receipt chain checks into the existing backend-development.md gate; no new long-term document was created.
- Follow-up status set to ready_for_closeout after 133-test PASS and TypeScript PASS. Cleanup will remove only this round's temporary runner, baseline classes and logs after preserving their results in the three core task records.
- Follow-up CLOSEOUT: cleanup preview returned ready with no blocked paths/warnings; apply returned applied/exit 0. Both ran on int_main, linked=False, and removed only task-owned temporary artifacts. All production code and formal tests remain.
- Final follow-up status: blocked only at Git commit/push gate per current AGENTS.md; the two requested fixes, scoped tests, evidence updates and cleanup are complete. No Git commit/push, real-data writes, E2E, deployment or backend restart was performed.

## BDD Scenarios

- BDD: EDHR-STATIC-008 scrap verdict consistency -> Given a confirmed PQC task whose samples all pass but scrap quantity is greater than zero, When process-inspection evidence is written and later checked for release, Then producer, writer, and readiness checks all treat the task as failed unless approved by the formal QA disposition path.
- BDD: EDHR-STATIC-020 formal loss source -> Given production process loss exists but no matched production replenishment order is present, When the production leader completes the order, Then the system asks for explicit no-replenishment confirmation and records no formal loss only after confirmation.
- BDD: EDHR-STATIC-020 replenishment-driven loss -> Given matched production replenishment orders exist for the work order materials, When the production leader completes the order, Then formal loss evidence and release loss records use the replenishment order identity, quantity, batch, and source document instead of frontline process-loss quantities.
- BDD: EDHR-STATIC-023 cross-workorder allocation -> Given source order A has a production event and the leader allocates its quantity to target order B, When the allocation is confirmed, Then B progress is calculated from the target allocation quantity while preserving A as the source event identity.
- BDD: EDHR-STATIC-026 conditional loss form not applicable -> Given a route contains a conditional loss form requiring actual formal loss, When no formal loss applies, Then task creation, filling progression, progress statistics, and final readiness all treat the loss form as not applicable.

## RED Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesOutputMaterialProgressCalculatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, the new scrap verdict test reported a non-empty `PQC_QA_ITEM_MISMATCH` blocker and the cross-workorder allocation test threw `PRODUCTION_EVENT_IDENTITY`.

## GREEN Evidence

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesOutputMaterialProgressCalculatorTest,MesTeamLeaderOrderProcessCompletionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 39 tests passed.

## Continued implementation

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseLossSourceReaderTest#noReplenishmentMustAskForCompletionConfirmation" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, no-replenishment confirmation blocker was absent.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportWriterTest#reusableProcessLossDoesNotRequireFormalLossDocumentAfterNoReplenishmentConfirmation" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, writer returned LOSS_HAS_ACTUAL_LOSS_CONFLICT from process loss after explicit no-formal-loss confirmation.
- RED: `node tests/e2e/team-leader-no-replenishment-confirmation-static.spec.cjs` -> FAIL, completion confirmation handler absent.
- GREEN: `node tests/e2e/team-leader-no-replenishment-confirmation-static.spec.cjs` -> PASS, confirmation, cancellation, retry errors and request wiring verified without browser/E2E.
- Backend first regression: 44/45 passed; one existing dynamic loss summary assertion still expected process-loss reason instead of replenishment material. Updated that assertion to the new business source; rerun pending.
- Replenishment sources retain all header/item identities and document numbers. Materialized loss record ID is separate from replenishment source IDs. Unreviewed/invalid or unmatched replenishment is rejected; it cannot be confirmed away as absent.

## Notes

- GREEN: final 15-class backend regression command in verification-report.md -> PASS, 210 tests, 0 failures/errors/skips, completed 2026-09-14 22:25:10.
- GREEN: backend-api-delivery `validate_backend_api.py --evidence doc/tasks/20260914-edhr-main-flow-fixes/verification-report.md` -> PASS.
- GREEN: frontend-feature-delivery `validate_frontend_feature.py --evidence doc/tasks/20260914-edhr-main-flow-fixes/verification-report.md` -> PASS.
- GREEN: bug-regression-fix-loop `validate_bug_regression.py --evidence doc/tasks/20260914-edhr-main-flow-fixes/verification-report.md` -> PASS.
- GREEN: `git -c core.safecrlf=false diff --check` -> PASS after final implementation and documentation updates.
- Experience consolidation: merged reusable source-transition, confirmation replay and conditional-task evaluation checks into existing docs/backend-development.md.
- Current status set to ready_for_closeout before cleanup preview/apply. The consolidated verification-report.md is the retained skill evidence, so validator outcomes are preserved after temporary artifacts are removed.

- BDD: confirmed completion replay -> Given completion has recorded the leader's no-replenishment confirmation, When release application resumes against that completion receipt, Then source revalidation consumes the recorded confirmation instead of requesting it again or conflicting on idempotency.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrConditionalLossFlowTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, no-loss form blocked the next record and prevented READY_TO_CLOSE.
- GREEN: targeted 8-class flow regression -> PASS, 63 tests passed before the final boundary extensions.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest#missingScrapEvidenceCannotBeAssumedZero+overflowScrapQuantityCannotBecomeZero" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, both invalid scrap sources reached report-binding checks rather than being rejected as invalid PQC evidence.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderCompletionServiceTest#releaseRetryReusesPersistedNoReplenishmentConfirmation" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, persisted confirmation was lost on completion replay, producing an idempotency conflict.
- GREEN: `pnpm ts:check` -> PASS, exit 0; frontend checked using the repository's configured relaxed TypeScript project.
- GREEN: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> PASS after extending the original three-field request contract with the authorized optional confirmation field.
- GREEN: `node tests/e2e/team-leader-workbench-sfc-style-compile-static.spec.cjs` -> PASS.
- GREEN: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs` -> PASS.
- REGRESSION: 16 backend classes -> 391 tests, 382 passed, 9 failed. All 9 failures were in MesProEdhrBatchExecutionServiceTest.
- BASELINE: extracted that service from HEAD 70b53cf270fb989b48c3850e315e691011d69fe4, compiled into a task-owned isolated classes directory and executed the unchanged test class using JUnit Launcher 1.12.2 and the same test classpath. Result: 185 tests, 176 passed, the identical 9 failed. Workspace production source/classes were not replaced; no main runtime service was touched.
- Baseline failed methods: getReviewTimeline_blocksMissingRouteTasksWhenFrozenSnapshotIncomplete; closeCreatesArchiveWorkTaskAfterBatchClosedWhenFinalInspectionDossierPending; previewTask_generatesDynamicRouteFormSignatureMarkersFromSignatureRules; getDetail_blocksMissingRouteTasksWhenFrozenSnapshotIncomplete; qualityReject_unarchivedBatch_marksRejectedSignsAndCancelsActiveTasks; openOrCreateFromScheduleCompletion_usesFrozenRouteVersionCandidateWhenCurrentConfigDrifts; openOrCreateFromScheduleCompletion_acceptsRouteBindingCandidateForInitialFillTask; openExistingBatch_shouldRecoverMissingRouteProcessTasks; openOrCreate_prefersActiveRouteVersionSnapshotWhenLiveProcessConfigStale.

- Current branch: `int_main`.
- CLOSEOUT: `task_closeout.py --task-id 20260914-edhr-main-flow-fixes --mode preview` -> PASS, status ready, no blocked paths or warnings; all delete paths resolved inside the current task directory.
- CLOSEOUT: same command with `--mode apply` -> PASS, status applied; removed only task-owned baseline classes/source, runner, classpath and temporary logs. Kept the three core task records and all production/formal regression files.
- Duplicate-task review: older edhr-static-findings/reopened-static-fixes records concern different earlier scopes (002/012 or 003/005/008/011/013); they were not modified or used as a completion gate for this task.
- Final status: blocked only at Git integration, because current AGENTS.md disallows commit/push without explicit current-turn authorization and docs/task-closeout-rules.md requires them for completed. No commit, push, index staging or main-service action was performed. The task's source/test fixes and evidence are complete.
- Existing dirty files before this task: `docs/backend-development.md`, `docs/bugs/20260912-edhr-90-step-static-audit.md`, `docs/bugs/20260913-edhr-additional-logic-audit.md`.
- This task will not commit or push because the current-turn authorization is to fix directly in `int_main`, not to perform Git writes.

## 2026-09-15 continuation

- Previous goal turn: progress (new failing production and submission-context tests plus trusted context implementation); full objective remains active.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrRouteFormFillEffectExecutorTest#verifiedAutomaticEvidenceDoesNotWaitForAManualFillTodo+manualCompletionUsesActualSubmitterRatherThanCreator" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, automatic evidence requires a nonexistent downstream manual todo; manual submission calls creator 99 instead of actual actor 77.
- GREEN: 10-class main-slice selection in goal-main-slice-green.log -> PASS, 103 tests at 2026-09-15 00:36:51. Includes pre-completion production without ERP batches, runtime frozen configuration, completion formal sources, both dynamic ports/writers and effect executor.
- GREEN: connected executor/work-task and BPM submission/callback selection in goal-connected-backfill-green.log -> PASS. Real MES effect executor plus work-task service verifies inspection/loss approval, rejected wrong PQC actor, rejected MAIN bypass and actual manual actor. Persistence is isolated; this is not browser E2E.
- BDD: main form after automatic inspection -> Given an inspection form was completed by verified automatic backfill and its old companion todo belongs to PQC, When the production responsible person completes the main form after all required peer evidence is approved, Then the next process receives its fill todo; no completed automatic todo requires a second human submission. Ordinary manual inspection responsibility remains enforced.
- BDD: completion input-material detail -> Given production was signed before ERP pick-list sync and completion then froze two matching pick-list sources, When the leader opens order detail, Then both frozen batch codes, source document numbers and quantities appear under the configured input material; pre-completion detail remains pending and later ERP edits cannot rewrite completion evidence.

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#completeFillAndCreateNextFill_respectsManualInspectionButAdvancesAfterAutomaticInspection" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 2 cases/1 failure: automatic inspection complete but main filler cannot dispatch next process.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#completeFillAndCreateNextFill_respectsManualInspectionButAdvancesAfterAutomaticInspection+completeRouteFormFillAndCreateNextFill*+createNextFillAfterReview*+createNextFillAfterSpecialNodeResolved*,MesProEdhrVerifiedBackfillFlowTest,MesProEdhrRouteFormFillEffectExecutorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 24 tests, 2026-09-15 00:43:37.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderDetailServiceImplTest#completedDetailReadsBatchesFromCompletionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected LOT-A/LOT-B but detail input batches remain empty. Added read-only completion snapshot projection to order detail; original signed production input remains pending at its recording time.
- Audit: special report nodes currently self-block manual dispatch while awaiting upload; after resolution the existing special-node callback resumes route tasks. Selected same-process, special-node and next-process DB-backed regressions pass; did not change this existing sequencing rule based on the initial unconfirmed suspicion.

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderDetailServiceImplTest,MesTeamLeaderActiveOrderCompletionBackfillPortImplTest,MesProEdhrVerifiedBackfillFlowTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 28 tests, 2026-09-15 00:49:56.
- BDD: release reports do not block manual records -> Given PQC release created independent four-report upload todos and ordinary main records are still being confirmed, When a main form completes, Then the next route-form todo is dispatched without waiting for those report uploads; reports and completed ordinary records are both required by final-release readiness. Special prerequisites outside the release-report stage retain their ordering rules.
- Audit correction: the earlier special-node callback conclusion was incomplete. Production-release report completion does not invoke the legacy special-node resume callback, and final manager initialization requires all ordinary records. Need prevent the independent release-report stage from gating manual route-form progression.

- RED: report sequencing DB test with a real RELEASE_REPORT_NODE todo -> FAIL, expected next route FILL count 1 but actual 0. Initial parameterized fixture reused a unique assignment key; corrected the test-only route-process identity, then reproduced the business assertion failure.
- GREEN: release-chain selection in goal-release-chain-green.log -> PASS, 87 tests (2026-09-15 00:58:34), including report uploads, PQC approval, manager readiness/signoff, manual advancement, first-process automatic companion completion and completion details.
- GREEN: frontline input-batch contract, no-replenishment confirmation contract, active-order release-apply contract and pnpm ts:check -> PASS. No browser used.
- Endpoint audit: 3 archive/PDF/history tests PASS. Existing no-pick-list activation test fails because its production-config fixture lacks schemaVersion=1; original baseline had the same failure. Reviewing that boundary also revealed a separate real mismatch: UI materials and frontline reads use batchUseConfigs, but activation and the production-config validator demand materials in productionProcessConfigs, which the production-config UI type/default never defines.
- BDD: one formal material configuration -> Given route materials are configured through the normal input/output controls in batchUseConfigs and production settings contain their own device/loss/parameter fields, When a valid published route is added without an ERP pick list, Then activation freezes the same input/output IDs that frontline uses, without demanding duplicate hidden JSON material settings; missing or duplicate formal material configuration still fails.

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldAddWorkOrderToLeaderActivePoolWithoutPickList" "-Dsurefire.failIfNoSpecifiedTests=false" test` with real UI-shaped configuration -> FAIL, validator demanded duplicate productionProcessConfigs.outputMaterialIds.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldAddWorkOrderToLeaderActivePoolWithoutPickList,MesProRouteCandidateConfigServiceTest,MesFrontlineProcessMaterialServiceTest,MesFrontlineRuntimeConfigServiceTest,MesOutputMaterialProgressCalculatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 30 tests. Activation now freezes input/output IDs from the same batchUseConfigs read by the real material controls and frontline.
- Experience consolidation: updated existing backend-development.md source-timing rule to the user's clarified completion boundary; merged trusted automatic effect, actual actor, sequencing and frontend-to-snapshot contract lessons. No new long-term document created.
- Current status remains in_progress. This continuation made concrete code/test/doc progress; no goal completion claim, no E2E, Git write, deployment, main backend restart or live business DB writes.

- GREEN: activation strengthened snapshot assertion -> PASS, actual inserted process snapshots both contain the formal output IDs and empty configured inputs; no ERP binding created.
- Production/PQC/review/completion expansion: 127 tests initially 126 PASS/1 FAIL because rollback test mocked the obsolete parameter audit overload. Added the real per-material overload fixture; rerun same 12-class selection in goal-production-review-completion-green.log -> PASS, 127 tests, 2026-09-15 01:14:32. Production code unchanged for that fixture correction.
- Frontend verification: edhr-work-task-formcenter-navigation-static and frontline-pqc-switch-task-identity-static PASS. Three older archive/history scripts FAIL and are not described as PASS: final-archive script assumes the old direct manager submit/expectedVersion client call; actual page uses reviewApprovalTask and MesProEdhrApprovalTaskAdapter obtains the authoritative server version. Old history script forbids the present history route; history-entry script expects a download button while the current page exposes authorized printEdhrBatchArchive through getLatestEdhrBatchArchive. Inspected actual UI handlers/API/adapter; these specific old assertions do not demonstrate a broken main path. Scripts were not weakened or deleted.
- Pending audit evidence: current full main-flow matrix still needs source anchors for setup/publication, unified manager approval adapter/finalization, and completion-to-archive boundaries. Broader static script failures remain recorded as obsolete-contract evidence requiring separation from business failures. Do not report E2E or full goal complete.
- BDD: managed active-order finalization closes the source chain -> Given a completed ordinary order has authoritative completion/pick-list/report receipts and management signoff, When the actual release finalizer handles the managed application, Then it writes the release decision, closes the batch, creates archive work, links the decision to the exact origin and closes the active order/work order in the same service flow.

## Final main-flow boundary audit, 2026-09-15

- Previous goal turn: progress. This turn re-read current task/worktree and completed source-level review of publication, finalization and archive boundaries.
- Publication/finalization first expansion: 70 tests, 34 passed/36 errors. Errors were test fixtures: ReleaseService test omitted BatchTraceabilityService dependency; projection mapper mocks did not populate generated binding IDs; workflow test stubbed nonlocking read and omitted required production-config schema. Corrected fixture dependencies/IDs/schema, without changing these production services.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrApprovalTaskAdapterTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest,MesReleaseFinalizationValidatorTest,MesProRouteVersionWorkflowServiceImplTest,MesProRouteVersionPublishProjectionServiceImplTest,MesProRouteVersionBusinessApprovalEffectExecutorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 70 tests, 2026-09-15 01:21:10.
- Source/publication first expansion: 42 tests, 35 passed/7 failed or errored; configuration wiring fixture omitted PickListBindingMapper and lifecycle snapshots omitted the mandatory production-config schema. Corrected fixtures only.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesReleaseAuthoritativeContextPortImplTest,MesReleaseAuthoritativeContextConfigurationTest,MesProEdhrBatchTraceabilityServiceContractTest,MesQaInspectionRegulationServiceTest#publish*+getPublishedVersion*,MesProRouteVersionLifecycleServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 42 tests, 2026-09-15 01:25:25.
- Source audit: unified manager action invokes MesProEdhrApprovalTaskAdapter.review -> releaseService.approve/finalizeRelease -> authoritativeContextPort.require. It loads application, exact Flow-7 origin, immutable completion receipt, all pick-list sources and current MATERIALS_READY receipt before final validation. Final approval writes decision, marks batch CLOSED, creates archive task, links decision to source origin and calls upstream closure with current active-order version.
- Source audit: ERP-synced production orders are CONFIRMED in MesKingdeeProductionOrderSyncServiceImpl (sync creation and update), matching finishWorkOrderForRelease. Manual unconfirmed-order setup is not used to claim this ordinary ERP flow.
- Source audit: archive validates CLOSED state and assigned task, generates/validates PDF-A, persists stored artifact evidence, then marks ARCHIVED and completes archive todo. History page filters archived batches, uses review timeline and authorized latest-archive print action.
- Added managed-active-order finalization regression for the branch that creates source trace links (older close/archive regression covers manual finalization). First compile failed due a missing static test import; corrected import. Runtime verification pending; do not close the goal before this result.

- Continuation after interruption: inspected the original live handle 63132; authoritative terminal result was FAIL at 2026-09-15 01:36:45. Failure was Mockito overriding an existing answer via when(any()) which invoked that answer with null before business execution. Changed only the test override to doAnswer; new handle 27749 runs goal-managed-finalization-recheck.log. No live job was restarted merely due timeout.
- Evidence validators: backend-api-delivery, frontend-feature-delivery and bug-regression-fix-loop all PASS against retained verification-report.md. git diff --check PASS. These validate evidence structure, not proof that pending business tests passed.
- Ownership: docs/e2e-rules.md and docs/experience-index.md appeared dirty outside this task's edits; preserve them and exclude them from task closeout. Current work remains directly in int_main.

- GREEN: managedActiveOrderApprovalClosesBatchAndAppendsDecisionToItsOrigin -> PASS, 1 test at 2026-09-15 08:55:24. Real managed finalizer and DB persistence validate release decision, batch close, archive task call, exact source-origin link and versioned upstream closure. Test doubles and separate coverage are stated in verification-report.md.
- Final requirement-to-code/evidence review: ordinary main-flow code-analysis scope PASS; no pending confirmed defect among indexed 001—032. Prior unfinished notes are superseded by the final report section. E2E remains outside this turn's authorization.
- Experience already merged into existing backend-development.md; no new long-term document. Backend/frontend/bug evidence validators PASS, and their conclusions retained here before cleanup.
- Task set to ready_for_closeout for preview/apply of only task-local logs. No Git commit/push authorized; formal completed status remains pending that explicit gate after cleanup.

Final cleanup: preview ready and apply applied, exit 0, no blocked/warning paths, main worktree int_main. Deleted only the 28 task-local goal-*.log files after recording their outcomes above; retained the three core records and all formal tests/code. Backend/frontend/bug evidence validators and git diff --check PASS. Task status blocked solely for current-turn Git integration authorization per AGENTS.md; code-analysis result remains PASS. Goal status is not marked blocked on this first administrative-gate turn.
- Blocked audit, second consecutive goal turn: no progress on the remaining Git integration gate. Rechecked int_main working tree and retained final verification: changes remain uncommitted, code verification and cleanup are complete, no explicit Git authorization has arrived. Do not rerun completed tests or make unrelated edits to manufacture progress. Goal remains active until the third consecutive blocked turn or user direction.
- Blocked audit, third consecutive goal turn: same ungranted Git commit/push authorization remains. Rechecked working tree and retained task status; implementation, targeted verification and task cleanup are complete. No independent necessary work remains available within current authorization. Mark persistent goal blocked awaiting user direction; preserve task-owned and unrelated changes.

## Authorized Git integration

- User explicitly answered 授权 to task-owned eDHR commit/push. Prior blocker resolved.
- Base commit: 70b53cf270fb989b48c3850e315e691011d69fe4. No unrelated baseline commit: explicit task-only authorization and root ownership rule govern over broad dirty-baseline wording in closeout docs.
- Experience skill invoked again: prior durable lessons already merged in docs/backend-development.md; no duplicate long-term document required.
- Excluded unrelated dirty files: docs/e2e-rules.md and docs/experience-index.md.
- Implementation file manifest (95 paths):
- IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/model/FormActionExecutionContext.java
- IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/model/FormActionInstance.java
- IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeService.java
- IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java
- IntRuoyiBackend/yudao-module-bpm/src/test/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeSubmissionContextTest.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderCompletionReqVO.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderReleaseApplyReqVO.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/MesProductionReleaseController.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/vo/MesPqcProductionReleaseDecisionRespVO.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolEventMapper.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderCompletionBackfillMapper.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesBatchExecutionAuthoritativeContextResolver.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesCompletionBackfillReceipt.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrConditionalLossRequirement.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRouteFormFillEffectExecutor.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskService.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineProcessMaterialServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFlow6CompletionBackfillReceipt.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesOutputMaterialProgressCalculator.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillDraft.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionCommand.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionLossCondition.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionProgressPortImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionService.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplyCommand.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportPlanCommand.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossSourceReadResult.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPort.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionService.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseBusinessReadinessService.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseDecisionResult.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseDossierWriteResult.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteCandidateConfigServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/MesProductionReleaseControllerJsonTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/productionrelease/core/MesReleaseAuthoritativeContextConfigurationTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrConditionalLossFlowTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRouteFormFillEffectExecutorTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrVerifiedBackfillFlowTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitRollbackTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitSnapshotTestSupport.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineProcessMaterialServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesOutputMaterialProgressCalculatorTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionProgressPortImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportWriterTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossSourceReaderTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseBatchExecutionServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionLifecycleServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceImplTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionWorkflowServiceImplTest.java
- IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts
- IntRuoyiFronted/src/api/mes/pro/productionRelease/index.ts
- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue
- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue
- IntRuoyiFronted/src/views/mes/pro/processpool/activeOrderReplenishmentConfirmation.ts
- IntRuoyiFronted/tests/e2e/frontline-input-batch-completion-static.spec.cjs
- IntRuoyiFronted/tests/e2e/team-leader-active-order-release-application-static.spec.js
- IntRuoyiFronted/tests/e2e/team-leader-no-replenishment-confirmation-static.spec.cjs
- docs/backend-development.md
- docs/bugs/20260912-edhr-90-step-static-audit.md
- docs/bugs/20260913-edhr-additional-logic-audit.md
