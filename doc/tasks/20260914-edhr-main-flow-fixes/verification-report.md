# eDHR主流程修复验证

最新结果见文末Follow-up章节：本轮修复跨订单完工链及动态损耗放行，共133项定向测试通过。此前210项及9项基线失败为上一轮证据。

## Scope / Feature / Bug

在现有 `int_main` 工作区修复 EDHR-STATIC-008、020、023、026，以及完成订单时无补料信息确认与回执重放。基线为 `70b53cf270fb989b48c3850e315e691011d69fe4`；任务开始前已有三份文档修改，保持原改动。

## Expected / Acceptance / Contract

1. PQC报废大于零时，正式过程检验writer与提交判定一致；缺失或溢出的正式数量明确失败。
2. 完工及放行正式损耗以已审核生产补料单实补数量为依据，按订单编号和冻结输入物料匹配。所有单据身份、明细、批号纳入证据；多个生产提交不得重复累计同一补料明细。
3. 无补料单时，完工返回 `NO_REPLENISHMENT_CONFIRMATION_REQUIRED`。页面弹框，确认后以 `confirmNoReplenishmentInfo=true` 重试同一幂等键，取消不继续完工。
4. 未审核、数量无效、物料未绑定或多工序归属歧义明确失败，不能按无补料确认放过。成功回执保存确认人和无正式损耗事实；放行重试读取原确认。
5. 补料来源ID集合与物化损耗记录ID分开保存。现场损耗/PQC报废作为过程事实保留，不要求补料数量与现场损耗公式相等。
6. 跨订单分配确认按来源事件和目标分配数量计算生产进度。
7. 条件损耗表使用逐路线工序的正式完工事实；从建批、表单实例、填写待办、前置门禁、进度到最终readiness一致处理不适用。

API变更：完成订单及 `/active-order/release/apply` 请求增加可选布尔字段 `confirmNoReplenishmentInfo`。不新增权限、数据表或迁移；生产组长归属、正式签名与事务回滚校验继续执行。依赖现有ERP补料表/Mapper和订单冻结物料服务。

## Root Cause

- 008：writer只汇总样本判定，未计入正式PQC报废；缺值及溢出可能变成零值。
- 020：正式损耗消费生产反馈数量、分类及原因快照，且与当前分配数量做不适用于补料依据的对平；缺少完成时显式确认链。
- 023：分配属于目标订单，但原始事件按目标工单查询/校验，合法外部来源被判非法。
- 026：只有最终readiness求值，表单及待办创建、前置关系和统计仍按静态requiredFlag处理。
- 确认重放：完工请求确认未从既有回执恢复，导致重放幂等冲突。

## BDD / Reproduction / Validation

BDD: 补料作为正式依据 -> Given 现场损耗与补料数量不同，When 完工及放行写表，Then 使用补料实补数并保留现场事实。

BDD: 无补料确认 -> Given 完工时无补料单，When 首次申请，Then 要求确认；When 取消，Then 零后续写入；When 确认，Then 完工并记录确认人。

BDD: 条件表单 -> Given 本工序无正式损耗，When 建批、填表及检查进度，Then 不适用损耗表不生成填写任务、不阻断后续、不稀释进度。

BDD: 确认重放 -> Given 已保存无补料确认，When 放行重新校验完工回执，Then 沿用原确认并检查当前来源。

RED: 损耗reader新增确认场景 -> FAIL，旧代码没有确认要求。

RED: 损耗writer现场可复用损耗场景 -> FAIL，旧代码返回LOSS_HAS_ACTUAL_LOSS_CONFLICT。

RED: 条件损耗表门禁/进度 -> FAIL，后续记录被阻挡，批次无法READY_TO_CLOSE。

RED: 缺失及溢出PQC报废证据 -> FAIL，未拒绝非法证据而进入表单绑定检查。

RED: 既有确认回执重放 -> FAIL，确认丢失导致幂等冲突。

RED: 前端无补料确认处理 -> FAIL，缺少处理器。精确命令和原始失败摘要均在execution-log.md。

## GREEN / Verification

后端最终命令（在 `IntRuoyiBackend`）：

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesOutputMaterialProgressCalculatorTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderCompletionBackfillPortImplTest,MesTeamLeaderActiveOrderCompletionServiceTest,MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest,MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest,MesBatchExecutionAuthoritativeContextResolverTest,MesProEdhrConditionalLossFlowTest,MesProEdhrBatchExecutionTaskGateTest,MesProEdhrWorkTaskServiceImplTest,MesProductionReleaseBusinessReadinessServiceTest,MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

GREEN: 上述命令 -> PASS，210 tests，0 failures，0 errors，0 skipped；2026-09-14 22:25:10完成。

GREEN: `pnpm ts:check` -> PASS，退出码0，使用项目既有tsconfig.relaxed.json。

GREEN: `node tests/e2e/team-leader-no-replenishment-confirmation-static.spec.cjs` -> PASS。实际执行确认处理器，覆盖正常请求、确认后重试、取消、非确认错误及重试错误；同时检查页面和API字段衔接。

GREEN: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> PASS。新字段加入正式合同，既有回执校验、锁定及不确定响应处理仍受验证。

GREEN: `node tests/e2e/team-leader-workbench-sfc-style-compile-static.spec.cjs` -> PASS。

GREEN: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs` -> PASS。

GREEN: `git diff --check` -> PASS。

这些`tests/e2e/*static*`文件执行静态或隔离逻辑测试，未启动浏览器，不属于真实订单E2E。弹框复用现有Element Plus组件、按钮及加载状态；本次未进行真实页面响应式/键盘操作验收。

## 扩展回归与基线对照

扩展16个后端测试类共391项，382项通过、9项失败。失败全部属于原有 `MesProEdhrBatchExecutionServiceTest`。将该服务从上述HEAD提取并编译到任务独有的baseline/classes，再使用相同测试类、依赖和JUnit Launcher 1.12.2执行：185项、176通过、相同9项失败。工作区源码及共享target/classes未被替换。

| 原有失败方法 | 失败类型 |
|---|---|
| getReviewTimeline_blocksMissingRouteTasksWhenFrozenSnapshotIncomplete | 未抛预期异常 |
| getDetail_blocksMissingRouteTasksWhenFrozenSnapshotIncomplete | 预期阻断未出现 |
| previewTask_generatesDynamicRouteFormSignatureMarkersFromSignatureRules | 签名标记为空 |
| openOrCreate_prefersActiveRouteVersionSnapshotWhenLiveProcessConfigStale | 测试前置绑定为空 |
| openOrCreateFromScheduleCompletion_usesFrozenRouteVersionCandidateWhenCurrentConfigDrifts | 测试前置绑定不存在 |
| openOrCreateFromScheduleCompletion_acceptsRouteBindingCandidateForInitialFillTask | 测试前置绑定不存在 |
| openExistingBatch_shouldRecoverMissingRouteProcessTasks | BATCH_PROVISIONING_RECORD_REQUIRED |
| closeCreatesArchiveWorkTaskAfterBatchClosedWhenFinalInspectionDossierPending | 签名时间校验400 |
| qualityReject_unarchivedBatch_marksRejectedSignsAndCancelsActiveTasks | 签名时间校验400 |

这些失败没有被改成PASS，也不能仅凭测试失败断定新增业务bug。保留为后续测试基线维护事项。

## Blockers / 风险边界

- 本次定向修复验证通过；未执行真实订单生产至归档E2E，因此不承诺任意订单已实测走通。
- 已有物料多工序匹配歧义需要正式配置解决；系统拒绝猜测归属。既有正式回执的业务来源改变仍会被来源哈希校验拒绝。
- 当前AGENTS.md要求当轮明确授权才能提交/推送；本轮仅授权直接修复。未提交/推送、未发布、未写业务数据库、未重启主服务。
- 经验合并到现有docs/backend-development.md，未新建长期经验文件。
- 收尾状态和cleanup结果将在下方记录。

## Evidence Validation

- backend-api-delivery/scripts/validate_backend_api.py -> PASS。
- frontend-feature-delivery/scripts/validate_frontend_feature.py -> PASS。
- bug-regression-fix-loop/scripts/validate_bug_regression.py -> PASS。
- 三项validator均直接检查本verification-report.md；该文件按默认收尾规则长期保留。

## Closeout

- ready_for_closeout状态下cleanup preview通过，无blocked/warnings；随后apply返回applied。
- 当前是int_main主工作区，未执行合并或worktree删除。临时源码、字节码、运行器和日志已清理，三份核心任务记录与正式回归测试保留。
- 最终状态为blocked，仅表示Git收尾未获当轮明确授权；实现、210项定向测试和本次清理均已完成。该状态不代表本次产品修复失败。

## Follow-up / Scope / Contract / Verification

本轮修复审计发现的023遗漏和新增027，继续直接在int_main工作。原有210项通过不能证明这两个遗漏已经闭环，以下是本轮新证据。

### 实现结果

- 列表进度和完工检查从目标订单有效分配回查源事件；不再按目标工单过滤掉其他订单的合法来源。
- 损耗读取及writer分别核对“反馈属于源事件工单”和“分配属于目标订单”，仍检查路线、工序、签名与复核闭环；补料查询继续使用目标订单正式编号。
- PQC生产资料读取只收集目标订单的当前分配和它们引用的事件、复核，避免把源事件分给其他订单的分配一并纳入目标批记录。
- 批记录writer接受合法跨订单事件但不改写其工单身份；重新分配后工序完成记录的最后事件取自保留的分配。
- 动态损耗实例编号与传统执行编号分开传递，审计ID和哈希一起进入PQC决定JSON及重试回执。两层放行门禁均接受完整动态证据；缺失审计或无损耗状态携带损耗实例仍拒绝。
- API增加lossReportFormCenterInstanceIds、lossReportFieldAuditIds、lossReportFieldAuditHeadHashes；Long列表按字符串序列化，前端同步类型。没有修改表结构、权限或服务运行态。

### BDD / RED

BDD: 跨订单来源 -> Given A生产100而B分配60且目标60，When查询列表、完成B及准备放行，Then B生产进度100%，来源仍是A；源反馈不匹配或事件缺失不能通过。

BDD: 动态损耗放行 -> Given有效动态损耗实例及审计，When真实DossierPort与PQC批准服务串联，Then进入REPORT_UPLOAD_PENDING，四份资料任务回执齐全，存储及回放保留动态损耗身份。

RED: 完工进度+动态损耗资料测试 -> FAIL，分别报PRODUCTION_EVENT_FOR_CURRENT_ALLOCATION和损耗回执不一致。

RED: 列表进度+重新分配来源测试 -> FAIL，分别报来源事件缺失和lastEventId仍为已移除事件1001（应为1002）。精确命令保存在execution-log.md。

### GREEN

在IntRuoyiBackend执行：

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderCompletionProgressPortImplTest,MesTeamLeaderActiveOrderServiceTest#activeOrderListProgressIncludesAllocatedSourceFromAnotherOrder,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesPqcReleaseDossierPortImplDynamicInspectionEvidenceTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseControllerJsonTest,MesTeamLeaderActiveOrderCompletionServiceTest,MesTeamLeaderActiveOrderCompletionBackfillPortImplTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesProEdhrRouteFormFillEffectExecutorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

GREEN: 上述命令 -> PASS，133项，0失败/错误/跳过，2026-09-14 23:06:47完成。

GREEN: pnpm ts:check -> PASS，退出码0。

GREEN: git -c core.safecrlf=false diff --check -> PASS。

关键组合测试dynamicLossPassesTheRealDossierPortAndPqcDecisionAndReplaysTypedEvidence调用真实资料汇总和PQC批准服务，验证四任务回执、存储JSON及重复请求；writer和外部系统边界使用测试替身，未宣称数据库事务/E2E验证。跨订单另覆盖真实损耗reader+writer、生产批记录writer、放行来源读取、列表及完工进度入口。

### 原有失败隔离证明 / Blockers

扩展9类测试共158项，128通过、30失败。30项全部为MesTeamLeaderActiveOrderServiceTest既有用例，主要是旧冻结快照/物料配置及领料绑定fixture不符合当前合同。

将HEAD 70b53cf270fb989b48c3850e315e691011d69fe4的MesTeamLeaderActiveOrderServiceImpl编译到任务独有followup-baseline/classes，使用同一测试类、依赖和JUnit Launcher 1.12.2运行：73项，42通过，31失败。失败集合对比：原有30项完全相同，加上新增跨订单列表测试；新增测试在修复代码通过。未替换工作区源码、共享target/classes或正在运行的服务。

原有失败项（仅记录，不冒充PASS）：

- copyLatestSimulationActiveOrderShouldCreateIndependentWorkOrderAndUseCurrentSources
- rebuildActiveOrderShouldDeleteRuntimeHistoryThenRebuildSnapshotsFromCurrentSources
- rebuildActiveOrderShouldResolveLatestPublishedQaVersionInsteadOfRegulationPointer
- shouldAddUsingProductionRouteWhenWorkOrderProductDiffers
- shouldAddWorkOrderToLeaderActivePoolWithServerResolvedProductionRoute
- shouldAddWorkOrderToLeaderActivePoolWithoutPickList
- shouldAllowUnconfirmedWorkOrderWhenProductionRouteExists
- shouldBindSecondPickListWhenActiveOrderAlreadyExists
- shouldCalculateProductionProgressFromFormalRouteWhenActiveOrderSnapshotIsIncomplete
- shouldDeduplicateSamePqcTaskIdentityWhenRegulationContainsRepeatedQaItemRows
- shouldDisplayActiveOrderFromFrozenRouteSnapshotWhenRouteMasterWasDeleted
- shouldFreezeCanonicalDeviceParametersForExactRouteProcess
- shouldGenerateFinalPqcTasksPerQaItemWhenSameQaProcessHasMultipleFinalItems
- shouldGenerateItemScopedPqcTasksForSameQaProcessRuleWhenItemsDiffer
- shouldKeepFirstAndPatrolQuantitiesPerQaProcessWhenProjectFinalInspectionIsDisabled
- shouldListActiveOrdersWithFormalRouteDisplayFieldsUsingBatchQueries
- shouldListActiveOrdersWithPerProcessRemainingAndQuantityConflictAfterOverage
- shouldListActiveOrdersWithProductionProgressByFormalProcessAndInspectionProgressByFixedPqcTasks
- shouldReactivateFrozenRemovedOrderBeforeResolvingRepublishedRoute
- shouldReactivateRemovedActiveOrderWhenSameWorkOrderRouteVersionIsJoinedAgain
- shouldReactivateRemovedOrderByRebuildingLatestPublishedQaVersion
- shouldRecalculateProductionProgressFromCurrentAllocationAfterQuantityReduction
- shouldRecoverRemovedOrderWithBrokenHistoricalQaLockByRebuildingLatestPublishedQaVersion
- shouldRejectActiveOrderWhenPqcTaskIdentityAlreadyExists
- shouldRejectDuplicateParameterCanonicalKeyBeforeSnapshotInsert
- shouldReturnExistingBindingWhenConcurrentSecondPickListInsertConflicts
- shouldRoundPatrolRatioUpForAmAndPmTasks
- shouldSkipInvalidActiveOrderWithoutBlockingValidRows
- shouldSnapshotAllRouteProcessesAndCreateQaOwnedPqcTasks
- simulationCopyAddShouldFreezeLatestSourcesAndPersistSimulationMetadata

本轮已完成两个发现的代码修复及定向验证；未运行真实订单E2E。既有测试基线失败未纳入本轮修复范围，也没有隐藏或删除。未提交/推送Git，最后按AGENTS.md与task-closeout-rules.md记录Git收尾限制。

本轮backend-api-delivery及bug-regression-fix-loop证据validator均PASS，验证对象直接为保留的本报告。经验合并到既有backend-development.md。

Follow-up cleanup preview/apply均PASS（status ready/applied，无blocked/warnings），三份核心记录及正式代码/测试保留。最终任务状态仅因未获当轮Git提交推送授权而为blocked；本轮实现、133项测试与清理已完成。

## 2026-09-15 Persistent Main-flow Audit (current, in_progress)

Added findings 028—032 and fixes on int_main. Full goal is not yet closed.

| Requirement | Current evidence | Scope boundary |
|---|---|---|
| Join without ERP pick list using normal route material controls | Activation test corrected to actual schema and UI-shaped material configuration; original code RED at production JSON output requirement; 30-test selection PASS | Real activation service, mocked persistence; no browser order |
| Start/submit before pick sync | Production-material/runtime/frontline submit tests in 103-test selection PASS | Input evidence explicitly pending; no guessed batches |
| Completion requires and freezes formal sources | Completion backfill/source tests in 103 and 28 selections PASS | Formal ERP source readers isolated from live DB |
| Completed detail shows all matching input batches | Two-source completion snapshot test RED then 28-test selection PASS | Snapshot projection, original production JSON unchanged |
| Automatic dynamic inspection/loss and manual actual actor | 33 BPM/MES tests PASS, then first-process companion included in 87-test selection | Real effect/work-task chain; public form data cannot select automatic mode |
| Ordinary main form advances after automatic inspection | DB-backed parameterized RED then 24/87 selections PASS | Existing manual inspection responsibility preserved |
| Four report uploads independent of ordinary fill dispatch | DB-backed pending report RED then 87-test selection PASS | Final manager readiness still requires both stages |
| PQC/report/manager release handoffs | 87-test selection PASS across service stages | Connected portions plus mocked port boundaries; not full E2E |
| Archive generation, readable PDF, failure rollback and historical timeline | 3 selected BatchExecutionService tests PASS in endpoint audit | Test DB/files only; activation test in same command initially failed fixture |
| Frontend contract and type integrity | 3 static scripts and pnpm ts:check PASS | No real browser run |

Commands and exact selections are in execution-log.md and task-owned goal-*.log while the task remains active. Prior baseline failures remain recorded; they do not prove the full current flow. A final requirement-to-code pass and affected regression review are still required before claiming the entire main flow complete.

Latest checks: 103 main-slice PASS; 33 BPM/connected PASS; 24 auto-advance PASS; 28 detail PASS; 87 release-chain PASS; 30 formal-material PASS. These sets overlap and must not be added into a unique test count.

Additional current evidence: 127 production/PQC/review/allocation/completion tests PASS after fixing an obsolete parameter-audit test stub. Activation's inserted frozen material snapshot assertion also PASS. Current history UI has a working source-level print chain (getLatestEdhrBatchArchive -> printEdhrBatchArchive); unified manager approval routes through reviewApprovalTask -> MesProEdhrApprovalTaskAdapter -> releaseService.approve with server authoritative version. Old scripts expecting direct client expectedVersion submission, forbidding history routes or requiring a removed download button failed; no false PASS recorded and no browser run performed.

## Ordinary workflow source map (90-step description, current audit)

| Steps | Code boundary inspected | Evidence |
|---|---|---|
| 1–14 | RouteFlowGraphDesigner configuration requests; MesProRouteFlowConfigServiceImpl; route publication projection | Normal material-control contract and 028/032 tests; projection includes independent batch bindings/form slots and responsibility rules |
| 15,21 | MesProRouteVersionWorkflowServiceImpl.submitCandidate; LifecycleServiceImpl.publishCandidate; QA regulation publish/getPublishedVersion | 70-test publication/finalization and 42-test source/publication selections PASS |
| 16–20 | QA process/item/version rules; activation snapshots rule type/quantity/version | QA selected publish tests, 127-test PQC context/overlay/aggregation and prior task generation audit |
| 22–28 | TeamLeaderWorkbenchPage -> active-order add -> frozen route/QA/process targets | No-pick-list real activation service and inserted material snapshot assertion PASS; route references retain independent bindings |
| 29–41 | FrontlineFixedTemplatePanel -> signed feedback -> submission review -> allocations/completion | 103-test and 127-test selections PASS; real source attribution and reviewed progress preserved |
| 42–51 | Frontline PQC task context, employee identity, submit, leader review/aggregation | 127-test selection and PQC task-switch frontend static contract PASS |
| 52–56 | MesTeamLeaderActiveOrderDetailServiceImpl and formal completion materialization | 028 two-pick snapshot projection, 020 replenishment/no-loss evidence and detail tests PASS |
| 57–65 | CompletionServiceImpl/ProgressPort/BackfillPort and release-application apply | Both progress and formal evidence checked; 127/103 selections PASS; no-replenishment confirmation contract PASS |
| 66–69 | MesPqcProductionReleaseServiceImpl -> writers -> trusted dynamic effect -> report initializer | 87 release-chain, 33 BPM/MES context selections PASS; automatic evidence independent of manual todo timing |
| 70–75 | Report task uploader -> ReportServiceImpl.complete -> BatchExecutionServiceImpl.completeProductionReleaseReportNode | 87 selection validates four distinct report stages; independent upload tasks do not block ordinary forms |
| 76–79 | WorkTaskBoardPage.reviewApprovalTask -> MesProEdhrApprovalTaskAdapter -> ReleaseServiceImpl.finalizeRelease -> manager approval/closure | 70/42 selections PASS; dedicated managed-active-order finalization test pending |
| 80–84 | BatchExecutionServiceImpl.generateArchive/getReviewTimeline; BatchRecordHistoryPage print handler | Three archive/PDF/history service tests PASS; current frontend print chain inspected; obsolete static assertions recorded separately |
| 85–90 | Existing freeze/correction/conditional-loss/signature audit boundaries retained | Existing 008/016/020/026 regressions; exceptional QA rework/concession not expanded beyond the requested ordinary main flow |

This matrix is a code-analysis/test audit, not a claim of real-browser execution. Final managed closure test and task evidence/closeout gates remain pending.

## Final code-analysis verification, 2026-09-15 08:55

Result: PASS for the authorized ordinary ERP-order code-analysis and targeted regression scope. No remaining confirmed ordinary-main-flow defect was found in the audited frontend/backend path. This is not a real-browser E2E claim or a guarantee covering all configurations and rare events.

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest#managedActiveOrderApprovalClosesBatchAndAppendsDecisionToItsOrigin" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test, 2026-09-15 08:55:24. This runs the actual managed finalization branch and DB-backed release/batch/decision persistence; manager authorization and source-owner ports are isolated test doubles, covered separately by their service tests. It verifies RELEASED decision persistence, exact source-origin trace command, batch CLOSED, archive task creation and versioned active-order/upstream closure command.

The earlier source-map rows marked pending are now resolved: material snapshot assertion PASS; 70 publication/finalization tests PASS; 42 authoritative-source/QA-publication tests PASS; managed finalization 1 PASS. Earlier 103/33/24/28/87/30/127 sets overlap; no fabricated total is claimed. Archive/PDF/history 3 tests and frontend type/static contracts retain their stated scope. Obsolete static scripts remain documented as failures of old assertions, not rewritten into PASS.

Backend, frontend and bug evidence validators PASS on this retained report. `git diff --check` PASS. No live business data writes, browser E2E, deployment, service restart or Git commit/push performed.

Implementation deliverables: bug index 001—032 reflects current reviewed fixes; task-owned code/tests and three retained task records. Git integration remains unperformed because current AGENTS.md requires explicit current-turn authorization; task-closeout-rules.md separately requires commit/push for formal task completion. Task may be ready for cleanup but cannot be marked completed under both rules without that authorization. This administrative gate does not represent an unresolved code defect.

Final cleanup: preview ready and apply applied, exit 0, no blocked/warning paths, main worktree int_main. Deleted only the 28 task-local goal-*.log files after recording their outcomes above; retained the three core records and all formal tests/code. Backend/frontend/bug evidence validators and git diff --check PASS. Task status blocked solely for current-turn Git integration authorization per AGENTS.md; code-analysis result remains PASS. Goal status is not marked blocked on this first administrative-gate turn.
