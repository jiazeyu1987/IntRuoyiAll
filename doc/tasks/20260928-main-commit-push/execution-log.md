# Execution Log
- 用户授权：提交推送主干代码。
- 分支：int_main；origin已配置。
- 历史验证：doc/tasks/20260928-edhr-flow-08-15-fixes/verification-report.md 记录607 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS；本轮未重跑。
- 经验沉淀：已阅读project-experience-consolidation技能；现有docs/backend-development.md已含本次修复经验，不重复新建长期文档。

- 业务基线提交：7f1d74e60f6bb2e75a3e88914a38ff84799c9618；文件清单：
IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolPqcRecordMapper.java
IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcInspectionPieceDetailMapper.java
IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java
IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java
IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcProcessInspectionAggregationServiceImpl.java
IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionSourceEvidence.java
IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderFifoAllocationService.java
IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderReportConfirmationServiceImpl.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFrozenProductionWriterReaderR3Test.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionServiceTest.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFifoOutputMaterialAllocationTest.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesP0ActiveOrderFifoClosedLoopTest.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesP0PqcQualityAllocationGateTest.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesP0TeamLeaderReviewSignatureServiceTest.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderFifoAllocationServiceTest.java
IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderReportConfirmationServiceTest.java
docs/backend-development.md
docs/bugs/20260928-edhr-flow-improvement-backlog.md
docs/bugs/20260928-edhr-flow-runtime-blocker-audit.md
- PASS: 业务提交已推送origin/int_main；cleanup preview/apply通过，无删除项。收尾提交仅包含本任务三份记录，其hash由Git历史关联。
