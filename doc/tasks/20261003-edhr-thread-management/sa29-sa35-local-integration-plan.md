# SA29—SA35 精确本地融合方案

## Current Status
in_progress — 授权42项修复已精确提交并本地融合int_main，main与worker无关资产原封、暂存为空。另5项验收/融合记录独立提交，随后对实际主干开始完整八方向静态审查；未达到最终零问题门槛。

## 本地操作范围

以届时核对一致的实际 int_main 为基线，只提交和融合下列42项经独立验收的修复差异，包含3个新文件；不需要额外既有基线提交。验收及本地融合记录另做任务自有记录提交。保存所有未列入清单的既有改动和审查资料。两个worktree仍承载循环证据，继续保留。

当前 main HEAD：0c8d6cc195f3d9dbb53a01b89fcc6c3f6cd8173a。待操作前再次核对来源、HEAD、索引和并行资产；若发生重叠变动先重新review，不套用旧授权范围。

不推送、不操作数据库、不启停服务、不E2E，不联系重置后。获准并实际融合后，原八个独立Astra功能线程对融合后来源开展下一轮完整只读静态分析，再统一甄别。

## 实际验收

后端122个新鲜实际报告共2619项，失败/错误/跳过均0；独立前端64行为和2SFC通过。14667完整构建输入、343组合输入、2144旧资产及既有根经验文档保护通过。迁移仅13项闭包policy/static合同，不声明真实库执行。详情见sa29-sa35-manager-verification-report.md及经理worktree原始来源、日志、报告。

## 实现差异（42项）

- IntRuoyiBackend/sql/mysql/20261005_mes_report_fifo_cycle_identity.sql
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderReportAllocationConfirmReqVO.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderSubmissionReviewReqVO.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProcessPoolFifoAllocationLineDO.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/ProcessPoolTimelineEventReadDO.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationService.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesSharedProductionReportCorrectionGuard.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationQuantityFragmentService.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationSaveCommand.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesSubmissionReviewExpectedContext.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewReqBO.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceImpl.java
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java
- IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesNcrScopeAuditTransactionTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportCorrectionAuditTransactionTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportCorrectionServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesSharedProductionReportCorrectionGuardTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelinePqcGroupSqlTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineTestSupport.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesProductionDisplayedContextFixture.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationConcurrencyTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCyclePersistenceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationFlowCycleRegressionTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationQuantityFragmentServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierBusinessFileAccessProviderTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/create_tables.sql
- IntRuoyiFronted/src/api/mes/pro/processpool/index.ts
- IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts
- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue
- docs/backend-development.md

## 单独任务记录提交

- doc/tasks/20261003-edhr-thread-management/sa29-sa35-local-integration-plan.md
- doc/tasks/20261003-edhr-thread-management/sa29-sa35-local-integration-plan.json
- doc/tasks/20261003-edhr-thread-management/sa29-sa35-manager-review-result.json
- doc/tasks/20261003-edhr-thread-management/sa29-sa35-manager-verification-report.md
- doc/tasks/20261003-edhr-thread-management/sa29-sa35-local-integration-receipt.json

## 保留的并行资产

共5项未提交资产，与42修复不重叠；Vite变更及其依赖已纳入实际组合输入，只保持原字节，不纳入本任务提交。

- IntRuoyiFronted/build/vite/index.ts
- IntRuoyiFronted/vite.config.ts
- IntRuoyiFronted/build/vite/windowsReadFileLimit.mjs
- IntRuoyiFronted/scripts/purge-source-scope.test.mjs
- IntRuoyiFronted/scripts/windows-read-file-limit.test.mjs

## 授权依据

当前AGENTS要求当轮明确授权Git提交；此前SA22—SA28的17+48授权已执行完。本方案只等待上述42项及单独5项任务记录的本地提交融合授权。继续的静态审查无需数据库、服务或E2E授权。

## 本轮授权与单行增量

用户实际回复“授权”，批准上述42项及独立5项记录，仅本地提交融合。此前待授权文案作为方案形成历史保留，当前授权以本节和JSON为准。仅MesReportAllocationConcurrencyTest第83行删除一个行尾空格；原2619项全量绑定整改前原字节，增量新原字节由1项实际定向回归及编译字节码完全一致证明。其他41项原输出、原完整验证记录保持不变。

## 实际本地融合

修复线程精确实现提交：b876124a822935161847e1a9266660f45675c9d8。主干融合提交：82297d8d4f810a903689a03b87dabad525339749。仅42项差异，0相关基线，其他main/worker资产、stash和无关refs未变；14667份实际构建来源核验一致，单行空格增量作为明确来源覆盖。原全量及新定向证据边界保留。另5项记录单独提交，不推送；完整下一轮分析与最终收尾尚未完成。
