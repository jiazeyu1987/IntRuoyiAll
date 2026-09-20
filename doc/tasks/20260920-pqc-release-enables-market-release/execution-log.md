# 执行日志

BDD: PQC放行后可上市放行 -> Given P2已生成详情里的正式单据且P3已推送到PQC生产放行 When PQC完成生产放行 Then 同一批次执行必须存在正式上市放行事务，批次执行列表点击上市放行不应提示缺少正式放行事务。

BDD: 上市放行事务前置严格失败 -> Given PQC放行后管理者代表或业务就绪检查缺失 When 系统尝试初始化上市放行事务 Then 必须明确失败，不得默认成功或生成不完整事务。

## RED/GREEN

RED: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> FAIL, 当前代码缺少 PQC 放行后初始化上市放行事务的连接点，上市放行仍只认可旧四报告快照。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL, 活跃订单正式事实已将申请状态置为 MANAGER_RELEASE_PENDING，但管理阶段初始化仍只接受 REPORT_UPLOAD_PENDING。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS, 54 tests passed.

RED: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> FAIL, PQC放行持久化仍写入 REPORT_UPLOAD_PENDING，无法与管理阶段初始化的 MANAGER_RELEASE_PENDING 对接。

GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS, 3 tests passed；PQC放行持久化状态已与正式活跃订单事实链路一致。

GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS, 4 tests passed；上市事务移交同时支持旧报告状态和活跃订单正式事实状态。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS, 29 tests passed。

RED: node tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> BLOCKED, 真实页面登录阶段后端返回 500；日志明确为 dcc_controlled_file 缺少代码必需字段 read_only_file_id，尚未进入重置订单、P1、P2、P3、PQC 放行或上市放行。

BLOCKER: 仓库已有 IntRuoyiBackend\sql\mysql\20260919_dcc_controlled_file_dual_version.sql，但执行该数据库迁移属于数据库写入，等待用户当轮明确授权。

GREEN: 用户授权数据库写入后执行 IntRuoyiBackend\sql\mysql\20260919_dcc_controlled_file_dual_version.sql -> PASS，确认 dcc_controlled_file.read_only_file_id 与 editable_file_id 已存在。

RED: node tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL，P1/P2/P3/PQC 均成功，上市放行接口返回 500；后端明确阻断 flow 6，批次执行不是 BATCH_READY。

BDD: PQC放行推进上市前置 -> Given 活跃订单详情正式事实已完成P2且PQC完成生产放行 When 系统提交PQC放行结果 Then 同一正式批次必须推进为 BATCH_READY，上市放行仍经过原有正式校验。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，测试编译明确提示 MesProductionReleaseBatchExecutionPort 缺少 markReadyForMarketRelease。

GREEN: 同一 Maven 定向命令 -> PASS，19 tests passed；PQC审批成功后调用正式批次状态推进动作。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS，61 tests passed。

RED: node tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL，P1/P2/P3/PQC 均成功且批次 provisioning_status 已是 BATCH_READY，但旧批次状态被同步回“已创建”，上市放行仍按旧状态阻断 flow 6。

BDD: 活跃订单正式事实可上市放行 -> Given 活跃订单详情单据已由P2形成并经PQC生产放行 When 批次执行执行上市放行 Then 活跃订单路径以正式事实 provisioning_status=BATCH_READY 为上市前置，旧EDHR非活跃订单路径仍要求传统批次状态待关闭或已关闭。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，新增用例 activeOrderFormalFactsUseProvisioningReadinessWhenLegacyBatchStatusIsCreated 被 flow 6 batch execution is not BATCH_READY 阻断。

GREEN: 同一 Maven 定向命令 -> PASS，5 tests passed；活跃订单正式事实使用 provisioning_status 判定上市前置，旧链路仍保留传统状态要求。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS，66 tests passed。

GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS，4 tests passed。

GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS，生成包含活跃订单正式事实最终复核修复的 yudao-server-exec.jar。

RED: node IntRuoyiFronted\tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL，P1/P2/P3/PQC均成功且上市放行进入正式写入，但操作审计写入失败：after_summary_hash char(64) 被写入 ACTIVE_ORDER_FACTS: + 64位哈希，超过数据库列长度。

BDD: 活跃订单正式事实操作审计摘要符合数据库契约 -> Given 操作审计来源是带业务前缀的活跃订单正式事实快照 When 写入 after_summary_hash Then after_summary_hash 必须是64位摘要，完整来源关系保留在 metadata/sourceSnapshotHash 中。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceTest#activeOrderOperationAuditHashesLongFormalFactSnapshotForSummaryColumn' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，afterSummaryHash 仍为 ACTIVE_ORDER_FACTS: + 64位哈希，超过 char(64) 数据库契约。

GREEN: 同一 Maven 定向命令 -> PASS，1 test passed；操作审计 afterSummaryHash 在源快照超过64字符时写入其 SHA-256 摘要，完整源快照仍留在 metadata/sourceSnapshotHash。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceTest#activeOrderOperationAuditHashesLongFormalFactSnapshotForSummaryColumn,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS，97 tests passed。

GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS，4 tests passed。

GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS，生成包含 datetime(0) 规范化修复的 yudao-server-exec.jar。

RED: node IntRuoyiFronted\tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL，P1/P2/P3/PQC均成功，上市放行仍返回500系统异常；错误日志为 business readiness checks changed before final release；只读结果显示活跃订单正式事实链路在管理阶段初始化使用了活跃订单就绪口径，但最终上市放行复核仍误走旧EDHR表单完整性口径。

BDD: 活跃订单正式事实上市放行复核沿用活跃订单就绪口径 -> Given P2/P3/PQC放行后的管理事务来自活跃订单详情正式事实 When 上市放行最终复核业务就绪状态 Then 应复核活跃订单正式事实就绪状态，不得要求旧EDHR表单任务补齐。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest#activeOrderFormalFactsPrepareAfterDatabaseDatetimePrecisionRoundTrip' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，activeOrderFormalFactsPrepareAfterDatabaseDatetimePrecisionRoundTrip 被 business readiness checks changed before final release 阻断，证明最终复核仍调用旧 resolveBusinessReadinessChecks。

GREEN: 同一 Maven 定向命令 -> PASS，1 test passed；最终上市放行准备阶段对活跃订单正式事实快照改为复核 resolveActiveOrderFormalFactsReadiness，不再误走旧EDHR表单完整性口径。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS，96 tests passed。

GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS，4 tests passed。

RED: node tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL，P1/P2/P3/PQC 均成功，上市放行被批次来源映射阻断 TRACE_MAPPING_BLOCKED；真实批次包含多条正式领料来源，追溯图完整性校验仍错误要求同一 linkType 只能出现一次。

BDD: 活跃订单多条物料来源是正式业务事实 -> Given 活跃订单详情单据包含多条领料单和领料行 When 上市放行读取批次追溯图 Then 追溯图必须允许这些多行物料事实，同时仍阻断工单等非重复来源的重复映射。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，activeOrderTraceGraphAllowsMultipleMaterialIssueFactsFromFormalDetail 断言失败，当前追溯图校验拒绝多条 MATERIAL_ISSUE / MATERIAL_ISSUE_LINE。

GREEN: 同一 Maven 定向命令（MAVEN_OPTS 限制为低内存） -> PASS，20 tests passed；正式追溯图允许多条物料领料单/领料行，同时继续拒绝其他来源类型重复映射。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，新增正式 BOUND 批次凭证预检用例被当作无可用流程8来源，且已有诊断断言提示捕获失败未保留具体校验范围。

GREEN: 同一 Maven 定向命令 -> PASS，19 tests passed；流程8正式批次凭证同时接受系统真实持久化的 BOUND 和 CAPTURED 状态，捕获失败保留错误范围。

BDD: BOUND正式来源可执行上市放行 -> Given 活跃订单的批次来源关系已由P2/PQC正式流程持久化为 BOUND When 批次执行提交上市放行 Then 流程7必须接受该正式来源状态，并继续校验批次、来源快照和映射完整性。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，BOUND正式来源被流程7错误阻断为 Flow 7 Origin/TraceLink/Manifest mapping is not ready。

GREEN: 同一 Maven 定向命令 -> PASS，5 tests passed；流程7接受正式 BOUND 来源，同时继续校验批次关联、来源快照、追溯链接和映射完整性。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS，85 tests passed。

GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS，4 tests passed。

RED: node IntRuoyiFronted\tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL，P1/P2/P3/PQC均成功，上市放行返回500系统异常；后端明确为 one or more frozen release evidences changed before final release。

BDD: 活跃订单正式事实快照按数据库时间精度稳定 -> Given PQC放行时生成正式事实快照且决定时间落库为 datetime When 上市放行重新读取申请并校验冻结事实 Then 同一PQC放行事实不得因纳秒精度丢失被误判为证据变化，其他正式字段仍严格一致。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest#activeOrderFormalFactsPrepareAfterDatabaseDatetimePrecisionRoundTrip' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，上市放行准备阶段复现 REPORT_SNAPSHOT_CHANGED，错误提示 one or more frozen release evidences changed before final release。

GREEN: 同一 Maven 定向命令 -> PASS，1 test passed；活跃订单正式事实快照对 LocalDateTime 使用数据库 datetime 秒级精度规范化，其余字段继续严格参与哈希。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS，96 tests passed。

GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS，4 tests passed。

GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS，生成 yudao-server-exec.jar。

RED: node IntRuoyiFronted\tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL，P1/P2/P3/PQC均成功，上市放行仍返回500系统异常；错误日志仍为 one or more frozen release evidences changed before final release；只读比对确认数据库 datetime(0) 将 04:09:20.xxx 四舍五入为 04:09:21，冻结快照使用入库前秒。

BDD: 活跃订单正式事实时间按数据库 datetime(0) 规则稳定 -> Given PQC放行时间包含小数秒 When 正式事实落库并在上市放行前重新读取 Then 快照时间必须按数据库秒级四舍五入规范化，避免同一PQC放行事实被误判为证据变化。

RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest#activeOrderFormalFactsPrepareAfterDatabaseDatetimePrecisionRoundTrip' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL，上市放行准备阶段复现数据库 datetime(0) 四舍五入后 REPORT_SNAPSHOT_CHANGED，错误提示 one or more frozen release evidences changed before final release。

GREEN: 同一 Maven 定向命令 -> PASS，1 test passed；活跃订单正式事实快照对 LocalDateTime 使用数据库 datetime(0) 半秒进位到秒级的规范化规则，其余字段继续严格参与哈希。

GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS，96 tests passed。

GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS，4 tests passed。

GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS, 最新后端运行包已生成并加载到 48081。

BDD: 上市放行的活跃订单操作审计摘要符合数据库契约 -> Given 活跃订单正式事实标识为 ACTIVE_ORDER_FACTS:<hash>，When 上市放行记录活跃订单操作事实，Then afterSummaryHash 只能写 64 位摘要且 metadata 必须保留原始 sourceSnapshotHash。
RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseAuditRecorderTest#activeOrderFormalFactSnapshotUsesDigestForAuditSummaryAndKeepsSourceInMetadata' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL, afterSummaryHash 原样写入 83 位 ACTIVE_ORDER_FACTS 标识且 metadata 缺少 sourceSnapshotHash。
GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseAuditRecorderTest#activeOrderFormalFactSnapshotUsesDigestForAuditSummaryAndKeepsSourceInMetadata' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS
GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceTest#activeOrderOperationAuditHashesLongFormalFactSnapshotForSummaryColumn,cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseAuditRecorderTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS, 101 tests.
GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS, 4 tests.
GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS, 最新后端运行包已生成，包含活跃订单操作审计摘要修复。
RED: node IntRuoyiFronted\tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL, P1/P2/P3/PQC放行通过，上市放行失败：当前 eDHR 批次状态不允许该操作；批次状态为已创建。
BDD: PQC放行后的活跃订单正式事实批次允许上市放行 -> Given P2/P3/PQC放行已完成且批次来源为活跃订单正式事实，When 批次执行列表点击上市放行，Then 即使批次执行状态仍为已创建也应完成上市放行并关闭批次。
RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest#managedActiveOrderApprovalClosesCreatedBatchAndAppendsDecisionToItsOrigin' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL, closeBatchAfterFinalRelease 仅允许 READY_TO_CLOSE/CLOSED，CREATED 批次报“当前 eDHR 批次状态不允许该操作”。
GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceTest#activeOrderOperationAuditHashesLongFormalFactSnapshotForSummaryColumn,cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseAuditRecorderTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS, 101 tests.
GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS, 4 tests.
GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS, 最新后端运行包已生成，包含已创建批次允许按活跃订单正式事实上市放行的修复。
RED: node IntRuoyiFronted\tests\e2e\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL, P1/P2/P3/PQC放行通过，上市放行进入最终关闭后被旧EDHR工序任务规则阻断：eDHR 工序缺少任务分配规则。
BDD: 活跃订单正式事实上市放行不依赖旧eDHR归档任务规则 -> Given P2/P3/PQC放行后的批次来源为活跃订单详情正式事实 When 上市放行关闭批次 Then 批次应关闭并记录上市放行业务事实，不得创建需要旧工序分配规则的归档任务。
RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest#managedActiveOrderApprovalClosesCreatedBatchAndAppendsDecisionToItsOriginWithoutArchiveTaskRule' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL, 活跃订单正式事实上市放行关闭批次后仍调用 createArchiveTaskAfterBatchClose，导致真实页面被 eDHR 工序缺少任务分配规则阻断。
GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest#managedActiveOrderApprovalClosesCreatedBatchAndAppendsDecisionToItsOriginWithoutArchiveTaskRule' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS, 活跃订单正式事实上市放行关闭批次但不创建旧eDHR归档任务。
GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceTest#activeOrderOperationAuditHashesLongFormalFactSnapshotForSummaryColumn,cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseAuditRecorderTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS, 101 tests.
GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS, 4 tests.
GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS, 最新后端运行包已生成，包含活跃订单上市放行跳过旧归档任务规则的修复。
BDD: 活跃订单上市放行后进入历史追溯 -> Given 活跃订单详情单据已经成为正式业务事实并完成上市放行 When 批次关闭 Then 该批次应直接进入已归档状态并出现在历史追溯，不再依赖旧eDHR归档任务。
RED: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest#managedActiveOrderApprovalArchivesCreatedBatchAndAppendsDecisionToItsOriginWithoutArchiveTaskRule' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> FAIL, 活跃订单上市放行后批次状态仍为 CLOSED(30)，历史追溯仅显示 ARCHIVED(40)。
GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest#managedActiveOrderApprovalArchivesCreatedBatchAndAppendsDecisionToItsOriginWithoutArchiveTaskRule' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS, 活跃订单上市放行后批次直接进入 ARCHIVED(40)，且不创建旧eDHR归档任务。
GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPortImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityServiceContractTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionServiceTest#activeOrderOperationAuditHashesLongFormalFactSnapshotForSummaryColumn,cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseAuditRecorderTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseBatchExecutionServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceTest,cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessServiceTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-DskipITs' -> PASS, 101 tests.
GREEN: python -m pytest IntRuoyiBackend\script\tests\test_pqc_release_enables_market_release_static.py -> PASS, 4 tests.
GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS, 最新后端运行包已生成，包含活跃订单上市放行直接归档进入历史追溯的修复。
RED: node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL, 测试脚本在空活跃订单列表时错误要求目标订单行先存在，未进入重置业务动作；产品页面本身已显示重置按钮。
RED: 重置指定测试订单真实页面返回 HTTP 500/code=500 系统异常；需读取真实响应确定服务端失败前置。
BDD: 重置已上市放行的固定测试订单 -> Given 活跃订单已有 CLOSED/RELEASED 事实 When 点击重置指定测试订单 Then 该订单全部非删除活跃订单事实被清理并重新加入，不能因唯一键冲突返回系统异常。
RED: node IntRuoyiBackend\\yudao-module-mes\\src\\test\\js\\mes-active-order-test-reset-static.spec.cjs -> FAIL, 重置查询仅覆盖 ACTIVE/REMOVED，无法覆盖 CLOSED/RELEASED。
GREEN: node IntRuoyiBackend\\yudao-module-mes\\src\\test\\js\\mes-active-order-test-reset-static.spec.cjs -> PASS, 固定测试订单重置范围覆盖该工单全部非删除活跃订单状态。
RED: node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL, 重置后固定工单仍为非 CONFIRMED 状态，上市放行被业务校验拒绝。
GREEN: node IntRuoyiBackend\\yudao-module-mes\\src\\test\\js\\mes-active-order-test-reset-static.spec.cjs -> PASS, 重置流程要求恢复固定工单 CONFIRMED 状态。
GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS, 后端运行包包含固定工单状态恢复修复。
BDD: 历史追溯展示上市放行 -> Given 活跃订单已完成PQC放行和上市放行 When 进入批次执行历史追溯 Then 统一时间线必须显示上市放行事务、负责人和签核证据。
RED: node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL, 历史追溯统一时间线缺少上市放行历史信息。
GREEN: node yudao-module-mes\\src\\test\\js\\edhr-history-missing-batch-config-static.spec.cjs -> PASS, review-timeline 合同纳入上市放行事务事件。
GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS, 最新运行包已生成。

BDD: 上市放行后历史详情仍读取同一份活跃订单正式事实 -> Given P2生成详情单据、P3推送PQC生产放行、PQC放行和上市放行均已完成 When 从批次执行历史追溯点击详情 Then 页面必须继续显示与活跃订单详情一致的PQC放行签名、上市放行操作事实和物料/PQC明细，不得因活跃订单退出当前池报系统异常。
RED: node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL, P1/P2/P3/PQC放行和上市放行成功后，历史追溯详情加载失败：班组活跃订单不存在；日志显示批次详情源订单读取后，冻结工序物料仍调用只允许 ACTIVE 的 ActiveOrderSnapshotResolver.requireEffective。
RED: mvn -pl yudao-module-mes '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> FAIL, 旧测试契约仍要求批次详情走生产组长当前详情投影，不能覆盖上市放行后的归档正式事实。
GREEN: node IntRuoyiBackend\\yudao-module-mes\\src\\test\\js\\mes-edhr-batch-active-order-detail-source-static.spec.cjs -> PASS, 批次执行详情读取批次绑定的归档正式活跃订单来源。
GREEN: mvn -pl yudao-module-mes -am '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailServiceTest,cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderDetailServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -> PASS, 26 tests passed；归档详情使用正式源读取冻结工序物料，一线当前填报仍要求 ACTIVE。
GREEN: mvn -pl yudao-server -am '-DskipTests' package -> PASS, 最新后端运行包已生成并加载到 48081。
GREEN: node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> PASS, runId=STAGE1-P1-P2-BOUNDARY-20260920093224；从重置指定测试订单开始，P1生产/检验100%，P2回填批记录和76条过程检验记录，P3生成PQC放行申请，PQC放行签名在PQC放行详情、批次执行详情、历史详情一致，上市放行成功且历史详情不再系统异常。

BDD: 上市放行后历史追溯详情保留上传资料文件 -> Given P2已经把活跃订单详情单据生成为正式业务事实，且用户通过详情页真实上传来料检、灭菌和成品检资料文件 When 后续完成P3、PQC生产放行和上市放行并进入批次执行历史追溯详情 Then 历史追溯详情必须继续显示同一活跃订单的上传文件名、上传元信息和资料上传操作事实。
RED: node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> FAIL, 上一轮通过结果 runId=STAGE1-P1-P2-BOUNDARY-20260920093224 中 afterP2.dossierTabs 均为“暂无文件”，afterMarketRelease.historyAttachmentItems=0，未证明上传文件信息进入历史追溯。
GREEN: node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release -> PASS, runId=STAGE1-P1-P2-BOUNDARY-20260920094235；通过真实详情页上传来料检、灭菌、成品检三个资料文件，P3/PQC放行/上市放行后进入批次执行历史追溯详情，三条文件名、上传人、上传时间均可见，操作事实包含“活跃订单资料上传”“PQC生产放行”“批记录上市放行”。
