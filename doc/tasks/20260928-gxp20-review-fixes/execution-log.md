# Execution Log

07:45权威检查点：session36614于07:43:57自然终态exit0，权限187=Command39+Persistence148全PASS/0F/0E/0S，新增15首次PASS、原172保留，system源码前后零漂移。原始证据permission187-event-first/command.txt、combined.log、exit-code.txt、before/after XML。当前无Maven或后续自动组。Huygens独占新PQT Mapper测试；Rawls独占NCR离线增量/seed生成器；Ampere独占Correction六fixture离线生成器（Dalton未开始任何Correction生成器），M9合法fixture F6639B6已冻结待147复测；Maxwell已获新PermissionReceiptMysqlConcurrencyTest测试窗口，生产不改。Dalton仅正式DDL核验/装配和全部DB/Maven执行，所有相关Java冻结前不编译对应reactor。权限拟独立库gxp_permission_receipt_round3尚未创建，需正式闭包清单后装配，不复用JSON2/PQT。

PQT36与只读probe限定PASS保持；只读实库核验发现全DO仍缺8列：qa_process_id、qa_item_code、inspection_rule_key、submitted_content_hash、submitted_event_id、simulated、simulation_stage、simulation_run_id。完整Mapper尚NOT READY，不把缺列当五列迁移RED；待按正式来源和约束装配。M9当前145/147通过、2合法fixture待复验；完整G1–G5/026027、权限正式MySQL/消费者、NCR/Correction服务事务、PQT真实Mapper及整包门禁仍未关闭。

### 以下历史记录保留，不代表当前调度

## 07:29 当前权威检查点

2026-09-29 07:29检查点：权限111（39+72）全PASS/exit0，新增33首次即PASS，原78保留。session55062后组M9 green-r1因HashMap编译失败，147 NOT RUN。限定包名修正后session10988于07:26:06自然终态，M9 green-r2为147/2F/0E/0S、exit1：原四交错反例及两新合法对照PASS，仅旧aggregate合法[1]/[8]缺event→task字段被拒绝，交Ampere仅修真实装配，不弱化断言/guard。源码前后零漂移；原始command/log/XML/exit在permission111-t1t2-first及m9-simulation-147-green-r2。当前无后续Maven，Maxwell两测试窗口已释放，下次须所有相关Java重新冻结。

PQT36矩阵exit0（07:14:55）和修解析后的只读probe exit0（07:29:19）分别归档pqt-matrix-36、pqt-probe-green-r2。旧IndexError/exit1保留，新C3084B48使用实际两SQL receipt，仅证明五列物理合同；未重跑矩阵、不触JSON2。原RED表/35场景保留。完整Mapper/服务/依赖链、M9 G1–G5/026027/事务并发、NCR R03正式增量及两业务fixture仍开放，局部PASS不等于整包完成。

### 以下历史阶段记录不代表当前调度

## 05:06 M9/NCR实际GREEN检查点

05:06:27 session91660实际终态，调度/Maven两组均0，无所属integration Java。m9-simulation-109-green：109/0/0/0，05:05:28归档，原49与新增60（50拒绝+10合法同run候选消费）全通过；ncr-scope-audit-green-r2：47/0/0/0，05:06:27归档，Rawls仅unused stub修复SHA904E4D3C...已核。原RED及fixture失败目录保留，本次command/log/exit-code/before/after独立保留，不复用旧XML。M9服务BAFBD3A7FB53BA3960B474C8790173569B095BBCC7823F9549526A8A9C4A5FE9，测试F4A114FCE7EC650B80294FCCD4B096B37FA1DA6C0332899A690F522A73084DA3，Mapper4D20C07B05FD64AFA329436C652C66506C8D330A538AFAB3302725D51070C6CD；可交Laplace复审G2切片，未声称独立放行。G3–G5完整来源/外部引用/根对象、真实Mapper租户/服务事务并发及026027仍开放，合法正例为mock编排非DB证明。现构建冻结释放，下一写集由主协调；不启动后续Maven，Ampere COR-SIG-01测试窗口可开放。

GREEN: m9-simulation-109-green/command.txt -> PASS，109/0/0/0。
GREEN: ncr-scope-audit-green-r2/command.txt -> PASS，47/0/0/0。

## 04:56 五组检查点实际终态

04:56:34 session9838实际终态，调度0，五组Maven退出0/0/0/1/1，进程复核无integration Java。strict-loader-parse67-green：67PASS，主回报Laplace解析闭包独立限定PASS；pqc-correction-audit-green：23PASS；formal-reverse-trace-r4-green-r3：48PASS；m9-simulation-109-red：109/60FAIL/0ERROR，原49全部PASS，新增50负例（reset25命中workOrder.updateById，cleanup25命中aggregate.deleteByActiveOrderId）及10合法正例未调用候选查询，各为真实预期行为RED但不等于60个独立漏洞；ncr-scope-audit-green：47/0FAIL/1ERROR，唯一unifiedAppendFailureEscapesAfterBusinessAndSpecializedAuditWrites因prepareUnifiedDisposition:1442 UnnecessaryStubbing，业务断言与fixture终结错误区分，不宣称47GREEN。原始各组command/log/exit-code/before/after保留；Java构建冻结现可释放，后续Maven暂停待重新冻结。Dalton按G2真实RED修五表自身身份/来源并保留合法路径，G3–G5与026027仍开放，不以109GREEN关闭完整M9。

## 04:47 SL-PARSE-01七项真实RED

04:47:33 session48059终态，调度0/Maven1，无所属Java。strict-loader-parse67-red-r2：Loader14PASS、Strict53/7FAIL/0ERROR，合计67/7/0/0，原60通过；新增policy三例/schema四例全部预期ServiceException却未抛，配对合法输入已通过，为7实际行为RED。测试SHA87517AA3...核对匹配，仅修合法policy尾随TAB，生产未变。原5FAIL/2ERROR保留于strict-loader-parse67-red；本次command/log/exit-code/before/after独立归档。按主授权释放Maxwell仅SL-PARSE-01最小解析生产修复窗口，冻结后再GREEN，Maven当前暂停，MES未编译。

## 04:45 SL-PARSE-01实际取证

04:45:01 system session25520终态，调度0/Maven1，无所属Java，现释放system编辑窗口。strict-loader-parse67-red原始XML：Loader14PASS、Strict53/5FAIL/2ERROR，合计67/5/2/0；原60仍通过。新增schema四例及policy第二YAML文档一例均预期ServiceException却未抛，为5实际行为RED；policy另两例在配对正例因尾随TAB被YAML扫描拒绝，属fixture/配对前置ERROR，尚未到目标断言，不能计为这两负例行为RED。Loader CB384FEF...与schema DFA7E082...保持旧60GREEN指纹，Strict测试F5AD586B...核对匹配。独立目录.review-fix-loop/runs/gxp-integration/strict-loader-parse67-red保留command/log/exit-code/before/after；未编译MES。后续Maven暂停至system再次冻结。

## system60归档复核

两份本次原始XML已在strict-loader-sixty-green/after归档，分别14与46、均0FAIL/ERROR；四输入文件SHA与运行前冻结一致，四源码/schema及两XML完整SHA存同目录source-hashes.txt。主任务已核验GREEN，Laplace独立review待结论。Maven暂停，待NCR/Correction最终冻结后按M9 109、NCR、Correction、R4各独立命令执行，不提前编译MES，不以定向60替代运行bundle或整包验证。

## 04:37 P2a严格fixture定向GREEN

GREEN: mvn -B -f IntRuoyiBackend/pom.xml -pl yudao-module-system -am test '-Dtest=GxpAuditPolicyBundleLoaderTest,GxpAuditPolicyStrictLoaderTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -> PASS，60/0/0/0。

04:37:25 session62133终态、调度/Maven均0，无所属integration Java。strict-loader-sixty-green本次原始XML：BundleLoader14+Strict46=60/0FAIL/0ERROR/0SKIP。四文件冻结SHA均实核匹配；源码直接/传递依赖检查及实际Reactor Build Order均仅18模块，不含MES/server，未消费MES编辑。完整command.txt/combined.log/exit-code.txt及before/after XML位于.review-fix-loop/runs/gxp-integration/strict-loader-sixty-green。仅严格fixture读取合同GREEN；运行YAML未迁移，旧8非MES动作14profile映射仍缺，旧运行bundle会被拒绝，不宣称整包可启动/可激活或P2b/P3通过。M9 109及其它MES继续等待owner冻结，未追加构建。

## M9 G2五组测试检查点（NOT RUN）

BDD: G2子证据自身身份与来源 -> Given 同run合法模拟父及fragment/PQC record/snapshot/binding/item候选，When reset或cleanup，Then 正式/异run/缺身份/异stage/错配来源必须CLEANUP_BLOCKED且首写前拒绝；合法同run候选必须被实际检查且不因新增身份守卫一概拒绝。60新增实例加原49目标109，未运行RED，不允许先接入对应生产守卫。映射A10/A11/A20/A23/A22；合法正例仅身份/键一致性fixture，不是领料正式来源或真实DB闭包证明。G1四真实RED补丁待GREEN；G3–G5及026027继续开放。

## 04:25 MES四组真实测试终态

04:25:07 session90412已终止，调度0，四组Maven均1，无所属integration Java。ncr-scope-audit-red-r2实际47/6FAIL/1ERROR：六项未append/未ledger锁/审计异常未触发为行为RED，invalidSignature用例UnnecessaryStubbing为fixture。pqc-correction-audit-red-r2实际23/4FAIL/0ERROR：两append未调用、两审计故障不抛为新增实际RED。formal-reverse-trace-r4-green-r2实际48/0FAIL/48ERROR，统一cellValuesJson is required for eDHR field audit，原gxpAuditService空指针已越过但仍fixture，非GREEN。m9-simulation-49-red-r2实际49/4FAIL/0ERROR，原45（含前六真实RED修复）全部通过，新增四个软删正式/外部共享allocation两入口均RED；不代表33表闭包完成。每组command/log/exit-code/before/after独立保留，未覆盖首次testCompile失败。运行器已具公共前置停止与mes-r2.pause组间停点；本批实际到目标测试故继续。现Maven暂停，Java构建冻结可释放给各owner限定修复，下一构建重新汇总冻结。

## 04:18 五组调度终态及公共编译阻塞

04:18:17 session90480已终止，调度退出0、五组Maven均退出1；终态无本worktree Java/Maven/Surefire。Strict总45/23FAIL/2ERROR（Loader14PASS、Strict31/23/2），较上一轮减少2FAIL和1ERROR，完整P2a未通过。NCR/Correction/R4/M9四组均在MES testCompile因CorrectionServiceTest缺JsonUtils import（113/114/253行四引用）失败，after XML均0，目标全部NOT RUN，不能作产品RED或旧六修复GREEN。用户暂停消息到达时R4已自动开始，既有脚本无组间暂停入口并自动接续M9，未杀进程；不另启构建。现开放Ampere仅import修复，待其冻结后完整MES四组重跑，不skip编译。保留所有原失败日志/命令/退出码；下次调度应支持组间暂停，避免已知公共编译阻塞重复构建。

## system编辑窗口与计数口径同步

03:56终态主核同步：MySQL11PASS（日志03:56:34）；Strict队列合计45=原Loader14+Strict31，25FAIL/3ERROR，不另加14。正例被旧writeBoundaryScan硬编码拒绝及不支持schema静默接受由Maxwell按精确RED实施；classpath哈希测试的JUnit关闭ERROR为Windows临时p.yaml/s.json文件占用，独立处理资源/fixture，不作为策略业务RED。session9040已结束且无integration Maven。Maxwell P2a编辑窗口已开放，所有后续Maven暂停：MES49/R4/NCR/更正的-am依赖system，必须等system再次冻结，不因MES自身冻结提前编译。M9仅继续独立写集编辑。

## 03:56 system独立取证

03:56:48 system独立session9040终止，调度0，两个Maven退出0/1；无所属Java进程。writer-mysql-eleven-fixture-r2实际11/0/0/0，生产字段handler夹具修正后本次九业务及两装配全部通过，原9ERROR保留。strict-loader-p2a-red原Loader14/0/0/0，Strict31/25FAIL/3ERROR，共45/25/3/0；其中14个配对负例被合法正例writeBoundaryScan根合同拒绝阻断，不能冒充负例验证；另两个正例同根合同异常，1例扩展上下文关闭ERROR需单列。可达输入违规九例、非法UTF8、不支持schema约束未拒绝共11项行为断言失败。仅本次after XML计数，各目录保留command/log/exit-code/before/after。未启动MES/server；后续构建继续等待相关owner冻结。

## 2026-09-29 03:49 五组实际终态

2026-09-29 03:49检查点：四组session82949已03:47:52终止（调度0；Maven分别0/1/0/1），追加R4 session12482已03:48:59终止（调度0、Maven1）；终态复核本worktree无Java/Maven/Surefire。Startup26及Release54全PASS；writer MySQL11为2PASS/0FAIL/9ERROR，策略fixture插入create_time为null，Rawls已定位缺生产DefaultDBFieldHandler，未到九项业务断言；M9 45为39PASS/6FAIL/0ERROR，六项均命中首写MesProWorkOrderMapper.updateById，为正式下游/共享事件保护真实RED；R4为48/0FAIL/48ERROR，全部gxpAuditService.acquireLedgerLock空指针，属手工装配fixture，不是产品RED。释放本批Java构建冻结，Maven暂停至下一相关owner冻结。G1–G5完整闭包、真实服务事务/并发、CR2-026/027及其它本轮覆盖继续开放，不融合主线。

本批原始目录位于.review-fix-loop/runs/gxp-integration/：startup-26-green、writer-mysql-eleven、release-rt-54-green、m9-simulation-45-red、formal-reverse-trace-r4-fixture；逐组command.txt/exit-code.txt/combined.log/before/after保留，仅after新XML计数。运行器dalton-frozen-four-next.ps1与dalton-reverse-trace-next.ps1保留完整命令。Startup -pl yudao-server -am package，其余分别system/MES -am test，均指定目标类并保持surefire.failIfNoSpecifiedTests=false、style.color=never，不跳编译。

- GREEN: startup-26-green/command.txt -> PASS，Guard18+MapperIntegration8，共26/0/0/0。
- GREEN: release-rt-54-green/command.txt -> PASS，根包2+batchrecord52，共54/0/0/0。
- RED: m9-simulation-45-red/command.txt -> FAIL，六新增反例首写哨兵命中，预期CLEANUP_BLOCKED却进入工单update。四类正式receipt/backfill/申请/NCR与两类候选外allocation/task引用均实际失败。
- FIXTURE ERROR: writer-mysql-eleven/command.txt -> 11/0/9/0；formal-reverse-trace-r4-fixture/command.txt -> 48/0/48/0。上述装配错误不记作业务RED。
- 下一窗口：Rawls真实handler装配与NCR、Ampere R4及更正包、Maxwell技术合同、本workerM9完整闭包及026/027各自范围编辑；下一构建必须再次冻结相关依赖。不改DDL掩盖fixture，不复用remove/rebuild operation。

## G1–G5完整闭包续接（未启动新构建）

续接检查点：session54985已结束，四组退出1/0/1/0及原始证据保留；当前不运行Maven。M9现有39项GREEN仅旧切片，新加6例（四类正式下游、两类reset共享事件引用）尚未运行，合计45例不是完整闭包。已读取Laplace可达33表/不可达30分支矩阵，下一检查点按G1–G5一次补齐候选、来源与外部引用映射及测试后冻结，再跑完整新增RED，未RED不改相应服务守卫。Ampere RT已冻结；Maxwell已直接回报Startup18+Mapper8及五文件冻结，待Rawls与本worker冻结后集中构建，不另启局部队列。

BDD: 正式下游与共享事件保护 -> Given 合法模拟父且无batch/event但存在receipt、backfill、release application或NCR，或待删事件被候选外allocation/task引用，When固定reset，Then CLEANUP_BLOCKED且首写前拒绝。六新增实例NOT RUN；不能记为RED。完整矩阵其它G1–G5用例仍在补齐，原39原始GREEN不变。

## 2026-09-29 03:25 新四组实际终态

- session54985于03:21:02启动、03:25:32末组完成，调度退出0；每组Maven退出分别1/0/1/0，调度成功不是全绿。确认本隔离目录无Maven/Surefire后释放构建冻结，由主统一派工。启动前四owner指纹核对通过；E:/IntRuoyi另任务Maven未干预，不共享本target。
- RED: Startup正式`-pl yudao-server -am package -Dtest=GxpAuditPolicyStartupGuardTest`（其余完整参数见command.txt） -> FAIL，13/12/0/0，03:23:18归档。实际组件扫描未发现guard、Loader未调用、多租户包括disabled/deleted/orphan未校验、跨租户读取异常未传播；1项静态合同通过。MDEP-98与Mockito前置现已越过，不覆盖旧故障记录。
- GREEN: `-pl yudao-module-mes -am test -Dtest=MesM9SimulationHistoryProtectionTest` -> PASS，39/0/0/0；六首写反例已转GREEN，保持原正例。只证明本批mock服务边界，完整reset其它子表、真实Mapper/事务/并发仍未关闭。
- RED: `-pl yudao-module-mes -am test -Dtest=MesProEdhrReleaseServiceImplTest` -> FAIL，54/5/2/0。根包2PASS，目标52/5/2/0。五失败为rejectPrecheckPassed、terminalAuditUsesIndependentOperation两个参数实例、terminalRealWriterPersistsPresentEnvelope两个参数实例，预期PRESENT实际PRECHECK_PASSED/PENDING_APPROVAL；两ERROR为terminalReplayReadsCommittedEventDespiteOlderReadSnapshot两实例，实际ServiceException“当前eDHR放行状态不允许该操作”，系旧普通读取未重放的行为RED，非fixture。M8缺ledger依赖旧失败本次已消失。
- GREEN: `-pl yudao-module-system -am test -Dtest=GxpAuditWriterMysqlSnapshotTest` -> PASS，2/0/0/0，真实任务MySQL装配/事务前置；九组业务快照/并发/回滚尚未完成，不能从2PASS外推。
- 四组目录：`.review-fix-loop/runs/gxp-integration/startup-guard-thirteen-red-r2`、`m9-simulation-39-green`、`release-rt-red`、`writer-mysql-assembly-two`。各有完整command.txt、exit-code.txt、combined.log、before/after原始XML；仅本次after计数，无旧报告混算。未跳插件/测试/编译、未合并主线。

## 2026-09-29 03:05 集中队列实际终态（Dalton）

- 原session46476完成system后，任务PowerShell脚本复用typed XML变量导致第二组启动前退出；仅修任务脚本，session40862从Release续跑，无重跑system。40862于03:05:49结束；最后进程复核无Maven/Surefire，现释放Java编辑窗口，Maven暂停。队列脚本退出0表示四组已取证，不代表每组通过；各组exit-code.txt为权威。
- GREEN: `mvn -B -f IntRuoyiBackend/pom.xml -pl yudao-module-system -am test '-Dtest=GxpAuditWriterCurrentReadTest,GxpAuditServiceImplTest,GxpAuditPolicyActivationLockBehaviorTest,GxpAuditPolicyActivationReplayBehaviorTest,GxpAuditPolicyBundleLoaderTest,GxpAuditPolicyActivationIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never'` -> PASS，76/0/0/0，退出0。分别7+30+10+10+14+5；主agent已回传writer限定独立PASS和六文档限定PASS，不延伸为MySQL或全部GxP完成。
- Release: 同上公共参数，`-pl yudao-module-mes -am test '-Dtest=MesProEdhrReleaseServiceImplTest'` -> 50/1FAIL/0ERROR/0SKIP，退出1。两同名类分别48/1和2/0。唯一m8MissingPolicyRollsBackRealSpecializedAuditAndBusiness的ledgerSequenceMapper为null（GxpAuditServiceImpl.lockLedgerSequence:167），fixture装配失败，不能冒充终态业务回归；RT01/RT02仍须独立闭环。
- BDD: reset无event正式子候选 -> Given合法模拟父及正式task/piece/aggregate/completion/feedback，When固定reset，Then首写前CLEANUP_BLOCKED。BDD: task读取删除集合一致 -> Given live为空、全历史候选非空，When模拟cleanup，Then保护未验证历史。新增空reset正例仅为addActiveOrder替身下编排合同。
- RED: `mvn -B -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=MesM9SimulationHistoryProtectionTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never'` -> FAIL，39/6/0/0，退出1。五个reset反例嵌套栈为Unexpected first write: MesProWorkOrderMapper.updateById；遗漏task反例为MesPqcProcessInspectionAggregateDetailMapper.deleteByActiveOrderId。完整堆栈保留combined.log和原始XML。旧32及新编排正例通过，不关闭S01/S03或真实SQL/事务。
- Startup: `mvn -B -f IntRuoyiBackend/pom.xml -pl yudao-server -am package '-Dtest=GxpAuditPolicyStartupGuardTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never'` -> BUILD FAILURE/1，03:05:47，testCompile第29行org.mockito不存在；目标13例NOT RUN/无新XML。package已解决旧unpack MDEP-98前置；未跳插件/测试/编译，未改pom。Maxwell已收到装配修复窗口，未有guard行为RED前不改生产。
- 每组原始目录位于`.review-fix-loop/runs/gxp-integration/`：writer-adjacent-green、release-terminal-green、m9-simulation-39-red、startup-guard-thirteen-red；各含command.txt/exit-code.txt/combined.log/before/after。仅计after本次报告，不累计before或其它类旧XML。
- 运行环境：Maven路径apache-maven-3.9.16；实际Maven进程及Surefire XML为Temurin JDK21.0.10+7（java.home指向jdk-21.0.10.7-hotspot），javac target17。root/server/report pom无profile，用户/发行版settings无profile/activeProfile，未传-P。没有把编译目标当运行版本。
- 后续：Rawls真实MySQL writer在任务容器59241内使用独立system测试库，不复用M9表；正式DDL/触发器与Spring事务代理尚待装配。M9完整候选/并发/服务回滚及P2保持开放；不批准策略、不接业务DB、不融合主线。

## 2026-09-29 新一轮四组验证准备

- Rawls冻结writer确定性交错7例及ServiceImpl新增3例事务回归，Ampere冻结Release终态新增5例及既有断言增强，Maxwell冻结Startup6，Dalton冻结M9 32补丁；已交唯一执行者串行独立运行。此处仅为冻结交接，不记录尚未返回的PASS/RED。
- 主agent比对policy-delivery第25行后发现启动设计以ENABLE租户枚举缩小了“已激活租户”范围；Maxwell确认禁用/删除/不存在系统记录但有activation会漏查，tenant拦截器上下文也未覆盖。原Startup6先取得缺守卫RED，下一窗口必须补全activation租户全集和跨租户只读边界/上下文恢复，不用当前线程或系统启用列表代替原规格。

## 2026-09-29 02:29 后修复窗口

- 主agent核对`m9-simulation-s01-s03-red-r2/combined.log`：32项10失败、0错误/跳过，02:29:21 BUILD FAILURE；原21项及新增合法piece正例通过，其余新增保护反例交Dalton逐项核对真实RED后修复。生产不得只因mock通过就宣称全历史SQL和事务验证完成。
- 主agent读取ReleaseService的finalizeReject/finalizeWithdraw，并结合worker闭包核对，确认两动作仅专项审计且专项record为REQUIRES_NEW。CR2-010/011并非已排除权限删除，也未由M9覆盖；已授权Ampere测试优先、独立DRAFT、真实RED后最小统一审计与专项同事务修复，不批准或激活策略，不改变驳回重提新轮规则。
- 队列结束后开放Rawls通用writer测试、Maxwell启动守卫测试、Ampere终态审计测试与Dalton M9修复四个不同写集；测试仍唯一Dalton串行，禁止编译正在修改的依赖模块。

## 2026-09-29 集中失败后最小修复交接

- Dalton确认session42639已全部退出，02:13:18无Maven/Surefire；主agent据此正式释放Rawls、Ampere不同文件编辑窗口，避免因子agent不能互相通信形成等待互锁。
- Rawls完成OperationMapper tenant/version/operation锁定读及Activation调用点最小修复，原Lock10测试未改，已冻结待GREEN；此前有效RED为10项1失败。
- Ampere完成REEXECUTE审计afterSummaryHash参数与本次command sourceSnapshotHash一致的单参数修复，保留原断言和重执行业务规则，已冻结待258项重跑；此前有效结果为258项1失败。
- M9 S01—S03继续由Dalton先补测试。ACT01—05原始独立报告剩余软件缺口另由Maxwell整理可执行合同，不得用局部集成5PASS或外部批准缺失替代未实现的软件要求。

## 2026-09-29 01:50 后集中运行核验

- 后续源码复核：通用writer的append在ledger锁之前普通读活动策略及幂等事件，分配序号后仍普通读尾事件。主agent已读取真实Service/EventMapper/OperationMapper，确认独立评审登记风险仍存在；交同一system owner先制定旧RR快照、重放零水位写、当前尾hash与真实MySQL验收设计，尚未修改writer，不宣称关闭。
- Dalton已完成M9 review/aggregate身份与completion来源保护补丁并冻结，未编译/GREEN；交独立reviewer按现有边界复审，不把既有21项RED或旧9项MySQL PASS当作新补丁结果。
- 主agent读取五组实际combined.log确认终止：ACT04集成3项PASS；Loader/Replay合同回归24项PASS；M9模拟历史保护21项6失败、0错误；主线组合258项10失败、1错误；Lock10扩展运行存在字段元数据断言夹具失败，不能将其当成预期并发业务RED。
- Rawls解冻限定Lock夹具窗口，先取得真实行为RED再修当前读；Ampere仅对逐项确认真实RED的主线差异修复；Dalton继续M9且是唯一Maven执行者，所有编辑者再次冻结前不重跑构建。
- 独立reviewer首次联系因主agent误抄ID（7863而非run.json的7862）返回not_found，新建请求遇到agent thread limit。核对run.json后已成功向原Laplace发送I01-I03独立评审，撤回Huygens替代评审排队；不是reviewer实际丢失。未取得报告前不宣布通过。
- Lock10字段元数据夹具已修复冻结（C3F63AA8B6901A2E4AA8D1BFDE6EC640EED224F0986EE07FF0A5B8DE8DB2D438），等待真实行为RED。Maxwell获准在其独占集成测试中补I05/I06旧算法证据保护覆盖；只能使用可核对的测试golden vector，不能冒称生产历史，不裁决I04。
- 未提交、合并或删除worktree。上述局部测试通过不证明完整激活协议、历史保护和最终融合门禁完成。

## 2026-09-29 后续验证检查点（取代下方对应待执行状态）

- M9六项保护修复实际GREEN：session20009退出0，m9-green/after.xml为6/0/0/0。独立m9-six-case-round3.md限定通过；不是整个M9完成。
- M9扩展运行m9-concurrency/combined.log为9/0/0/0，01:10:52 BUILD SUCCESS；其中新增3例仅SQL当前读、InnoDB锁等待和显式连接回滚，不能替代业务服务审计失败自动回滚。9项包含此前6项，不相加为15项。
- ACT04实际RED为14项4失败；修复后session52148退出0，act04-green/combined.log及after目录XML为Loader14+Replay10共24/0/0/0。独立act04-semantic-hash-round3.md限定通过；完整ACT协议与历史版本升级未关闭。
- H01/H02首轮10项9失败；扩展m9-simulation-expanded-red/combined.log为13项10失败、0错误/跳过。新增共享事件拒绝仍越过保护到首写哨兵；正向用例保留。生产修复及GREEN继续由Dalton独占Java/Maven执行。
- 覆盖证据review/coverage-evidence-round3.md限定fail：7文件影响21条记录漂移和可读报告状态不一致，交Huygens补证；未刷新批准摘要、未伪造批准。主线mainline-reconcile-current.md实测17条输入漂移，逐hunk核对尚未完成；不据文件hash直接覆盖来源。
- 当前未提交、未融合、未删除worktree。剩余服务事务/并发、模拟子证据闭包、ACT完整合同、覆盖及主线核对继续；用户排除的权限删除审计保持范围外且已知未覆盖。

## 本轮授权与业务裁决

- 实际M9 RED：session55645已退出1；主agent读取m9-red/six-business-red.xml确认6 tests/6 failures/0 errors/0 skipped。3项重建/恢复预览未识别孤立历史，3项普通清理越过预期业务拒绝到首次写入断言哨兵；不把这6项描述成真实生产数据已被删除。Dalton最小保护补丁已落盘，GREEN和并发/回滚尚待执行。
- 更正前次用户解释：rejection-ui-path.md已证实qualityReject只有API定义、未见前端页面调用；实际NCR“返工”结论在关联批次存在时可设置批次质量拒绝。普通放行“退回”只修改放行事务。不能向用户虚构“质量拒收”按钮或把下游批次页当活跃订单主链。

- 新独立证据review/simulation-history-boundary.md：M9-H01固定test-reset未验证模拟身份便可物理删除正式事件/批记录历史；M9-H02模拟副本仅验证父订单身份，子证据与共享PQC引用缺完整保护。静态条件可达，不冒充真实复现；已交Dalton同一写集先RED后最小修复，避免多人改ActiveOrderService。
- 最新只读来源核对：int_main HEAD为895272ee0c19d9351544ee97400577e5eb8bf45b，原manifest中17条main来源摘要变化。未覆盖或重新机械导入来源；最终融合仍需核定这17项最新差异并保护隔离修复。

- 持续目标仍为解决本轮范围内问题、提交、融合int_main并清理任务worktree；权限删除明确排除不被目标泛称重新纳入。上轮分类为progress：更新授权验收范围、核验实际建表、找回26条精确旧基线；本轮继续真实验证，未完成也未标blocked。
- MySQL当前实际阶段已从容器准备推进到正式DDL切片及SHOW CREATE；为真实Java JDBC另建仅localhost暴露的任务专用容器，归属和准确ID由Dalton记录。两容器均须最终按归属清理，不能遗漏；不得把建表成功当服务/事务验收成功。

- 续作检查：主agent只读任务MySQL SHOW TABLES确认五张目标表存在；仅证明表已建立，完整迁移闭包、真实Mapper/Spring事务及并发行为尚待Dalton证据，未记PASS。独立reviewer另查模拟清理的正式证据边界，不改worker写集。
- 覆盖历史基线补查：Huygens在只读gxp_20同路径找到26条与旧hash精确匹配的原文，原43条缺口缩至17条；已要求继续26条真实语义diff。找到基线不等于重新批准，不更新审批摘要。

- 2026-09-29用户“不纳入”：删除用户/角色/菜单导致的权限撤销审计正式移出本轮修复范围，记录为已知未覆盖而非批准排除；不修改业务流程、不再重复请求同项授权。保留MySQL及其它范围内覆盖验证，不以此单项阻止限定交付，也不声明FULL_COVERAGE通过。
- 独立review/focused-java-round3.md已对三项MES及ACT03局部合同给出pass；92项集中回归证据已复核。该限定通过不包含真实MySQL、完整ACT协议或全系统覆盖。

- GREEN: Maxwell修复夹具短编号路径并新增超限写入前拒绝测试后，主agent四脚本组合独立重跑 `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_method_boundaries.py IntRuoyiBackend/script/tests/test_gxp_audit_policy_static.py IntRuoyiBackend/script/tests/test_gxp_audit_d09_static.py IntRuoyiBackend/script/tests/test_gxp_audit_compliance_wrapper.py -q -p no:cacheprovider --basetemp=.tmp-gxp-r3b` -> PASS，session29276退出0，78项/54.67秒。不代表全仓coverage PASS。
- project-experience-consolidation：将Windows测试报告路径预算及本次Surefire计数边界合并至既有docs/powershell-memory.md；没有新增长期文档。代码尚未最终稳定，不启动最终AOCI维护或收尾清理。

- GREEN: Dalton集中四类选择的本次combined.log为BUILD SUCCESS，主agent逐一核对Running/Tests run：system激活重放10；MES同名根包放行2、Frozen34、batchrecord放行43、PQC轮次3，共92项/0失败/0错误/0跳过。证据 `.review-fix-loop/runs/gxp-integration/dalton-final-green/combined.log`；归档中混有历史其它类XML，不计入本次总数。该结果关闭最新定向集中回归失败，不代表全系统/M9 MySQL或coverage通过。

- 组合独立回归session20345：四个Python脚本 `test_gxp_audit_method_boundaries.py test_gxp_audit_policy_static.py test_gxp_audit_d09_static.py test_gxp_audit_compliance_wrapper.py -q -p no:cacheprovider --basetemp=.tmp-gxp-r3` -> FAIL，71 passed/5 failed；5项PS5 wrapper fixture写XML DirectoryNotFoundException。单项短路径32PASS不能替代组合失败，已交Maxwell限定修测试夹具路径脆弱性，不放宽断言、不隐瞒异常；独立reviewer同步知悉。

- 用户最新要求“先修复1,2”：集中回归及真实MySQL/覆盖门禁优先；本轮不执行主线融合。Ampere与Rawls已确认Java冻结，Dalton获唯一Maven窗口先跑上次RED四类，再继续M9；不得并发编辑编译源。
- 主agent独立wrapper回归：首次深目录basetemp导致32项fixture写XML FileNotFoundError，未进入门禁断言；保留该失败，不作为业务RED。改用短任务目录后 `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_compliance_wrapper.py -q -p no:cacheprovider --basetemp=.tmp-wr3` -> PASS，session16523退出0，32项/19.06秒。测试替身只证明脚本行为，不代表真实Maven或全量覆盖通过。

- 用户新增授权：允许本任务专用临时空MySQL容器及测试数据；不得连接现有业务数据库或启动业务服务。归属、端口/网络、fixture、清理路径和真实MySQL验证证据另记integration-mysql-round2.md。
- 主agent独立验证：session20389 `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_method_boundaries.py IntRuoyiBackend/script/tests/test_gxp_audit_policy_static.py IntRuoyiBackend/script/tests/test_gxp_audit_d09_static.py -q -p no:cacheprovider --basetemp=.review-fix-loop/runs/gxp-integration/main-round2-pytest` -> PASS，44项、退出0。此为扫描器/策略合同测试，不代表全仓覆盖通过。
- 用户要求：先解决主线稳定以外的问题，在隔离融合工作区推进，不提前合入。
- 用户明确：普通上市放行申请被驳回后再次提交，采用“新建一轮，保留旧轮记录”。旧测试复用已驳回事务的期待已被该裁决替代；验证须检查旧轮不改、新轮身份不同和重放不重复，不能只删除旧断言。
- BDD: 普通驳回重新申请 -> Given 旧上市放行轮已驳回 / When 再次发起放行 / Then 新建一轮且旧轮驳回事实不变，再次重放不重复建轮。
- 批记录拒绝后重新执行所使用的生产事实和冻结版本仍等待用户选择，未猜选前驱或修改来源规则。

## 隔离融合最终检查点（blocked，非完成）

- session71972：重新构建system，package跳过测试，退出0。原target可恢复保留于 .review-fix-loop/runs/gxp-integration/system-target-before-resource-check，未删除原测试报告。
- 新JAR SHA256：7E7EF97325E8717B157356D0E0AC37182D37DA2289ABC315DFAB4BA319BDF75A；gxp_resource_contract.py 对解压产物39个资源逐文件核验PASS（35图片、2SPI、2GxP配置）。system测试依据仍为41086的54项。
- session27870：冻结业务源码后的覆盖门禁FAIL，退出1。final-boundaries.jsonl分组：REGISTERED24、APPROVED_EXCLUSION4699、PRIVATE_HELPER102、UNREGISTERED_ENTRY36、UNREGISTERED_CANDIDATE50、STALE_APPROVAL91。不自动批准排除或修改历史审批摘要。
- Ampere分类：SYNC断言仍期待record而实现使用recordInCallerTransaction；嵌套completionService漏注入gxpAuditService导致重执行与冻结读写测试空指针；轮次查询b'0'为已有MySQL/H2方言差异。均需后续修复/核定后复验，不宣称业务通过。
- Dalton分类：4个ActiveOrder错误发生在finally SHUTDOWN，可能遮蔽原断言；其他fixture缺businessStatus/productMasterId。现有失败报告保留，不能将其视为通过。
- 独立review第二轮限定system修复合同通过，整包fail。主线漂移、MES失败、最终覆盖失败及未闭合合同阻止合入。当前不执行cleanup apply，不标记ready_for_closeout/completed，不删除worktree或释放仍占用的任务资产。
- 下一轮必须先核定稳定来源；禁止重跑prepare-integration.ps1覆盖当前修复。来源业务文件未改，未提交/推送/合并/数据库写入/策略激活/服务启动/真实E2E。

## 2026-09-28 隔离融合续作

- RED: system GxpAuditServiceImplTest session63746 -> FAIL，27项/12 failures/2 errors/0 skip；同实例和跨事务同实例冲突、失败事实变更未拒绝、非法status重放被放过。原始报告证明实际执行；worker冻结通知与启动时序未独立锁定，不宣称整包固定快照RED。
- GREEN/FAIL: 组合Maven session41086 -> EXIT1。system九类GxP测试54项全部通过（writer27项）；MES541项，12 failures/23 errors/0 skip。不得把system通过写成整包通过。前端结果不受此结论替代。
- SOURCE DRIFT: 229项来源摘要再次核验时int_main先15项、后16项变化，涉及批记录、PQC修订/聚合、FIFO及测试；gxp_20未见漂移。停止任何写回主线和机械重新同步，保留隔离版本，等待并行主线工作稳定后再建立新验证基线。
- system资源产物核验计划：将隔离worktree的system/target可恢复移到任务临时目录，重新package且跳过测试；测试通过依据上述54项，不把skipTests打包当测试通过。

- RED: 重跑上述MES定向命令session32404 -> FAIL，8项中2项符合预期失败（applyGenerated审计定位错误；PQC审计after缺UDI），0 error/skip。批记录双身份审计控制器6项行为通过。已交worker最小修复。
- GREEN: 主agent独立前端审计身份/乱序/提交错误/任务切换/重建/ERP组合 -> PASS，90项；主线追溯、UDI、材料名称、详情布局等13文件组合 -> PASS，128项。两组有重复用例，不相加宣称独立总数。
- GREEN: 主agent `pnpm ts:check` -> PASS，session56886退出0。未运行真实页面E2E。

- 用户授权子agent并行融合；隔离目录C:/Users/BJB110/.codex/worktrees/gxp-integration/IntRuoyi，分支codex/gxp-integration。主线与gxp_20源码只读；逐文件来源和SHA256保存在integration-inputs/manifest.json。
- 机械整合103份主线参考与126份GxP文件，LF规范化后三方文本冲突8处；逐域worker处理，不把参考文件归入本任务提交。
- CHECK: 来源229项SHA256复核 -> PASS，0漂移；端口guard -> PASS，slot40/8215/48215，未启动服务。
- GREEN: `python -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_method_boundaries.py IntRuoyiBackend/script/tests/test_gxp_audit_policy_static.py IntRuoyiBackend/script/tests/test_gxp_audit_d09_static.py -q -p no:cacheprovider --basetemp=.review-fix-loop/runs/gxp-integration/pytest` -> PASS，35项。
- CHECK: 全仓覆盖门禁 -> FAIL，session45189；源码编辑中中间报告：36 UNREGISTERED_ENTRY、49 UNREGISTERED_CANDIDATE、88 STALE_APPROVAL。不是最终稳定扫描，不自动登记或批准。
- BUILD: 后端 `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest#pushGeneratedUsesPersistedP2WithoutBackfill,MesPqcReleaseBatchExecutionServiceTest#pqcApproveCreatesBatchExecutionOnlyAfterPqcRelease,MesBatchAuditSourceSelectionTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> FAIL，session64331，MES测试编译引用已移除readSourceSnapshotHash方法。不是目标行为RED，未开始这三类测试；由放行worker核对正式API后适配旧测试。
- 独立system评审报告见.review-fix-loop/runs/gxp-integration/review/report-round-1.md；打包资源及幂等合同交独立worker按RED/GREEN处理。审批/激活仅记录前置依赖，不自动修改。
- 使用project-experience-consolidation补充既有docs/worktree-memory.md：来源摘要、换行规范化、机械同步与worker写集隔离。无新长期文档。

## M9 订单维护与证据保留

- RED: 后端目录执行 `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderActiveOrderServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` 的定向重建场景 -> FAIL，session4879确认false返回旧确认错误，true允许有证据重建；前次83748的空指针属于fixture不足，不作为有效业务RED。
- RED: 前端目录 `node --test tests/e2e/active-order-rebuild-evidence.behavior.spec.cjs` -> FAIL，6项中2项失败，有证据仍发送重建请求。
- GREEN: 后端目录 `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderSimulationServiceTest,MesTeamLeaderDataCleanupRetentionTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> PASS，session19056退出0，82+5+18=105项，失败/错误/跳过均0。
- GREEN: 前端目录 `node --test tests/e2e/active-order-rebuild-evidence.behavior.spec.cjs` -> PASS，6项；`node tests/e2e/production-leader-active-order-rebuild-static.spec.cjs` -> PASS。这些是提取处理函数的替身行为测试及静态检查，不是真实页面E2E。
- 已加入直接重建及移除后再加入的共同证据预检；恢复路径专属有证据拒绝测试、孤立子记录和真实MySQL并发仍需补验。整体M9保持in_progress，不能批准合入。

### 本轮前端操作方式

1. 进入“生产组长”，在活跃订单列表找到需要处理的订单。
2. 点击订单的“重建”，系统先检查已有报工、生产进度、检验及放行证据。
3. 已有证据时，系统提示禁止重建并保留历史记录，不再提供确认删除放行。
4. 无证据时，确认重建生产与PQC快照；失败时保留失败提示，不显示成功。
5. 使用推荐修复时同样检查证据，禁止重建后不再继续模拟操作。

### 历史执行记录（以下等待选择已解除）

- 用户已确认：已有正式报工/检验证据禁止重建；无证据订单重建及独立测试副本清理保留。不采用“扩充审计JSON后仍删除正式记录”的方案；此前等待选择解除。
- BDD: 重建保护正式证据 -> Given 活跃订单已有生产事件、PQC提交、检验汇总或生产完成记录 / When 点击重建（即使确认删除为true） / Then 明确拒绝且零删除、零重建、零成功审计。
- BDD: 无证据重建 -> Given 只有未提交的工序/PQC快照 / When 重建 / Then 生成当前版本快照且统一审计同事务，缺策略或审计失败仍回滚。
- BDD: 恢复不得绕过保护 -> Given 已移除订单有正式证据 / When 再加入活跃订单触发内部重建 / Then 重建前拒绝且不删除历史；独立测试副本清理沿原身份校验执行。
- BDD: 前端两入口 -> Given 重建预览报告历史证据 / When 使用重建或推荐修复 / Then 展示证据保留提示，不发送重建及模拟请求；无证据仍进入原流程。

- GREEN: `mvn -B -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderDataCleanupRetentionTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderSimulationServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> PASS，session55671退出0，18+81+5=104项，失败/错误/跳过均0。静态合同PASS；定向diff检查退出0，仅CRLF提示；backend证据validator PASS。
- 本轮独立修复完成：普通清理执行使用事件/批次当前锁定读，账本锁前置；新增预览后出现批次的零删除拒绝回归。未运行MySQL并发或E2E，不宣称孤立证据已全覆盖。
- 当前停点：已发送业务选择问题，等待用户决定正式证据存在时禁止重建，还是另外设计正式归档恢复；不以扩大审计JSON替代持续可查，不擅自改变重建可用条件。已有无关工作区改动、策略与真实数据库保持不动。
- 经验沉淀：依project-experience-consolidation合并到既有docs/backend-development.md，记录当前读、统一锁顺序与真实并发验证边界。

- RED: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-team-leader-data-cleanup-static.spec.cjs` -> FAIL，缺执行阶段`selectAllCleanupEventIdsForUpdate`调用。随后新增两条带租户、排序、FOR UPDATE的事件/批次读取，执行清理先锁账本、再走预览/订单锁/当前证据检查；预览仍普通读取。
- GREEN: 同一静态合同 -> PASS；新增Java场景“预览无批次、锁定读取有批次”断言范围变化拒绝、零删除及账本先行锁顺序。组合Maven最终结果见上方session55671。
- 验证边界：只证明SQL声明和服务调用使用当前读、既有测试替身中新证据阻断；真实MySQL隔离级别、索引范围锁及并发写入口未运行，因此不宣称完整并发保留通过。

- BDD: 清理检查使用当前证据 -> Given 预览未看到报工或批次但并发提交已产生证据 / When 执行清理并锁定订单 / Then 通过FOR UPDATE重新读取证据并拒绝删除；锁顺序为账本、订单、证据，避免和既有统一审计写入反向获取账本锁。静态SQL及服务替身测试不冒充MySQL并发验收。
- 待用户选择：重建物理删除正式证据与持续可查要求冲突。建议有正式报工/检验的订单禁止重建，无证据订单及独立模拟副本清理保留；另一方案须正式归档/恢复设计，不能仅靠审计JSON代替原记录。

- GREEN: `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderDataCleanupRetentionTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderSimulationServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> PASS，session75383退出0，Surefire17+81+5=103，失败/错误/跳过均0；最终源码固定后执行。
- GREEN: `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-team-leader-data-cleanup-static.spec.cjs` -> PASS；bug/backend evidence validator均PASS；生产服务、维护测试、静态合同的`git diff --check`退出0，仅CRLF提示。
- 本次结果：普通清理存在事件/任一批次/放行证据时零删除拒绝；空范围不误拒绝；独立模拟清理保持原路径。维护原因明确SYSTEM，两入口缺策略和审计异常均回滚，草案隔离通过。历史状态参数测试现在均由“存在批次ID”统一拒绝，不宣称逐状态查询或批次行锁已验证。
- Remaining（取代下面历史待办）：重建被删子记录的完整快照/原始证据保留、普通清理并发读写一致性、遗留无父记录证据范围仍未完成。无真实E2E、无MySQL并发验证、无策略激活、无提交合并。M9继续in_progress。
- 使用project-experience-consolidation把系统原因来源与非终态证据检查经验合并到既有docs/backend-development.md；未新建长期文档。

- RED: `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldReturnUnreleasedAllocationsBeforeRemovingActiveOrder+rebuildActiveOrderShouldDeleteRuntimeHistoryThenRebuildSnapshotsFromCurrentSources' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> FAIL，session70883退出1，两项均expected SYSTEM实际null；随后维护命令显式设置reasonSource=SYSTEM。

- Scope: `MesTeamLeaderActiveOrderServiceImpl`的移除、重建、普通清理；独立模拟清理行为不变，不涉及schema迁移或真实数据库写入。
- Contract: 既有Controller/BO不变，认证和订单归属校验沿用原入口；维护事务新增统一append和真实before/after，新增策略仅DRAFT。清理有证据时用现有PRO_PROCESS_POOL_DATA_CLEANUP_BLOCKED明确失败，不跳过部分数据后宣称成功。
- Validation: 使用Maven reactor编译依赖，Mockito业务断言、Spring/H2事务代理及真实GxP writer缺策略分支；统一审计与维护日志为可观测证据，错误不吞掉。
- Verification: 本轮只运行定向后端与静态合同；H2/替身边界见各次记录，不声明真实前端E2E或生产合规通过。
- Blockers: 完整重建子记录快照和并发保留仍需核验；未审批激活策略，不能作为运行态可用或合入批准。

- RED: `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderDataCleanupRetentionTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> FAIL，session53055，16项7失败，五种非终态、事件单独存在、放行单独存在均未拒绝，先于保护范围生产变更。
- 中间组合session64289退出1：103项0业务失败、3夹具错误，重复覆盖preview的spy stub被Mockito严格检查拒绝；改为单个动态预览夹具，不放宽strictness或业务断言。
- BDD: 系统原因不可冒充用户理由 -> Given 移除/重建原因是服务生成的动作说明 / When 构造统一审计 / Then reasonSource明确SYSTEM，不能由writer根据非空文案推断为USER。

- GREEN: `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderActiveOrderServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> PASS，session80184退出0，81项，失败/错误/跳过均0。新增真实GxpAuditServiceImpl缺活动策略错误，两维护入口均在H2事务外验证业务及专项日志回滚；草案未审批、未装载且被运行态loader拒绝。Mapper仍为JDBC桥接替身，不是完整业务数据库验收。
- BDD: 非终态证据同样保留 -> Given 批次状态为0/10/15/20/25，或只有报工事件、只有放行申请 / When 执行普通清理 / Then 整次业务拒绝且零删除。此项落实既有“普通清理不得删除正式批记录证据”，替换上一中间切片允许非终态清理的临时边界。
- 继续使用bug-regression-fix-loop与backend-api-delivery；本轮新增缺策略/草案隔离测试，无对应生产变更，不伪造RED。

- GREEN: 后端cwd `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderDataCleanupRetentionTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderSimulationServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> PASS，session40397退出0，Surefire14+78+5=97，失败/错误/跳过均0。
- 移除与重建的H2事务回归通过：真实Spring事务代理，Mapper/分配服务替身执行JDBC写入；失败点前断言已有写入，统一append抛错后事务外验证版本、删除/新增快照、分配状态和专项日志回滚。不是完整MyBatis/真实业务数据库/E2E。
- 历史Remaining（97项时）：当时未覆盖非终态保护、缺策略与草案隔离；这些已有本轮103项证据，完整快照与并发等剩余项以上方最新Remaining为准。
- 本轮不启用子agent、不激活策略、不改真实数据库、不提交合并；新增策略仅任务内DRAFT。旧清理static要求不阻断正式终态，需按新授权调整，不能直接作为验收PASS。

### M9 当前前端操作说明（未完成最终验收，不作为上线指引）

1. 进入“生产组长”，在活跃订单列表找到目标订单，核对订单号和当前状态。
2. 点击“移除”移出活跃订单；已有放行申请仍按原规则拒绝，审计失败时不应完成移除。
3. 点击“重建”，先阅读历史数据影响，再确认；已有放行申请仍禁止重建。
4. 点击“一键清理生产数据”并确认时，存在报工、检验、任一批次或放行证据将拒绝清理。
5. 测试副本继续使用“清理测试单”；只允许归属正确、标识完整且无受限制下游数据的副本。
6. 新审计策略尚未审批激活，移除和重建不能作为可上线功能交付，不得绕过缺策略错误。

- RED: 后端三类组合 `-Dtest=MesTeamLeaderDataCleanupRetentionTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderSimulationServiceTest` -> FAIL，session37086退出1，90项仅移除新断言失败（没有统一append），清理9项通过；执行期间增加了移除断言，最终仍需固定源码重跑。
- RED: 后端 `-Dtest=MesTeamLeaderActiveOrderServiceTest#rebuildActiveOrderShouldDeleteRuntimeHistoryThenRebuildSnapshotsFromCurrentSources`（其余Maven参数同上）-> FAIL，session67895退出1，1项因缺统一append失败，先于重建生产改动。
- 中间验证session29108：11项2失败，揭示移除after只含增量/字符串及重建仅统计数；session88093：11项1失败，移除after缺工单。未放宽最终验收，改为完整订单及工序/PQC/分配/完成/事件快照，修改前立即序列化；去除缺版本默认1。
- 中间组合session56845退出0；随后新增非终态边界及移除H2事务回滚测试，需最终固定源码复跑，不能用此结果替代新增测试验证。
- RED: 后端cwd `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderDataCleanupRetentionTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> FAIL，session66949退出1，9项均因预期业务拒绝但没有异常而失败。此前session94178是测试Resource未注入的夹具错误，不作为业务RED；修复夹具后才获得本次有效RED。
- 当前验证切片：先按既有批次不可修改状态30/40/50/60和关闭/拒绝签名实施删除前锁定保护，覆盖缺失身份/状态拒绝；不是“所有关联一律禁止”，也不是完整证据保留验收。事件、非终态签名、审计归档保留及移除/重建统一审计仍待完成。
- 用户本轮“继续”确认：移除/重建补统一审计，普通清理不得删除正式批记录证据，独立模拟测试副本清理保留。先处理清理保护，再推进维护审计；不更改审批或激活策略。
- 静态根因：普通清理读取全租户事件/批次，随后删除签名、专项审计、放行与原始事件；现有确认和版本检查没有证据保留校验。旧static合同明确要求全范围清理，不再作为允许删除正式证据的依据。
- BDD: 普通清理保护证据 -> Given 清理范围存在报工/PQC事件、批次执行或放行记录 / When 确认普通清理 / Then 明确拒绝且不执行任何删除，不伪报清理成功。
- BDD: 并发新增证据保护 -> Given 预检时未有证据 / When 锁定和最终解析目标时发现新增证据 / Then 删除前拒绝且零删除。
- BDD: 独立模拟清理保留 -> Given 当前账号完整标识的测试副本且无批次/领料/放行副作用 / When 使用独立模拟清理入口 / Then 继续沿既有严格校验执行，不自动跳转普通清理。
- BDD: 维护操作统一留痕 -> Given 合法归属和状态的活跃订单 / When 移除或重建 / Then 同事务保存真实before/after，审计失败回滚，未批准策略不启用。
- 验证计划：新测试先RED，再最小生产改动GREEN；复跑活跃订单服务、模拟清理及相邻静态合同。Mockito零写校验不冒充真实DB回滚。M9尚未完成。

## M8 重启恢复验证

- CLOSEOUT: 标记ready_for_closeout后执行 `python C:/Users/BJB110/.codex/skills/task-closeout-cleanup/scripts/task_closeout.py --task-id 20260928-gxp20-review-fixes --mode preview` -> BLOCKED，session29190退出1；保留8份任务文件含两份策略草案，无删除项。主线脏且不可快进；脚本unrelated分类不代表已核定文件归属，全部保留，未执行apply。任务最终状态blocked。
- GREEN: `python C:/Users/BJB110/.codex/skills/bug-regression-fix-loop/scripts/validate_bug_regression.py --evidence doc/tasks/20260928-gxp20-review-fixes/execution-log.md` -> PASS；首次缺证据标题，补齐后验证通过。定向测试及文档 `git diff --check` -> PASS，仅CRLF提示；生产代码检查此前已通过。
- GREEN: 后端cwd `mvn -B -pl yudao-module-mes -am test '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrFourMaterialGateReleaseContractTest,MesProEdhrReleaseTransactionLifecycleContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> PASS，session87485退出0，42+5+1+5=53，失败/错误/跳过均0。
- Bug: 预检和提交缺统一审计，专项成功日志独立事务可能残留。
- Expected: 预检CREATE/UPDATE分开、提交SUBMIT真实快照；审计失败全部回滚，重放零新增。
- Reproduction: M8原始RED命令及session42628在下方记录，4/4真实失败。
- Root Cause: 两类写入口未调用统一append，专项日志调用REQUIRES_NEW入口。
- Verification: 本轮53项通过；真实专项服务/Mapper、待办Mapper/H2写入回滚，真实writer缺策略失败、草案不装载、两提交入口重放、首次和重复预检均覆盖。
- Blockers: 正式策略审批登记激活、全量覆盖闭环和合入仍未完成；未运行E2E，待办候选解析服务仍为替身。

- 重启后无遗留Java进程；旧XML仍含失败，不据此宣称最终通过。继续使用原4类定向Maven命令。
- session97047退出1：53项、2 errors，待办测试覆盖已有Mockito answer时when(any)触发原answer空参；改doAnswer，生产代码不变。
- session56005退出1：53项、2 failures，待办真实INSERT缺task_code等必填字段；按现有schema补taskCode/assigneeUserId/actionUrl，不修改schema、不放宽异常断言。
- 待办服务使用替身调用真实Mapper/H2；专项审计使用真实服务代理与Mapper；测试验证同事务持久化与回滚，不声称完整候选人解析或真实页面链路已验证。
- 使用project-experience-consolidation将统一/专项成功审计事务验证及幂等键长度经验合并现有docs/backend-development.md，不创建新长期文档。

## M8 授权与BDD

- 实现范围：precheck/submit/submitForApproval先取得统一账本锁；预检首次CREATE与重复UPDATE分开，真实事务及全部检查项快照在修改前序列化；提交使用独立SUBMIT操作。专项日志只对PRECHECK/SUBMIT加入调用事务，不改变其他终态审批逻辑。草案gxp-audit-release-preparation-policy.draft.yaml不进入正式bundle、不激活。
- 首轮GREEN attempt session56605退出1：4项中1项失败，数据库事务已回滚但检查项测试错误假设全库零行；改为操作前后数量相等，不删除既有数据。
- 中间组合session82139退出0（该时点尚无后续真实writer及草案补充验证，不能代表最终源码通过）。
- 加强真实专项审计服务、真实Mapper/H2及真实writer缺策略回归后，session88896退出1，41项中1项失败：新幂等键超过96字符，先于缺策略校验拒绝。以结构化[operationId,transactionId,requestKey]的SHA-256生成固定长度键修复，未截断业务身份。
- 最终回归命令：`mvn -B -pl yudao-module-mes -am test '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrFourMaterialGateReleaseContractTest,MesProEdhrReleaseTransactionLifecycleContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> BLOCKED，session90033退出1。Surefire 2026-09-28T16-13-34_527-jvmRun1.dumpstream：insufficient memory，mmap 528482304 bytes失败，hs_err_pid76248.log。不是测试PASS，也不按业务断言失败归因。
- 待验证：修复后的真实writer缺策略路径、草案隔离测试与相邻合同最终GREEN；任务服务仍为mock，真实待办写入回滚需补测。未执行E2E、真实数据库、策略激活、服务停止、提交或合并。最终验证缺前提，暂停而不降级测试。

- RED: 后端cwd `mvn -B -pl yudao-module-mes -am test '-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest#m8*' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> FAIL，session42628退出1，4 tests/4 failures/0 errors；预检及两个提交入口缺append，注入审计异常未被触发。生产代码修改前真实运行。

- 用户同意预检和提交审批统一审计、同事务回滚测试与独立草案；不改变审批流程，不激活、不改真实数据库。
- BDD: 放行预检留痕 -> Given 正式批次与既有或尚未创建的放行记录 / When 运行预检 / Then 保存真实检查结果及前后状态的统一审计，检查不通过仍记录实际结果。
- BDD: 提交审批留痕 -> Given 满足既有权限和业务前置的放行记录 / When 两个提交入口推进审批 / Then 同事务记录提交动作，重放不重复写入，不伪造签名。
- BDD: 审计失败原子回滚 -> Given 预检或提交已产生业务变化 / When 统一或专项成功审计失败 / Then 业务、检查项、任务及成功审计均不独立提交；缺少有效策略明确失败。

## M7 主审独立验证

- CLOSEOUT: ready_for_closeout后运行task_closeout.py --task-id 20260928-gxp20-review-fixes --mode preview -> BLOCKED，session85154退出1；保留7份文件含策略草案、无删除项，主线脏且不可快进。脚本的unrelated分类不代表已核定文件归属，全部保留；未执行apply。任务状态blocked，不标completed。
- GREEN: 定向源码/docs `git diff --check`及状态JSON解析 -> PASS，仅CRLF提示。

- GREEN: 后端cwd `mvn -B -pl yudao-module-mes -am test '-Dtest=MesTeamLeaderActiveOrderReleaseApplicationBindingTransactionTest,MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest,GxpAuditServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> PASS，session79219退出0，Surefire 11+8+9=28，失败/错误/跳过均0。
- RED与BDD见r1-execution-log.md M7：真实失败先于实现；主审核对两入口、修改前序列化、版本化幂等、CAS及事务回滚。未把H2桥接替身视作完整MyBatis/真实DB或E2E。
- GREEN: backend-api-delivery及bug-regression-fix-loop的evidence validator检查r1-execution-log.md -> PASS。
- 策略仅新增任务内非运行态DRAFT片段；无审批/激活/真实DB写入。正式启用前需批准完整bundle、登记两入口并按租户激活；当前缺策略时明确失败并回滚。
- 按project-experience-consolidation将既有记录补绑的独立UPDATE审计/回滚规则合并docs/backend-development.md。无新长期经验文档。
- 定向验证完成，先标ready_for_closeout；仅预览，不提交/推送/合并/清理apply。全仓gate沿用历史FAIL判断，未伪造新的扫描计数。

## Round 3 / M6 续作

- GREEN: 前端cwd `node --test src/views/mes/pro/feedback/frontline-pqc-task-switch-employee-behavior.spec.cjs src/views/mes/pro/feedback/frontline-pqc-submit-error-behavior.spec.cjs src/views/mes/pro/processpool/active-order-gxp-audit-behavior.spec.cjs` -> PASS，64/64，主agent独立退出0。
- GREEN: `node tests/e2e/frontline-pqc-extra-restrictions-removed-static.spec.cjs` 与 `node src/views/mes/pro/feedback/frontline-pqc-task-switch-employee-static.spec.cjs` -> PASS，主agent各退出0；`validate_quality_assurance.py --evidence .../r3-execution-log.md` -> PASS。不是E2E。

- 用户明确授权M7补绑漏审修复及审计失败回滚；随后明确授权独立补绑策略草案/测试，不自动审批、激活或写真实DB。现有mes.pqc-release.apply定义CREATE/ABSENT_TO_PRESENT，不能用其伪记既有对象更新；R1恢复执行独立补绑事件及事务测试，保留已批准策略原样。

- 使用quality-assurance-test-suite核对现行验收映射；复用原R3 worker只改过期测试，主agent核查后端与独立复验。当前docs/backend-development.md约800-812已明确禁止按生产报工唯一事件绑定PQC，与旧static合同136行相反。两个被引用历史verification-report不在worktree，未伪称读取；当前正式规则及源码为本轮证据。
- BDD: PQC正式任务与生产事件解耦 -> Given 合法活跃订单/PQC任务/QA/员工及多个生产事件 / When 正式PQC提交 / Then 不查询或绑定生产事件，仍写正式检验和审计事实。
- REGRESSION: `mvn -B -pl yudao-module-mes -am test '-Dtest=MesFrontlinePqcContextServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -q` -> PASS，session38946退出0，Surefire 23 tests/0 failures/0 errors/0 skipped。使用单元fixture，无真实数据库写入。
- 静态新发现M7候选：MesTeamLeaderActiveOrderReleaseApplicationServiceImpl#apply在existing非空但batchExecutionId为空时调用bindBatchExecution并直接返回，无append；applyGenerated对应补绑分支已有append。MesProcessPoolTeamLeaderController调用两个公开入口，不能因一个分支已覆盖而忽略另一个。已向用户询问纳入修复，未改业务或审批。

## Round 2 最终复验

- 最终cleanup preview session3468退出1/BLOCKED：keep六份任务记录、delete为空，主线脏且不可快进。脚本对其他待提交文件的自动“unrelated”分类不证明真实归属；全部保留，不执行apply/提交/合并。任务由ready_for_closeout转blocked，未标completed。

- 用户明确授权PQC业务/审计失败不转成功、网络超时才核对、历史缺身份只读不补造。R3真实RED 12项失败后最小修复，最终20项PQC行为通过；R2泛Function.apply豁免被主审退回，两个回调反例真实RED后撤回豁免。
- GREEN: `python -m pytest IntRuoyiBackend/script/tests/test_gxp_audit_method_boundaries.py IntRuoyiBackend/script/tests/test_gxp_audit_policy_static.py IntRuoyiBackend/script/tests/test_gxp_audit_d09_static.py -q -p no:cacheprovider --basetemp=.review-fix-loop/runs/20260928-gxp20-review-fixes/main-m4-final-pytest` -> PASS，35项，session3812退出0。
- GREEN: 前端cwd执行 `node --test src/views/mes/pro/feedback/frontline-pqc-submit-error-behavior.spec.cjs src/views/mes/pro/processpool/active-order-gxp-audit-behavior.spec.cjs` -> PASS，55项，退出0。
- GREEN: 前端cwd连续提交静态合同、活跃订单审计静态合同各退出0；`pnpm ts:check` -> PASS，session97497退出0。
- R2/R3日志分别通过bug-regression evidence validator；指定源码diff检查退出0，仅换行提示。
- 主agent独立全仓gate退出1，36未登记方法候选、14失效批准、5未登记文件/类别候选；详细分类及两项相邻PQC静态合同失败见verification-report。无自动批准、无放宽断言掩盖失败。
- 使用project-experience-consolidation将不确定提交与回执绑定规则合并至既有docs/frontend-development.md，不新增长期文件。
- M1-M5定向工作完成，先标ready_for_closeout运行cleanup preview；不得将此解释为整体覆盖通过或可以合入。

## M4 剩余候选核定

- 用户明确答复“纳入本轮，按上述规则修复”，新增M5。原R3负责FrontlineFixedTemplatePanel及相关测试/API类型，主agent核对后端回执证据合同，不执行历史回填。
- 主审拒绝将Function.apply仅凭类型当作无写入：回调可有持久化副作用。要求R2新增反例，保留未知回调阻断；BigDecimal数学运算与通用回调必须区分。

- 用户继续；复用原任务目录，不重做前三项已完成修复。
- 核对历史PQC：getSubmittedPqcInspection只读查询正式提交结果，submitPqcInspection重放会追加/核对GxP快照。只读与重新提交不能混为一个操作，也不以只读存在掩盖旧记录重试限制。
- 下一步只收窄有明确类型证据的扫描误报；真实未知写入、公开委托登记缺口、失效审批仍阻断。主agent核对业务入口与历史记录路径，原R2负责脚本回归和最小修改。

## 最终复审

- cleanup preview session97394退出1/BLOCKED：keep六份任务记录，delete为空；主线脏且不能快进合并，脚本对工作树大量改动的自动归属分类不作为文件真实归属结论。未执行apply，未删除任何文件；业务修复验证结果与自动收尾阻塞分开。

- R1交付后主agent独立Maven组合session72707退出0，78/78；最终gate session64019退出1，36方法候选、14失效排除、5未登记文件/类别候选。
- 定向修复通过与集成NO-GO分别记录；全部里程碑已完成，状态ready_for_closeout，不冒充已经合并或正式合规。
- 使用project-experience-consolidation将冻结身份/重放经验补入现有后端规则；使用task-closeout-cleanup仅preview，不执行会自动提交、合并和删除worktree的apply。
- gxp_20无正式AOCI索引，未修改其托管对象，不启动全仓认知建档；不影响源码与测试结论。

## 2026-09-28 重启续接

- 主 agent 独立重跑前端35项行为、静态合同、类型检查以及Python24项回归，均通过；全仓gate实测退出1，报告保存为 `.review-fix-loop/runs/20260928-gxp20-review-fixes/main-resume-boundaries.jsonl`，统计36未登记方法候选（含误报）、6失效批准、1未登记文件候选。R1运行中，此扫描不是后端最终稳定快照。
- 按 project-experience-consolidation 将方法级覆盖与异步结果归属经验分别合并进现有 backend-development/frontend-development 文档，不新建长期文档、不更改旧业务规则。

- 用户明确要求电脑重启后继续；恢复原三个 native 子 agent，不创建新聊天、不替换工作区。
- 已核对 gxp_20 分支及落盘修改；保留全部已有 staged/unstaged/untracked 内容。
- R3 日志已落盘 33/33 行为测试及最终类型检查 PASS；这是此前证据，尚不等于本轮主 agent 独立复审通过。R1/R2 日志仍待补齐真实验证。
- R1 独占 Maven、R2 Python、R3 Node/pnpm；继续禁止提交、合并、服务启停及真实数据库写入。本轮沿用原任务范围。

- 用户明确授权：前三项交给三个子 agent 并行修复，由主 agent 统一复审。
- 来源：doc/tasks/20260928-gxp20-merge-review/verification-report.md，R1 实际执行人、R2 文件级覆盖漏检、R3 异步响应串单。
- 已读 worktree AGENTS、task-closeout-rules、worktree-restrictions、相关前后端规则及 review-fix-loop 技能和三个引用合同。主 agent 本地工作为范围/交叉契约控制、独立审查和组合验证；不与 worker 重复实现。
- 采用 review-fix-loop 记录修复和复审；用户指定主 agent review 优先于技能默认 reviewer 子任务，不新增第四个子 agent。当前不涉及视觉改版，UI 验证只评价本次展示逻辑，真实页面 E2E 单列未运行。
- 共享工作树已有 staged/unstaged/untracked 资产，全部保留；不以旧规则自动提交全部脏改动。

- 已启动真正子 agent（不是新聊天）：Boyle R1、Turing R2、Ohm R3。ID 记录于 .review-fix-loop run.json。
- 主 agent 检查跨层契约：后端 GxpAuditEventRespVO 已有两个身份 JSON，控制器 BeanUtils 已透传；前端类型缺两字段，由 R3 在既有范围补齐。R1 需明确正式员工快照字段后通知 R3。
# M9下一测试检查点增量（2026-09-29，未运行）

评审口径更正：已重读校正后的review/m9-127-static-round3.md，结论为本127补丁限定静态PASS，并撤回piece绕过的未证实判断；127实际GREEN亦已取得。此前把该校正报告写成当前静态FAIL的表述作废，仅原完整G1–G5、真实SQL/事务并发及后续来源矩阵仍未闭合，不是本127补丁新漏洞。

BDD: aggregate来源身份闭合 -> Given同run非空task/piece/PQC/event/review链，PQC无生产绑定时aggregate也不推断生产来源 / When reset或cleanup，逐一替换aggregate的六项来源ID / Then合法链候选被实际消费，外来来源在首写前CLEANUP_BLOCKED。新增14例，RED NOT_RUN，生产未修；当前141总数只作计划，实际以新XML为准。

最新GREEN检查点：05:53:54 session46155实际终态，调度及三组Maven均exit0：Correction30/0F/0E/0S、M9127/0F/0E/0S、NCR59/0F/0E/0S，总216PASS。首组clean test -am完整重编system及MES全部依赖；源码ErrorCodeConstants mtime05:39:44早于权限RED05:43:58—05:44:29，hash仍EDBA87BA...，无时间重叠证据，不因怀疑强制重跑。新归档correction30-signature-green-r2、m9-simulation-127-green、ncr-contract-59-green各保留command/log/XML/exit及before。队列无后续命令、复核无integration Java。M9旧15FAIL属于修前RED，当前127GREEN；review/m9-127-static-round3.md完整范围仍FAIL（delete证明/piece集合/真实SQL等），不混同本检查点。NCR59GREEN不代替AO18必签与无签名创建裁决、完整事务证据；权限30/21FAIL仍OPEN。后续Maven暂停待新冻结。

GREEN: mvn -B -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=MesM9SimulationHistoryProtectionTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -> PASS，127/0F/0E/0S。Correction首组同reactor clean test，NCR独立test；精确命令以各command.txt为准，未合计旧XML。

最新执行纠正：05:34:34 session58343三组实际终态：Correction30=0F/11E（缺ElectronicSignatureRecordMapper bean装配），M9127=15F/0E（旧109全PASS），NCR59=8F/0E；三组Maven均exit1，调度exit0且无后续目标。原始证据分别correction30-signature-green、m9-simulation-127-red、ncr-contract-59-red。M9依据本18增量实际RED完成最小补丁并冻结，GREEN待新一轮全部owner冻结，不自行Maven。NCR47历史GREEN不覆盖完整合同FAIL；M9 aggregate/pickList/组合链、G3–G5余项、026027及真实DB/事务并发仍开放。

RED: mvn -B -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=MesM9SimulationHistoryProtectionTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dstyle.color=never' -> FAIL，127/15F/0E；12首写拒绝负例、3任务删除集合断言，新增3正例与旧109通过。

最小修复：reset删除piece使用已验证锁定task ID；reset保留无模拟身份revision/transfer/maintenance；模拟cleanup保护正式receipt/backfill/release/NCR及revision；snapshot必须有与owner相同routeVersion。保留127全部断言，仅旧合法父订单及snapshot fixture补真实一致702，避免新增非空合同下旧合法夹具不完整。修前/后全文归档m9-127-fix-before和m9-127-fix-after。GREEN: PENDING owner冻结后执行，diff --check通过不算业务GREEN。

BDD: reset锁定任务与删除集合一致 -> Given旧普通task快照为空、外来ID、匹配或合法软删，而锁定验证集合为301 / When reset / Then仅删除301的piece，不能使用旧快照999或漏掉301。新增4例，选择直接使用锁定集合合同。

BDD: 模拟工序快照属于冻结路线版本 -> Given同run订单冻结routeVersion702及快照 / When reset或cleanup / Then702合法非空候选被消费，999或缺失版本在首写前以CLEANUP_BLOCKED拒绝。新增6例。

RED: NOT RUN，其他owner编辑期间Maven暂停；本次服务代码未修复。当前127例仅109有GREEN，新18未执行。完整后续矩阵见integration-mysql-round2.md顶部，aggregate/pickList/组合合法链及G3–G5剩余未计已完成。
# 用户要求直接融合的检查点

用户原话：直接融合，不用验证了。已停止六个子agent及新增实现/构建/DB操作，保留已保存半成品。后续不再运行业务验证或把旧FAIL改为PASS；仅准备任务提交、检查Git冲突并保护主线未提交内容。主线HEAD 895272ee0，整合HEAD 0ece51c1c；主线83个已跟踪脏文件，20个与整合区同路径修改，尚未stash、提交或覆盖这些主线文件。
## 2026-09-29 用户授权暂存主线并直接融合

- 用户允许暂存主线未提交改动，融合后恢复并处理冲突；不开展后续构建、业务测试或E2E。
- 实现提交：71855dced（codex/gxp-integration）；融合前主线：895272ee0c19d9351544ee97400577e5eb8bf45b。
- 主线已跟踪及未跟踪改动保全：stash 7780b938869d472475fb80bed6a459a8023317f6，名称 codex-preserve-int-main-before-gxp-integration-20260929；保留已有其他stash不动。
- 主线端口检查通过。正式merge产生20个文件冲突，按源码逐项保留双方变化；该检查仅用于Git整合，不代表业务测试通过。
