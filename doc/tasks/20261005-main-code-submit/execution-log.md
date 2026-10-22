# Execution Log

## 授权和范围

- 用户当轮授权：提交推送主干代码。
- 起始分支：int_main；起始 HEAD：20c4601af；本地跟踪状态：领先 origin/int_main 9 个提交。
- 当前任务只负责冻结现有主干代码、Git 检查、提交与推送；不宣称业务验证或 E2E 通过。
- 已读取 AGENTS.md、docs/task-closeout-rules.md、对应开发与 PowerShell 规则，以及 task-closeout-cleanup、project-experience-consolidation 技能。
- 并行状态核对：同主目录其他活跃聊天分别执行真实页面验证、只读报告收集；附加 worktree 的修复任务独立保留。不得修改其运行资源或任务状态。

## Milestone Evidence

- 文件冻结、Git 核验、提交、收尾与远端同步证据将在实际执行后记录。

## Frozen Baseline Files

- 冻结 155 个既有待提交文件；初始完整 HEAD：20c4601af3e10fe9b97d3f42c2ed6bdb39819aef。

| Status | Path | SHA-256 |
| --- | --- | --- |
| M | IntRuoyiBackend/config/gxp-audit-boundary-exclusions.jsonl | ec55c96f80e880598a15eef251839be4dab0656878d4895d1b0d1a6e5269d36e |
| M | IntRuoyiBackend/config/gxp-audit-file-scope-review.json | 09734e6367f1a687adff30abf4ba4042d180398f756942d6299281c461e71e3e |
| M | IntRuoyiBackend/config/gxp-audit-method-scope-review.json | f19390c54dddc31a85a9884f040b1b690ef6e4797ad48aee8f60e33a87e6ef53 |
| M | IntRuoyiBackend/config/gxp-audit-policy.yaml | ac644fc500715c1852b339e613ffe5480e122f6065b413c220aac7ef891e3d38 |
| M | IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1 | 11f0f287affd267a1ff20c2504d1875991401e66142d1923dc3afbc4667b284d |
| M | IntRuoyiBackend/script/tests/test_runtime_control_scripts.py | 4a326fabeff42a59f720d8d23b0f8e716ba8f14a5a72189e254d6fceee77109b |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesFeedbackFormalReviewProjection.java | d7222c09108bc843f7ea7369dc1c4421ddfb1282d6cdffee6c046746eaf0db2e |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/vo/MesPqcProductionReleasePageItemRespVO.java | d74cc9a8839a6f3bf229b21a2b51ba7f8b4704bb70e13dcf2218d60076cf0d07 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolSubmissionReviewDO.java | 0872216d55e4f7a043a7424f2d06b5fd063e0d5c8467a8fa5ea77e9d6b3083f9 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolEventRevisionMapper.java | d3ec64cf9e0b3cd6d25c8b28163858cfd6254617e2c7d60e6cafb2fe32a32052 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderCompletionReceiptMapper.java | 02735280e2dbdbef5eacc39ad8584ec7ac4e5c473bb9179f0929bd6c3c5693c9 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchVoidEffectServiceImpl.java | 5d5c1eb19b072e32a406d2cb2963d5e9f7433bf1c2f5c1f839b08c27d88c33cc |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormalReverseTraceAdapter.java | 42567915b3cf2d14073dcac42087bbb8ab5ee7edf952dabb96d40022c0bf5be2 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProductionSignatureEvidenceService.java | cf9f7b2db313f1ba8f6a5e1854d48fec0726656517ebd87ccfef4ea0a5070583 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java | 4944f9dcd3f17cad474b8a1a7de7d891bdcf1f118118a498996908bf003f2886 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesSignedReturnCorrectionResolver.java | 508f7a6e199a19caa021d82124dbf574fc460f85626e66a6e8cf957edd56800c |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java | 43e43715833ba038e506046ac5430ac1afe4f6f20fb55eb0242a6727f7a0e761 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderSignatureEvidenceService.java | a3c400b38135eedc74678f9e4efc462e9f721188384d0732509e363feda23a9c |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java | c8eb1e11339fc9f7760dc6ae879ef0d0bf59ce80311bac8d6b831159cf4461d1 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java | adbf642fa4b6483e5ddb6bf1e782073bb44474d90f93dd915b197ac435263e1e |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java | 79515bc3bd0ddf6a6bd34c7c8e77e82b0d046f7ce31f0387202776ba49a048e9 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationAudit.java | c6b285674c0caf29a6b4ac044815843a14d238bf2d26b713a939f05bb8d5762f |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationDispatchService.java | bdc02e57175457359ec07906a0918c7e28796c1f07618d07af4f67aa09b38b1a |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationTransactionService.java | d9eca2e2cff6636656d767d968869032522915fe5496e55ed50ccc2b39476d08 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierBusinessFileAccessProvider.java | d67f6c2d43996169ac6fcd0d9d4b744661eda58ebfda91965dfad643dde09ab0 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleasePageItem.java | ef7e8e2449d15eaa0a2aa5ff13dcaee1b955260c104e2aa9a7c0a01d17574900 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java | 5faf21f153f3bc07215d160c5b489e1ac0a23bb91aa68ff32f68ed4d0d2a0ffc |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseOrderDetailService.java | d7cc4932bd7250d23d71eecafe1c392dd16a623793fc088c9999d3387161ef33 |
| M | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/report/MesProductionReleaseReportServiceImpl.java | 6e2c47575a73e389b18084ecbd162cb481995638ac491341a79c8f78d8d204fa |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesFeedbackFormalReviewProjectionTest.java | dd382ec85daf76fede821010ae13661617c2a70a6234f7e527c403352aceefad |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesProEdhrApprovalTaskAdapterSignaturePropagationTest.java | de3e592759c8c6f5511858b4a6c969ca8fbfbe73ef9c4a937c59ad934fce32b3 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesProEdhrApprovalTaskTimelineAdapterTest.java | 67f937afa0edf20f3e5b8fd8cdb8cffd5df10e7a0beea13eec9d446c0dea7cca |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesNcrDispositionMysqlTransactionTest.java | f2fb0fa40536dc6e2b1aeccc422f6be446772de2f55f4d77ddeeea3d1441aeb2 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesNcrSignatureMysqlJsonRoundTripTest.java | 8e109ec0235a0a5959200281d476c390122f1fa8f373c22ade7fbc30f9a77808 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java | a8a4fe201cdb3e7ca751309d4485c735d65f99d118cb8c07e624093d3348d0f6 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchVoidEffectServiceImplTest.java | 38409209323155e604f5bd6cf344bae03a042a2d0334db1327c42dd082a9ed64 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationNcrIntegrationTest.java | c502ec2a28d8ee329200a5e95ebe084361a9938dd523ec5d328fd251bf36a16a |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormalReverseTraceAdapterTest.java | 02c9ba8deb3956eb86d955268806e6e8b0f1cfb1c97cfe2697ab9f032738c4cb |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFrozenProductionWriterReaderR3Test.java | 2956a2c554a1bb46fef616219ec7e05468b51492e9b7e841cad1115a75fa4abf |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNcrBatchDisposeStateTest.java | 850a6d29cdf4ce378bcb9cc9ca861f621385d00fef1dc40f95d2ce41237307b1 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNcrBatchFreezeProjectionTest.java | 5d6cba6e8d6f4ea2d62c0f640c265a39ec9c0c9a21dd01a434ade5f130c68e35 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNcrReleaseGateTest.java | aa22c1e1608d04ae20e09dc3f6c48d07da9d57eb6ec34acc0fb255fa490c9cc6 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNcrUploadStorageContractTest.java | f8abfe7a76f1bfe5a73fb123b808e38d15dc898701db5f6c0fdf11019c5e39a9 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImplTest.java | 28331434b2b1b4e6eb5f5b0c5f70be3f0a160b31c187b4ddd2011b927e1d1957 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProductionSignatureEvidenceServiceTest.java | fbc1a393c85c7c451d4ff9797fa3953cc8780f80d2afdb4d7156a16fcb24d693 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesFrontlineActiveOrderInitialAllocationContractTest.java | 7ce06592bed74fa482867aa2f5d82f0b6c1d55bd8d51412e1ec2c94c152b040f |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesP0ProductionSubmitClosedLoopContractTest.java | 199e5ec6a486c81bcaa61c5136b6489c6be74aaca4c2c1dcf72d2adc50a7469c |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackRawLimitBypassTest.java | ba0169dbbd12c5e9bbb30e044ce345857e487650d92f2beb54918a25e8132d1b |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackRouteOrderGateTest.java | 5c133e8efb7ac2cbb83b7012c0c1d9193490b7aca1aa314354beba6bd3a4e6ac |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitDetailContractTest.java | 395042cc038bd9c1598f1b7dac981272fd8cf5e39696eaeba6120bbe79d41dac |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitRollbackTest.java | bf2d70d725ffadae975092064c707c7e036a7a1af2a1777098c539ab394448f0 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceTest.java | a665a965a405895f2776a9d8c1dee8261a50d2bcb856368d80f24dfd3d0c8675 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcEmployeeSwitchServiceTest.java | dfff695a46596cdf1376c0396787ff4c16e71a2acec0af34fd879ac3b7c71026 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigProcessScopeTest.java | b0f79c9c533a5b9e3d011f21ecc39f03c53df2153fbe131d0c678e1f0f024d23 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesSignedReturnCorrectionResolverTest.java | cb6c7bec9be14d71ad3ccae1b3cfdab126b86ab966de86eca33ef181cc75a544 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesPqcCorrectionMysqlTransactionTest.java | 9ca84de76127918371e32a6353bccd9d57f099d43ea64f9231fe6aa599a91828 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionFifoLockTest.java | 86d767c1f04c24850bb5a70b697911f003659cacba9b7b0819c36ce74826cfc2 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionServiceTest.java | c79ff1a50c97ef3a099f8975484a2e586bdd2ebfbd7febef183e31a7b7e0900a |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportRevisionPolicyTest.java | fa5ad033fe0258d6a93efa6c35e85e6ec8a932729e20d63e8b0689096b75a0c4 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderReworkCycleTransactionTest.java | 222fb4e643b053ce4eeca97dc829b176000ce1baa4c8b9b3e8482fd48d51caa9 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCompletionAuditFragmentContractTest.java | 01bc9e430d4da6ff1c4e6ae871f3d6aaac55fdd66c81cdbb44e2d01f510c5ba2 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCompletionAuditPayloadContractTest.java | 7c67857b1aa660fd93108e352a410c37e49618042f64a70fef14fe661a164a85 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesM9SimulationHistoryProtectionTest.java | 54ff221d25a517ffb7017179805307411a9905cf5f81f54a6b0bfe5c0db5d058 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcCorrectionSignatureEvidenceTest.java | ec1ed3026c125b4ed5f8d82b119d88cda26c76ab04e41a6574a27a279af5eddb |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImplTest.java | 1245f3278c0dc2f53b8d5b97424be025e407f08dac639461afddd5bd8d12f7d8 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceTest.java | ed28ce0556bb6ffb6a48876679d85bea9d9492b98ef404fb15f0c6d226f3b3c1 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderErpPlannedStartTest.java | 796a7a27b90f8551bcbe08f11c57aa336d07678971dfc22fe558847de6f82ae1 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationBindingTransactionTest.java | f02872f3ab4a94f3e23a81d815a1a80461e9e007a81029dbfa1a6b93ddf40cea |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest.java | c7400a8d3233a74ac4084f5b6287ea3af330c6cc982c1a48247331863e0c7eab |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/MesReleaseParentAffectedStateTest.java | 1fa90d3810dd8a317d44d3fb70d3e153e196d4ea0ae4be211f3e6c445dae0676 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/MesReleaseProvisioningAffectedStateTest.java | 8dd4988c30e11efe444ce80baef3a460e85361f565edf86e5f8aee4c0e04d68a |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceTest.java | d626d4f285060ba15e0a7b21324c19037c7704a49486d2477612da5d9ea3f757 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationKernelTransactionTest.java | 5de88d844ba9f20626694687804cb1c724d0786b4d75673f7cfa56341aef333c |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationReliabilityTest.java | eeb33eea68be8fb25307fc045cc1afda26d65a66b68e2652cdbc192a82e887e0 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierBusinessFileAccessProviderTest.java | db3e69d31bb20089f056147fe5a08bbaa1b384397a32a1ac0af949823040be49 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseBatchExecutionServiceTest.java | 38e4e3442a7a164b24cd45b8450cf2188a64ea2ad33fe52d0784a8e14416e829 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseOrderDetailServiceTest.java | 675634a01b82093ea6b53e06a12f94d8cbbb2bf92675b02ace6ae4a2a2730da0 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/report/MesProductionReleaseReportServiceTest.java | aeab6c8d0239c055f31682c3c15e3f29956270950e9cf55906681622b0ef0544 |
| M | IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/sa07-read-contract.sql | 463ad91f9fa84cf30e7d53fb3cdff4453cd2c6abdb9e0910d604d7732d27a590 |
| M | IntRuoyiFronted/scripts/edhr-reverse-trace-behavior.test.mjs | 419b37077b095ffe095a6133364c52a1ec213fd886a7cff67bd025a4687cb392 |
| M | IntRuoyiFronted/src/api/mes/pro/edhr/nonconformanceReview.ts | 140203f3a1a2f4a06f73db787b72dacf8ece8655cdbfd64cf2a48eccd05825b7 |
| M | IntRuoyiFronted/src/api/mes/pro/handoff/index.ts | 5cbcef68cbd7842c0913b582c236bb1fca6915c4c98d5a89885c8f89f01c3b7f |
| M | IntRuoyiFronted/src/utils/notifyMessageNavigation.ts | 2b710d730d50e7b3e24bb2c5f7c7e1c69d2cbfe8cb788de9a0ed3127adca78f7 |
| M | IntRuoyiFronted/src/utils/routerHelper.ts | a3c59a8860ecd21b60b4a38366a18c4de3f02a6adadc1d293e71a9dbd5f027a7 |
| M | IntRuoyiFronted/src/views/approval-center/index.vue | eeb1b88bcd5620d5dd0a791ff32411567a3889f555ca38ae1070a93ee9781ad6 |
| M | IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue | c237f28495b483ae78ba717aba469537febe236f42b9bd7218fbadf1c2abee4b |
| M | IntRuoyiFronted/src/views/mes/pro/edhr-batch/components/BatchReverseTracePanel.vue | e6e36d2f6a685e91f21437b31e9a40bd70f8453802a17cae0297a0cd12b58a7b |
| M | IntRuoyiFronted/src/views/mes/pro/edhr-deviation/DeviationDetail.vue | ca695136985efc8beb5d65e3d42a906617e801defbc3dcc4529abb2760994595 |
| M | IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue | 6d1f74dc3e2108dd0528feed4bd31861a13f36c4021b98147a413933b228b55d |
| M | IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue | a43a4ddaf90468a8f4a00226bc2b8f6c5c0b1ad9779d10c09624a97000e26b5f |
| M | IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue | 47146884589ad011e5ca924cf3455abff3fe5c28f44171bb7ab007ca2ec15d18 |
| M | IntRuoyiFronted/src/views/system/notify/my/components/MyNotifyMessageList.vue | 6cc0370203cdb7763c6ce74d383436945eb864c03fa115f03f39f8bd78606370 |
| M | IntRuoyiFronted/tests/e2e/edhr-manager-market-release-entry-behavior.spec.cjs | c3bdda93ccf3c6d78874314e18eb1550af714f8696f4e7153a66bdef041a838d |
| M | doc/tasks/20261004-edhr-sa06-identity-read-fix/task.md | 6c5328418e3acf12bdaa09d70cbe368a0f41e0890cffecd16ca8db86751ee3b3 |
| M | doc/tasks/20261004-edhr-sa07-sa08-fix/task-state.json | c372f9c544caaaf8765bf6130aa47b6ded505a2e1a5b281e34deb5c6f191a155 |
| M | docs/database-rules.md | 0d2bfa965362043771ab2028a5384a13ff1af1394267005bcf79ccd2d4e39069 |
| M | docs/e2e-rules.md | d737bb4fb18b1b88c022c33eaf2b2ac14fd4be71e585accbb32eeca3eb63d381 |
| M | docs/experience-index.md | ec4201ba708f71d458dcc71552dc9bb81060082b58c8bf6400c2963d3c005d00 |
| M | docs/local-runtime.md | 107ee9396ad5071ef7fc4bb3d82bd60d15a1e4cb9c6188622a70d2cb3327e5b5 |
| M | docs/release-build-preflight-lessons.md | 6e0fadf0b555a21fde710983adea6b7d4e96b1c5bb1dcfd362e6c3242746034b |
| M | docs/worktree-memory.md | bdc712e7d88d5c310842788c423f519db7515fde946880e15595b67220ebcee8 |
| ?? | IntRuoyiBackend/script/tests/test_gxp_pqc_handoff_assignment_scope.py | 5b1a0adeca41812724222c7ea3c29deb4589000ee5852e82a7718f97e82212bd |
| ?? | IntRuoyiBackend/script/tests/test_gxp_signature_audit_lookup_migration.py | 923f2b6f0c2811ebc2b41407067b8f27aa4f1f038c954d5e756eeb6670709339 |
| ?? | IntRuoyiBackend/sql/mysql/20261005_gxp_signature_audit_lookup_index.sql | 117397e3687bf61f2cfe551ffd2081d6ff095aebed43d0daec7d1cedd9c78805 |
| ?? | IntRuoyiBackend/sql/mysql/20261005_mes_active_order_handoff.sql | 182f13cf29653771f8c9a535f216cd0e875384f0f9900a0a9957fb605fe4d4b0 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesActiveOrderHandoffApprovalProjection.java | ac7ebd7ed0b680a89a5b04f7d20512a9289625f594b08ad3966a4e85f071a974 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/handoff/MesActiveOrderHandoffController.java | 417e644df31f0a7d1ab55888aee30ae4d40c7ac6568d285776d14cd997670ff5 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProFrontlineReturnCorrectionController.java | 1365efa063120247fcef6c8f4cb4afd8d14bef3a13f9b5670988c715561e82df |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderCorrectionEvidenceController.java | 3f9bc0c9b60917d24a9ba983e2bd29cce8e98050716b57b9d656a5413f9f9532 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/FrontlineOwnPqcReturnCorrectionReqVO.java | 36907499291c92c28b5af0624206a0fae8687f6c425953dcdd68a853402d40a7 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/FrontlineOwnProductionReturnCorrectionReqVO.java | e35d7a7ac9c90b8b57a416dea527c4157095d6806068b027f5ab783f008c2748 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/handoff/MesActiveOrderHandoffDeliveryDO.java | 36f29d21f0a357d10f9e637ac83414c201cf5298af8d0a3c6266eb761d48687c |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/handoff/MesActiveOrderHandoffTaskDO.java | 4022deac55b352e5d93c451baa796e935ba2ccea52b124cd41ce3574ea4450dd |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/handoff/MesActiveOrderHandoffDeliveryMapper.java | ad77c52eb1eec2e5c90e07678652175c28b2f0be3f069e9467d8958ce68e38b0 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/handoff/MesActiveOrderHandoffTaskMapper.java | f84283901a4335661ad8c64c2af71f37512d50b15b2f191e39d11b798028163b |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffAudit.java | 884ad1edc2f8c9c3ff4cbb67bd3526511ea6a466b9029462133a127ef53fe09f |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffDeliveryService.java | 1e917e45db8430a8fe6ed2f6286c144b72ea9891ebf996a24de18e5c150b1cd2 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffDeliveryTransactionService.java | f8f89eac36b96fb1655a510475faee0ef54860e28eb883a4f0aecfdde073c537 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesFrontlineReturnCorrectionService.java | 4a536c4c3a4216b9e9ab4f3522663fa7145461699b5b25a8accb7a344a0edc7a |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderCorrectionEvidenceService.java | c0e5c93b764d580f6065940ed28302ec56a5fdec8edab27fe7df0e45118c86d0 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCorrectionSignatureEvidenceReader.java | 5f013458d29bbf8d9f614a6c885178bf42f89122f1fa5daf101600c43cc0c55e |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesProEdhrApprovalTaskAdapterDoneActorProjectionTest.java | 133ad72db1fc51327a45a70f939f0f767b533aa3ba76ceb515ebf6fe74397a8b |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderCorrectionEvidenceControllerTest.java | fcda0c70831a982a7d98e707224d215719fb70367783a62801dc5eb959be134e |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesMarketReleaseDoneVisibilityTest.java | 92679a0e351fbefad52fb5d36e96fc2972efc7d28963c0471682c05f91efc204 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormalReverseTraceCatalogOccurrenceTest.java | 2460772f82964f09ec14ac0a9d78a8fe745f6f6bfe845418a100c62fb1dddb0b |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffAuditTest.java | 13cf0001c118b29701a1c54c8c0f816269d0c8c7dd09ad9506eb807716af0bf3 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffContractTest.java | 2751873412eb166fa096306bd614bdb89bceb5da55ce63af885edbd883a7eb87 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffDeliveryReliabilityTest.java | d6a395dc973445ca85e6459da68f090ad8fd72edd32ea349e1f0df5875b603b2 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesQaHandoffAssignmentServiceTest.java | 5d61eaf25f99daf78c23035980756a260a3e3cf70a00c40304a2dc5bbafb9a60 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesFrontlineReturnCorrectionServiceTest.java | ebff8b7a41ffa51fb070be4e552eaf86e60b3af3f2ff17f41726057595d2b353 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderCorrectionEvidenceServiceTest.java | b3f750450e8fe2b0f5aa1453ff86656cee2a4b5620723d39be2c70f41448bca8 |
| ?? | IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCorrectionSignatureEvidenceReaderTest.java | d8cff28fcaf0b035bdf07ede11d48ee6dbc8e55b790fb19d5422b2b086dbba65 |
| ?? | IntRuoyiFronted/scripts/astra-c-continue1004-r8-catalog.test.mjs | f790cde0a77b6533e27222d552d742b854c6910166472a58ecf7c4f87de25690 |
| ?? | IntRuoyiFronted/scripts/astra-c-ncr-authorized-menu-1004.test.cjs | df12ab142f05aec468592396620f2f5ef1dbf24b30c515d104545321cd659499 |
| ?? | IntRuoyiFronted/scripts/astra-c-reverse-drawer-size-1004.test.mjs | 7dd5561fb1e7d898bc455e250bf61b41f9776c7da37d840ab6922e096798b53e |
| ?? | IntRuoyiFronted/src/api/mes/pro/edhr/activeOrderCorrection.ts | b622a456fc108d286b0be66501c387e891a3a45c1d0aa2f168d4c9aefe0d23fb |
| ?? | IntRuoyiFronted/src/api/mes/pro/processpool/frontlineReturnCorrection.ts | 4dcd670d16c7df641b164f964c6ddcfc399e5cb99f7ed09cfeba78d4d8c3c7b9 |
| ?? | IntRuoyiFronted/src/utils/activeOrderHandoffNavigation.ts | 505f219788b591f272e9c29facc7bb59d3bb105e8c0b55d6e4ad8315584b9e1f |
| ?? | IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineReturnCorrectionPanel.vue | d29bcda65b671a74e69cefa6bcf60fed3434b6fb82c09f5c9a0ec81a65b5876f |
| ?? | IntRuoyiFronted/src/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue | 0c76662eaf4ba294fc7002c147ad9212066e01e5c2527351a018164505df82df |
| ?? | IntRuoyiFronted/src/views/mes/pro/handoff/PqcHandoffAssignmentConfig.vue | a2814f2bf8c9397ca3d673140de56650b9b2f1051d5f9c8ba138c023f2dfdf49 |
| ?? | IntRuoyiFronted/src/views/mes/pro/handoff/QaHandoffAssignmentConfig.vue | 69f5404d64e2445e3718b227bbd9040600c4580dac1d4dc87121b7457957e38a |
| ?? | IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderCorrectionHistoryPanel.vue | 2ace693c83471c3eea4ceb645034071a5e83ac45f9a72f1f3c5084f72097050a |
| ?? | IntRuoyiFronted/src/views/mes/pro/processpool/components/activeOrderCorrectionHistory.ts | a8e67373a6fa82d5e1a007588ef7490862074045657005ee7afab333348454f9 |
| ?? | IntRuoyiFronted/tests/e2e/active-order-correction-history-behavior.spec.cjs | 71e8741d23f2b5b53a95395d030e45076cfd8786076dd32ca423f0c96768efe7 |
| ?? | IntRuoyiFronted/tests/e2e/active-order-handoff-behavior.spec.cjs | b2ac1517bca185a0da40b5736141bfa22fde2021d8048f0f8a728086aff9a43f |
| ?? | IntRuoyiFronted/tests/e2e/active-order-handoff-notify-entry-behavior.spec.cjs | dedf979647e22391c1f4c6bc51325813af45108bf6387848fed9415935650f76 |
| ?? | IntRuoyiFronted/tests/e2e/edhr-manager-completed-notification-entry-behavior.spec.cjs | 399aa2b546af89708f321fcb7de5cfb262e92e45ca7f933421d0f535c269abdc |
| ?? | IntRuoyiFronted/tests/e2e/edhr-nonconformance-create-entry-permission-behavior.spec.cjs | bcf4e83842c0493046e8a38936d377a92c1c2357f6c16d3a16d0fab088e53fd8 |
| ?? | IntRuoyiFronted/tests/e2e/edhr-nonconformance-review-exact-entry-behavior.spec.cjs | d2ab96bbc3a874074c5485150767eb49de1a3975e02fc8d0e7feff0780b528bd |
| ?? | IntRuoyiFronted/tests/e2e/frontline-initial-handoff-exact-entry-behavior.spec.cjs | 9fd9c6b0114c52f7d06930dcd4464188829370235356bd43ec0ffc62e9e13cca |
| ?? | IntRuoyiFronted/tests/e2e/frontline-own-return-correction-behavior.spec.cjs | 25ae9b9b59bd43e1fb0870a7b11f98b91f7150431122a67e011b1c6a269ecc76 |
| ?? | IntRuoyiFronted/tests/e2e/frontline-own-return-parent-isolation-behavior.spec.cjs | 9a5c362114e606b308450a6d9232c5587d3e90a0fff408bc178fc1f255c57fe1 |
| ?? | IntRuoyiFronted/tests/e2e/pqc-handoff-assignment-behavior.spec.cjs | 71c80e95100330bc494b963e75e413c699ba7107702434b2f3a3212449bf823e |
| ?? | IntRuoyiFronted/tests/e2e/team-leader-production-handoff-detail-behavior.spec.cjs | 8420e0c060f85bf7c6504b9bc860301ddc24d5d32414d7bb9f8327a2759a9524 |

## Git Pre-commit Verification

- 远端 fetch：PASS；HEAD...origin/int_main = 9/0，无需合并或强推。
- 初始 diff --check：FAIL，2486条换行相关报告；11个源码/测试文件的历史blob混用LF/CRLF，docs/worktree-memory.md包含503处CRCRLF。
- 保留core.autocrlf=true；通过git add --renormalize规范化暂存blob，不重写业务源码。仅将worktree-memory.md重复CR规范为LF，规范化前后文本完全一致。
- 首次git add返回1：两个既有跟踪任务记录位于本地忽略目录；已核对其仍完整进入155项暂存清单。随后git add --renormalize退出0，不改忽略规则，不纳入忽略目录的其他文件。
- 首次staged检查另发现一处新增测试脚本EOF空行；仅移除该末尾空行，脚本正文逐字节一致。复核已通过。
- 已核验155个冻结文件无并行漂移；实际暂存155个差异文件，逐文件blob与工作区内容在标准换行归一化后完全一致。
- git diff --check：PASS；git diff --cached --check：PASS。
- 155个文件UTF-8：PASS；敏感信息扫描5处候选经核对均为签名模式常量或纯本地测试夹具，不含真实凭据。未保存候选值。

### Staged Baseline Blobs

| Path | Git blob | SHA-256 |
| --- | --- | --- |
| IntRuoyiBackend/config/gxp-audit-boundary-exclusions.jsonl | 7863ea2db656061de7ffa91fb36e0a4fc66674cc | ec55c96f80e880598a15eef251839be4dab0656878d4895d1b0d1a6e5269d36e |
| IntRuoyiBackend/config/gxp-audit-file-scope-review.json | 82de82d2871ec654368c8c45492d88e6fa794ae0 | 09734e6367f1a687adff30abf4ba4042d180398f756942d6299281c461e71e3e |
| IntRuoyiBackend/config/gxp-audit-method-scope-review.json | c02f8cbd2a464b48fc14b2dff1842707ceff0960 | f19390c54dddc31a85a9884f040b1b690ef6e4797ad48aee8f60e33a87e6ef53 |
| IntRuoyiBackend/config/gxp-audit-policy.yaml | 1c09c909ab445dc738267db36d05f668c9dc3626 | ac644fc500715c1852b339e613ffe5480e122f6065b413c220aac7ef891e3d38 |
| IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1 | 72f972a15b3f364d5aee6eca3b21cb26a030b242 | 94bbc5c57a9bd0cad054e88a38b6580e810ee08a3cdef23cf2208053208e75b7 |
| IntRuoyiBackend/script/tests/test_gxp_pqc_handoff_assignment_scope.py | 04892ec1251a9871f3302dc6843f3e9afb4ba794 | 5b1a0adeca41812724222c7ea3c29deb4589000ee5852e82a7718f97e82212bd |
| IntRuoyiBackend/script/tests/test_gxp_signature_audit_lookup_migration.py | 5078d93720c99c7ca8f3b3c8af4d9e94e4278005 | 923f2b6f0c2811ebc2b41407067b8f27aa4f1f038c954d5e756eeb6670709339 |
| IntRuoyiBackend/script/tests/test_runtime_control_scripts.py | 8dfd935d0ef069f6f826a96ade1c000da130c179 | 9b659bd1b675e2e77a068f1d49c4937927f3756823749aaf0974b1ea122f000e |
| IntRuoyiBackend/sql/mysql/20261005_gxp_signature_audit_lookup_index.sql | 9db0e1746c0ac842ac2eaa8391b95e6123d39658 | 117397e3687bf61f2cfe551ffd2081d6ff095aebed43d0daec7d1cedd9c78805 |
| IntRuoyiBackend/sql/mysql/20261005_mes_active_order_handoff.sql | af8c7a2ae330dc23cc030e77e1435e1d3c2dd6dd | 947f189fc7b9fe121f947cca0f24774b8e0812a461f34793bffc5227caea5974 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesActiveOrderHandoffApprovalProjection.java | 9b809d5b6551b61f548a5b2d3fe920babe99ac89 | 868aa8deaa50ba8b6116f422ed9d88aed2bb86312ee634e91ed26bfc51afa147 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesFeedbackFormalReviewProjection.java | ba6fafda60fdb55709c396081d78e8fc5a7e8de1 | c3e48eef49198c946795410042a2ce773c0053ff2710f25475fc9e0d4066a325 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/handoff/MesActiveOrderHandoffController.java | 0ee864593206410c074d0c38bdeeb78b20fc59c3 | 4327f6388201fb0fe4c1a5e5f0fe3d7e1bf8efb5988cb08eaab982c238366689 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProFrontlineReturnCorrectionController.java | 28eb87024d50273f4238eb428b23fdd171eca17c | 1365efa063120247fcef6c8f4cb4afd8d14bef3a13f9b5670988c715561e82df |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderCorrectionEvidenceController.java | 0e5d697d5566938492055cc5e307ad49077ea08b | 3f9bc0c9b60917d24a9ba983e2bd29cce8e98050716b57b9d656a5413f9f9532 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/FrontlineOwnPqcReturnCorrectionReqVO.java | 31cf525ee214c66d1f9f403354820f125eedf76e | 36907499291c92c28b5af0624206a0fae8687f6c425953dcdd68a853402d40a7 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/FrontlineOwnProductionReturnCorrectionReqVO.java | 4bc8ab737e61e3682f5a20a02bccbbf17bca333a | e35d7a7ac9c90b8b57a416dea527c4157095d6806068b027f5ab783f008c2748 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/vo/MesPqcProductionReleasePageItemRespVO.java | bd317f37a7342fc21b73b2f25212395ce9da6670 | b6a00d7270782ea78c0e33a760e55ebe98253358a55264cd138b4bd41d4902d4 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/handoff/MesActiveOrderHandoffDeliveryDO.java | 54f5e59f5e718aa7421d40b54e850f1b1e025241 | 36f29d21f0a357d10f9e637ac83414c201cf5298af8d0a3c6266eb761d48687c |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/handoff/MesActiveOrderHandoffTaskDO.java | accecf7fee28df9c0a8bb52f05e20a30e117590a | 4022deac55b352e5d93c451baa796e935ba2ccea52b124cd41ce3574ea4450dd |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolSubmissionReviewDO.java | 6dc2893bc069eeffd6c74417e78be3d599a1ab2e | cbc05e7002d7847683b9a9016c1899e4c67d31c99bac38107a0196cb5c3285b5 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/handoff/MesActiveOrderHandoffDeliveryMapper.java | 1072915c0738461ac17e6ccfa0c3fd64210f96cc | ad77c52eb1eec2e5c90e07678652175c28b2f0be3f069e9467d8958ce68e38b0 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/handoff/MesActiveOrderHandoffTaskMapper.java | 8ac4010b8140ce2d63f7d795d144b6e8c5665998 | f84283901a4335661ad8c64c2af71f37512d50b15b2f191e39d11b798028163b |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolEventRevisionMapper.java | 9309e88ea51efd28acf5b1461f748b8288315936 | 0ef7094f2484c500839d83ea81cd53c154d110de025fd5464c789d0fad587a6a |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderCompletionReceiptMapper.java | 87a7f14b9f1792fb4ab0fa85793948a8ea3c68e7 | a93b433b3c68c39c69c1b10da4cc3662dbd9a5d8c8ba57ba727120797731196a |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchVoidEffectServiceImpl.java | 7d60a239f2ca2054f994386ada5e822b9a5a6b20 | 877140c2ccb0ee4bf1403eff9e27856a75001e5b494755a39305a7950e5b250a |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormalReverseTraceAdapter.java | 691616dd0f76d28d125ee84f20c24572cdf57448 | 42567915b3cf2d14073dcac42087bbb8ab5ee7edf952dabb96d40022c0bf5be2 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProductionSignatureEvidenceService.java | 4fab600493e8265e6560e57284ec02dd0fb79e3d | e3065feb17723381f6e43d49b7d978087c654929f886337c4c3f00c4a29e7edd |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java | a20b85a4c89dce41ee456720596ab0b64ac643ea | 774656710ee793f74d56c36ffed9c85919c1a41a21d7316f91c6c7d9bd666c12 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffAudit.java | a02958f5e345efd871c8f380a9672b33cc933af4 | 884ad1edc2f8c9c3ff4cbb67bd3526511ea6a466b9029462133a127ef53fe09f |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffDeliveryService.java | 365a2dea6067a54a00f6b61569f374234568a057 | 43788c25b98ed1b4fc032631d9cdb06cd08201b567b31675ffa014ed670aaf34 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffDeliveryTransactionService.java | ccbb5994b7e0649be81e059383ec507868d25027 | 7303d339ea5701404a2563fe23ab5f9a190a0cf03e83db1cb6886c4ac223683d |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesSignedReturnCorrectionResolver.java | ec7d84dd0e4b334500c0b6c66fa1acbf1c9306cb | 7d88e6a4b58b6a60aac4a16746ee7038d986d598045d1f3033fe531fd901f854 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesFrontlineReturnCorrectionService.java | 8519d4c3ecf2377eb42186fd76956247eb55f56c | 4a536c4c3a4216b9e9ab4f3522663fa7145461699b5b25a8accb7a344a0edc7a |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java | 196b98efe6b22300ad4723536420e46cb0a57820 | 837cc38a6c0b359df2b544c47c7f1672d7df0318425950d6e686e785589d436b |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderCorrectionEvidenceService.java | f0ee7fc31c7feb272c9e9ea52a6c21a6e7da6f47 | c0e5c93b764d580f6065940ed28302ec56a5fdec8edab27fe7df0e45118c86d0 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderSignatureEvidenceService.java | 0de8c4298617466f00f9a5f58338791412df9d9a | 1ab6f04cbbbb4371b61d8c3750baf723184161dac2a8d3fa11b0700a83d1910f |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCorrectionSignatureEvidenceReader.java | fa6e4fee48db0ed4ed7c67959f9d0d8fe7fef73a | 5f013458d29bbf8d9f614a6c885178bf42f89122f1fa5daf101600c43cc0c55e |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java | 366bf251a168bb7211ac2bcaf3538722574492d9 | a1d8aabad9659124d7584505da2146c41594a970d50c30e2000e1b748c0606ed |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java | 304f5f6beae29772f7fdb5854ce06777ce065ebe | 0af0d5079a9ee20e04db8e97249c59be607c819fff4ed31acbe384ce76dd1ca2 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java | 49f5789f88ce73dc882f805b2e8f21041aefeece | e0d45bfdf705ae06574c25044f732824d302e5193eedbbae698a621aa27fb7e1 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationAudit.java | 5dbf47b245588e714ad240e702924a885888be11 | c6b285674c0caf29a6b4ac044815843a14d238bf2d26b713a939f05bb8d5762f |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationDispatchService.java | 14d45858f92dd4d300c7bc70634dc9904dbbab79 | bdc02e57175457359ec07906a0918c7e28796c1f07618d07af4f67aa09b38b1a |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationTransactionService.java | 89f580d82942f93fdfb9e6582f07e8ccb4874c13 | d9eca2e2cff6636656d767d968869032522915fe5496e55ed50ccc2b39476d08 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierBusinessFileAccessProvider.java | 60cce579f7a6297507fbfb5fd7718bed8f50dea6 | c217ba618c6be802569550e744d7a8e67641f26671d7731a2cd28a40c23c05f8 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleasePageItem.java | d1b4402a6de033eab5316d40582daf110482c2b3 | 1fbea585031b3759f1d0d9e7183637ad9ff939578f63c769cf43af40f15bc7ad |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java | c30f6d8c7274c3af557664a75687aeae896ad2b5 | e07b79e9b63feb123348cf7fb393fceaace356ead78c5a31589abcd73c262f07 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseOrderDetailService.java | 5eee397729c55cbab754e822cfd0409a410b89c2 | d2e3cd552dc021a66fd57dc49c387568f16d7f39a12ffaf922d559cd43d97c4c |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/report/MesProductionReleaseReportServiceImpl.java | 4a60fc5ed47f7a31ac3e7db3753ec4a622d98171 | c901e73154c71fa1fe8b50f6c4e0a72c620c84c6c84295fc6c0c9eefe8f88c4b |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesFeedbackFormalReviewProjectionTest.java | 9dbd166cfa983d42390465c2fe682be6e6d797ba | dd382ec85daf76fede821010ae13661617c2a70a6234f7e527c403352aceefad |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesProEdhrApprovalTaskAdapterDoneActorProjectionTest.java | 23fac2b03c5eac62f5826a78dcb77b1e90a6f80a | 133ad72db1fc51327a45a70f939f0f767b533aa3ba76ceb515ebf6fe74397a8b |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesProEdhrApprovalTaskAdapterSignaturePropagationTest.java | 576fae278b8c0db6d0e0c011e5c5172d8301214a | de3e592759c8c6f5511858b4a6c969ca8fbfbe73ef9c4a937c59ad934fce32b3 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesProEdhrApprovalTaskTimelineAdapterTest.java | b43510221ada2ad8ad21ba20d3cb12611cdb2e64 | 67f937afa0edf20f3e5b8fd8cdb8cffd5df10e7a0beea13eec9d446c0dea7cca |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderCorrectionEvidenceControllerTest.java | 3e3fbc33e1381f1ee47a0b5f155e2dd091f3ec9f | fcda0c70831a982a7d98e707224d215719fb70367783a62801dc5eb959be134e |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesMarketReleaseDoneVisibilityTest.java | e3cc04d4117e69ff84570dc70d86be4c9b061f7d | 92679a0e351fbefad52fb5d36e96fc2972efc7d28963c0471682c05f91efc204 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesNcrDispositionMysqlTransactionTest.java | 54f4de16d9382bcac1f34d68f237feed47f3b3b8 | f2fb0fa40536dc6e2b1aeccc422f6be446772de2f55f4d77ddeeea3d1441aeb2 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesNcrSignatureMysqlJsonRoundTripTest.java | 98ca9c918f36eaa1832dd6bff5a5992d1e1284f3 | 8e109ec0235a0a5959200281d476c390122f1fa8f373c22ade7fbc30f9a77808 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java | 437abbc8c23f0b140b2ff19dcfc6a1460a40f2b7 | a8a4fe201cdb3e7ca751309d4485c735d65f99d118cb8c07e624093d3348d0f6 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchVoidEffectServiceImplTest.java | a3cda8b5ec262a949b146042edfbab8e07602684 | 495618ff0c799323592f937a405af3da56eb6969cca0575871f968403b953404 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationNcrIntegrationTest.java | aefeb65b2d73230bd9d17b5521073568b5f3a881 | c502ec2a28d8ee329200a5e95ebe084361a9938dd523ec5d328fd251bf36a16a |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormalReverseTraceAdapterTest.java | 87b849eb2903b6c21a29b120fed5819091f6a17e | 02c9ba8deb3956eb86d955268806e6e8b0f1cfb1c97cfe2697ab9f032738c4cb |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormalReverseTraceCatalogOccurrenceTest.java | 6176781216dc03b81d2581ffb87e7afeed892ecb | 2460772f82964f09ec14ac0a9d78a8fe745f6f6bfe845418a100c62fb1dddb0b |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFrozenProductionWriterReaderR3Test.java | 769233ffcde41d60bca70409b36374e316f50a06 | 44a2b6f3f161ad1d92d85231ee6e51b35a5fb40a636cd9c8613a0e090f752251 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNcrBatchDisposeStateTest.java | d806ad0677101da844bdec26765afef9b6859df2 | 850a6d29cdf4ce378bcb9cc9ca861f621385d00fef1dc40f95d2ce41237307b1 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNcrBatchFreezeProjectionTest.java | b79ff6f92c86db0c1e33b9d2e4f8ca2fa0848066 | 5d6cba6e8d6f4ea2d62c0f640c265a39ec9c0c9a21dd01a434ade5f130c68e35 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNcrReleaseGateTest.java | 5de9c974425bc022bdfd92fe37c3aa9cfdcadbd4 | aa22c1e1608d04ae20e09dc3f6c48d07da9d57eb6ec34acc0fb255fa490c9cc6 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNcrUploadStorageContractTest.java | d08be456f782cabb471d95b72c3a0b87a6b6bcc7 | f8abfe7a76f1bfe5a73fb123b808e38d15dc898701db5f6c0fdf11019c5e39a9 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImplTest.java | 77fc7ce9a822691fed25259630ac73543315c2e6 | 28331434b2b1b4e6eb5f5b0c5f70be3f0a160b31c187b4ddd2011b927e1d1957 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProductionSignatureEvidenceServiceTest.java | bb7eabe4a2bbc884d61b3ed1189ff71af0fa8ed3 | dd0f496fc09e3ff5de740c88cdbfddb6b957115fb6e4ac2b025288caf37746de |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesFrontlineActiveOrderInitialAllocationContractTest.java | 9ceacbbc4b703303cfa5b2b532f28c76cf5986ab | 081db3d9fb919015114c5aa91f2ae5fda8b9321c2d1bf53164febacf48a0f6e4 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesP0ProductionSubmitClosedLoopContractTest.java | a984ede9b8e690f1200d266a948e74d405a46b93 | 199e5ec6a486c81bcaa61c5136b6489c6be74aaca4c2c1dcf72d2adc50a7469c |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackRawLimitBypassTest.java | 962ae0dc0125d8b86714e7080076cfa9c38b26a4 | ba0169dbbd12c5e9bbb30e044ce345857e487650d92f2beb54918a25e8132d1b |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackRouteOrderGateTest.java | 81dd10cb22dfc80cfb06212bc4c7ba01989d50c9 | 5c133e8efb7ac2cbb83b7012c0c1d9193490b7aca1aa314354beba6bd3a4e6ac |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitDetailContractTest.java | 677c3830ab0ec77c0e73858c2af2347f17f85029 | 395042cc038bd9c1598f1b7dac981272fd8cf5e39696eaeba6120bbe79d41dac |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitRollbackTest.java | ffb7be8d207e396cd3b83905394752e1f78ffafe | bf2d70d725ffadae975092064c707c7e036a7a1af2a1777098c539ab394448f0 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceTest.java | 2bfb611e69f4b5e227cdb9a7776955dcbafc2a9a | a665a965a405895f2776a9d8c1dee8261a50d2bcb856368d80f24dfd3d0c8675 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcEmployeeSwitchServiceTest.java | a9d6654113b8fefd0570efbc58c2c44ac74ae4be | dfff695a46596cdf1376c0396787ff4c16e71a2acec0af34fd879ac3b7c71026 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigProcessScopeTest.java | 995484d0214a5508bf2cf4146e1c394c86cc3781 | cf93c1ad7123adc129d688e71ca78cc01547ce86aeeab66a9b2b3892aaa596e4 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffAuditTest.java | 807230d35801b2cb302fd11cd04089e6ce32d8b1 | c97800bae48d913bf7464c7600928b2014d968c987c181641907b2f308da67d9 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffContractTest.java | 509b50e5144196172965016978c2b883ca5ab1ba | d07d564797bd1e7f3c97559981ed6f49ece7018e3034e4726b09da7ac0c86cd2 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffDeliveryReliabilityTest.java | 49818c62622570ba072892647b44258d9ac1da2e | 2c452dbea8f9eed01f5c1fe442828f3af1c482953dfd944240bad068c2425d6d |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesQaHandoffAssignmentServiceTest.java | 9150de7774209ae5cdc47e0ff38a2bd1b5eb84ed | ebe8d6750b28b891f9b7e80c6b477b5a4fb937ddafff443fdb03badb2d3cfcfb |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesSignedReturnCorrectionResolverTest.java | 6d9b5a653bd3f554cb04e2140e0c01e38411edf5 | eb07c9385a49fbb3b6de3fc863aaba89533a80f48525277a23f8e0bbe1f4dde5 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesFrontlineReturnCorrectionServiceTest.java | ffbb283ca30cfce879e466f4465425fa95e8256a | ebff8b7a41ffa51fb070be4e552eaf86e60b3af3f2ff17f41726057595d2b353 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesPqcCorrectionMysqlTransactionTest.java | b876f9d451b2ed4efd945ba71724d0aca11b5e04 | 98f5a05cea021b75d2a64b6ca791f39f5d8cc74a4d21affc853db436ae611cbf |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionFifoLockTest.java | b6ceed8929d17d6ebfff5544f74f3c1840ddef94 | 1b84d4ce8f79c62967344b333f3c1a5da6a4ae3be5398a23dc4711a759557e87 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionServiceTest.java | 7b802d6e8da76a39dd96c500862861f51c0bc659 | a8dd740bb30a9f4c776d450e80e71e50b9f691423f7cd42b4dee77579ad09039 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportRevisionPolicyTest.java | df076930ba3ef7d4249d18683f1eb3e55e8c04cd | a9ccce48bafee37b418c80a25fbfc863cd178a63ffe7327d4ce44a7b29cbfb57 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderCorrectionEvidenceServiceTest.java | 30a4d7ec3571e5508f74f732e8f34bf7577d27f1 | b3f750450e8fe2b0f5aa1453ff86656cee2a4b5620723d39be2c70f41448bca8 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderReworkCycleTransactionTest.java | 2d377380e35ab003bf0b9c90700de3f36b759114 | 222fb4e643b053ce4eeca97dc829b176000ce1baa4c8b9b3e8482fd48d51caa9 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCompletionAuditFragmentContractTest.java | 8fff5ec7c8da849c813fc361e32a3dfa5d078966 | 01bc9e430d4da6ff1c4e6ae871f3d6aaac55fdd66c81cdbb44e2d01f510c5ba2 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCompletionAuditPayloadContractTest.java | a2f2f595ce6863c682fde9e3cdb920096bb1ef50 | 7c67857b1aa660fd93108e352a410c37e49618042f64a70fef14fe661a164a85 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCorrectionSignatureEvidenceReaderTest.java | 2ccc915dabd6ccdf070e5f66d40cf7013f39b5ce | d8cff28fcaf0b035bdf07ede11d48ee6dbc8e55b790fb19d5422b2b086dbba65 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesM9SimulationHistoryProtectionTest.java | 578f108b96219e3af5770dd6fb53224f0802cbfe | 54ff221d25a517ffb7017179805307411a9905cf5f81f54a6b0bfe5c0db5d058 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcCorrectionSignatureEvidenceTest.java | 02365ec25684756250b07251faf4b08a7e81194c | 03ce5bcef0a019c6421fa887cdab0f33bb8b35c619c3e86a4060a67a5e741698 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImplTest.java | e9cdbcafe8d9d913f5ad92c6cbd5a14ce92c2ac5 | 1245f3278c0dc2f53b8d5b97424be025e407f08dac639461afddd5bd8d12f7d8 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceTest.java | 638cd8f97398f6df4219f1343dcba8bf114c66ef | ed28ce0556bb6ffb6a48876679d85bea9d9492b98ef404fb15f0c6d226f3b3c1 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderErpPlannedStartTest.java | e9329980f5beef70a25bbabadc51ed25d7234649 | 68106fd3dd596d28e8b298a8a408f2b5c4e40ccfdb6d601e20ebf88605909a9f |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationBindingTransactionTest.java | c1fd7649f3bf35f16eecd559f52a369b892fecc6 | f02872f3ab4a94f3e23a81d815a1a80461e9e007a81029dbfa1a6b93ddf40cea |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest.java | 170b99ccc0bfe4b06d7b3199b03126331ca972e4 | c7400a8d3233a74ac4084f5b6287ea3af330c6cc982c1a48247331863e0c7eab |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/MesReleaseParentAffectedStateTest.java | 45cea5869754c73a97eabf90f8e9a1846d9d22c2 | 1fa90d3810dd8a317d44d3fb70d3e153e196d4ea0ae4be211f3e6c445dae0676 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/MesReleaseProvisioningAffectedStateTest.java | 3483618e5d2d0683f653b814079cf393da0f40f2 | 8dd4988c30e11efe444ce80baef3a460e85361f565edf86e5f8aee4c0e04d68a |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceTest.java | d73af05e5a20f3e18b12b01ec6ccbe3dc3247268 | 107d09425c7c73216d2a54d483e08631c8505c586cdb6e5e190c51e7f295208b |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationKernelTransactionTest.java | fc387632140c95d9dde847ee316c98b11ae7824a | 5de88d844ba9f20626694687804cb1c724d0786b4d75673f7cfa56341aef333c |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationReliabilityTest.java | 386cac99b54b110dcca709b3400b4396e4b7b14b | eeb33eea68be8fb25307fc045cc1afda26d65a66b68e2652cdbc192a82e887e0 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierBusinessFileAccessProviderTest.java | 124d11d5d8f7a936d582c680076b6600cac95417 | c2f0d798ec4a3be0ed14ee5fb4be2c549436006bf8fc1fa508c989a3f0ade708 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseBatchExecutionServiceTest.java | a1bf598f160b80158519115f8db823796fbcaff7 | 3c6c8438893ec395d79f3a0d3dea4af1ec60bf21182ae1239d628cae02a898e1 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseOrderDetailServiceTest.java | cc68ead05af87d4e5d3889fec85ce6187931b4b2 | ec28bb8f64272e6a87f53a02dfcd29e7be7f215d39dac47211cdba1312e16367 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/report/MesProductionReleaseReportServiceTest.java | 1ac004f58681dda0b37efd15d50250cb961e85a2 | 0a94836a4871dc38c850c98de523a6f3a2971c553237ab043c07f25e8fa8e755 |
| IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/sa07-read-contract.sql | 5e67a003b86f54b4ca7d5ff2524c8bd6234f4692 | 463ad91f9fa84cf30e7d53fb3cdff4453cd2c6abdb9e0910d604d7732d27a590 |
| IntRuoyiFronted/scripts/astra-c-continue1004-r8-catalog.test.mjs | 29ace0bf3fa4a9039f98ef7599d760f1dcca5aa1 | c8155f0e9d7b12911e917fd01adae47bdfadcd2084e51b4607d671bdce685fa7 |
| IntRuoyiFronted/scripts/astra-c-ncr-authorized-menu-1004.test.cjs | dd063a3db9b85de68c104e95b509018bf1aff1b6 | df12ab142f05aec468592396620f2f5ef1dbf24b30c515d104545321cd659499 |
| IntRuoyiFronted/scripts/astra-c-reverse-drawer-size-1004.test.mjs | 8ed7365046db3a254d2e6c8bd776af9b4d4d0b77 | 7dd5561fb1e7d898bc455e250bf61b41f9776c7da37d840ab6922e096798b53e |
| IntRuoyiFronted/scripts/edhr-reverse-trace-behavior.test.mjs | 58598eb6c37fedb079c406efa6a07c9afd6f8f74 | 419b37077b095ffe095a6133364c52a1ec213fd886a7cff67bd025a4687cb392 |
| IntRuoyiFronted/src/api/mes/pro/edhr/activeOrderCorrection.ts | 08e83fb2ae15507b1627fdcb99fd58dacee03960 | b622a456fc108d286b0be66501c387e891a3a45c1d0aa2f168d4c9aefe0d23fb |
| IntRuoyiFronted/src/api/mes/pro/edhr/nonconformanceReview.ts | 8d94338ebec073afceeabac9917f6e4b2fb38d5d | 9c4f220140be3f6668f9c03aa1a0d5345b6f266b48737a62ee9410009b4d5c85 |
| IntRuoyiFronted/src/api/mes/pro/handoff/index.ts | e5bb5903cb6365cd62ebcfedea765db951729820 | 3bad82d35e78b06ed4dc3636c40aa6c38434c3eac94fa4e4d9bfb7d4a7f70249 |
| IntRuoyiFronted/src/api/mes/pro/processpool/frontlineReturnCorrection.ts | cb10bf07a7532dcb79cb2e676f34cb4165b3d762 | 4dcd670d16c7df641b164f964c6ddcfc399e5cb99f7ed09cfeba78d4d8c3c7b9 |
| IntRuoyiFronted/src/utils/activeOrderHandoffNavigation.ts | 3174cdf5cd191b013a586ba57b5c2908ec38fcd6 | 505f219788b591f272e9c29facc7bb59d3bb105e8c0b55d6e4ad8315584b9e1f |
| IntRuoyiFronted/src/utils/notifyMessageNavigation.ts | ac134993ea67a9a9cc423a9eff96a35ecb6dadf8 | e48db2f0d7a0135ea661e9ba699fb056e018622920f65fc7aa5cabb7537ed038 |
| IntRuoyiFronted/src/utils/routerHelper.ts | e90969410ecdc185629d9629b4afb08be5292cfa | a3c59a8860ecd21b60b4a38366a18c4de3f02a6adadc1d293e71a9dbd5f027a7 |
| IntRuoyiFronted/src/views/approval-center/index.vue | 0650e3344c62019797a3fe6f9a1129fb9ea2bb3c | eeb1b88bcd5620d5dd0a791ff32411567a3889f555ca38ae1070a93ee9781ad6 |
| IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue | f590762869a6fc694cce3518cd3bf0cb476d1b31 | 9d9528808ef06d3e1c163f84264508ca3ab5b2bb0cea91d5a46b04ef59a9c104 |
| IntRuoyiFronted/src/views/mes/pro/edhr-batch/components/BatchReverseTracePanel.vue | 62601c7249ad0949ca84cc8a408e15663cc13443 | e6e36d2f6a685e91f21437b31e9a40bd70f8453802a17cae0297a0cd12b58a7b |
| IntRuoyiFronted/src/views/mes/pro/edhr-deviation/DeviationDetail.vue | 47fcdc5a314c18d95d1ed16dff2800aefeea1cb0 | e7ef9cd4430b168de55879aab5dea3d6cd17613ca86326ad8a048a8e68c805b2 |
| IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue | 83d770c54de574e180f431596ef0f19143a89127 | 453d7dae213fb35cc75291d38646b66a41a1fb6945f40932bf82a82d955cc7fd |
| IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue | 7ffc717cb280c06dbc876328a17c32e22cbdfa3d | 3cc1823853baa9196321999ef0762614a47ae8d2e3690a4977539f807b88116f |
| IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineReturnCorrectionPanel.vue | 6a857fc59c2da74798a64b9ae6c1304b19b36d6a | d29bcda65b671a74e69cefa6bcf60fed3434b6fb82c09f5c9a0ec81a65b5876f |
| IntRuoyiFronted/src/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue | 8f3ba2eb958fa63b81c1aa9ec1d9a3f6f6ce5879 | 0c76662eaf4ba294fc7002c147ad9212066e01e5c2527351a018164505df82df |
| IntRuoyiFronted/src/views/mes/pro/handoff/PqcHandoffAssignmentConfig.vue | 45cd6a2330b76ad7020715dad878cde0fca60010 | a2814f2bf8c9397ca3d673140de56650b9b2f1051d5f9c8ba138c023f2dfdf49 |
| IntRuoyiFronted/src/views/mes/pro/handoff/QaHandoffAssignmentConfig.vue | 06a83281b44c0b649157f4f3199136c32e7edb35 | 47ccaf55a5aa077ffedaadf9b514b87de817d51175bc6ba28d4468f7198a6446 |
| IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderCorrectionHistoryPanel.vue | 45b5a2e973d497bf6ca7b9227495cb84357299a4 | 2ace693c83471c3eea4ceb645034071a5e83ac45f9a72f1f3c5084f72097050a |
| IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue | 0b4c5fb64018eeb894970599f222870f2c8e400a | 4de831cf5455f6d7199ed23d9d847a463fd7fac9f91815ff3b8595959cfc44a2 |
| IntRuoyiFronted/src/views/mes/pro/processpool/components/activeOrderCorrectionHistory.ts | 306a7698b6319b0f0ca2d860a3eb625c60b9b799 | a8e67373a6fa82d5e1a007588ef7490862074045657005ee7afab333348454f9 |
| IntRuoyiFronted/src/views/system/notify/my/components/MyNotifyMessageList.vue | b5790a44fd98007173bfe325b6f1dcac5e1a6102 | 6cc0370203cdb7763c6ce74d383436945eb864c03fa115f03f39f8bd78606370 |
| IntRuoyiFronted/tests/e2e/active-order-correction-history-behavior.spec.cjs | 7716bd44957a1a38ddd9d533f639c16ed12493be | 71e8741d23f2b5b53a95395d030e45076cfd8786076dd32ca423f0c96768efe7 |
| IntRuoyiFronted/tests/e2e/active-order-handoff-behavior.spec.cjs | 90fa6fd045327e65c4b3173dbf22251ede012a29 | c3ed59a9a298f5b52a0b3bfa6c05dc25082a8b39d199302933d3919f9688fea2 |
| IntRuoyiFronted/tests/e2e/active-order-handoff-notify-entry-behavior.spec.cjs | 194f93c02dfa00216f4addbc7b19a2fcc7bd6d7e | dedf979647e22391c1f4c6bc51325813af45108bf6387848fed9415935650f76 |
| IntRuoyiFronted/tests/e2e/edhr-manager-completed-notification-entry-behavior.spec.cjs | b9eac8deb7366e24b90ab3b49f71cc426feef4e8 | 399aa2b546af89708f321fcb7de5cfb262e92e45ca7f933421d0f535c269abdc |
| IntRuoyiFronted/tests/e2e/edhr-manager-market-release-entry-behavior.spec.cjs | 68a824a776292d817f27c5b07034434df0c9b889 | c3bdda93ccf3c6d78874314e18eb1550af714f8696f4e7153a66bdef041a838d |
| IntRuoyiFronted/tests/e2e/edhr-nonconformance-create-entry-permission-behavior.spec.cjs | 76a82e184b934804aa4e1c1d3913a55def53f3b7 | bcf4e83842c0493046e8a38936d377a92c1c2357f6c16d3a16d0fab088e53fd8 |
| IntRuoyiFronted/tests/e2e/edhr-nonconformance-review-exact-entry-behavior.spec.cjs | b598cca58b24fe03718dd1042321ef5c0c026c7e | d2ab96bbc3a874074c5485150767eb49de1a3975e02fc8d0e7feff0780b528bd |
| IntRuoyiFronted/tests/e2e/frontline-initial-handoff-exact-entry-behavior.spec.cjs | df7239354c905292b1b19c9dbf18a48a8d605271 | ecb50813d38fb9f5944f17487d311c1ba2b6839f1d88cb28535650e195650433 |
| IntRuoyiFronted/tests/e2e/frontline-own-return-correction-behavior.spec.cjs | e2b340d786d95ee32cffb8e1fd47035ca06e27f1 | 25ae9b9b59bd43e1fb0870a7b11f98b91f7150431122a67e011b1c6a269ecc76 |
| IntRuoyiFronted/tests/e2e/frontline-own-return-parent-isolation-behavior.spec.cjs | 66f9fc270f815a8d359227610f898579ac012b10 | 9a5c362114e606b308450a6d9232c5587d3e90a0fff408bc178fc1f255c57fe1 |
| IntRuoyiFronted/tests/e2e/pqc-handoff-assignment-behavior.spec.cjs | 5573bb6b8dc008759dba2874b7c4851472286107 | 71c80e95100330bc494b963e75e413c699ba7107702434b2f3a3212449bf823e |
| IntRuoyiFronted/tests/e2e/team-leader-production-handoff-detail-behavior.spec.cjs | f70c3f49e82d8feb61880f6b538a4091e7141f61 | 8420e0c060f85bf7c6504b9bc860301ddc24d5d32414d7bb9f8327a2759a9524 |
| doc/tasks/20261004-edhr-sa06-identity-read-fix/task.md | 4e37a978c02b89e5f1595bc1b06c0aa1b109b827 | 1719779d833ed19fcc53698b01605317f99b2a2fa76ff3c37d502c2ff05f1d9c |
| doc/tasks/20261004-edhr-sa07-sa08-fix/task-state.json | 3412f0af21a5903dcff918bb26d9a05f8d20ec85 | 6b9c6144bec8b58038aa09923f36ba9317ca1e1f360530c9d87bdd0395aa6cb6 |
| docs/database-rules.md | 3d3625719f6c281951dc4dcdcbfde88ef436c9ae | 3f78c5fe442ed2f004c145a8f569e5a4625da121a346b18f1a8700483215b691 |
| docs/e2e-rules.md | 02f9b01847508e6c15b5a0839a79ca9ee8302250 | 70f9f3d4bc95b2c32d8a914cc35a5e4d618bfbbf64ae50e2b798d85eee3f6637 |
| docs/experience-index.md | 3a64f34a3b9cfca920ab72edbc41361733cb4708 | a714a2dd70f922b5873f94598098addfa603b2432636541b1f9aa603c7266896 |
| docs/local-runtime.md | 92897a8f559813726d3247cdd4cff3d30cc0242d | dc2dc22993a18884b3eec97a9cdebe7355265c3cc7bb90998d002a1a0161d80e |
| docs/release-build-preflight-lessons.md | dfb62a336246178c62454c9af59dd472f817efa1 | 5a2395f6347c144f0813877d9a34beb3842b7d4c61f7228d3912ae910c6591f3 |
| docs/worktree-memory.md | 1a84795740befd5d1d4545f5f2722f8ba0c50566 | 205ba7e13e8318042c09d1b8a638714c432191448945f7a68fc1849b97f981ed |

## Baseline Commit And Experience

- 基线提交：39608b8620bd6a986d7da04c5ab5c0883a92edde；155个文件，23799 insertions(+)，7036 deletions(-)；文件清单见Staged Baseline Blobs。
- pre-commit运行时端口守卫：PASS；未启动、停止或重启任何服务。
- project-experience-consolidation：归并到既有docs/powershell-encoding.md，新增历史混合换行暂存规范化与重复CR核验规则。
- 任务记录与机器可读状态已设为ready_for_closeout；当前任务没有业务实现提交，经验文档变更将单独提交。
- 代码基线首次push：PASS；origin/int_main由50dd3177a推进到39608b862，原9个未推送提交及本轮代码基线均已同步。
- 经验文档提交：1ba66728fc6e771eba254e7623e73e827dd3ce72；仅docs/powershell-encoding.md；staged清单、diff --cached --check及pre-commit均PASS。

## Cleanup Evidence

- preview：PASS；keep为3个核心Markdown和task-state.json；delete为6个本任务冻结/诊断中间文件；blocked=[]，warnings=[]。
- preview确认全部delete路径均位于当前任务目录，重要文件清单、指纹和检查结论已归档到本日志，允许apply。
- apply：PASS，删除6个当前任务诊断/冻结中间文件，保留4个正式记录；blocked=[]、warnings=[]；主工作区，无worktree合并或移除。
- 经验提交push与实现远端核验：PASS；本地和远端HEAD均为1ba66728fc6e771eba254e7623e73e827dd3ce72；ahead/behind=0/0。
- 清理及主干代码同步完成后，task.md和task-state.json标记completed。

## Final Closeout Records

- 最终提交只包含task.md、execution-log.md、verification-report.md与task-state.json；忽略规则位于.git/info/exclude第17行，需要对这4个明确路径使用git add -f，不修改忽略规则。
- 收尾提交主题：docs: complete October 5 main code push；提交hash通过git log -1 --format=%H -- doc/tasks/20261005-main-code-submit/task.md解析，避免文档自引用hash。
- 记录结构、UTF-8、最终staged清单和diff --cached --check须通过，再提交推送；最终以git ls-remote和HEAD一致及ahead/behind=0/0核验，失败必须停止并报告。
- 首次结构校验器仅按LF匹配标题，误拒绝合法CRLF记录；已用UTF-8通用换行读取复核，文档标题、真实多行及正文未变。
- 正式4文件结构、UTF-8、状态一致性与cleanup保留文件核验：PASS。
