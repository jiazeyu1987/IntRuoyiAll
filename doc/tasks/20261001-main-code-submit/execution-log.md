# 执行记录

- 用户授权：提交并推送主干代码。
- 已读取 AGENTS.md、docs/task-closeout-rules.md、docs/worktree-restrictions.md、docs/powershell-encoding.md 和 docs/worktree-memory.md 的提交分类门禁。
- 初始分支 int_main，HEAD 63aee9ddf；origin/int_main 初始与本地同步。
- 初始改动 39 个 tracked 文件及 15 个 untracked 源码/测试文件；不包含运行备份、Office 输入或临时产物。
- 原实现 BDD/RED/GREEN 证据见 doc/tasks/20260929-edhr-repeatable-test-reset/execution-log.md 等对应记录；本轮是提交任务，未新增生产行为。
- 当前 blocker：无；提交前仍需定向回归及远端快进检查。
- 上述为初始待验证状态；本轮定向验证、提交、推送和清理实际结果如下。
- 基线提交：5264c8e48469263098a5cdb7f6485e5807feac5a，54 文件，完整清单见 Frozen Baseline Files；git push origin int_main -> PASS，63aee9ddf -> 5264c8e48。
- 本任务经验实现提交：4fb95c4b22b1f6dbbac3dc5943dea4ec298a958d；仅 docs/powershell-encoding.md，5 行新增，文档结构及空白核验 PASS。

## Frozen Baseline Files

- AGENTS.md | SHA-256 fc1f7e0a28bc2720e543808f7e3fbd759c1201469d6a3488025d5e4eff5cea74
- IntRuoyiBackend/sql/mysql/20260924_gxp_audit_event_v2.sql | SHA-256 994ec4120445eb165fba9e31b039dd9b8ee6616c09a1e24d0ab41699c2a975ba
- IntRuoyiBackend/sql/mysql/20260928_mes_active_order_rework_cycle.sql | SHA-256 dfd19aaae0a017e231344b5eeaf36acbfec80188471249c278a5fbcf70aef817
- IntRuoyiBackend/sql/mysql/20260929_system_permission_command_receipt.sql | SHA-256 7ad16bed80d5ea281f8ed3f2acba70348fffbb1a99dc71dbc1fe4366cc73a617
- IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-data-permission/src/main/java/cn/iocoder/yudao/framework/datapermission/core/aop/DataPermissionContextHolder.java | SHA-256 b95a4a2817b71c70a486036e96f8978c3d8e0f495afb515edb41825855be9e6b
- IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-data-permission/src/test/java/cn/iocoder/yudao/framework/datapermission/core/aop/DataPermissionContextHolderTest.java | SHA-256 8ea7ff803e9fb178c6bde5fcb36b07e2d4fdd1fb0f7bfcc860598b9df77d752b
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java | SHA-256 409bdfd17e586798daafddd2c90702961fb7ad1180143de4e3237a14da861738
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java | SHA-256 a486ac5c8b8795aefcc76ad5713d0f4a7d9ba0513c8ee1b0f4fd385e80da238c
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskService.java | SHA-256 8570f526aa7cb55d1e0b3409ca67eb58a2e7d12319853593ce2a2a7a5788771f
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java | SHA-256 e3c41bbff61b37b34b7ab46e5d09d4f6f719b0fd8b3a5b8ba133ea0d918881ba
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java | SHA-256 c6628aa62f894fb9cbceba820ff2d057ea1cf4691cfb6090c45e8987f3cb6051
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderSimulationService.java | SHA-256 96b4895b0bb8a78af233e16a74d40c15b41ba3e0f4311ecdcde23c645cde4cba
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderWorkbenchServiceImpl.java | SHA-256 1b15beffd8034243a35cdb2e9b8a917e6d458915dac746e10ac3d7f94e7b554e
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/MesStage1ActiveOrderCompleteSimulationServiceImpl.java | SHA-256 f67d0afeb26e9005c8c279426dda23472456e7af4bfc6b2c8bf895dd6c038e74
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationNcrIntegrationTest.java | SHA-256 bc57ad1430428ad2653f64559ff7c116f27f9b45770b5f20c3abebb391c631bd
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImplTest.java | SHA-256 871af71f3f2e3283fd4138c76c457e3a66068b895203cab3a289371dfeeee4a2
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineFilterTest.java | SHA-256 4577dc9dcd987d7b52ea99d3499de11628d075d6453bd89fbe039f1def4c5512
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesM9SimulationHistoryProtectionTest.java | SHA-256 4514e21d020f91a5ce8a8875310d5dc536e52a210bf7e09215d3c471a4d5da9c
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImplTest.java | SHA-256 81dd5757b9affc0c9ce8a67b6baaf7c2fdab662add25993be19385609b5d1b12
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderSimulationServiceTest.java | SHA-256 7039f7686d4068f5fc94cac265154f580ab793e10ecece591e90d2186bf7faac
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderWorkbenchServiceImplTest.java | SHA-256 1d924e30621ab7c1b28e64dd26c0b7545c60589a019f12bd9aeaa7875e3d6a16
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/MesStage1ActiveOrderCompleteSimulationServiceImplTest.java | SHA-256 a144263c0300d86d9053ba378f545634297325ceb4876b7e95b682d37eb6d295
- IntRuoyiBackend/yudao-module-mes/src/test/js/mes-active-order-test-reset-static.spec.cjs | SHA-256 815308f2b03eb6930dfe06030bd2819131de3b1363ef77746cc059f6d43545b1
- IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureServiceImpl.java | SHA-256 b3e7cc2c971a92fc8be179d39607aa8ca7222cc35ccd2bf494cb349a9f26287f
- IntRuoyiBackend/yudao-module-signature/src/test/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureServiceImplTest.java | SHA-256 c63f00e08863de354222dabee6cc1dd28a5d324fd9a0258a4518c6de85a4a4cc
- IntRuoyiFronted/src/api/mes/pro/edhr/nonconformanceReview.ts | SHA-256 ef3b0fab7ce5aa0f805181499f00a17dc43f0a938f8e4f4118e991c49a480faf
- IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts | SHA-256 378cc69b148d7f3fae3d25c17786f21e447b0002ec1912fb89192eaa5c40453d
- IntRuoyiFronted/src/views/mes/pro/edhr-deviation/DeviationDetail.vue | SHA-256 0838edccb9981c1e1e4778bf6d166dc3572a1c3103910919c184b1a86a0b2a8f
- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue | SHA-256 0eb40126d40b2e76cd2839ab7442f5ecc3e58ecbfa349c085544a5e68918a855
- IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue | SHA-256 f10df65bf732708ad208a5a03e5943ecb7bd480e80711de778f4efba5ad092ae
- IntRuoyiFronted/src/views/system/menu/MenuForm.vue | SHA-256 59c7c0b5372366127b017c330e289950576d992038a99dd94ab278147a61a9c4
- IntRuoyiFronted/tests/e2e/edhr-deviation-eight-regression.spec.cjs | SHA-256 befa7f7354125488f9b69e0102e9b4025b347677f56dcf8df562bd90f43e137f
- IntRuoyiFronted/tests/e2e/edhr-deviation-transfer-eligibility.spec.cjs | SHA-256 ac9cb65bde82c9a161be9d4e651d570882bf0803ab9520adb269dc50006cea8d
- docs/backend-development.md | SHA-256 651597aa5edd9d3ef775bac75de861d463f830c2234f207d9b834e43aaccc1da
- docs/database-rules.md | SHA-256 d434de327ab6817906f14f602c4b7ea43d5abd89cf9f02b3a2aa542055d754e7
- docs/e2e-rules.md | SHA-256 1f91c95e9e3b810ca2647715626baf79c576df38ec87ff7a4c450105d923909f
- docs/login-access.md | SHA-256 a5961bdc662ead436760587467fefc887349e8d0e77f4330cee208e22961544d
- docs/release-backup-restore.md | SHA-256 8d54128293facf28dcbf42b105813edba1dc76ad7f44511e2835f7f2795873f8
- docs/worktree-memory.md | SHA-256 f1216e977ae3eef0d2b2498fc0fa2befa2c6b2d07fbb2a6c817e75b5babaf119
- IntRuoyiBackend/script/tests/test_gxp_permission_migration_dependencies.py | SHA-256 3a8c5cac9b8d70fdf454e0974a9c5250fec7cd55dbcb7784bf28bc7ef58ae1ed
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesFixedTestOrderDownstreamCleanupMapper.java | SHA-256 175161cbc0d5f5447e299fbdea795b9a9add81d2ce7b55470164b3b82026684f
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderBpmCancellationService.java | SHA-256 52d8acba4eefcfa2fb1a0701b0ee712ce4e1b392a79ad54881df0fc0a9e230f2
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderDownstreamCleanupService.java | SHA-256 ac7e2c97b10bc56263028f4a13164cc3fcf456254f7d58e912b11f0f663ca135
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesFixedTestOrderWorkTaskCancellationTest.java | SHA-256 2f52135fae6ffb29d1ca34d5bbb5b632352447c96371c0f3b9c5622b38c04a90
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderBpmCancellationServiceTest.java | SHA-256 955fc2ea5bbd87701120cec1a97adca0553cb02c4951e7cb6f964122e7a90359
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderDownstreamCleanupServiceTest.java | SHA-256 92f7f07bfcb15917a927d6257804e6470d9c1a69c11b5c56a844fe65dc6608f4
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderDownstreamScopeSqlTest.java | SHA-256 904f2a11c08c8db02ad43044024179c9026e61be55837eceb29ef1dcde4e5f44
- IntRuoyiFronted/tests/e2e/active-order-completion-timeout.behavior.spec.cjs | SHA-256 20eaf92ed2e2c3fc71f9890c17b3be9137f3229481d575ad34bbe2728ed23784
- IntRuoyiFronted/tests/e2e/active-order-reset-operation-label.behavior.spec.cjs | SHA-256 f84b45458852a120519a83b635d28fedcce3988d5e99bb6c68a2a6a9b3a62edd
- IntRuoyiFronted/tests/e2e/active-order-test-reset-behavior.spec.cjs | SHA-256 f98072029c07806cc5615396e3cafd17937494340142867a49f02441d723e59b
- IntRuoyiFronted/tests/e2e/edhr-deviation-close-confirm.spec.cjs | SHA-256 fa35726e1ea2be904c752b10d555c693efaba0750d7ad781fe9f64a3ca42fe85
- IntRuoyiFronted/tests/e2e/ncr-material-upload-response.behavior.spec.cjs | SHA-256 0074530a38032bc34fd898e3b70635b03aaf4a27826671e61758fa8f736c64f6
- IntRuoyiFronted/tests/e2e/system-menu-absolute-child-path-static.spec.js | SHA-256 41c628384729ef7a47b87e8ad8fba9ae8cd6145e025686f88eda17bd3e2464df
- IntRuoyiFronted/tests/e2e/system-menu-path-validation.spec.cjs | SHA-256 e1ab0ab8b31e1936b435de29d80682bb47fc125ca9e492b232b8b099de452b55

- REGRESSION command: C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.CMD --batch-mode -f IntRuoyiBackend/pom.xml -pl yudao-module-mes,yudao-module-signature,yudao-framework/yudao-spring-boot-starter-biz-data-permission -am test -Dtest=DataPermissionContextHolderTest,MesProEdhrDeviationNcrIntegrationTest,MesProEdhrReleaseServiceImplTest,ProcessPoolTimelineFilterTest,MesM9SimulationHistoryProtectionTest,MesTeamLeaderActiveOrderDetailServiceImplTest,MesTeamLeaderActiveOrderSimulationServiceTest,MesTeamLeaderWorkbenchServiceImplTest,MesStage1ActiveOrderCompleteSimulationServiceImplTest,ElectronicSignatureServiceImplTest,MesFixedTestOrderWorkTaskCancellationTest,MesFixedTestOrderBpmCancellationServiceTest,MesFixedTestOrderDownstreamCleanupServiceTest,MesFixedTestOrderDownstreamScopeSqlTest -Dsurefire.failIfNoSpecifiedTests=false -Dstyle.color=never

- GREEN: git fetch origin int_main / rev-list HEAD...origin/int_main -> PASS, 0 ahead / 0 behind at initial freeze.
- GREEN: branch-runtime-port-guard.ps1 -> PASS, int_main/int_main 8081/48081.
- GREEN: frontend node --test (nine changed files) -> PASS, 56/56, 0 skipped.
- GREEN: python -m pytest script/tests/test_gxp_permission_migration_dependencies.py -q -> PASS, 1/1.
- GREEN: node yudao-module-mes/src/test/js/mes-active-order-test-reset-static.spec.cjs -> PASS.
- First diff check mistakenly disabled core.autocrlf, exposing CRLF as whitespace; repeated with repository normalization retained: git -c core.safecrlf=false diff --check -> PASS. No source files were edited.
- GREEN: exact frozen staging scope + git diff --cached --check + unresolved conflict check + added-credential check -> PASS, 54 files.

- project-experience-consolidation: searched existing PowerShell/Git memory; merged Windows Git newline/output lessons into docs/powershell-encoding.md. No new long-term document.
- GREEN: experience document UTF-8 / heading / git diff --check -> PASS. This task-owned documentation change will be committed separately from the 54-file baseline.

- Maven runner summary failed while decoding mixed UTF-8/non-UTF-8 native log bytes; this is an evidence-reader failure, not a product test failure.
- GREEN: Maven ASCII BUILD SUCCESS plus 14 fresh matching Surefire XML reports independently verified -> PASS, {'tests': 344, 'failures': 0, 'errors': 0, 'skipped': 0}; all selected classes executed, none skipped. Raw log is temporary and will not be committed.
  - cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolderTest: 5 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeviationNcrIntegrationTest: 10 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest: 59 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.processpool.ProcessPoolTimelineFilterTest: 4 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesM9SimulationHistoryProtectionTest: 172 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImplTest: 29 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderSimulationServiceTest: 5 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderWorkbenchServiceImplTest: 8 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.simulation.stage1.MesStage1ActiveOrderCompleteSimulationServiceImplTest: 15 tests PASS.

## Closeout

- GREEN: task-closeout-cleanup preview -> PASS，keep=3，delete=2，blocked=0，warnings=0；两删除路径绝对解析均在本任务目录内。
- GREEN: task-closeout-cleanup apply -> PASS，仅删除 baseline-manifest.json、maven-regression.log；54 文件清单/指纹和 Maven 测试结论已归并本记录及 verification-report.md。
- GREEN: git push origin int_main -> PASS，5264c8e48 -> 4fb95c4b2；git status --short --branch 显示无 ahead/behind 且无脏改动。
- 当前为主 checkout，未创建、合并或删除 worktree；未修改其他任务记录或运行服务。
- 本任务 3 份核心记录被全局 ignore，按任务记录保留规则选择性 git add -f；不暂存任何原始输出或临时产物。
- 收尾记录提交文件清单：doc/tasks/20261001-main-code-submit/task.md、execution-log.md、verification-report.md。提交标题 docs: complete main code push verification；其完整 commit hash 可由 git log -1 --format=%H -- doc/tasks/20261001-main-code-submit/task.md 精确取得，避免在提交内容中写入无法自引用的 commit hash。
- 最终 Git 门禁：该收尾提交推送后，实际执行 git status --short --branch、git rev-list --left-right --count HEAD...origin/int_main 和 git ls-remote origin refs/heads/int_main；必须工作区干净、0/0 且本地/远端 HEAD 相同才向用户报告完成。
  - cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImplTest: 14 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesFixedTestOrderWorkTaskCancellationTest: 5 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderBpmCancellationServiceTest: 4 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderDownstreamCleanupServiceTest: 7 tests PASS.
  - cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderDownstreamScopeSqlTest: 7 tests PASS.
