# 流程 8 全 MES 回归失败分类与 Owner 矩阵

## 1. 证据边界

本清单只消费流程 8 独立 worktree 中已经产生的 Surefire XML，不重跑测试、不连接数据库、不写入运行态，也不修改流程 8 业务代码。证据源为：

`D:\IntRuoyiWorktree\20260822-flow-repair-08-design-development\IntRuoyiBackend\yudao-module-mes\target\surefire-reports\TEST-*.xml`

只读聚合结果为 479 个 suite、3575 tests、59 failures、93 errors、19 skipped。以下 152 条 failure/error 均从 XML 逐条列出；`skipped` 单独归入覆盖缺口，不作为业务 RED。直接属于流程 8 四材料/最终放行 gate 的 failure/error 为 0：流程 8 定向 215 tests 已在其报告中通过，但这不等同于全链路 Go。

分类代码是每个测试行的 primary owner；`E` 是二次环境/fixture/依赖标记，不会把环境问题写成业务 RED。

## 2. Owner 矩阵

| 代码 | primary 范围 | 行数（F/E） | 责任 owner | 是否阻断流程 8 | 最小复现 | 后续动作 |
|---|---|---:|---|---|---|---|
| `F8-GATE` | 四节点 COMPLETED/APPROVED、manifest/hash、MATERIALS_READY、唯一 RELEASED | 0 (0/0) | 流程 8 / 流程 10 | 否（本基线无直接失败） | `mvn -pl yudao-module-mes -Dtest=<class>#<method> test` | 保留四节点定向绿证；不得用本行数为 0 宣称全链路通过 |
| `F7-TRACE` | pre-release Origin/TraceLink、正式来源关系、来源快照和放行追溯 | 5 (4/1) | 流程 7；最终状态联动由流程 10 owner | 是（映射未完成时必须 `TRACE_MAPPING_BLOCKED`） | 同上，逐行替换类/方法 | 修 resolver/source snapshot/Tx-C；先映射完成再允许流程 8 gate |
| `A456` | 流程 4/5/6/9/10 的回填、损耗、签名、批次执行和批记录报告 | 84 (37/47) | 对应流程 owner；批记录报告由流程 6，回填/损耗由流程 4/5 | 回填、签名、批次创建行为为是；报告导入行为为条件 | 同上 | 由对应线程修复，流程 11 只保留合同回归和阻断证据 |
| `PAR` | 前线运行时、反馈、排产/路线/QA、ERP 和其它并行 MES 模块 | 63 (18/45) | 前线/运行时、排产路线、ERP、反馈等对应 owner | 条件（会阻断上游来源/签名，但不是流程 8 材料 gate 自身） | 同上 | 通知对应 owner；不得由流程 8 线程越权修改 |
| `ENV` | XML 已暴露的 fixture、H2 schema、缺少资源、Mockito 严格 stub、依赖/工具问题 | 二次标记 | 测试基础设施/fixture owner | 否，除非该 fixture 是正式来源前置 | 同上或先修复 fixture | 先修环境再重跑原测试；不得把它改写为业务 PASS/RED |

### Owner 与阻断规则

- `F8-GATE` 仍必须保持四个独立节点：来料检报告、灭菌报告、成品检报告、成品检记录；节点有效状态为 `COMPLETED`，有批准字段时还必须 `APPROVED`，version/file hash/source snapshot hash/manifest 必须一致。成品检报告和成品检记录不可互代。
- `F7-TRACE` 的 5 行是流程 8 的前置阻断证据；它们不能通过上传材料或默认成功绕过。流程 7 owner 必须先完成 Origin/TraceLink 映射，流程 8 才能判断材料门禁。
- `A456` 中 `MesP0*`、`MesProEdhrBatchExecution*` 以及三类正式回填/批次报告行为会影响流程 6 的 BATCH_* 或流程 4/5 receipt；这类行的 `blocksFlow8=Y`。批记录 Word/路线导入 fixture 行只标 `C`（条件），不能被误写为流程 8 gate failure。
- `PAR` 中前线签名/运行时行标 `Y`（上游完成前置未满足），排产、路线、反馈、ERP、QA 行标 `C`；它们由相应 owner 处理。`batchrecordcelllink` 的前端 `pnpm run ts:check` 失败是 `PAR+ENV`，不属于流程 8 Java failure。
- slot=31 按 runtime v6 合法范围 1..50 处理，不计入 152 行，不是业务 failure，也不是 blocker。

逐条行的 `blocksFlow8` 继承规则：`F7-TRACE=Y`；团队回填/损耗/签名/批次行 `A456=Y`；批记录报告 `B6/B7=C`（仅在其结果作为正式来源或批次输入时升级为 Y）；`E-FIX=N`；Word parser ownership `N`；`PAR` 前线 CAS/运行时 `Y`，排产/路线/反馈/ERP/QA `C`。因此每个清单行都能由自身前缀和所属分组确定阻断结论，不以“全量失败”替代逐条判断。

## 3. 逐条 failure/error 清单

下列每个 `kind class#method` 对应一个 XML testcase；同一分组共享表 2 的 root-cause/owner/repro/action 判定。`F` 为 failure，`E` 为 error；`E` 只表示二次环境/fixture 标记时仍保留原始 error，不会被降级为 PASS。

### `PAR`：排产、路线、QA 与权限（29 行）

- `E cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.MesProScheduleOrderControllerTest#exportScheduleOrderExcel_usesSelectedColumnsAllRowsAndExportPermission`
- `E cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.MesProScheduleOrderControllerTest#getScheduleOrderPage_backfillsWorkOrderCodeFromLinkedWorkOrderWhenScheduleCodeMissing`
- `E cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.MesProScheduleOrderControllerTest#getScheduleOrderPage_manualFinishedKeepsLockedSummaryAndRealProcessMetrics`
- `E cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.MesProScheduleOrderControllerTest#getScheduleOrderPage_returnsProductionMaterialListSummaryByWorkOrderId`
- `E cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.MesProScheduleOrderControllerTest#getScheduleOrderPage_returnsProgressAggregatedFromProcessSnapshots`
- `F cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.MesProSchedulerWorkbenchControllerPermissionContractTest#writeEndpoints_shouldNotReuseQueryPermission`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#getConfigRespListByRouteVersionId_shouldReadCandidateScheduleSnapshotForReadonlyStatuses(String)[1]`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#getConfigRespListByRouteVersionId_shouldReadCandidateScheduleSnapshotForReadonlyStatuses(String)[2]`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#getConfigRespListByRouteVersionId_shouldReadCandidateScheduleSnapshotForReadonlyStatuses(String)[3]`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#getConfigRespListByRouteVersionId_shouldReadCandidateScheduleSnapshotForReadonlyStatuses(String)[4]`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#getConfigRespListByRouteVersionId_shouldReadDraftCandidateScheduleSnapshot`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldAcceptManualOverrideAndKeepHourlyCapacityAsExplicitOverride`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldCreateDraftCandidateConfigWhenSnapshotDoesNotContainProcess`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldIgnoreItemThatDoesNotBelongToRoute`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldNotSynchronizeActiveWipSnapshotsForDraftCandidate`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldSaveResourceCalculatedDraftSnapshotWithoutActiveWipSync`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldUpdateHistoricalRouteProcessConfigInsteadOfRejectingIt`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldUpdateInfiniteFormulaAndNightShiftRequirementWithoutItemDimension`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldUpdateSameRouteProcessConfigAcrossDifferentItems`
- `E cn.iocoder.yudao.module.mes.MesProRouteScheduleConfigServiceTest#saveConfig_shouldWriteDraftCandidateScheduleSnapshotWithoutUpdatingActiveConfig`
- `F cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowConfigServiceImplTest#saveRouteFlowConfig_shouldRejectPartialGlobalGroupTamperingWithoutSavingSnapshot`
- `E cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionPlatformAdapterTest#publishCandidate_shouldSupersedePlatformActiveAndActivateCandidate`
- `F cn.iocoder.yudao.module.mes.service.pro.schedule.MesProAutoScheduleServiceImplTest#replanPreview_shouldExtendRouteProcessVirtualWindowsWhenPriorOrdersConsumeDailyCapacity`
- `F cn.iocoder.yudao.module.mes.service.pro.schedule.MesProAutoScheduleServiceImplTest#replanPreview_shouldUseLatestPublishedUnboundRouteProcessCapacityInsteadOfCurrentProcessWorkstation`
- `E cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderAdmissionTest#createFromWorkOrder_shouldMarkReadyRouteAsAutoSchedulable`
- `F cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderPreflightServiceTest#preflight_shouldNotBlockInvalidBatchRouteConfigWhenDefaultReportMissing`
- `F cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderPreflightServiceTest#preflight_shouldNotBlockMissingBatchCodeWhenBatchRouteEnabled`
- `F cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderServiceImplTest#updateScheduleOrder_shouldPersistAllowedFieldsAndWriteOperationLog`
- `F cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationWordImportServiceTest#importWordDraft_upgradesAndInheritsExactNamedItemConfiguration`

判定：候选路线快照不完整、权限方法块缺失、排产预览计数/状态不符和 QA Word fixture 不属于流程 8 材料 gate。`MesProRouteScheduleConfigServiceTest` 还带有 `ENV` 二次标记（测试快照 fixture 不完整）；owner 为排产/路线线程，最小复现为对应类方法命令，先修复正式快照 fixture 后再复跑。

### `PAR`：前线、反馈与 ERP（28 行）

- `F cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesFrontlineActiveOrderInitialAllocationContractTest#submitMustCreateFormalInitialAllocationWithFullOutputQuantity`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackSubmitServiceTest#shouldCreateFeedbackRecordbookAndProcessPoolEventInSingleCommand`
- `F cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackImportRecordServiceImplTest#attributeImportRecord_shouldResolveCheckFlagFromFrozenScheduleRouteProcess`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#approveFeedback_shouldRejectScheduleLinkedFeedbackWithoutFrozenProcessSnapshot`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testApproveFeedback_keyCheck_enterUncheck`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testApproveFeedback_keyNonCheck_success`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testApproveFeedback_nonCheck_uncheckQuantityReject`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testApproveFeedback_nonKey_directFinish`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testApproveFeedback_nonKeyCheck_directFinishAndCleanUncheck`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testApproveFeedback_shouldUseFrozenRouteProcessForScheduleSnapshot`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testCreateFeedback_autoLinksScheduleOrderAndProcessSnapshot`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testCreateFeedback_shouldAllowRouteProcessTaskWithoutBoundWorkstation`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testCreateFeedbackWithScheduleSnapshot_shouldKeepFrozenRouteProcessForScheduleSnapshot`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testCreateFeedbackWithScheduleSnapshot_shouldResolveZeroScheduleProcessIdByRouteProcess`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testRejectFeedback_shouldReturnToDraftAndPersistRejectReason`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testSubmitFeedback_shouldSyncScheduleProgressWhenLinked`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testUpdateProFeedbackWhenIpqcFinish_feedbackAlreadyFinished`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testUpdateProFeedbackWhenIpqcFinish_feedbackNotUncheck`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testUpdateProFeedbackWhenIpqcFinish_success_allQualified`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#testUpdateProFeedbackWhenIpqcFinish_success_withUnqualified`
- `E cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackServiceImplTest#updateFeedback_shouldPreserveFrozenScheduleSnapshotInsteadOfRefillingCurrentEffectiveSchedule`
- `F cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmissionConcurrencyTest#conflictingContentConcurrentTransactionsAllowOneCasWinnerAndRejectTheOther`
- `F cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmissionConcurrencyTest#differentActualEmployeesShareOneTaskIdempotencyDomainUnderConcurrency`
- `F cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmissionConcurrencyTest#sameContentConcurrentTransactionsReturnOneReceiptAndOneFormalWriteSet`
- `F cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineRuntimeConfigProcessScopeTest#submitValidationMustUseCurrentRouteProcessForLossDeviceAndParameterRules`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderErpPlannedStartTest#shouldGeneratePqcTasksByQaProcessItemAndRuleKeyWhenSameProcessHasMultipleFinalItems`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderErpPlannedStartTest#shouldKeepUnscheduledCandidateEligibleWhenErpPlannedStartMissing`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderErpPlannedStartTest#shouldUseJoinedDateForUnscheduledPqcTasksWhenErpPlannedStartMissing`

判定：前线 CAS/签名、feedback `loss_reason_id` schema、运行时快照和 ERP planned-start 是流程 2/3/4/5 或并行 owner 的前置问题。前线签名/损耗相关行标 `blocksFlow8=Y`，反馈/ERP 行标 `C`；不得由流程 8 修复。`MesProFeedbackServiceImplTest` 的 18 条 `loss_reason_id` H2 错误标 `ENV` 二次标记，先由 fixture/schema owner 修复。

### `A456`：流程 4/5/6 团队回填与批次（27 行）

- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionLegacyProcessTest#resolveBatchTaskConfigs_shouldKeepFrozenHistoricalRouteProcessBinding`
- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionTaskGateTest#resolveTaskGate_shouldAllowDynamicCompanionFormBeforeMainApproved`
- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionTaskGateTest#resolveTaskGate_shouldBlockWhenPredecessorSnapshotDoesNotResolveToTask`
- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionTaskGateTest#resolveTaskGate_shouldEnforceSequentialModeInsideSameProcess`
- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionTaskGateTest#resolveTaskGate_shouldUnlockPreRouteSpecialNodeBeforeRouteForms`
- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionTaskGateTest#resolveTaskGate_shouldUnlockSiblingBranchesAfterSharedPredecessorApproved`
- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionTaskGateTest#resolveTaskGate_shouldWaitForPriorRouteFormsBeforePostRouteSpecialNode`
- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionTaskGateTest#resolveTaskGate_shouldWaitOnlyForDirectPredecessor`
- `E cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskLegacyProcessTest#calculateDueTime_shouldUseFrozenRouteProcessRuleForHistoricalWorkTask`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ActiveOrderFifoClosedLoopTest#shouldPassOnlyThisConfirmationQuantitiesToFifoConsumptionAcrossActiveOrders`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ActiveOrderFifoClosedLoopTest#shouldRejectManualAllocationWhenCurrentProcessRemainingIsInsufficientBeforeTerminalWrites`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0ActiveOrderFifoClosedLoopTest#shouldRejectManualAllocationWhenTotalDoesNotMatchSubmittedQuantityBeforeTerminalWrites`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0BatchRecordBackfillClosedLoopTest#shouldWriteAuditableBackfillCommandWithSourceValuesOldValueHashCellLocationAndIdempotencyKey`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0PqcQualityAllocationGateTest#shouldPersistConfirmationOnlyAfterSuccessPqcBindingIsVerified`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0PqcQualityAllocationGateTest#shouldPersistFifoConsumptionFromProductionSubmitFragmentsBeforeTerminalWrites`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0PqcQualityAllocationGateTest#shouldRejectFailedPqcBindingBeforeAnyTerminalWrites`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0PqcQualityAllocationGateTest#shouldRejectFifoConfirmationWhenPersistedConsumptionLeavesQualifiedQuantityShort`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0PqcQualityAllocationGateTest#shouldRejectMissingStructuredPqcBindingBeforeAnyTerminalWrites`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0PqcQualityAllocationGateTest#shouldRejectSuccessPqcWhenAnyFormalSampleFailsBeforeTerminalWrites`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0TeamLeaderReviewSignatureServiceTest#confirmSubmissionShouldPersistReviewSignatureOnApprovalReview`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0TeamLeaderReviewSignatureServiceTest#confirmSubmissionShouldRejectMalformedReviewSignatureSnapshotBeforeReviewOrAllocationWrites`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0TeamLeaderReviewSignatureServiceTest#confirmSubmissionShouldRejectMissingReviewSignatureBeforeReviewOrAllocationWrites`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0TeamLeaderReviewSignatureServiceTest#confirmSubmissionShouldRejectMissingReviewSignatureSnapshotBeforeReviewOrAllocationWrites`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0TeamLeaderReviewSignatureServiceTest#confirmSubmissionShouldRejectSignatureUserDifferentFromLeader`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0TeamLeaderReviewSignatureServiceTest#reviewSubmissionShouldPersistStructuredReviewSignature`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesP0TeamLeaderReviewSignatureServiceTest#reviewSubmissionShouldRejectSignatureUserDifferentFromLeader`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationFrontlineSnapshotGuardTest#frontlineInitialAllocationMustNotRequireOrderProcessTargetSnapshot`

判定：流程 6 task gate 的 7 条 `Method not found` 是实现/合同不同步，legacy 两条是 Mockito strict-stub fixture；P0 FIFO/PQC/leader signature/backfill 行直接覆盖流程 4/5 的 receipt、损耗和签名前置，标 `blocksFlow8=Y`。最小复现为单类单方法 Maven 命令；对应 owner 必须先取得 Tx-A 成功才 `BACKFILL_SUCCEEDED`，失败不得写 failure receipt。

### `A456`：批记录报告与 Word 导入（56 行）

以下方法均来自 `cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest`；`F`/`E` 是原始 XML 类型。`B6` 为流程 6 批记录/版本行为，`B7` 为正式 DCC/路线来源或快照前置，`E-FIX` 为缺少 Word/route/H2 fixture 的环境标记。

- `E B7 recognizeUploadedRoute_whenCurrentVersionRouteDiffersFromPreflightRoute_acceptsSelectedCurrentRouteProduct`
- `E B7 preflightUploadedRoute_whenPendingApprovalVersionAlreadyExists_locksImportActions`
- `E-FIX uploadExtraFormSlot_whenLossReportWordHasMergedBody_expandsAllFillableFieldsAndDoesNotReuseOldHashReport`
- `F B6 recognizeUploadedRoute_rejectsEmptyProductNamesAndRollsBack`
- `F B6 recognizeUploadedRoute_whenDccProjectProductAlreadyBoundToOtherRoute_rollsBackGeneratedContent`
- `E B7 preflightUploadedRoute_keepsStaleVersionWhenBusinessReferenceExists`
- `F B6 recognizeUploadedRoute_whenProductInfoMissing_rollsBackAllGeneratedContent`
- `F B6 recognizeUploadedRoute_whenProductInfoNotFirst_rollsBackAllGeneratedContent`
- `E B7 recognizeUploadedRoute_withSubmitterSubmitsV2ToV3UpgradeApprovalImmediately`
- `E B7 recognizeUploadedRoute_whenSameNameAndRouteExistsWithUpgrade_createsPendingVersionSnapshotWithoutMutatingCurrent`
- `F B6 recognizeUploadedRoute_whenAllSelectedProductNamesHaveNoCode_rollsBackAllGeneratedContent`
- `F B6 preflightUploadedRoute_whenBatchRecordNameDiffersFromDccProjectName_rejectsBeforeLookup`
- `F B6 recognizeUploadedRoute_whenMultipleHistoricalReferencesRemain_listsAllCleanupEntrances`
- `F B6 recognizeUploadedRoute_whenNoMainReportsButExecutionExistsWithoutUpgrade_rejectsAsExistingBatchRecord`
- `E-FIX dccProjectGovernanceStatus_exposesCurrentVersionNosForLinkedObjects`
- `F B6 recognizeUploadedRoute_whenOnlyProductInfo_rollsBackAllGeneratedContent`
- `F B6 recognizeUploadedRoute_whenPendingApprovalVersionExists_rejectsBeforeRecognizerAndWritesNothingNew`
- `E B7 recognizeUploadedRoute_whenDeletedBatchRecordReimported_startsFromV1Again`
- `E B7 recognizeUploadedRoute_usesUploadedWordBytesAndRouteScopedMetadata`
- `E B7 preflightUploadedRoute_ignoresStaleVersionWhenNoCurrentMainReportOrProductBindingExists`
- `F B7 recognizeUploadedRoute_whenSelectedProductIdNotBoundToPreflightRoute_stillFailsFast`
- `F B6 recognizeUploadedRoute_whenBatchRecordNameDiffersFromDccProjectName_failsFastBeforeRecognizer`
- `E B7 recognizeUploadedRoute_whenDisabledRouteDraftBindingCandidateExists_updatesSnapshotStatusToEnabled`
- `E B7 recognizeUploadedRoute_generatesEnabledRouteAndBatchRecordRouteBindingsSkippingProductInfo`
- `E B7 recognizeUploadedRoute_whenUpgradingRoute_keepsStableProcessConnectionInfoOnActiveRoute`
- `E B7 recognizeUploadedRoute_whenSameHashPrecheckVersionTargetsOlderVersion_voidsOldPrecheckAndCreatesRequestedApproval`
- `F B6 recognizeUploadedRoute_whenUpgradeMissingExpectedSourceVersion_failsFastBeforeGatewaySave`
- `E B7 recognizeUploadedRoute_whenUpgrade_createsStructuredPhaseTwoMigrationDiffAndBlocksUntilConfirmed`
- `F B6 recognizeUploadedRoute_whenSameNameAndRouteExistsWithoutUpgrade_failsFastBeforeGatewaySave`
- `E B7 recognizeUploadedRoute_whenProcessNameAlreadyExists_reusesExistingProcessWithoutCreatingDuplicate`
- `E-FIX importPilotDocTwice_updatesExistingRowsInsteadOfDuplicating`
- `E-FIX uploadExtraFormSlot_usesSelectedProductNameAndSlotDisplayNameInListMetadata`
- `E B7 recognizeUploadedRoute_whenOnlyDefinitionVersionsRemain_cleansOrphanAndStartsFromV1`
- `E B7 recognizeUploadedRoute_acceptsUploadedWordWithSixteenParsedTemplates`
- `E B7 recognizeUploadedRoute_whenSameHashReimportedForPendingVersion_returnsIdempotentResultWithoutNewSnapshots`
- `F B6 recognizeUploadedRoute_whenExistingVersionWithoutMainReportsHasExecution_rejectsV1Reset`
- `E B7 recognizeUploadedRoute_whenSelectedProductIdBelongsToPreflightRoute_acceptsProductIdentity`
- `E B7 recognizeUploadedRoute_whenSelectedProductItemIdCollidesWithOtherRouteProductId_acceptsCurrentRouteProduct`
- `E B7 recognizeUploadedRoute_whenSameHashApprovedVersionIsReimported_canApproveAsNextVersion`
- `F B6 recognizeUploadedRoute_whenRebuildV1ActionNotAllowed_failsFastBeforeGatewaySave`
- `F B6 recognizeUploadedRoute_whenDccProjectNameMissingEvenWithWorkOrder_rollsBackAllGeneratedContent`
- `F B6 recognizeUploadedRoute_rejectsWhenNoRebuildScopeSelected`
- `F B6 recognizeUploadedRoute_whenCurrentVersionChangedAfterPreflight_failsFastBeforeGatewaySave`
- `F B6 recognizeUploadedRoute_rejectsNonWordFileWithoutWritingMetadata`
- `E-FIX dccProjectGovernanceStatus_aggregatesRouteMainRecordAndAuxiliarySlotUniquenessByProjectName`
- `E B7 recognizeUploadedRoute_whenSameFileReimportedUnderNewBatchName_createsDefinitionScopedVersionSnapshot`
- `E B7 preflightAndRecognizeUploadedRoute_whenOnlyRouteSelected_usesDccProductBoundRouteWithDifferentName`
- `F B6 recognizeUploadedRoute_whenMainReportsExistInPendingVersion_rejectsReimport`
- `F B6 recognizeUploadedRoute_whenNoMainReportsButExecutionExists_rejectsVersionResetBlocked`
- `E B7 recognizeUploadedRoute_whenOnlyRouteRebuildHasNoBatchRecordVersion_generatesRouteWithoutBatchRecordBinding`
- `E-FIX uploadExtraFormSlot_whenLegacySlotAdopted_recordsDirectApprovalAndObsoletesStaleApproved`
- `F B6 recognizeUploadedRoute_whenOrphanDefinitionStillBoundWithoutUpgrade_rejectsAsExistingBatchRecord`
- `F B6 importPilotDocWhenGatewayFails_rollsBackMetadataRows`
- `E-FIX uploadExtraFormSlot_whenLegacySlotAlreadyExists_createsUpgradeVersionAndKeepsOldVersion`
- `E B7 recognizeUploadedRoute_whenOnlyRouteRebuildUsesExistingRouteProductWithoutBatchRecordVersion_allowsUpgrade`
- `E B7 recognizeUploadedRoute_whenSameHashUsedByExistingApprovedVersion_generatesVersionScopedReportCodes`
`B6` 行是批记录/版本导入 owner 的行为回归，不能被流程 8 线程直接修复；`B7` 行缺少正式 DCC/路线快照时先标 `ENV+SOURCE`，待 fixture 具备后才判断 resolver 是否真正失败；`E-FIX` 行是 Word 表格资源/H2 generated-column/NPE 环境阻断。上述最小复现仍是单方法 Maven 命令，禁止改成 API-only 或 mock 默认成功。

### `F7-TRACE`：来源映射与放行追溯（5 行）

- `F cn.iocoder.yudao.module.mes.MesC015RouteDccQaReconciliationSchemaTest#reconciliationMustConvergeToVersionedRouteBindingAndGeneratedQaActiveIdentity`
- `F cn.iocoder.yudao.module.mes.MesProEdhrTraceTerminalPartitionContractTest#batchExecutionPageSupportsTraceOnlyExclusionFilters`
- `F cn.iocoder.yudao.module.mes.MesProEdhrTraceTerminalPartitionContractTest#releaseTracePageFiltersByCompletedBatchScopeBeforePagination`
- `F cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRouteIdentityContractTest#wordImport_resolvesGovernedRouteOnlyThroughFormalProductBinding`
- `E cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseTraceContractTest#completedTraceForcesReleasedTransactionInsteadOfArchivedOrRejectedBatchFallback`

判定：这是流程 7 pre-release formal source/Origin/TraceLink 或 post-release trace 与流程 10 final state 的合同缺口。owner 为流程 7（最终 RELEASED 联动由流程 10）；任何映射不完整必须 `TRACE_MAPPING_BLOCKED`，不得让流程 8 材料 gate 或流程 10 放行继续。

### `A456`：流程 6/批记录解析合同（1 行）

- `F cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordWordParserOwnershipContractTest#ownershipContract_coversEveryModelFieldAndParserHelperBidirectionally`

判定：批记录 Word parser helper ownership 不一致，归流程 6/批记录报告 owner；不是四材料 gate。若输入 fixture 缺失，同时标 `ENV`，最小复现为该类方法命令。

### `PAR`：流程池 schema 与其它并行模块（6 行）

- `F cn.iocoder.yudao.module.mes.MesProcessPoolSchemaTest#shouldCreateDedicatedProcessPoolTables`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportRevisionPolicyTest#productionLeaderCanCorrectAnApprovedProductionReport`
- `E cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportRevisionPolicyTest#productionLeaderCanCorrectAnUnreviewedProductionReport`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolReviewCopyServiceTest#shouldRejectReviewCorrectionForAllocatedQuantityFragment`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineDateFilterTest#shouldOnlyReturnTheSelectedDay`
- `F cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineFilterTest#shouldFilterPqcLeaderSubmissionByProductTaskRoundReviewStatusAndKeepPageTotal`
注：流程 4/5 团队行已列于上方 `A456` 27 行分组，ERP planned-start 已列于前线/ERP 28 行分组。此处 6 行均为并行流程/环境 owner，不能由流程 8 线程修改。

## 4. 环境与工具阻断（不计入业务 RED）

1. `MesProScheduleOrderControllerTest` 的 5 条 `scheduleIssueMapper` NPE、`MesProRouteScheduleConfigServiceTest` 的 14 条“候选版本快照不完整”、批记录 Word 的 6 条“解析出 0 表格”、`MesProFeedbackServiceImplTest` 的 18 条 H2 `loss_reason_id`、`MesProBatchRecordReportServiceImplDbTest` 的 generated-column/NPE，以及 strict Mockito stub 行，均须先核对测试 fixture/schema/依赖；它们不能直接归类为流程 8 业务 RED。
2. 本轮主工作树完整 MES 回归未重新成功启动：未引用参数的 PowerShell Maven 命令被拆成无效 lifecycle phase；修正引号后 JVM 报 native memory allocation failure，未进入 surefire。两次均是工具/环境阻断，不改变上述 XML 工件的 3575/59/93/19 事实。
3. 前端 `batchrecordcelllink` 的 `routeProcessId` 重复声明/unknown property 属于 `PAR+ENV`，owner 为前线运行时/前端并行线程；流程 8 不得修改。slot=31 是 v6 合法槽，不列为 failure。
4. 19 skipped 仅记录为测试覆盖缺口；在依赖/fixture 完备后必须原命令复跑，不能用 skip 代替通过。

## 5. 发送给 owner 的摘要（持久化通知）

- **流程 8 owner**：152 条全 MES failure/error 中 `F8-GATE=0`；四材料定向 215 tests 仍保留为通过证据。不得接手 `F7-TRACE`、`A456` 或 `PAR` 的跨模块修复；放行仍必须消费流程 7 `TRACE_MAPPING_BLOCKED`/完成态和四节点硬门禁。
- **流程 7 owner**：5 条 `F7-TRACE`（4 F/1 E）是来源映射/追溯前置，必须先修 formal source relation、Origin/TraceLink、snapshot 和 Tx-C，再允许流程 8；这些行是流程 8 条件阻断。
- **流程 4/5/6/9/10 与并行 owner**：`A456=84`、`PAR=63`，按类名和表 3 owner 分派。前线签名/回填为上游阻断；排产、反馈、ERP、Word/H2 fixture 由各自 owner 处理。流程 11 只维护本清单与回归门禁。
- **测试基础设施 owner**：先补齐 Word/route/DCC/H2 fixture、缺失依赖并复跑 19 skipped；环境错误不得写成业务 PASS。

## 6. 关闭条件

分类覆盖证据为 `152/152` failure/error 行，`F8-GATE=0`，但全链路仍 No-Go。重新运行必须使用真实 Maven/JUnit 和真实 fixture；不得 mock、API-only、直接 SQL、默认成功、跳过测试或把静态检查冒充业务 GREEN。只有各 owner 修复并提供原命令 GREEN、流程 7 映射与流程 8 四材料/流程 10 唯一 RELEASED 的真实证据后，才可清除对应 blocker。
