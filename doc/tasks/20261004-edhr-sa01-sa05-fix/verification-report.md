# Verification Report

## Status

review_ready：SA01—SA05及第一轮R1/R2/R3/V1整改完成。最终精确JDK17组合555项PASS，失败0/错误0/跳过0。业务文件已停止编辑，管理者独立复核后负责本地集成；没有标记completed。

## Final command and runtime

本次正式输入基线50dd3177a942090d7bf409a56839a42aa5c3289a；分支codex/edhr-sa01-sa05-fix。命令在当前worktree根目录运行，JAVA_HOME/Path仅作用于任务进程，没有安装或改变持久工具环境。

~~~powershell
$env:JAVA_HOME='C:\Users\BJB110\.jdks\jdk-17.0.20+8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_OPTS='-Xmx3g -XX:MaxMetaspaceSize=768m -XX:ReservedCodeCacheSize=256m -XX:CICompilerCount=2 -Xss1m -Dfile.encoding=UTF-8'
& 'C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd' -v
& 'C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd' -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=ElectronicSignatureServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineSubmitAuthorizationTest,MesFrontlineAuditIdentityTest,MesProFrontlineFeedbackSubmitServiceTest,MesActiveOrderSignatureEvidenceServiceTest,MesProductionSignatureEvidenceServiceTest,MesEdhrBatchLifecycleGuardTest,MesActiveOrderDossierReadScopeServiceTest,MesActiveOrderDossierFileServiceTest,MesProEdhrBatchVoidEffectServiceImplTest,MesProductionReleaseManagerStageInitializerTest,MesPqcReleaseBatchExecutionServiceTest,MesProEdhrReleaseServiceImplTest,MesProEdhrNcrReleaseGateTest,MesProEdhrNcrBatchDisposeStateTest,MesProEdhrApprovalTaskAdapterTest,MesProEdhrApprovalTaskAdapterSignaturePropagationTest,MesProEdhrNonconformanceReviewApplicationScopeTest,MesProEdhrNcrReworkRoundTest,MesProcessPoolEventServiceTest,MesProcessPoolPqcEventTest,MesProEdhrNcrManagerDispositionTest,MesFrontlinePqcContextServiceTest,MesFrontlinePqcSignatureContractTest,MesPqcApprovedDossierContractTest,MesProBatchRecordExecutionFieldResponsibilityMapperTest,MesFrontlinePqcSubmissionConcurrencyTest,CanonicalPqcSubmissionV2Test,MesFrontlineRuntimeConfigControllerTest,MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackRawLimitBypassTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesProFrontlineFeedbackSubmitRollbackTest,MesFrontlineSessionSnapshotServiceTest,MesFrontlineSubmitIdentityTraceTest,MesReleaseParentAffectedStateTest,MesReleaseTaskNotificationHandoffTest' '-Dsurefire.failIfNoSpecifiedTests=false' -B -ntp
~~~

- Maven3.9.16；Microsoft JDK17.0.20，路径C:/Users/BJB110/.jdks/jdk-17.0.20+8，UTF-8。round1-java17-runtime-final.log记录mvn -v；40个本轮实际运行类的Surefire XML逐一核对java.version/java.vendor/java.home，全部一致为该JDK17，精简证据在round1-final-test-summary.json。
- round1-java17-review-ready.log：BUILD SUCCESS，结束2026-10-04 16:14:09 +08:00，4分11秒。退出码文件round1-java17-review-ready.exit-code.txt为0；统一签名17+MES538=555项，失败0/错误0/跳过0。
- 该命令与round1-java17-combined.log使用同一精确选择列表。-am编译依赖源码；surefire.failIfNoSpecifiedTests=false用于没有匹配测试的上游模块，不跳过匹配JUnit。选择列表内MesProEdhrApprovalTaskAdapterTest.java是基线已有0字节文件，没有JUnit测试；不计为通过。MesProEdhrReleaseServiceImplTest命中两个包的2项与59项；40个实际类逐类计数之和555，无重复累计历史重跑。
- 只执行本地JUnit/H2和跨模块compile/testCompile，没有访问共享业务数据库或运行真实E2E。

## Round 1 findings and evidence

| 项目 | 最终修正 | 本轮实际测试及证据 |
| --- | --- | --- |
| R1 | 冻结PQC职责在PQC_RELEASE_PENDING允许TODO/DOING/OVERDUE；DONE且APPROVE时允许REPORT_UPLOAD_PENDING/MANAGER_RELEASE_PENDING资料操作。先决定职责入口，独立批次上传权限不被已结束PQC职责遮挡；租户、冻结候选、唯一正式申请、批次来源仍校验 | MesActiveOrderDossierReadScopeServiceTest 128项；其中批准DONE并有/无批次权限、拒绝/终态/决策不一致负例。新增MesPqcApprovedDossierContractTest 2项调用真实approve/H2 DONE writer，再走真实授权/资料关系writer，验证合法上传、批次40/50/60及待审NCR拒绝且不再写存储 |
| R2 | MesFrontlinePqcContextServiceImpl从服务端loginUserId写operator事件事实；事件服务允许独立正值操作者，出现物理deviceId/workstationId时仍要求完整有效，未造设备/工位、未新增客户端操作者参数 | 新增MesFrontlinePqcSignatureContractTest 2项：真实Controller请求无operator/device/workstation→真实上下文/统一签名/H2事件→真实签名证据读取；A=B与A≠B均事件存A/签名存B且VALID，篡改operator拒绝。MesProcessPoolPqcEventTest 9项、MesFrontlinePqcContextServiceTest 23项、并发5项及Canonical1项同一组合通过 |
| R3 | delete传deleting=true，生命周期只调用一次；仅上传权限不能删除。现行Controller/前端删除职责仍为leader-maintain/PQC-approve | MesActiveOrderDossierFileServiceTest 16项，actualDeleteUsesDeleteAuthorizationAndChecksLifecycleOnce捕获实际参数true和一次调用；128项scope测试含batchUploadPermissionAloneDoesNotAuthorizeDelete |
| V1 | 仅同步MesProEdhrNcrManagerDispositionTest的当前正式来源、签名规范、返工目标、写后审计状态及辅助审计依赖；未修改生产NonconformanceReviewService、WorkTaskService或AuxiliaryAudit | MesProEdhrNcrManagerDispositionTest 6项全部通过，包括disposeVoid/disposeRework关闭上市事务/任务、取消权益和异常拒绝；断言真实afterState为任务CANCELED/事务REJECTED。正式申请范围73、NCR放行13、批次处置2、返工1同时通过 |
| JDK17前置 | 用户明确授权MesProBatchRecordExecutionFieldResponsibilityMapperTest:246仅getFirst()→get(0)，保留size=1前置断言 | 该类6项通过，全模块测试源码在JDK17编译；没有切换JDK21作为最终依据 |
| 相邻回归夹具 | H2父级审计夹具补明确notificationService双；通知测试仅给实际initializer注入lifecycleGuard，移除错误Persistence字段注入；资料NCR负例使用正式先行错误码1040760409，终态1040760456 | MesReleaseParentAffectedStateTest 4项、MesReleaseTaskNotificationHandoffTest 4项；与资料合同合计10项先定向通过，后全部在最终组合再次通过 |

外部权限、目录、配置和存储边界仍用单测替身；真实业务writer/H2持久化/证据reader范围已逐项注明，这些证据不替代真实环境验收。

## Original SA01—SA05 mapping

| 项目 | 最终行为 | 最终JDK17对应验证 |
| --- | --- | --- |
| SA01 | 内部生产/PQC认证选中系统签名人B，独立保留登录操作者A、租户与身份域，通用HTTP签名保持既有登录人契约 | 统一签名17、桥接20、生产提交21、新PQC写读2；错密/禁用/未授权拒绝 |
| SA03 | 临时档案凭自身凭据生成统一签名，不写旧投影；身份域显式传递、证据核验操作者/租户，持久化重放拒绝跨域 | 切换4、授权14、审计11、生产证据28、活跃订单证据17、事件6；前端历史类型/契约PASS |
| SA02 | 正式最新申请、唯一批次关联、租户/职责/可见范围和来源校验；生命周期在文件写入前执行，批准后资料阶段可继续操作 | scope128、文件16、真实批准资料合同2；领导/PQC正向及跨租户、伪造、终态/NCR拒绝 |
| SA04 | 待审NCR阻止独立作废入口/回调；QA正式NCR处置独立保持，回调复验状态/内容/归档 | 门禁10、作废4、QA处置6、申请范围73、NCR处置2、返工1 |
| SA05 | 待审作废阻止PQC/上市初始化/批准及最终生效，现有写锁内复验；撤回/驳回恢复 | PQC22、上市初始化8、上市批准59+2、NCR放行13、审批适配器签名传播1 |

各行复用同一组合测试，不把映射各行相加作为测试总数。

## Historical evidence and resolved attempts

- 初次final-regression.log的468项和replay-persistence-regression.log新增1项使用Temurin JDK21.0.10、编译目标17；此前把运行时标为Java17不准确，已纠正，仅作历史记录。
- 历史ncr-regression.log 80项3错误没有另跑隔离基线，不能宣称基线实测失败。现已按V1授权精确修复正式writer夹具；最终JDK17 QA6项全部通过，旧QA未修夹具结论作废。
- round1-java17-final.log 514项中2个QA严格Mockito错误，经补正式审计实际selectOne入口与写后断言解决；不用lenient。round1-java17-combined.log 538项3失败6错误均已按上表修复，最终555项全部通过；不把旧失败结果计为PASS。
- 前端pnpm ts:check退出0（frontend-typecheck-final.log）；两个Node签名域/模板静态契约退出0。round1未改前端，无需重复运行；静态契约不称为真实E2E。锁文件无变更。

## Exact handoff files and integration boundary

- task-owned-files.txt：最终72个源码/测试/经验改动，64修改、8新增；包含新Java/SQL/Node文件。round1-allow-list.md逐项列第一轮追加和授权依据。review-ready-source-fingerprints.json保存这些文件最终原始字节SHA256；管理者复核时可重算，任何变化应重新核对。
- task.md、execution-log.md、verification-report.md、task-state.json以及allow-list/清单/精简测试摘要均在当前任务目录；该目录被仓库忽略，不能误报已提交。日志/临时编辑脚本/target为本地审查材料，不纳入源码清单，原始大输出与凭据不得提交。
- git diff --check通过；新文件另做UTF-8/尾空格及完整路径清单检查，记录见最终execution-log.md。
- 用户已授权管理者审核通过后的必要本地提交及融合，管理者须只提交本任务文件、保护主干与MesProductionSignatureEvidenceService及其测试重叠的P1成果。当前执行者没有提交/推送/合并/部署/重启/共享数据库写入/真实E2E，也未消息通知其他线程。
- M6独立复核与集成由管理者负责；review_ready为交付状态，不是completed或ready_for_closeout。当前worktree/槽位与证据保留供审查，不执行cleanup apply。

## Experience and retention

已调用project-experience-consolidation，更新docs/worktree-memory.md既有Maven环境段落：编译目标与实际运行JDK分开取证，-Dtest仍编译全部测试源码。writer/reader夹具同步经验由既有backend-development.md覆盖，未新增长期经验文件。
