# DCC G02 延迟确认受控时间

## Task Goal

延迟申请以 G01 延迟生效确认节点的真实电子签名时间 S 记录 controlledTime，不能用后续系统处理时间 T 替代；无有效确认签名不得受控，保持生效、盖章、下发的既有安全约束。

## Authorization And Scope

- 初始分派为 DEPENDENCY_ONLY，仅自有任务目录；初始共享生产/测试未授权。
- 当前 A07 为 WRITE_GRANTED：仅 Lifecycle/Finalization 两生产文件、两 G02 专属新测试及自有任务目录。先前设计交付已 review；不得扩大到 G01 的模型/resolver/date/signature/audit/API。
- 禁止子 Agent、Git 提交/推送、分支切换、worktree、E2E、服务重启、远端和真实业务库写入。Maven/全类型/大构建须 Root 唯一验证票。
- Root 主动读取报告并继续分派；不自行跨线程发送消息。
- Root 本轮接续已 review 依赖方案：C01 冻结 DELAYED、LocalDateTime、客户端 YYYY-MM-DD HH:mm:ss、Asia/Shanghai 与可信签名同秒精度，历史 PRESET_DATE 不变。继续只复核 End actor/签名时序及两类独立测试设计；M01 期间不写 Java/测试，不重跑历史测试、不创建平行 resolver。Lifecycle/Finalization 待 G01 合同冻结后由 Root 整体转交。
- 最新 C02 覆盖 C01 的同秒解释：E 规范化到秒，真实 S 保持原签名精度，严格 E>=S 不截断。V2/V6、确认 stage/status/meaning、四文件投影与签名 selectedEffectiveTime/attributeRound、新 payload 已冻结。
- 当前写权 WRITE_GRANTED_FROZEN：Root 已明确授 DccControlledFileLifecycleService.java、DccControlledFileFinalizationServiceImpl.java 和 DccG02ControlledSignatureTimeTransactionTest.java、DccG02DelayedFinalizationTransactionTest.java；G01 不写两生产文件。M01 后端冻结仍有效，释放通知前不改 Java/测试；消费 G01 唯一 ConfirmationEvidence 方法、Bound/签名合同，不新增 resolver。
- 最新 M02 接续：M01 已回收，M02 授 G12 GREEN/回归；G02 BE 冻结延续至 Root 正式释放。Root 已核准 DccDelayedEffectiveConfirmationEvidence.java 与 VerifiedConfirmation(selectedEffectiveTime,signedAt,signatureId,processInstanceId,taskId,attributeRound)，G01 负责真实当前 End/历史完成边界验真，G02 消费同一合同。实际 callable 方法名/参数以 G01 接入同步为准。
- A07 最新已释放 BE 冻结、M02 回收。本轮已获实际 RED/GREEN 授权；Root 明确 Evidence 未创建可先完整接口组合设计，等待真实类再编译。私有隔离 javac/JUnit 允许，禁止写共享 target/冒称旧 jar 为当前源码；Maven 仍未发 G02 唯一票。
- A11 明确 G01 独占 FrozenApprovalSignatures V2 四审批、ContentAdapter DELAYED activation reason、FileStateAudit确认/activation边界；这些依赖不再索取授权，等待其实际可执行源码。G02仍四文件边界，A07释放不变；实际 Evidence 出现后主动接入，不用 stub/callerBoolean。
- A13 已接收实际 requireConfirmed(file[,bound])，Root核准8字段record（原6+actorId/evidenceHash）；不构造猜测旧6参数、不增SignatureDO不存在tenant。本轮已在授予四文件内真实RED/GREEN，最终私有14/14PASS、正式-am仍待唯一票及G01公开命令联合验证。
- C05：新延迟签名由正式server adapter创建前选MICROS tickclock产生唯一S，统一/DCC/HMAC/持久化原样消费，DDL6由G01负责，无历史UPDATE。G02 fixture S=10:00:00.123456、TIMESTAMP(6)，符合微秒范围；联合producer API到位后再验统一/DCC/signature/control roundtrip，不把任意nanos向量当数据库证明。
- 最新公开链补验已完成：当前scoped intent/MICROS官方producer/公开confirm真实CAS、GxP、BpmTaskServiceImpl.approveTask与官方End组合两新增场景2/2PASS SOURCE_STABLE，仅执行新方法，不重复14。当前生产依赖实际PRESENT，G01生产未并写；完整reactor/有培训/platform与下发等范围仍待联合票。
- Root M04唯一票为4会签/引用类正式-am定向，G02不执行Maven/共享target/全类型/大构建；本轮无运行中私有验证句柄，不同时重跑旧14或新公开2。Root定向类结果不扩大为G02全项验收，编译阻断按精确owner接收。
- M04真实结束后继续新公开guard三方法；M05开始前已启动的run观察3PASS但SOURCE_DRIFT/script1，当前GREEN未成立，等待M05结束后只重验这三项。M05通知后没有再启动privateJava/Maven/target，源变更不当业务RED。
- M05实际结束/回收后，新guard只三方法public-guards-02=3PASS稳定；再新增必要培训消费两方法public-training-01=2PASS稳定。各scope各sourcehash分开，不把Root80或旧14重复为新阶段通过。
- M06 Root唯一八类-am同源正式联合，G02保持四源码不写/不跑privateJava。只读actualreceipt已结束exit1：signature旧11PASS，DCC testCompile被非G02授权DccW08PreparedCreationDatabaseTest缺TenantContextHolder导入阻断，G02目标tests未执行；等待Root精确owner修复和下一票，不当作业务RED/PASS。
- M06已回收，无本线程pending句柄；按Root要求只做pre-M07培训proof当前源checkpoint，46源与public-training-01全部相同、无漂移；两公共schema只读登记hash。未重跑旧14或培训2，不把来源一致当新业务PASS，正式M07仍Root唯一票。
- M07已正式启动同八类-am复验，G06缺import已实际存在；G02四源码保持稳定，不privateJava/Maven/sharedtarget/fulltype并行。按RootfreshXML/真exit记录结果，票结束即更新状态，不因通知保持旧冻结。
- Root已正式接收M07 G02软件授权范围：两类10+11=21全PASS0F/E/skip，四源码按g02-software-handback.json冻结交还；不再G02写入/重复私有验证，需Root重新分派才修。整票V6图path错由G01处理，不影响G02软件接收，不扩成真实ports/E2E或全12完成。
- M08同八类Root唯一票进行中，G01已修V6资源定位，G02四源码冻结保持；本线程只读回执不编译/不读半target当结论，不把M07本范围21PASS说成M08已完成。
- M08实际结束exit0/23reactorBUILD SUCCESS，八目标55PASS0F/E/skip，Root联合正式接收G01/G02软件范围；G02两类21、4源码0漂移。只读XML时间窗/hash核对归档完成，无新增构建或Java改动；其它3source动态变化不扩为全工程freeze/custom/附件/E2E。

## Baseline

- 核对日期：2026-10-14（Asia/Shanghai）。
- 实际分支：int_qms。
- HEAD：822c31fc0856516cf59ba9cd84bf2bc7f455d17f。
- 工作区已有大量并行改动；以当前读取源码及 UTF-8/LF SHA-256 指纹为证据，不以旧报告、HEAD 或历史 PASS 代替。
- 自有目录修改前不存在。源文件修改前指纹将保存到 baseline-fingerprints.json；本阶段源文件只读。

## Milestones

1. 核对 AGENTS、对应 docs 规则和本轮 requirements/coordination（完成）。
2. 追踪当前受控时间、最终生效、签名及轮次合同，复核相邻测试（完成，静态证据）。
3. 写出最小端到端合同、BDD/真实 RED 方案及文件接入清单（完成）。
4. 结构验证本阶段文档并提交 review-ready 报告，等待 G01 合同冻结和 Root 转交（本阶段交付完成；接入待授权）。
5. 转交后才实施 RED/GREEN/REGRESSION；本阶段不宣称修复或运行验收完成。
6. C01/C02 接续：复核 BPM actor 生成、签名时间源、preflush End 与晚失败，形成两个专属测试的可执行设计（完成；仅文档）。
7. A07：完整逐方法接口组合与当前可执行依赖核对（完成；真实 G01 Evidence/确认/V6 模型当前 ABSENT，字段/Rule 未接入；不造替身闭环）。
8. A13：合法V6/Bound/真实HMAC/实际Flowable建立，S!=T数据库RED及确认preflush End接入RED；两服务GREEN、两专属类14项私有回归（完成，证据范围见a13-implementation-evidence.md）。
9. 公开G01确认命令、真实platform/audit/下发/授权/有培训及精确legacy联合验证、正式-am票（待Root/G01接续，不冒称已验）。
10. 独立公开confirm producer→统一row→DCCprojection→CAS→真实GxP→真实BPM approve/End：两新场景2/2PASS（完成，无培训/本报告明确外部目录端口）；余有培训/platform/下发/正式-am待Root。
11. M07 Root正式current -am两G02 whole-class 21/21PASS、freshXML/时间窗/hash核验、Root接收软件范围（完成）。
12. 四源码冻结交还Root，工程Git未授权不completed；最终整体freeze/ports/运行验收按Root总任务，不由G02并行执行（已归档）。

## Expected Verification

- 实际分支核对和当前源码读取证据。
- UTF-8 文档结构、路径/锚点、BDD、授权边界、测试状态及指纹检查。
- 本轮私有真实RED/GREEN及回归已经执行，最终14/14PASS0skip；只证明报告所列内域消费/官方End组合，公开G01命令和完整reactor仍待联合票。不得复制历史 PASS。
- 接入后的定向 Maven 验证范围由 Root 发票；真实事务回滚必须真实隔离测试数据库和 Mapper，不用 mock 事务替代。

## Current Status

blocked — 工程Git收尾当轮未授权；M08八类55PASS/BUILD SUCCESS已Root联合正式接收G01/G02软件，G02自身21项通过，四源码冻结交还0漂移，无业务修复阻塞或pending验证。最终全工程freeze/真实ports/运行验收归Root，不标completed。

## 设计约束检查

- controlledTime、actualEffectiveTime、publishedTime、selectedSignedAt 是不同事实，不能互相替代。
- C02 新 DELAYED 与历史 PRESET_DATE 严格分开；所选 E 为 Asia/Shanghai 的 LocalDateTime/秒，实际 S 保留原精度；不按 LocalDate 比较，不截断 S，不由服务器默认时区补推。
- C05存储精度：新签名产生前的官方微秒tick选择属于G01 producer，G02不截断任何已产生S；G02不改Signature/DDL，现有fixture保留权威微秒S，公开producer与迁移实库未验边界明确。
- S 来自服务端签名领域事实，必须验真且绑定租户、申请、版本、流程、轮次、任务和原编制人。
- 保留正式组织/权限、版本与 Master 精确身份、事务、签名、幂等、重放及旧正式版保护；禁止 fallback、吞异常、伪成功或默认原因。
- G01 冻结字段、流程及签名合同；G02 不并写 G01 文件，不新增第二套 API 或状态机绕开归属。
- 第 8 项详细顺序尚未收到；仅确认“未有效签名不得受控”“受控且生效后下发”等明确约束，不猜新盖章/作废规则。
- Git 收尾未授权，不自动标工程 completed。

## BDD

- BDD G02-01：Given 延迟确认真实签名时间 S 与后续受控处理时间 T 不同；When 完成受控；Then controlledTime = S，后续处理不能覆盖 S。
- BDD G02-02：Given 缺失确认任务完成事实或签名；When 请求受控或调度生效；Then 明确拒绝且无受控/生效/盖章/下发推进。
- BDD G02-03：Given 伪造签名、篡改 signedAt、错误租户/版本/轮次/流程/任务或非原编制人；When 请求受控；Then 明确拒绝且零业务推进。
- BDD G02-04：Given 同一申请/版本已受控，S 已固定；When 在更晚时间重放同一动作；Then 回读原事实且不修改 S，不重复盖章/下发/签名。
- BDD G02-05：Given 确认签名合法但最终事务晚期子快照写入失败；When 完成受控；Then 主事务回滚、旧正式版和 Master 保持、无成功事件；修复依赖后重试仍取原 S。
- BDD G02-06：Given 选定未来生效时间 E >= S；When 已确认但当前时间早于 E；Then 不提前 ACTIVE/正式下发，最终生效时不将 controlledTime 改为调度时间。
- BDD G02-07：Given 文控审核后立即生效申请；When 真实文控签名后完成；Then 保持已确认的立即生效合同，不误用延迟确认节点。
- BDD G02-08：Given E=10:00:00 而真实 S=10:00:00.123456；When 确认或消费受控证据；Then E<S 被拒绝，不通过截断 S 放行；E=10:00:01 可通过，保存真实原精度 S。
- BDD G02-09：Given 当前正式producer/CAS/BPM/账本及合法原编制人任务；When 调公开confirm并重放同键；Then 唯一S=统一行=DCCprojection=confirmedAt=controlledTime，实际GxP/任务/End成立，重放零新增。
- BDD G02-10：Given 公开confirm已真实签名/CAS/审计并进入End；When 控制GxP INSERT定点晚失败；Then 同事务签名/确认/账本/任务整体回滚，原待办可公开confirm重试成功。
- BDD G02-11：Given 合法精确公开确认上下文；When 错轮次/定义/摘要/流程/任务、非原编制人、缺时间/秒精度非法/过去时间；Then 正式command拒绝且无producer复认证、统一/DCC写入、GxP或任务推进。
- BDD G02-12：Given 公开confirm已保存S/E；When 同键改变E重放；Then 载荷冲突拒绝、不重签且所有已保存事实保持。
- BDD G02-13：Given 必要培训的本版本本轮真实receiveTask未完成/实际完成且正式原上传人附件完整；When 调公开confirm；Then 前者没有确认任务且不受控，后者本轮确认后以真实S受控；错轮次培训记录仍拒绝且零签名/推进。

## Cleanup Keep

- doc/tasks/20261014-dcc-gap-controlled-signature-time/task.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/execution-log.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/verification-report.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/review-ready.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/changed-files.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/integration-notes.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/design.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/baseline-fingerprints.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py
- doc/tasks/20261014-dcc-gap-controlled-signature-time/c01-end-event-test-design.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/c01-followup-baseline.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a07-interface-composition.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a07-integration-baseline.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/check-integration-source.py
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a11-source-checkpoint.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a13-production-before.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a13-production.diff
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a13-changed-source-manifest.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/a13-implementation-evidence.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/PrivateJUnitRunner.java
- doc/tasks/20261014-dcc-gap-controlled-signature-time/run-private-junit.py
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-command-before.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-command-ready.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-command-verification.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-guards-before.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-guards-current.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-guards-verification.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-training-before.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-training-ready.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/public-training-verification.md
- doc/tasks/20261014-dcc-gap-controlled-signature-time/pre-m07-training-source.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/m07-formal-g02-evidence.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/g02-software-handback.json
- doc/tasks/20261014-dcc-gap-controlled-signature-time/m08-formal-joint-evidence.json
