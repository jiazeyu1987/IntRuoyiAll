# 执行日志

## 用户授权与范围
- 2026-10-03 用户当轮要求：提交推送主干代码。
- 本次任务只提交当前已存在的代码、测试、SQL 和经验文档；不接管七账号真实验收、部署及迁移任务。
- 已核对根 AGENTS.md、docs/task-closeout-rules.md、backend-development.md、frontend-development.md、database-rules.md、powershell-encoding.md、worktree-restrictions.md。
- 使用 task-closeout-cleanup 与 project-experience-consolidation 技能；只处理本次任务目录。
- 初始主干 HEAD：de95bb7437ba2572f9e30ada313115d234a8d53d；暂存区为空。后续白名单与指纹由只读快照记录。

## 验证与里程碑
- M1 completed：固定 98 个文件，UTF-8、冲突标记、凭据特征、git diff --check 与快照一致性均 PASS。
- branch-runtime-port-guard PASS：int_main，8081/48081；未触碰服务。
- M2 in_progress：前端 10 个定向脚本、21-spec 静态套件、11 个 SFC/TS 源码编译与新组件 ESLint 均 exit 0；后端 35 类 reactor test 运行中。
- 验证脚本最初的凭据特征扫描误把 task-* 的 sk-* 子串当成令牌；补齐词边界后复查 PASS，未修改任何待提交生产文件。

## M1 文件白名单与原始内容指纹
初始路径数：98；远端 fetch 成功，本地/远端 ahead/behind=0/0。
- IntRuoyiBackend/sql/mysql/20261003_mes_release_task_notification.sql | SHA256=250be9cea1fba8fd40a065975501d98681ad687dfb280faedfd000d1867df5fa
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrBatchExecutionController.java | SHA256=e7b5fffd1da400c8e0f38cbe7c2d746c82b34ddfb0705402f5765b0611e24c25
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/MesProductionReleaseController.java | SHA256=ab7b9ffb5cdfbec159942e36301aa64914a344dae4238c20a77dd70d5e621b1d
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/MesReleaseTaskNotificationController.java | SHA256=647b3ac66f8494b081d73ded4079a0c594fa76be0382fbab45bb41d5287d5210
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/vo/MesPqcProductionReleasePageReqVO.java | SHA256=b57bda01f3708656d78287c61231c08f7dd4eaa3b854251ee17d2e736de7d3e4
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/vo/MesReleaseTaskNotifyDeliveryRespVO.java | SHA256=45d4402d4de0d8012089c27a9b3df9834f0350b82c140081255048541e865801
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/vo/MesReleaseTaskNotifyRetryReqVO.java | SHA256=0ede4fd5d749c92e20ef45d6548de05afe36e06bf0d7026efdb951d6905cb8fa
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/productionrelease/MesReleaseTaskNotifyDeliveryDO.java | SHA256=87644f7c3d9a5c9fc848ad4dff8953268ca1b144dea81b99480c3289507836b4
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderReleaseApplicationMapper.java | SHA256=f565ecb961838c9d3a532aa76a325f58f1034787646454d52d93b03a38bce6fa
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/productionrelease/MesReleaseTaskNotifyDeliveryMapper.java | SHA256=fc414443b6e60f040ecba6ffabcd23e72af2ef3a44f85380cc356d6380dbbf44
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java | SHA256=a1eae7f3925778d0501540085ca3850f72979ac738fba76814a70f708e8a1acf
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionVisibilityService.java | SHA256=9b57737f80697dc7ae572487a47b5422aa8c59e9a36142b9b311cbc3547205b1
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProductionSignatureEvidenceService.java | SHA256=6317b687e4b818a80546a184c64efedb36f2c0c2dc4b463d89f6c76d67b72226
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesTeamLeaderReviewSignatureContext.java | SHA256=82551eea38772f3f141bca9866fed7260f3fb206fc9491bbfbc6ced1ee748482
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionServiceImpl.java | SHA256=fb47bfb2faf0d8574f15ee953facdbacad7e7795b72c8bca08d0c148f3b44cea
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java | SHA256=22757725008003ee658feb14018859321d8657eafa770cb6470210741fba7258
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolSubmitEventServiceImpl.java | SHA256=adf8e8ebbc81d4e2f9cba2c475e12b95be35809522b2fe0e7624af3d12632cd2
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderSignatureEvidenceService.java | SHA256=354016473a05c75d46fddf22a8dc527ea7cabf948043321466e8e7ea9ec9b49b
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCompletionAffectedRowAudit.java | SHA256=066a816cce0280c6b2fa992f262497babb94b32153c9b9862405027d6715507b
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java | SHA256=964ae97cffd2b9b1130efbd2f77425c7b7ad8a323c655000476d09213bf1bb83
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.java | SHA256=07c857e8684d36238427599ee676376f16bfaa9ecd41ba8852f9a7e9ae3a9f96
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.java | SHA256=17d96e4b8e9a126652846c750dbdd2ecda215d44c31987ca85ae19efce76dc2a
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.java | SHA256=e0d6dbfb10bf3d836a94fb9c6027d5bda4da554ad9c6e497e41d29918c10e137
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.java | SHA256=564fb080afec89756c1e039b1e7ec73a6df18584c2516d12b0a57b93748430c1
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceImpl.java | SHA256=7ea62e2bfa2a4e86987c9226f4229adf25d19395214be8a713e3dc098016210d
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java | SHA256=0e5eb707c7831969e1c41176926d911807e881c01d72c07856662498f9d0d906
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationAudit.java | SHA256=fe3eaced4d1abd91d1f90380d910e3409fb88229ad9e1b0603ebab78a0bd2c5d
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationContract.java | SHA256=7a958bedf282196617402e011ca3430c2829bdcbc9f0edcd750644a18b88ee71
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationDispatchService.java | SHA256=ee48a509cebc727499d79c1977060a97bf6dfbb77884b35be58c3d47795815f3
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationPlatformSender.java | SHA256=d3b448bcdcb7a7ecc6c652a4fffaffc328532c0d6e64e53dd7fd1ec283224bc9
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationService.java | SHA256=25da8cd0b1ce6c01936ca74c0cfb2a2f9037ad8dc8bf38cc09474cd1da0b1a6a
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationTransactionService.java | SHA256=b7512d2d02f7f43235846fbe91bffebb9a82b784448ca546bc646b4bbde593e5
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java | SHA256=655ccb7a9e43aabf4762946e4c3da0fff40372f833f7bb8bf37b752813c89b55
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierReadScopeService.java | SHA256=b350881de836b9f060ab7c8ebed5ce08d7862d44d20f2d3b11d07370d02365a6
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleasePageQuery.java | SHA256=d81b0cc53fda6590d68572727d8cdd0f44dc706dde729d5b95dc0f1088978a5a
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java | SHA256=8a8ec810a4d70168b148d9d03d6290ffb96f1b36cd036c05c7d30fb68a9a34df
- IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml | SHA256=a671eec4d5dc323e9b79a64d2404c926fb35d38ae9cad3b87e7a4a0e0b991ac8
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrBatchExecutionControllerTest.java | SHA256=424cf048a8671f2d4a82a923f58c99c188bfce054f466b6122fa711d6833bede
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/productionrelease/MesPqcReleaseExactPageControllerTest.java | SHA256=54d40cac8f2a5c77b6c74359498cd3e0e4c4a088c5a2aecf1b92cf13d5da5bf7
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java | SHA256=d789853865094578b506d734eeb1917d04821ffa995b92c1e26b5727453eb761
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrManagerBatchVisibilityTest.java | SHA256=18380df3dac529737ab9f31996ee4ee73098332e863d9f66e4358f812f3f8ae2
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProductionSignatureEvidenceServiceTest.java | SHA256=7e40ba4421ad9a370201368fbc6bef3990a21d17be604808b3e0a2215448a509
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesPqcCorrectionMysqlTransactionTest.java | SHA256=d5759572cf26250f71cce8453ba0fa172bcad18875f604e9fe5a1462fee27d8c
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesPqcRevisionTaskIdentityTest.java | SHA256=20f1086ff16befae6b4f17e9d1e38dc349ba305af14fd41e84ead8b6f7fe8cf0
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionServiceTest.java | SHA256=93a65e1bdaa7ee8cc24a7de4d707ef5363df5bbac0088df31764d1d44bcbf30e
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolSubmitEventServiceAdapterTest.java | SHA256=fa25267ddeeaddcad351a864a3be004aa9ccdd8d41aeed45adb44c7ada0dc6df
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelinePqcGroupSqlTest.java | SHA256=cd501deab9329c2aa5b121dad996349b28b04c3e065ef120f40b69e546da1954
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderReworkCycleTransactionTest.java | SHA256=5dbafcd385d7b35b594770947c4f4432f4773a128f56e9fa721815e024d1ef96
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCompletionAggregationAuditTransactionTest.java | SHA256=40ddb93f1e1ee8a0d484c04220a978a4090df608e57f7afb76a78b76af2c3000
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCompletionAuditFragmentContractTest.java | SHA256=08ec7adff01f6fa29828331a50794321cee232edb0442701d8ade7d16ea68a77
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesCompletionAuditPayloadContractTest.java | SHA256=972617bf7f552b1ad614dbb178d193173a48137df3e1db4363d8d2a432395e34
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcCorrectionSignatureEvidenceTest.java | SHA256=e84404e19cfce76fa493c5ccb5c663e1d7a44580d7e9292cfd4fe1b2dd3f637c
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesProductionReleaseSignatureContractTest.java | SHA256=23ff3c3dd0b5125749cc7d800efe2b87d0fa8d685e74373d708a609267f35c86
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceTest.java | SHA256=82d97a819c1ec46facca22929c55103bb8e9bedfd3954322c28f23d0fd682e4d
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterImplTest.java | SHA256=e5f797db93f14c8dd79aa4a5c1caca4585897dc1cf4f6b50632a8d5887991c83
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest.java | SHA256=63f9a5cfa342fdcffb8e262d44c78200a39d3f5fa81ced996bc42111cc740779
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportWriterTest.java | SHA256=cfd68b3a5676f6bcec3599bfecada9f10708a27e0f328a7e96d46457de6b3f56
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossSourceReaderTest.java | SHA256=98e4c2d80ab2a0ced5f05fcb54500a648ca7fc752a6f25012d118b2fc791c949
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderReviewSignatureRoundTest.java | SHA256=3a878958fd565d886afde49b4944cc355b7628d8e9b76b9c2bbd0ffffa7ad478
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceTest.java | SHA256=6d6c04777c8db081818236603708ea0601b84216c7f45644621e1a0e863c25e3
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProEdhrNcrReworkRoundTest.java | SHA256=354238cd58ccdc638ee2a0b453eab83ee0001b478720521bb302e07aeff57040
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerTest.java | SHA256=004a6e3fd61e60f17db3fe96c3cb75f9593335025df730ceb2cb0aadf64e16a4
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationHandoffTest.java | SHA256=0d6094ec910b262148b2f5acf3d1cbe7af2fb0c8aae276ae7fe0cf82b4c4d8b6
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationKernelTransactionTest.java | SHA256=a040e084ad63290d9ede3ab495eba4faacd3fbec71ce6633465f7504d98aa305
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationMigrationContractTest.java | SHA256=ac7f4dd0a44a44e1b1ca48440e325d8dbbc3c7417e2c142d1166e7be66c7a355
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationReliabilityTest.java | SHA256=6a13e4e51c70d436af083d12e3ef7c8b6e9f965244d99c837635e90f2d282a04
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileServiceTest.java | SHA256=ac1ecf9cb7e4ea91830ba2489ec203596c4917f2b1f62dff834b6291c4dbf148
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierReadScopeServiceTest.java | SHA256=8b791e60977991acc01c2aab91ea213b3a0b8a19a9290f34ca6bd0c102f34dab
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseBatchExecutionServiceTest.java | SHA256=e3a9911f2719c9b15eed85d87f394885944922a2aa1225ac3b3d43fd8c81a578
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleasePageRoundOwnershipTest.java | SHA256=b64b827ecaafe67476d64693ba77296007a19e6ed13f36ff74364d3b629e51b7
- IntRuoyiBackend/yudao-module-mes/src/test/resources/gxp/gxp-audit-release-notification-policy.draft.yaml | SHA256=26eabed8b8d667cd3a35586ccea7ee88c6d8b790f20011f820dcab16f95afa3c
- IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditTransactionContext.java | SHA256=43b70663295b46cfaf800c6e1adc336265263868696e81f7feb4928e1328ca43
- IntRuoyiBackend/yudao-module-system/src/test/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditTransactionContextBoundaryTest.java | SHA256=a8e82c027d449046c5385288830d5471b5653b6c20c4eeb97e7cecd5561ece11
- IntRuoyiFronted/src/api/mes/pro/edhr/batchExecution.ts | SHA256=c7163c328cd7cd93af47666841d94c141597ed373907860ef7089fb2ae4f6422
- IntRuoyiFronted/src/api/mes/pro/productionRelease/index.ts | SHA256=223c8aad57ab097b8465ce92fb39cd034b5ff0a6580b47645c1ae0e9a5bba06c
- IntRuoyiFronted/src/api/mes/pro/productionRelease/notification.ts | SHA256=f680d592920b3bd39d5294ed577de046e864abbbdf5520abf8df9a9667b0f1d7
- IntRuoyiFronted/src/utils/edhrWorkTaskNavigation.ts | SHA256=c4966295351d743bffaa36a53f3aeb7caa67982be8699775891da18d0959bea4
- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue | SHA256=84f24f5d832b256e66e96c0fc5fc8faaa02f23bbdd46a30382719d898c2686ff
- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue | SHA256=371f678c6536c224f447717e63b64c63270e622774eb80358348d200db1753c4
- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue | SHA256=c4c44620ba48effaa2c1c951a1266461e21fc2d82d0442eb960d809f1d1808e1
- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue | SHA256=f495f110e125137c9694cb2f5d8886467aec71d2ad8808eb46327dbbcd9b282c
- IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue | SHA256=7e339e9b0c40cb70b6e776d6ccc5fd7d36b5f8c1555e244c1cd09c114dac31de
- IntRuoyiFronted/src/views/mes/pro/processpool/team-leader-pqc-correction-source-behavior.spec.cjs | SHA256=7a7f5d54c5234ec01775c6513428057bc8b829d170e6bd1534ddc94ae2b8184f
- IntRuoyiFronted/src/views/mes/pro/production-release/PqcProductionReleasePage.vue | SHA256=774026f58f0630f707ad2d95bf44b06e3301d24baff85e9b36f54066cd85f7cb
- IntRuoyiFronted/src/views/mes/pro/production-release/components/ReleaseTaskNotificationEntry.vue | SHA256=94992d7652eb2b38e0520417e97ccfa41d621bd87858d443e6090286a8e777cb
- IntRuoyiFronted/tests/e2e/active-order-dossier-context-race.spec.cjs | SHA256=fa7134c625463eb1c6f19127ea0a2badadb09b196e98dcf1b34e58375a48ca22
- IntRuoyiFronted/tests/e2e/edhr-history-dossier-scope-behavior.spec.cjs | SHA256=73444c4b3376b98f2f46b843a51569c4bb70c8e46f56577ccc9ec594d77680d8
- IntRuoyiFronted/tests/e2e/edhr-inline-dossier-parent-scope-behavior.spec.cjs | SHA256=3c38e14db3142e01917b6ac360db79eb82ab817109ca5ee54804a5e64372e5ae
- IntRuoyiFronted/tests/e2e/edhr-manager-market-release-entry-behavior.spec.cjs | SHA256=2d767c02bd411da557d5ec42898b62a204656e3b9f5a2f4eeff59e087460a143
- IntRuoyiFronted/tests/e2e/edhr-manager-task-route-behavior.spec.cjs | SHA256=06623f19e50c3c9dc8d8e930200d53239eb226d2817ea58d351ef4c3bef78cd5
- IntRuoyiFronted/tests/e2e/pqc-release-dialog-isolation.spec.cjs | SHA256=96851f7dc861e4ecf97660ebfe568e44e72866a37fb3e1d8d1d2013ada57f835
- IntRuoyiFronted/tests/e2e/pqc-release-exact-task-route-behavior.spec.cjs | SHA256=e5a781887c55137bb64659d07de7097403663436e8f807fc55f9a1a4fbd7f999
- IntRuoyiFronted/tests/e2e/release-task-notification-entries-behavior.spec.cjs | SHA256=757b84694f0ef225f9dce7e1208bd575e4bf80ea887fa692f967f83e94799a91
- IntRuoyiFronted/tests/e2e/release-task-notification-recovery-behavior.spec.cjs | SHA256=b61e984e73992b2482dbf4085b572a7dbacc810c44e7c03ab48cd09b1e53ed87
- docs/backend-development.md | SHA256=59846df874b8c1bd8b34264fadeaf37c22eb2ad3ff3992b0187b24873b66efb7
- docs/e2e-rules.md | SHA256=d9ad45093be85ffa183bdc5cf20612ee82a4076081423648715b70b676900bcb
- docs/experience-index.md | SHA256=7e9195f32a979c561d63e8a358c9c5c3ca4be2a56ff64c06ca47c3e13c719d23
- docs/local-runtime.md | SHA256=32477a79516c00f3401f3cd69353e1ca47a192d64d4af38daf6fb35e678b0de1

## 定向验证范围
后端测试类：GxpAuditTransactionContextBoundaryTest,MesActiveOrderDossierFileServiceTest,MesActiveOrderDossierReadScopeServiceTest,MesActiveOrderReworkCycleTransactionTest,MesActiveOrderSignatureEvidenceServiceTest,MesCompletionAggregationAuditTransactionTest,MesCompletionAuditFragmentContractTest,MesCompletionAuditPayloadContractTest,MesPqcCorrectionSignatureEvidenceTest,MesPqcReleaseBatchExecutionServiceTest,MesPqcReleaseExactPageControllerTest,MesPqcReleasePageRoundOwnershipTest,MesPqcRevisionTaskIdentityTest,MesProBatchRecordExecutionSignatureServiceTest,MesProEdhrBatchExecutionControllerTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrManagerBatchVisibilityTest,MesProEdhrNcrReworkRoundTest,MesProcessPoolPqcInspectionCorrectionServiceTest,MesProcessPoolSubmitEventServiceAdapterTest,MesProductionReleaseManagerStageInitializerTest,MesProductionReleaseSignatureContractTest,MesProductionSignatureEvidenceServiceTest,MesReleaseTaskNotificationHandoffTest,MesReleaseTaskNotificationKernelTransactionTest,MesReleaseTaskNotificationMigrationContractTest,MesReleaseTaskNotificationReliabilityTest,MesTeamLeaderActiveOrderCompletionServiceTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterImplTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderReviewSignatureRoundTest,MesTeamLeaderSubmissionReviewServiceTest,ProcessPoolTimelinePqcGroupSqlTest
前端独立测试：
- IntRuoyiFronted/src/views/mes/pro/processpool/team-leader-pqc-correction-source-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/active-order-dossier-context-race.spec.cjs
- IntRuoyiFronted/tests/e2e/edhr-history-dossier-scope-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/edhr-inline-dossier-parent-scope-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/edhr-manager-market-release-entry-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/edhr-manager-task-route-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/pqc-release-dialog-isolation.spec.cjs
- IntRuoyiFronted/tests/e2e/pqc-release-exact-task-route-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/release-task-notification-entries-behavior.spec.cjs
- IntRuoyiFronted/tests/e2e/release-task-notification-recovery-behavior.spec.cjs
- MesPqcCorrectionMysqlTransactionTest 使用另一任务的固定 MySQL 容器/59241 和快照库，包含写入；本轮仅 reactor testCompile，不运行该外部数据库集成测试，不把其历史结果计作本轮 PASS。其相关服务另有本轮 unit/H2 回归；真实 MySQL、迁移及部署不属于本次代码提交验收。

## M2 completed
- 后端 35 类 722 tests，0 failures/0 errors/0 skipped，BUILD SUCCESS；各类本轮新生成 Surefire XML 计数已归档验证报告。
- 前端 10 个定向脚本累计 111 tests；21-spec 静态套件、11 个 SFC/TS、组件 ESLint 全部 PASS。
- 固定 MySQL 测试只参与 testCompile，不连接或写入其它任务数据库。通知迁移合同 2 项通过；真实迁移、模板配置、DRAFT 审计策略批准及激活不在本轮执行范围。
- UTF-8/冲突/凭据特征/git diff --check/98 文件 SHA256 一致性 PASS；待提交中包含正式 test/resources DRAFT 策略片段，runtimeLoad=false。
- project-experience-consolidation：复核已存在的 worktree-memory.md、powershell-encoding.md 及本次既有 backend/e2e/local-runtime 经验增补。大规模 Dirty 分类/白名单、测试报告新鲜度、凭据保护已有覆盖，本轮无新增可复用经验；不重复新建长期经验文档。
- 原始日志、结果 JSON、指纹 JSON 与一次性 verify.py 均为本任务清理产物，关键结果和完整白名单已归档 3 个保留文档。

## M3/M4 提交及清理
- 独立代码基线提交：18e814b4f3a239f6338b613ac3f9ed22679b91de，chore: snapshot verified eDHR release and signature changes；98 个文件的完整清单与原始 SHA256 见 M1。提交前快照一致、暂存白名单一致、cached diff --check 及 pre-commit 端口门禁全部 PASS。
- 本任务未修改业务代码，无单独实现提交。
- task.md 状态先设为 ready_for_closeout；task-closeout-cleanup --task-id 20261003-main-code-submit --mode preview -> ready，keep 3/delete 18/blocked 0/warnings 0。
- 已逐项确认删除路径全部属于 E:/IntRuoyi/doc/tasks/20261003-main-code-submit，关键证据已归档；--mode apply -> applied，实际删除 18 个临时产物，保留 3 个核心文档。
- 当前主工作区 linked=False、int_main；无 worktree 合并/删除，无停止/重启进程。
- git push origin int_main -> PASS；pre-push port guard PASS；代码基线 18e814b4f3a239f6338b613ac3f9ed22679b91de 已推送。HEAD/origin/int_main/ls-remote 三方一致、ahead/behind=0/0。
- 推送后检测到 docs/database-rules.md 和 docs/experience-index.md 两个后续并行经验文档改动。本轮不将它们暂存或覆盖；任务所有权规则允许无关并行资产保持未提交，最终不宣称整个工作区 clean。
- cleanup 通过及代码推送成功后，状态 completed。收尾提交仅包含 doc/tasks/20261003-main-code-submit/task.md、execution-log.md、verification-report.md；doc/tasks 被仓库忽略，需要选择性 git add -f。
- 收尾提交唯一定位：父提交 18e814b4f3a239f6338b613ac3f9ed22679b91de，消息 docs: complete October 3 main code push verification；其自身 hash 无法在同一提交内自引用，实际 hash 在最终 Git 输出及用户答复记录。
