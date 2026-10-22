# G02 Verification Report — 第一阶段

## 最新正式M08联合验收

Root M08 actual started11:03:33、ended11:04:41+08、exit0，23reactor BUILD SUCCESS。相同八类55/55PASS、0F/0E/0skip，其中G02两类10+11=21项全通过。逐份核对Root m08-junit归档与原freshXML SHA256相同，原mtime均在actualM08起止时间窗；方法计数按八suite独立无重复累加，完整记录m08-formal-joint-evidence.json。

四G02源码hash与冻结交还完全一致0漂移；Root全仓前后另有3source变化，故本八类结果不是最终全仓freeze，不扩大custom/附件/全12/E2E/真实ports。G01/G02软件范围已Root联合接收，M07 V6图资源cwdERROR本轮关闭；M07失败证据保留。g02-software-handback更新latestM08回执，源码仍冻结、无本线程新Java/private/Maven/target写入。工程仅Git收尾未授权blocked，不是产品故障，不completed。

最终文档/证据对账verify-design.py实际exit0：13文档/13BDD/4冻结hash、M08八XML55与G02自身21准确计数/0F/E/skip、归档hash匹配。一次状态marker校验因新文案未含旧“软件授权范围”字面退出1，仅更正自有文档validator后通过，不是业务失败。交还manifest当前正式票明确M08/exit0，原M07错误独立history保留。

## 正式M07当前最终软件范围

Root已接收G02授权软件：完整-am当前main+testCompile后，两G02 whole-class实际21/21PASS、0F/0E/0skip（时间类10、End/公开链类11）。非私有计数相加。freshXML修改时间处于M07真实10:47:51—10:48:57+08窗口，m07-formal-g02-evidence.json记录XML SHA256/方法清单/receipt，Root归档m07-junit/。

M07整票exit1为G01 DccWorkflowV6BpmnTest路径Backend/Backend/config错误；signature旧11PASS，DCC44中43PASS/1ERROR，未把G02通过扩成整票成功。Root明确软件范围接收、四源码冻结交还，最终manifest为g02-software-handback.json，当前4hash保持，G02无pending句柄/无再次私有重跑。最终freeze/M08统一由Root，图path由G01修。

真实platform/audience/distribution/账号images/存储/运行E2E端口限制保留；此为软件范围正式通过。工程Git当轮禁令下按规则blocked，不是业务失败，不标completed；cleanup apply/Git未执行。下文M06未执行及私有阶段为历史证据，不覆盖当前正式21PASS。

## 最新正式联合票M06的准确边界

M06回收后的pre-M07来源检查：46个实际培训proof编译源当前hash全部等于public-training-01记录，0漂移；DCC/signature两schema当前hash只读保存pre-m07-training-source.json。该轮未javac/JUnit/Maven、未读半编译class作业务结论，正式G02目标仍M06未执行、待M07。

Root唯一M06八类-am实际receipt10:29:24+08 ended/exit1。signature旧ElectronicSignatureServiceImplTest正式11/11PASS、0F/E/skip；DCC被DccW08PreparedCreationDatabaseTest.java:407缺TenantContextHolder导入阻断testCompile，G02自身目标类尚未执行。此为其它owner测试编译问题，不是G02业务RED/PASS；本线程未并行Maven/Java/target/fulltype或改授权4源码。

正式G02-am仍待Root精确修复及后续票；当前私有各scope sourceStable结果保留，不能替代M06。没有改变任务in_progress或宣称整体completed。

## 当前新增公开guard结果

最新public-guards-02 compile0/JUnit0/3PASS0skip SOURCE_STABLE（M05actualended后复验），首轮漂移保留。随后public-training-01仅两新training方法compile0/JUnit0/2PASS0skip SOURCE_STABLE。具体范围与当前hash在public-training-verification.md/ready.json；培训输入消费、公开producer/CAS/ledger/BPM/End真实，培训上传入口/平台/下发和正式-am还未全部验收。

收尾记录对账verify-design.py实际exit0：13UTF-8文档/13BDD/Keep/4当前授权hash及各scope独立原14-公开2-guard3-training2 result/exit，首漂移保留。两test diff-check0、分支int_qms。没有把分轮结果合计成同一源码全suite或借RootM05八十项关闭G02。

public-guards-01 javac0、JUnit0（3PASS0fail0skip），source_stable=false、script1；**OBSERVED_PASS_SOURCE_DRIFT_NOT_CURRENT_GREEN**。三新方法与source漂移证据/具体输入/命令在public-guards-verification.md。G02不复制原14/公开2作本三项新GREEN，M05期间不再启动验证，待Root结束后独立源稳定复验。

该段是首轮漂移记录；当前复验已按本节顶部完成，没有保留旧等待状态。

## 最新公开确认组合（本轮两新增方法）

`public-command-11` javac0/JUnit0、**2/2PASS0skip**、46当前编译源前后hash稳定。只跑publicCommandProducerBpmAndOfficialEndPreserveOneMicrosecondS / publicCommandLateRealGxpAppendFailureRollsBackUnifiedDccCasAndEngineThenRetries，原14未本轮重跑。

实际publicconfirm→官方producerMICROS/scope/真实统一DB row→DCCprojection→确认CAS/真实GxP→BpmTaskServiceImpl.approveTask/guard→真实FlowableEnd/官方事件→Finalization/control。两验签器重读合法；End/sourceactor和同键重放真实；晚失败由真实控制audit INSERT注入CHECK，证明统一/DCC/CAS/账本和任务回滚并公开重试。没有手写确认签名或mock engine/真实ledger。

详细命令、11轮fixture/编译问题分类、source/checkpoint/端口边界见public-command-verification.md；当前API已执行，旧下文“公开命令未验”是历史记录。实际只有账号/physicalimage/字节存储、platform/distribution端口替身，不冒有培训/完整reactor/真实账号系统/服务UI已验。无Maven票。M03Root编译diagnostic exit1非本组合业务结论。

`python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py` 实际exit0：11 UTF-8文档、10BDD、资产Keep、4当前授权源hash、原14及公开2真实exit/result分别对账。两生产服务hash未改变，公开阶段只两test/自有runner改动。此为文档证据验证，不重复业务运行。

M04本线程只读receipt：Root唯一票实际exit1，卡在非G02授权SignatureGovernancePolicyDccAdapterTest.CapturingDccSignatureVerificationService新增抽象方法未实现，4目标业务tests未执行。G02无Maven/私有Java并行，本轮不越界修改旧fixture，原公开2证据范围保持。

## A13 当前软件验证（覆盖下文NOT_RUN历史记录）

实际修改仅授予两服务/两专属测试。使用私有输出、当前21个关键DCC源码显式编译，真实H2/MyBatis/Spring事务、V6 Flowable当前/历史、唯一Bound/Rule/Evidence、真实签名canonical/HMAC/source hash/binding/盖章。

- RED `a13-red-time4`：javac0/JUnit1，7项3PASS4FAIL，source stable。实际controlledTime=10:20（重试10:21），应为真实S=10:00:00.123456；同日激活新分支拒绝。直接达到真实数据库时间差异。
- End RED `a13-finalization-red7`：javac0/JUnit1，9项7PASS2FAIL，真实官方Flowable End被旧preflush文控任务白名单拒绝。此前fixture空HttpServletRequest失败独立记载，不作RED。
- GREEN `a13-current-source9`：javac0/JUnit0，9/9PASS0skip，source stable。编译当前Frozen/ExecutionContract/Resolver/audit/adapter，避免V1旧target引起的伪失败。
- REGRESSION `a13-final14`：javac0/JUnit0，14/14PASS0skip，SOURCE_STABLE，实际JUnit285790ms。覆盖同日E精确到期、S/E真HMAC篡改/错轮次、原S重放、已提交晚失败保原S、同步End晚失败任务/签名回滚和retry新S2、R/D不同actor、新V6立即分支。

实际命令 `python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/run-private-junit.py a13-final14 DccG02ControlledSignatureTimeTransactionTest DccG02DelayedFinalizationTransactionTest`。完整阶段失败分类、命令、source manifest/exit.json路径与范围详见a13-implementation-evidence.md。运行输出变慢已如实等待实际退出，未把旧PASS覆盖pending。

private classpath包含既有模块接口/端口，不等于整个当前源码验证。公开G01确认command与BpmTaskServiceImpl.approveTask不在此composition中；平台真实core/GxP/audience/distribution、账户/签名图片真实资格、培训/完整V4/V5/PRESET_DATE/并发/授权仍待Root唯一-am联合验证。本轮没有Maven/E2E/真实库/服务/Git，不称运行验收或全项完成。

C05新时间/存储门禁已同步：本14项fixture S为微秒123456000 nanos、H2 TIMESTAMP(6)，真实回读HMAC、confirmedAt/control消费已覆盖；尚未调用官方server MICROS adapter/统一签名producer返回S，也未执行MySQL DDL，不能称完整官方producer roundtrip已通过。G01 API到位后接唯一producer，不签后截断重算；本轮无代码变更因此不额外重跑已通过14项。

交付文档结构 `python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py` -> exit0：10UTF-8文档/8BDD/自有Keep/四授权最终source hash与private14 exit/result证据核对PASS。只读取已存真实结果，不作为额外行为重跑。当前分支再次int_qms，diff-check exit0；未Git收尾，未completed。

## A11 最新只读进度（不代表行为测试）

`python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/check-integration-source.py a11-source-checkpoint.json` -> exit 0；实际记录16个源路径与当前hash/存在状态。自 A07 后仅 DatePolicy、Lifecycle Rule、FileDO、SignatureDO 这4个登记依赖变化；G01唯一Evidence/确认服务/V6两图仍ABSENT。已有字段/Rule不等于当前Bound/V6/签名完成证明已实现。

当前未编译或运行Java/历史行为测试；S!=T RED/回滚/重放仍NOT_RUN，Maven无票。A11已授共享审计/V2/activation依赖给G01，不再请求重复授权或旧M02冻结；不写共享生产。脚本仅校验可用性和来源，不能作为业务PASS。

`python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py` -> exit0，9 UTF-8文档、8 BDD、自有资产/Keep、29+18历史指纹/16当前源记录结构PASS，确认G02两生产文件未变。历史源快照只保留历史值，不把已授权G01变更要求为“当前必须未变”；无行为PASS。

## A07 当前实际执行证据

A07 已释放 BE 冻结，Root 授四文件内真实 RED/GREEN；本轮没有 Maven 票。`git branch --show-current`/`git rev-parse HEAD` 实际 exit 0，int_qms / 822c31fc0856516cf59ba9cd84bf2bc7f455d17f。

只读依赖检查实际结果：G01 Evidence.java、ConfirmationService.java、V6 upload/revision XML 均 ABSENT；G02 新测试也尚未创建，DO 新字段和生命周期新 Rule 尚未接入。不能执行到合法 G01 确认/受控链，不把缺符号/unsupported mode 作为产品 RED。

已保存 a07-integration-baseline.json：11 个自有资产改前 hash，16 个当前路径 raw/规范化 hash/ABSENT；编写 a07-interface-composition.md 完成接口组合。本轮源码未改，未编译/跑测试，不使用旧 jar/Mock Evidence 伪证。Java17 java/javac 已只读定位，但没有执行编译。

`python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py` -> exit 0，PASS：9 份 UTF-8 文档、8 个 BDD、资产范围/Keep、29+18 源路径/锚点/指纹，A07 16 路径状态记录完整。首次 A07 文档检查 marker“真实 S”与正文不匹配 exit 1，更正为“原精度”后通过；非产品 RED。

结束前再查 Evidence/确认服务仍 ABSENT，exit 0；`git status --short --untracked-files=all -- doc/tasks/20261014-dcc-gap-controlled-signature-time` -> exit 0，13 自有资产可见，未暂存/提交。此次只有文档静态验证，行为测试仍 NOT_RUN。

## 当前结论

本阶段仅文档/静态证据交付，未修复生产代码。源码表明 controlledTime 目前使用 T，新延迟确认的 S 消费与门禁尚无实现；该结论是当前源码推导，不是运行复现。

## C01/C02 接续状态

最新 M02 延续 BE 冻结，M01 回收不意味着自动释放。本轮 Java/生产均未写；G01 唯一 Evidence 类与六字段 DTO 已获核准，未复制另一校验器。

C02 授两生产文件/两专属新测试，M01 期间 BE 写入继续冻结；没有运行 Maven 或重跑历史测试。End 时序/actor/签名可信时间来源已独立只读复核，两类新增 Java 测试仍仅设计。E 秒精度、S 原精度、严格 E>=S 不截断；当前 source 精度差异已向 Root 以可主动读取文档提交，不通过模拟相等时间规避。

c01-followup-baseline.json 保存 10 个已有自有资产在 C02 文档修改前的 hash，以及 18 个当前只读时序源码/模型指纹；不覆盖首阶段 29 源指纹。接续结构校验 `python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py` -> exit 0，PASS：8 份 UTF-8 文档、8 条 BDD、自有资产范围/Keep、29+18 源路径/锚点/指纹；仅文档证据，RED/GREEN/REGRESSION 行为仍 NOT_RUN。

首次接续结构检查因 marker“真实 S 保持”与正文“实际 S 保持”不匹配 exit 1，更正自有 marker 后通过，非生产 RED。

## 已执行

- `git branch --show-current` -> exit 0，int_qms。
- `git rev-parse HEAD` -> exit 0，822c31fc0856516cf59ba9cd84bf2bc7f455d17f。
- 当前源码/相邻测试通过 rg/Get-Content UTF-8 独立读取；大输出截断后关键方法使用分段读取，不采用历史报告 PASS。
- 只读源码规范化 UTF-8/LF SHA-256 保存于 baseline-fingerprints.json（不是生产改动）。
- 文档结构验证命令：`python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py` -> exit 0，PASS：7 份 UTF-8 文档、7 条 BDD、全部自有资产范围/Keep、29 个源路径/锚点/指纹。此为文档验证，不能替代业务测试。
- 首次指纹采集 exit 1：本线程登记测试锚点漏写 Back；修正为真实 batchAcceptStorageFailureRollsBackActivationAndNoAfterCommitRuns 后重采 exit 0。不是业务 RED。
- `git status --short --untracked-files=all -- doc/tasks/20261014-dcc-gap-controlled-signature-time` -> exit 0，9 个自有文件可见，未暂存/提交/推送；验证脚本不被忽略。

## 未执行及原因

|验证|状态|原因|
|---|---|---|
|G02 生产行为 RED|NOT_RUN|A07 已解冻；真实 G01 Evidence/确认/图/新字段/Rule尚未可执行。可隔离 javac/JUnit，未运行；无 G02 唯一 Maven 票。设计中 RED 预期不能记成已失败。|
|G02 GREEN/REGRESSION|NOT_RUN|没有本轮生产实现或行为测试运行。|
|历史相邻 Maven 测试重跑|NOT_RUN|无 Root 唯一验证票；只读复核源码。|
|全类型/大构建|NOT_RUN|未授权且 G02 为后端依赖阶段。|
|E2E/服务/真实业务库/远端|NOT_RUN|用户明确禁止。|
|cleanup apply / Git 提交推送 / 工程 completed|NOT_RUN|本阶段仍需实现和接入，不进入工程收尾；Git 当轮禁止。|

## 后续唯一 Maven 票候选命令（未执行）

工作目录为 IntRuoyiBackend；完整 -D 参数加引号。依赖编译必须 -am，不允许单模块旧 jar 冒充当前源码。

RED/GREEN：

```powershell
mvn -pl yudao-module-dcc -am test "-Dtest=DccG02ControlledSignatureTimeTransactionTest,DccG02DelayedFinalizationTransactionTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

REGRESSION 候选：

```powershell
mvn -pl yudao-module-dcc -am test "-Dtest=DccG02ControlledSignatureTimeTransactionTest,DccG02DelayedFinalizationTransactionTest,DccControlledFileLifecycleEffectiveModeTest,DccWorkflowLifecycleTransactionTest,DccStandardizationEffectiveAuditTransactionTest,DccPublicFinalizationTransactionTest,DccControlledFileFinalizationServiceImplTest,DccLifecycleDistributionOutboxTransactionTest,DccLifecycleEffectiveContractResolverTest,DccLifecycleBoundContractConsumptionTest,DccElectronicSignatureManagementServiceTest,DccControlledFileSignatureEvidenceServiceTest,DccFrozenApprovalSignaturesTest,DccWorkflowDatePolicyTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Root 应按最终转交差异冻结范围，唯一发票再运行；false 仅允许 -am 的依赖模块没有指定测试，不允许 DCC 目标测试缺失/0 个 PASS。每轮必须核对具体测试类实际执行、失败原因、退出码和 surefire 报告；相邻测试旧日期 fixture 应按正式新/旧图区别更新，不能删除断言或赋造签名制造 GREEN。

## 证据强度与限制

真实防伪需生产 HMAC verifier/source hash/image/binding 行为；真实回滚需隔离数据库、真实核心 Mapper/Spring 事务及必要 Flowable 组合。Node/Python 结构检查、mock VALID、mock TransactionTemplate 均不能证明上述业务。无需为了证明计划可行提前运行被禁止的服务或真实页面。
