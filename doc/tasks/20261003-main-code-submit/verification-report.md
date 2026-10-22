# 验证报告

## 当前结论
completed：本轮定向验证、代码提交推送和任务清理 PASS；最终 3 个核心记录随收尾提交推送。

## 证据边界
本轮不执行真实 E2E、SQL 迁移、审计策略激活、发布或服务重启。本地回归通过不代表通知的线上配置、部署或七账号全业务验收完成。

## 验证结果
后端 722 tests、前端定向 111 tests、21-spec 静态套件及源码/文档/端口检查均通过，详见下文。

## 本轮前端验证
- IntRuoyiFronted/src/views/mes/pro/processpool/team-leader-pqc-correction-source-behavior.spec.cjs：PASS；exit=0；0.31秒
- IntRuoyiFronted/tests/e2e/active-order-dossier-context-race.spec.cjs：PASS；exit=0；0.61秒
- IntRuoyiFronted/tests/e2e/edhr-history-dossier-scope-behavior.spec.cjs：PASS；exit=0；0.77秒
- IntRuoyiFronted/tests/e2e/edhr-inline-dossier-parent-scope-behavior.spec.cjs：PASS；exit=0；0.55秒
- IntRuoyiFronted/tests/e2e/edhr-manager-market-release-entry-behavior.spec.cjs：PASS；exit=0；0.57秒
- IntRuoyiFronted/tests/e2e/edhr-manager-task-route-behavior.spec.cjs：PASS；exit=0；1.59秒
- IntRuoyiFronted/tests/e2e/pqc-release-dialog-isolation.spec.cjs：PASS；exit=0；0.49秒
- IntRuoyiFronted/tests/e2e/pqc-release-exact-task-route-behavior.spec.cjs：PASS；exit=0；0.73秒
- IntRuoyiFronted/tests/e2e/release-task-notification-entries-behavior.spec.cjs：PASS；exit=0；0.55秒
- IntRuoyiFronted/tests/e2e/release-task-notification-recovery-behavior.spec.cjs：PASS；exit=0；1.53秒
- IntRuoyiFronted/tests/e2e/edhr-ai-loop-static-suite.cjs：PASS；exit=0；3.02秒
- changed SFC/TS syntax：PASS；exit=0；1.1秒
- new component ESLint：PASS；exit=0；1.82秒

## 本轮后端验证
- Maven reactor test：PASS；{"tests": 722, "failures": 0, "errors": 0, "skipped": 0}；261.32秒；exit=0。
- 命令：mvn --batch-mode -f IntRuoyiBackend/pom.xml -pl yudao-module-mes,yudao-module-system -am test -Dtest=GxpAuditTransactionContextBoundaryTest,MesActiveOrderDossierFileServiceTest,MesActiveOrderDossierReadScopeServiceTest,MesActiveOrderReworkCycleTransactionTest,MesActiveOrderSignatureEvidenceServiceTest,MesCompletionAggregationAuditTransactionTest,MesCompletionAuditFragmentContractTest,MesCompletionAuditPayloadContractTest,MesPqcCorrectionSignatureEvidenceTest,MesPqcReleaseBatchExecutionServiceTest,MesPqcReleaseExactPageControllerTest,MesPqcReleasePageRoundOwnershipTest,MesPqcRevisionTaskIdentityTest,MesProBatchRecordExecutionSignatureServiceTest,MesProEdhrBatchExecutionControllerTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrManagerBatchVisibilityTest,MesProEdhrNcrReworkRoundTest,MesProcessPoolPqcInspectionCorrectionServiceTest,MesProcessPoolSubmitEventServiceAdapterTest,MesProductionReleaseManagerStageInitializerTest,MesProductionReleaseSignatureContractTest,MesProductionSignatureEvidenceServiceTest,MesReleaseTaskNotificationHandoffTest,MesReleaseTaskNotificationKernelTransactionTest,MesReleaseTaskNotificationMigrationContractTest,MesReleaseTaskNotificationReliabilityTest,MesTeamLeaderActiveOrderCompletionServiceTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterImplTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderReviewSignatureRoundTest,MesTeamLeaderSubmissionReviewServiceTest,ProcessPoolTimelinePqcGroupSqlTest -Dsurefire.failIfNoSpecifiedTests=false -Dstyle.color=never
- 上游依赖模块未指定测试允许不运行；所有指定类逐项验证本轮新生成 XML，不使用旧报告。
- {"class": "GxpAuditTransactionContextBoundaryTest", "tests": 6, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesActiveOrderDossierFileServiceTest", "tests": 15, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesActiveOrderDossierReadScopeServiceTest", "tests": 116, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesActiveOrderReworkCycleTransactionTest", "tests": 2, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesActiveOrderSignatureEvidenceServiceTest", "tests": 17, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesCompletionAggregationAuditTransactionTest", "tests": 8, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesCompletionAuditFragmentContractTest", "tests": 4, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesCompletionAuditPayloadContractTest", "tests": 3, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesPqcCorrectionSignatureEvidenceTest", "tests": 69, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesPqcReleaseBatchExecutionServiceTest", "tests": 21, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesPqcReleaseExactPageControllerTest", "tests": 1, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesPqcReleasePageRoundOwnershipTest", "tests": 9, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesPqcRevisionTaskIdentityTest", "tests": 9, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProBatchRecordExecutionSignatureServiceTest", "tests": 20, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProEdhrBatchExecutionControllerTest", "tests": 10, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProEdhrBatchExecutionServiceTest", "tests": 200, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProEdhrManagerBatchVisibilityTest", "tests": 10, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProEdhrNcrReworkRoundTest", "tests": 1, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProcessPoolPqcInspectionCorrectionServiceTest", "tests": 35, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProcessPoolSubmitEventServiceAdapterTest", "tests": 3, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProductionReleaseManagerStageInitializerTest", "tests": 7, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProductionReleaseSignatureContractTest", "tests": 4, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesProductionSignatureEvidenceServiceTest", "tests": 28, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesReleaseTaskNotificationHandoffTest", "tests": 4, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesReleaseTaskNotificationKernelTransactionTest", "tests": 17, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesReleaseTaskNotificationMigrationContractTest", "tests": 2, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesReleaseTaskNotificationReliabilityTest", "tests": 20, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesTeamLeaderActiveOrderCompletionServiceTest", "tests": 14, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesTeamLeaderActiveOrderReleaseBatchRecordWriterImplTest", "tests": 1, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest", "tests": 10, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesTeamLeaderActiveOrderReleaseLossReportWriterTest", "tests": 14, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesTeamLeaderActiveOrderReleaseLossSourceReaderTest", "tests": 10, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesTeamLeaderReviewSignatureRoundTest", "tests": 3, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "MesTeamLeaderSubmissionReviewServiceTest", "tests": 23, "failures": 0, "errors": 0, "skipped": 0}
- {"class": "ProcessPoolTimelinePqcGroupSqlTest", "tests": 6, "failures": 0, "errors": 0, "skipped": 0}

## 前端命令与计数
- node src/views/mes/pro/processpool/team-leader-pqc-correction-source-behavior.spec.cjs -> PASS，3 tests；exit=0。
- node tests/e2e/active-order-dossier-context-race.spec.cjs -> PASS，10 tests；exit=0。
- node tests/e2e/edhr-history-dossier-scope-behavior.spec.cjs -> PASS，10 tests；exit=0。
- node tests/e2e/edhr-inline-dossier-parent-scope-behavior.spec.cjs -> PASS，6 tests；exit=0。
- node tests/e2e/edhr-manager-market-release-entry-behavior.spec.cjs -> PASS，7 tests；exit=0。
- node tests/e2e/edhr-manager-task-route-behavior.spec.cjs -> PASS，22 tests；exit=0。
- node tests/e2e/pqc-release-dialog-isolation.spec.cjs -> PASS，9 tests；exit=0。
- node tests/e2e/pqc-release-exact-task-route-behavior.spec.cjs -> PASS，10 tests；exit=0。
- node tests/e2e/release-task-notification-entries-behavior.spec.cjs -> PASS，9 tests；exit=0。
- node tests/e2e/release-task-notification-recovery-behavior.spec.cjs -> PASS，22 tests；exit=0。
- 10 个定向脚本累计 111 项 node:test 用例（不与 21-spec 套件重叠累加）。
- node tests/e2e/edhr-ai-loop-static-suite.cjs -> PASS，21 specs；exit=0。
- vue/compiler-sfc parse/compileScript/compileTemplate、TypeScript transpileModule -> PASS，11 个改动源码文件。
- node node_modules/eslint/bin/eslint.js src/views/mes/pro/production-release/components/ReleaseTaskNotificationEntry.vue -> PASS，exit=0，无诊断。
- 7 个 Markdown 文档 UTF-8/标题与 3 个新增经验索引目标检查 PASS。

## 提交与收尾检查
- 代码基线 18e814b4f3a239f6338b613ac3f9ed22679b91de：98 文件，9534 insertions/138 deletions。
- 98 文件原始指纹、白名单/暂存区、UTF-8、冲突/凭据特征与 diff --check PASS；branch-runtime-port-guard 与 pre-commit hook PASS。
- cleanup preview/apply PASS；保留 3 个核心文档，删除本任务 18 个临时产物，blocked/warnings 均 0。
- git push origin int_main -> PASS，pre-push 端口门禁 PASS；de95bb743..18e814b4f 已推送。
- HEAD、origin/int_main 与 git ls-remote 的 refs/heads/int_main 均为 18e814b4f3a239f6338b613ac3f9ed22679b91de；ahead/behind=0/0。
- 推送后仅出现 2 个后续并行文档改动（docs/database-rules.md、docs/experience-index.md），本轮保留其未提交状态；不将整个工作区声称为 clean。
- 收尾提交文件仅为本目录 task.md、execution-log.md、verification-report.md；按唯一父提交 18e814b4f3a239f6338b613ac3f9ed22679b91de 与提交消息 docs: complete October 3 main code push verification 定位，最终提交 hash 由 Git 及本轮最终答复确认。
