# Execution Log
- 2026-10-04：读取根AGENTS、backend/frontend-development、test-release-preflight、task-closeout-rules、worktree-restrictions、branch-runtime-ports及PowerShell经验。最近根规则和用户委派优先，禁止提交推送与合并；本阶段仅review_ready。
- 初始工作区干净、detached HEAD 50dd3177a942090d7bf409a56839a42aa5c3289a；建立 codex/edhr-sa01-sa05-fix 分支。
- 正式输入只读：E:/IntRuoyi/doc/tasks/20261003-edhr-thread-management/severe-static-flow-audit-20261004.md 和 five-issue-repair-handoff.md。

## M1 completed
- 8个关键文件规范化SHA256与报告一致；baseline-fingerprints.txt保留核验。运行槽位2，8083/48083；未启动服务。
## M2 implementation / targeted PASS
- 内部身份契约限定生产/PQC提交；签名B认证、统一记录及内容哈希保留B，登录A/租户/身份域写入签名内容。通用HTTP签名仍取登录人。
- 临时档案由MES验证自身凭据，生成统一ID；取消旧表写入/读取fallback；员工切换请求、冻结快照、前端缓存显式域。
- 17个统一签名单测、MES桥接/生产提交/证据读取回归通过。
## M3 implementation / targeted PASS
- 正式申请唯一关联、登录租户、岗位权限、批次可见范围及正式来源校验；申请ID不能替代授权。上传/删除在文件服务前执行生命周期检查。
- 批次及PQC资料页上传传回服务端解析的applicationId。119项读取/写入范围测试及15项文件服务测试通过。
## M4 implementation / targeted PASS
- 共享生命周期门禁接入独立作废预检/申请/直通/BPM生效、PQC批准、上市初始化及最终批准。写路径获取现有GXP锁；回调复验NCR/状态/内容/归档。
- QA NCR处置链路不接独立作废门禁；审批任务适配器调用release.approve同样受约束。新增拒绝入口服务测试。
## Verification attempts
- 首次编译：FAIL（新增Objects import缺失）；已修正。
- 统一签名首轮：FAIL（测试适配器固定101，选中202）；已修正，后续17/17 PASS。
- MES testCompile：FAIL（TenantBaseDO链式返回类型、跨行class字段插入、workTask不含tenant getter、Mockito静态导入）；逐项修正。
- 定向回归首轮：FAIL（新依赖夹具未注入、H2夹具缺NCR表、冗余mock和异常文案）；补明确测试边界并保留独立真实门禁回归。
- 历史Maven跨模块编译+定向回归退出0：signature17 + MES365 = 382，0失败/0错误/0跳过；targeted-regression.log。当时QA正式NCR处置补验、前端类型检查及最终复核未完成，现已由下方最终交付结果覆盖。
- 本轮未运行真实E2E，未连接共享业务库、未提交/推送/融合。

## M5 completed / review_ready
- SA02最后补充：正式PQC取最新申请，批次关联仍必须唯一；领导/PQC上传正向和跨租户拒绝。文件上传/删除同时拒绝待审NCR和冻结/关闭活跃订单。
- SA03最后补充：员工档案切换夹具使用正确身份域；员工选择器key/激活状态包含域；提交幂等返回先核对持久化身份域，跨域重放拒绝且无写入。
- 最终组合命令：`mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=ElectronicSignatureServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineSubmitAuthorizationTest,MesFrontlineAuditIdentityTest,MesProFrontlineFeedbackSubmitServiceTest,MesActiveOrderSignatureEvidenceServiceTest,MesProductionSignatureEvidenceServiceTest,MesEdhrBatchLifecycleGuardTest,MesActiveOrderDossierReadScopeServiceTest,MesActiveOrderDossierFileServiceTest,MesProEdhrBatchVoidEffectServiceImplTest,MesProductionReleaseManagerStageInitializerTest,MesPqcReleaseBatchExecutionServiceTest,MesProEdhrReleaseServiceImplTest,MesProEdhrNcrReleaseGateTest,MesProEdhrNcrBatchDisposeStateTest,MesProEdhrApprovalTaskAdapterTest,MesProEdhrApprovalTaskAdapterSignaturePropagationTest,MesProEdhrNonconformanceReviewApplicationScopeTest,MesProEdhrNcrReworkRoundTest,MesProcessPoolEventServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' -B -ntp` -> PASS，退出0，17+451=468，无失败/错误/跳过，final-regression.log。
- 追加真实H2持久化身份域重放测试：相同命令仅`-Dtest=MesProcessPoolEventServiceTest` -> PASS，退出0，6项；replay-persistence-regression.log。该类原5项已在组合计数内，追加新测试1项，累计469个不同测试。
- `pnpm ts:check` -> PASS，退出0，frontend-typecheck-final.log。两个Node契约检查退出0。一次静态脚本误用scripts目录导致MODULE_NOT_FOUND，已按真实tests/e2e路径重跑通过，不是代码失败。
- 历史扩展QA诊断：ncr-regression.log退出1，80项3错误。当时managerDisposition测试/正式NCR服务/WorkTaskService/AuxiliaryAudit相对基线为空，未实测基线；这些错误已按V1授权精确修复正式writer夹具，最终JDK17组合QA6项全部通过，详见下方最终交付，不再作为当前未解决限制。
- `git diff --check`退出0；task-state JSON与task.md一致review_ready，正式回归/生产文件均保留，未提交。
- 已执行project-experience-consolidation检索：本次依赖安装、基线只读、宽回归诊断分开记录均由既有worktree-memory规则覆盖，不新增重复经验文档。
- 本阶段为review交付不是集成收尾，用户明确禁止提交/融合/删worktree；不运行cleanup apply，不宣称completed；全部任务资产保留供管理者审查。

## Review round 1 remediation
- 收到changes_requested，已只读读取完整评审。仍在原worktree顺序整改，无子Agent，禁止自行融合/推送/部署；本地单测授权有效。
- 版本核对：mvn -v 实测此前运行Temurin JDK21.0.10；pom.xml java.version/source/target=17是编译目标，不代表执行运行时。旧报告Java17运行时表述已更正。
- 本机现成JDK17：C:/Users/BJB110/.jdks/jdk-17.0.20+8，Microsoft 17.0.20。进程内设置JAVA_HOME/Path后mvn -v确认，不安装工具、不更改持久环境。
- round1-java17-regression.log：JDK17定向命令退出1，MES全部测试源码编译阻塞：既有MesProBatchRecordExecutionFieldResponsibilityMapperTest:246使用List.getFirst()；git show HEAD确认基线已有。未执行JUnit，不计作测试FAIL或PASS。后续已收到等价修正授权并完成get(0)，此阻塞已解除。
- round1-regression.log：此前JDK21初轮175项0失败4错误（QA写后状态2、H2逐件表缺失2）；已同步正式writer写后读取夹具及测试专用H2表，待重跑，不将失败记为通过。

- 已收到管理者明确授权：Java17编译前置修正加入allow-list，仅MesProBatchRecordExecutionFieldResponsibilityMapperTest:246的getFirst()→get(0)，已有size=1断言保证等价；不改生产代码或业务断言。最终依据必须JDK17，继续全部相关定向回归。

- 管理者明确授权R2必要相邻修正：操作者独立入账与物理字段完整性是两条合同，MesProcessPoolEventServiceImpl加入allow-list；不造设备/工位，不允许客户端选择操作者。
- round1-java17-targeted.log：183项2失败4错误，Java17全测试源码已编译；相邻物理设备约束、正式申请事实缺项及旧read-count断言已定位；必要相邻代码与测试夹具已修正。
- round1-java17-contract.log：19项2失败0错误；新PQC写读2、真实批准→资料上传2、PQC事件9均PASS；QA仅新增closed枚举大小写断言错误，已改为正式常量；业务服务未迎合夹具改动。

- round1-java17-final.log：signature17 PASS，MES514项0失败2错误。QA断言通过后严格Mockito暴露未使用managerTask按IDstub；发现正式审计先按事务范围selectOne再按ID读取，已补正式查询夹具并断言真实afterState含CANCELED任务/REJECTED事务；不使用lenient，不改生产服务。
- project-experience-consolidation：复用docs/worktree-memory.md既有Maven环境段落，补编译目标与运行JDK分开取证、-Dtest仍编译全部模块测试源码的通用经验；相邻writer/reader夹具规则已在docs/backend-development.md，未创建重复长期文档。

- round1-java17-combined.log：signature17项PASS，MES538项3失败6错误；QA正式作废/返工6项全部PASS。其余为新资料合同预期错误码与正式资料NCR先行门禁不一致、通知测试向无该字段的Persistence错误注入lifecycleGuard、真实H2父级审计夹具缺notificationService。保留现行生产判断，分别纠正错误码断言、移除错误注入、补显式通知端口MockitoBean；没有放松断言或使用lenient。
- round1-java17-fixture-final.log：上述三类定向测试退出0，10项，失败0/错误0/跳过0；正式上传NCR错误码为1040760409，批次终态门禁为1040760456。
- 管理者补充授权边界：独立审核通过后由管理者执行必要本地提交和融合，执行者当前仅交review_ready并停止业务编辑；不自行融合/推送/重启/共享DB写入或真实E2E。日志留在本任务供只读核验，不提交凭据或原始大输出。

## Round 1 final handoff / review_ready

- 最终同一精确组合：Microsoft JDK17.0.20、Maven3.9.16；round1-java17-runtime-final.log及40个实际运行类的Surefire XML运行属性已核对。最终命令见下方，JAVA_HOME/Path/MAVEN_OPTS只作用于任务进程，完整准备步骤见verification-report.md。
- 2026-10-04 16:14:09 +08:00：round1-java17-review-ready.log BUILD SUCCESS，退出0（独立exit-code.txt），4分11秒；统一签名17+MES538=555项，失败0/错误0/跳过0。round1-final-test-summary.json只提取本轮日志命中的40个类和对应XML，不把target目录旧报告或历史重跑累计为当前PASS。
- R1：scope128、真实approve→H2 DONE→实际资料writer2；批准后有/无批次上传权限均成功，终态40/50/60、待审NCR拒绝。
- R2：真实Controller新签名/事件/证据写读2（本人/选他人）；event.operator=A，signature.actor=B，物理字段保持空，VALID；篡改operator拒绝。PQC事件9、上下文23、并发5、Canonical1在同一组合通过。
- R3：文件服务16含实际delete true与生命周期一次，scope含仅上传权限不授予删除。
- V1：MesProEdhrNcrManagerDispositionTest 6项全通过，包含QA disposeVoid/disposeRework、上市任务取消、权益撤销和写后审计CANCELED/REJECTED；生产NCR处置服务/WorkTask/AuxiliaryAudit未修改。旧“QA不修夹具”的限制已解除，不宣称实测隔离基线失败。
- JDK17编译前置6项通过，getFirst()仅改get(0)，size=1断言保留。邻接通知4+父级H2审计4通过，没有改变生产判断迎合测试。
- task-owned-files.txt及review-ready-source-fingerprints.json冻结72个文件，64修改/8新增；精确路径、授权分类和摘要已交付。选择列表内基线0字节MesProEdhrApprovalTaskAdapterTest无JUnit，不计PASS；两个不同包MesProEdhrReleaseServiceImplTest为2+59项，计数可分别复核。
- task.md/verification-report.md/task-state.json状态同步review_ready；R1/R2/R3/V1完成，M6.awaiting_review为管理者独立复核与本地集成。业务文件停止编辑；本执行者未提交/融合/推送/重启/共享DB写入/真实E2E，不清理审查证据。
- 最终静态交付核验PASS：git diff --check、新增8文件UTF-8/无尾空格、72路径精确manifest与全部源码SHA256匹配；任务必需章节/JSON状态/测试摘要一致。40个Surefire运行时和报告/日志的最终命令匹配。一次文档校验脚本未将单元素regex结果包为数组，误报命令不一致；仅修正校验取值后通过，未改命令/源码/业务判断。精简结果handoff-validation.json；生产NCR处置/WorkTask/AuxiliaryAudit diff为空。


~~~powershell
& 'C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd' -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=ElectronicSignatureServiceImplTest,MesProBatchRecordExecutionSignatureServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineSubmitAuthorizationTest,MesFrontlineAuditIdentityTest,MesProFrontlineFeedbackSubmitServiceTest,MesActiveOrderSignatureEvidenceServiceTest,MesProductionSignatureEvidenceServiceTest,MesEdhrBatchLifecycleGuardTest,MesActiveOrderDossierReadScopeServiceTest,MesActiveOrderDossierFileServiceTest,MesProEdhrBatchVoidEffectServiceImplTest,MesProductionReleaseManagerStageInitializerTest,MesPqcReleaseBatchExecutionServiceTest,MesProEdhrReleaseServiceImplTest,MesProEdhrNcrReleaseGateTest,MesProEdhrNcrBatchDisposeStateTest,MesProEdhrApprovalTaskAdapterTest,MesProEdhrApprovalTaskAdapterSignaturePropagationTest,MesProEdhrNonconformanceReviewApplicationScopeTest,MesProEdhrNcrReworkRoundTest,MesProcessPoolEventServiceTest,MesProcessPoolPqcEventTest,MesProEdhrNcrManagerDispositionTest,MesFrontlinePqcContextServiceTest,MesFrontlinePqcSignatureContractTest,MesPqcApprovedDossierContractTest,MesProBatchRecordExecutionFieldResponsibilityMapperTest,MesFrontlinePqcSubmissionConcurrencyTest,CanonicalPqcSubmissionV2Test,MesFrontlineRuntimeConfigControllerTest,MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackRawLimitBypassTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesProFrontlineFeedbackSubmitRollbackTest,MesFrontlineSessionSnapshotServiceTest,MesFrontlineSubmitIdentityTraceTest,MesReleaseParentAffectedStateTest,MesReleaseTaskNotificationHandoffTest' '-Dsurefire.failIfNoSpecifiedTests=false' -B -ntp
~~~
