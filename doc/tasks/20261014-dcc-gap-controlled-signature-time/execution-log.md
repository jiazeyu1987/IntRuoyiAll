# Execution Log — DCC G02

## 2026-10-14 第一阶段

- 收到 G02 DEPENDENCY_ONLY 分派；只写自有目录，共享生产/测试未授权。Root 主动读取报告，不自行发跨线程消息。
- 已读 AGENTS.md、docs/task-closeout-rules.md、docs/backend-development.md 的实施/验证及 DCC 签名时间、发布事务门禁，docs/powershell-memory.md 的分支/编排门禁、docs/powershell-encoding.md，以及本轮协调目录 requirements.md/coordination.md。
- 历史 DCC shared-contract/ownership/three-workflows 用于识别既有资产；业务目标及最新授权以 2026-10-14 分派为准。
- `git branch --show-current` -> exit 0，int_qms；`git rev-parse HEAD` -> exit 0，822c31fc0856516cf59ba9cd84bf2bc7f455d17f。
- `git status --short --branch` -> exit 0，已有大量共享改动。本线程不改动或提交并行资产。
- `Test-Path doc/tasks/20261014-dcc-gap-controlled-signature-time` -> False；随后建立 task.md/execution-log.md，预先记录目标、BDD、里程碑、验证及设计约束。
- 当前阶段不会运行 Maven 或业务测试；RED/GREEN/REGRESSION 的执行须等共享文件转交及 Root 验证票。

## 当前源码与相邻测试独立复核

- 直接写入点：DccControlledFileLifecycleService.completeControl 取 datePolicy.now()，写 controlledTime(now)、publishedTime(now)，只对立即方式核验文控签名；PRESET_DATE 无新延迟确认门禁。
- 追踪正式 Workflow 文控签名、Finalization End/历史任务/冻结签名、派生盖章件和 binding、Lifecycle 控制及调度、旧正式版/Master/outbox/下发批次。
- 复核 Finalization.requirePublicEnd/requireNativeFinalizationActors/requireRetryApprovalEvidence/requirePublicCompletedReplay，目前最后审批来源依赖文控签名人；G01 确认节点不能仅改 Lifecycle 时间就接通。
- 读取相邻真实 H2/Mapper/Spring tx、Flowable End、HMAC、audit/platform/outbox 测试；明确记录 mock 外部端口及没有本轮重跑。完整证据和测试名见 design.md 第 6 节。
- 读取 G01 当前 task.md/execution-log.md：仍 SPEC_ONLY，拟 DELAYED 尚未成为冻结合同；不根据该设计写共享源码。
- 精确转交请求：Lifecycle/Finalization 两生产文件与 DccG02ControlledSignatureTimeTransactionTest、DccG02DelayedFinalizationTransactionTest 两专属新测试；公共 DatePolicy/DO/VO/API/图/审计等仍需 Root/G01 接入，详情见 integration-notes.md。

## 本阶段实际验证

- 指纹采集首次 -> exit 1，因本线程登记的测试锚点拼写 batchAcceptStorageFailureRollsActivation... 不匹配真实方法 batchAcceptStorageFailureRollsBackActivation...；此为文档锚点校验错误，不是生产行为 RED。修正自有登记后重跑。
- 指纹采集重跑 -> exit 0，29 个当前源文件/测试 UTF-8/LF SHA-256 与方法行号保存到 baseline-fingerprints.json；没有生产文件写入。
- `python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py` -> exit 0，PASS：7 份 UTF-8 文档、7 条 BDD、全部自有资产范围/Keep、29 个源路径/锚点/指纹。明确输出行为 RED/GREEN/REGRESSION NOT_RUN。
- 收尾前再次读取 docs/task-closeout-rules.md 适用条款，实际分支再次 int_qms；任务仍需开发接入，不进入 ready_for_closeout/cleanup apply，也不以当轮 Git 禁令为理由冒称 completed。
- `git status --short --untracked-files=all -- doc/tasks/20261014-dcc-gap-controlled-signature-time` -> exit 0，9 个自有新增资产可见，verify-design.py 未被忽略；未暂存、提交或推送。
- 当前 coordination G02 仍 DEPENDENCY_ONLY；A01 新授权只涉及 G12，本线程不借用。

## 交付状态

第一阶段全部自有交付可供 Root 主动 review。工程 in_progress，等待 G01 合同/具体文件转交/唯一 Maven 票；没有共享文件需要归还。不得把本阶段结构 PASS 写成业务 PASS。

## C01 接续：授权与本轮目标

- Root 已 review 首阶段。已冻结 DELAYED / LocalDateTime / YYYY-MM-DD HH:mm:ss / Asia/Shanghai / 可信签名同秒精度，历史 PRESET_DATE 不变。
- 本轮仅继续实际受控处理、End actor 与签名/事件时序复核，以及两类独立新增测试设计；M01 禁止 Java/测试写入、Maven 和历史测试重跑。不新增平行 resolver。
- 再读 AGENTS.md、docs/task-closeout-rules.md、backend-development/powershell-encoding 适用条款和最新 coordination 的 C01/M01；`git branch --show-current` -> int_qms，`git rev-parse HEAD` -> 822c31fc0856516cf59ba9cd84bf2bc7f455d17f，exit 0。
- 修改前已有自有资产 hash 与新读取时序源码指纹将记录 c01-followup-baseline.json，原 baseline-fingerprints.json 保留首阶段证据。新文档 c01-end-event-test-design.md 本轮创建前 ABSENT。
- 读到 G01 新 contract.md：除 C01 已确认部分外，stage/status/meaning/六列/payload/图仍为提请冻结；将消费其唯一 ConfirmationEvidence，不复制验真器。

## C02 / M02 最新同步及实际验证边界

- C02 已冻结 V2/V6、确认 stage/status/meaning、四文件投影和签名 selectedEffectiveTime/attributeRound/new payload；E 秒精度而 S 原精度，E>=S 不截断。两生产文件/两专属测试已明确授 G02，G01 不写它们。
- M01 回收，M02 GREEN/回归授 G12；Root 最新消息明确 G02 BE 继续冻结，不能因 C02 授写权或 M01 回收自动开始修改。
- 唯一 DccDelayedEffectiveConfirmationEvidence.java / VerifiedConfirmation(selectedEffectiveTime,signedAt,signatureId,processInstanceId,taskId,attributeRound) 已获 Root 核准；当前 End/真实历史完成由 G01验真，G02 仅消费同一证据/Bound。
- 本轮读实际 BPM approve->LAST_APPROVER->complete->PROCESS_COMPLETED->ProcessInstance convert->DCC listener->Finalization->Lifecycle 链；training trigger 不重写 LAST_APPROVER；新确认 End 来源为 R，立即培训路径 End=D 而当前操作者=R。精确源锚点和两类测试设计已记录 c01-end-event-test-design.md。
- 当前 DCC 签名时间在统一 sign 前取整秒，统一 sign 再取原精度时间，业务 datePolicy 使用配置 zone；已把时间源/新 payload subject adapter 的接入风险明确列为 G01 依赖，无并行修改或强行截断 S。
- c01-followup-baseline.json -> 实际保存 10 个已有自有资产 C02 文档更改前 hash + 18 个只读时序源码/模型 hash；原 29 源指纹未覆盖。
- 初次接续结构校验 -> exit 1，本线程 marker 写成“真实 S 保持”，正文为“实际 S 保持”；这是文档检查锚点错误，已更正，非业务 RED。未改变生产/Java测试使检查通过。
- 几次定位使用了猜测的 BPM convert/processinstance 与 system/signature 路径，rg 报缺路径；随后用 rg --files 找到实际 convert/task 和 yudao-module-signature 再读取。不得把该读路径失误当产品/依赖缺失或测试结果。
- 本轮未运行 Maven、旧历史行为测试、服务/E2E/实库/Git写入；没有新 Java 测试文件。当前仅设计/文档验证可交付，等待 M02 正式释放及 G01 可执行方法。
- 接续结构校验重跑 `python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/verify-design.py` -> exit 0，PASS：8 份 UTF-8 文档、8 个 BDD、自有资产范围/Keep、29+18 源路径/锚点/指纹；行为测试 NOT_RUN。后端授写资产仍未动，接续设计交付 ready for review。

## 2026-10-15 A07 释放与完整接口组合

- 收到 A07：M02 回收、BE 冻结释放，仅两 G02 服务+两专属测试 WRITE_GRANTED。Root 允许真实类未创建时先接口组合设计；私有 javac/JUnit 允许但不写共享 target，正式 Maven 仍无票。
- 再读 AGENTS/task-closeout/backend/powershell-encoding/database 和适用 private验证/旧jar条款及最新 coordination/contract；本轮实际分支 int_qms，HEAD 822c31fc0856516cf59ba9cd84bf2bc7f455d17f。
- `Test-Path` 真实 G01 Evidence/确认服务/V6 upload 图 -> False；两 G02 新测试 -> False。全新字段/DELAYED_CONFIRMATION Rule/current Bound V6 尚未进入生产源。以实际 source 为准，不基于旧设计结论。
- a07-integration-baseline.json 实际记录 11 个已有自有资产改前 hash、16 个源/测试/依赖路径，6 ABSENT（包含两未创建自有测试）；没有写共享生产。
- 完成 a07-interface-composition.md：逐方法 Lifecycle/Finalization 接线、Evidence 五消费边界、S/E/T/原精度、同日未来到期查询、status/actor/current-End/history/retry、真实首批 RED 可达条件以及私有当前源验证清单。
- 发现共享依赖：DccWorkflowFileStateAudit.recordActivation 当前 DELAYED 报方式不识别；FrozenApprovalSignatures exact contract 当前仅V1；DO/Rule/图尚无新合同。准确列给 Root/G01 主动读取，不绕过审计或并写共享文件。
- private java/javac 指向仓库 .runtime/tools/jdk-17/bin；仅定位未执行。无 Maven、编译、JUnit、服务/E2E/实库/Git写入；没有伪造平行 Evidence 或旧jar“闭环”。
- 本轮生产/Java测试尚未修改，正在交完整接口组合；真实依赖接入后在已授四文件内直接进入可达 RED/GREEN，无需再请写入许可。
- A07 首次结构检查 -> exit 1，“真实 S”字面 marker 与正文“原精度”不匹配；修正自有 marker后重跑 -> exit 0，PASS：9 UTF-8 文档/8 BDD/资产范围Keep/29+18源锚点指纹/A07状态记录。非生产 RED。
- 交付前再次 Test-Path Evidence/确认服务 -> False，仍未创建；13 个自有资产 git status 可见，无暂存提交。完整接口组合交付可 review；当前 in_progress/WRITE_GRANTED，剩余为真实 G01 可执行依赖，不重复索取已有授权。

## 2026-10-15 A11 实际源边界

- Root A11已经把FrozenApprovalSignatures V2四审批、ContentAdapter DELAYED activation reason、FileStateAudit确认/activation授G01，G02范围不扩大。A07释放保持有效，不重问旧M02冻结。
- 再核 int_qms/HEAD exit0；独立只读源码显示：FileDO四投影、SignatureDO selectedEffectiveTime/attributeRound、Rule DELAYED_CONFIRMATION_MODE已出现，DatePolicy.now保留精度且新增DELAYED提交与E>=S校验。当前existing Bound trainingAfterDocumentControl与catalog/resolver仍旧V5，signature HMAC链尚未新payload。
- 唯一Evidence/真实ConfirmationService/V6 upload+revision XML仍ABSENT，没有callable方法可消费；不创建mockService/DTO/resolver或以callerBoolean证明完成。
- 新增源可用性checkpoint脚本，只自有目录，记录16个源当前hash/存在状态及Evidence出现后的public声明供review；不是行为测试，不推断实现PASS。
- `python -X utf8 doc/tasks/20261014-dcc-gap-controlled-signature-time/check-integration-source.py a11-source-checkpoint.json` -> exit0，4登记当前源相对A07变化，4 G01依赖ABSENT。不覆盖旧baseline，不将正常G01源变化记成产品失败。
- 本轮未重复历史测试、未重复整套设计，生产/Java测试依然未写，编译/RED/GREEN未跑；当前真实证据不足以建立合法S!=T可达fixture。Evidence可执行后直接接入既有已授路径。
- 自有文档结构verify-design.py -> exit0，9 UTF-8文档/8BDD/资产Keep/29+18历史源指纹及16A11当前记录，G02两生产文件未变。更新validator只把历史指纹视为历史来源，避免将G01合法写入误判为产品FAIL或把旧hash称为当前源码一致。

## 2026-10-15 A13 实际TDD

- 本turn中检测到真实Evidence出现，马上读取requireConfirmed(file[,bound])和8字段record（Root明确核准），以及逐步出现的当前V6图/catalog/Bound/Rule/新payload/HMAC，不停止于旧ABSENT记录。
- 保存a13-production-before.json改前SHA；仅修改授予Lifecycle/Finalization，新增授予两测试。没有共同resolver/Evidence写入，没有SignatureDO不存在tenant字段。
- 首个私有编译PowerShell预清理含recursiveRemove-Item被工具策略拒绝，未执行；改为Python只建freshrun，无删共享路径。初次Python javac默认诊断编码无法UTF-8解码，显式-J-Dfile.encoding=UTF-8后编译结果可读；4测试重载错误/空HttpServletRequest等fixture失败单独保存，不称RED。
- 实际RED/GREEN各run表在a13-implementation-evidence.md：a13-red-time4证明真实库写T而非S；a13-finalization-red7证明真实官方End/preflush阶段拒绝；a13-green-time5为7PASS；当前V2 Frozen/ExecutionContract等source编译替代旧target后a13-current-source9=9PASS；最终a13-final14=14PASS0skip，compile0/JUnit0/SOURCE_STABLE（JUnit285790ms），含最新同步retry新S2断言。
- 本轮source manifest显式21关键DCC当前源；仍有既有编译模块接口/外部端口，因此不能声称完整当前source reactor。外部账户/image/storage/platform/audit/distribution端口替身明确；G01公开命令/真实审计与全部培训/legacy门禁未跑。Maven无票且未执行。
- a13-production.diff生成前核Root source-baseline两文件规范化SHA等于G02首次写入前SHA，确保只包含G02差异；a13-changed-source-manifest.json登记四授权文件最终hash，生产原有dirty不能混为本任务改动。
- 四文件实现可review，任务in_progress，未归还为全项完成；等待Rootreview/唯一联合验证票，不继续无关扩展，不提交推送或启动运行验收。
- C05Root新合同已读取coordination：官方adapter新延迟选择MICROS tickclock、先S后签名证据，signed_at/control DATETIME6guard无历史UPDATE，由G01独占。当前组合fixture已经微秒S/TIMESTAMP6、E秒，不需签后truncate或改共享Signature/DDL；官方producer API/统一result roundtrip未到位，报告追加联合依赖，不冒称14项覆盖该新门禁。
- 实际收尾前重读task-closeout适用条款、核分支int_qms，G02两服务diff-check exit0。verify-design.py exit0：10UTF-8文档/8BDD/资产Keep/四source最终hash/真实private14 exit/result对账。任务仍in_progress，尚有明确联合门禁，不执行cleanup apply/Git或标completed。

## 公开confirm/官方producer/真实账本/BPM补验

- Root恢复原turn后继续同一task和两test；读取当前公开confirm/officialproducer/subjectscope和G01producerReady，checkpoint public-command-before.json记录实际PRESENT生产7+测试2的改前hash，不复制G01producer2PASS或旧14为新链。
- 在授予两tests内新增nestedPublicCommandChain/两个publicCommand方法，private runner支持精确#method，仅执行新两项。公共Confirm/Signature/CAS/接口/Schema/G05/MainFile24未修改；两生产service保持A13相同hash。
- 当前46编译source涵盖official新接口/hook/scopedintent/producer/CAS/BPM/guard/officialEnd/GxP/正式DefaultDBFieldHandler，privateclasses优先；源hashstable。共享M03target重建后的missingclasses/FixedStageDefinition/lombok连锁仅编译问题，补必要当前source，不认为产品invalid或fullreactorPASS。
- 初次漏EvidenceService新接口(public01)记录compilefail；H2support COMMENT/UNIQUE KEY/HEX与旧fixturecapacity256 vs正式2048(public02..06)、正式insertfill未配置(public10)均精确repair任务自有fixture，保留全部失败。真实SQL mapper capture只diagnostic后重抛，不跳真实GxP。未改全局H2/数据库fallback/约束义务或sharedschema。
- 最新public-command-11实际compile0/JUnit0 2/2PASS0skip SOURCE_STABLE。publicconfirm成功true统一row+DCCprojection+四投影+controlTime同官方microS，actualtwoverifiersVALID、CAS/GxP/engineAPPROVE/Endactor99、同键重放onlyread。真实controlGxP CHECK晚失败已在officialEnd后，真实签名/CAS/账本/task/core全回滚，公开命令retry成功。
- actualschema容量依据20260908_gxp_audit_trail_core.sql subject2048/key512，只任务私有数据库等价；新DefaultDBFieldHandler真实填createTime，未nullable绕NOTNULL。producer/password/account/image/storage作为明确外部ports，GxP真实，不声称培训/platform/distribution/H2==MySQL运行验收。
- RootM03只读receipt：正式reactor诊断exit1，Root已回收无活跃Maven；没有本线程Maven或target写入，诊断半compiledDCC不能替代currentsource，未假BUSRED。
- public-command-ready.json登记4授权文件当前hash/46源/两method/actualexit及schema交付checkpointhash；源stable只按Java前后hash，不把共享schema称全轮freeze。所有生产stateG02仍in_progress，待完整联合验收不completed。
- Root看到较旧public10的create_time23502诊断已及时说明：实际public11已修正式DefaultDBFieldHandler而非nullable绕，真实两新场景已PASS。未重复跑原14，不将G01producer2PASS充整个链；保留public01/02/03/04/05/06/07/08/09/10所有混合失败分类。
- 交付前再次读task-closeout、核分支int_qms、两test diff-check exit0；verify-design实际exit0，11UTF-8文档/10BDD/Keep/4授权当前hash/原14与公开2各自真实结果provenance。本stage报告可review，任务in_progress；无Maven/Git/E2E/服务器/数据库写入或cleanupapply。
- RootM04通知已接收：四会签/引用定向-am及currentmain/testCompile由Root唯一执行，G02没有启动Maven/私有javac/JUnit/sharedtarget/fulltype并行；没有未收的本线程运行句柄。当前源保持完整可编译方法，无空文件或stub。
- 本轮仅只读核coordinationM04、分支int_qms与公开2已有真实exit0/sourceStable，保留精确源验证边界；不将M03/M04半compiledclasses缺失视作业务RED，不复制Root其它4类结果作G02全体完成。
- 只读M04实际receipt已经ended/exit1（09:41:55+08:00），log首个testCompile错误为SignatureGovernancePolicyDccAdapterTest的CapturingDccSignatureVerificationService未实现新接口方法。非G02授权两test，目标4类未业务执行；不越界改该fixture，不称G02业务RED/PASS。由Root精确转当前owner处理。
- Root明确M04终结、正常私有测试可续后，在已授finalizationtest新增三项公开guard；BDD先补11/12并采改前hash。public-guards-01编译0/JUnit0观察3PASS，但source变化script1，不能记当前GREEN。M05通知到来时该run已启动、随即收完，不再启动新run/Maven/sharedtarget。
- 退出后核源：DccApplicationExecutionContractService from51168ac5...to cbb7ffb9...，checked-after-exit-drift.json保存完整hash及“不是精确runtime时刻”边界；runner后续source-drift.json保存真实检测stage/beforeafter。当前guard复验pending，原稳定公开2不混为新guardPASS。
- 随后只读M05receipt真实已ended/exit0（Root后续通知38484回收），恢复已授私有验证；public-guards-02仅三guard方法，actualcompile0/JUnit0 3PASS0skip SOURCE_STABLE。没有重复原14/公开2，首轮漂移保留，更新准确current状态。
- 再预录BDD13并采两test改前public-training-before，新增nested needTraining=true fixture与两方法（非新增support文件），实际TRAINING receive等待→mapper持久原上传人本轮record+hashbytes→真实trigger形成confirm。公开confirm正向producer/CAS/GxP/BPM/End受控S；foreignround训练记录负向签前拒，task/audit/core不推进。
- public-training-01仅两新增方法，actualcompile0/JUnit0 2PASS0skip SOURCE_STABLE。realcompletedtraininginput消费成立，但不声明训练上传票据/publicworkflow命令已验；账户/image/storage/platform/distribution外部ports边界继续明确。
- public-training-ready记录当前4授权hash/46源/2method/actualexit；生产两service仍A13无变化，G01/sharedschema未并写。RootM05四类80PASS只own范围，非本任务全体pass；G02工程仍in_progress。
- 交付前再核int_qms、两test diff-check0、task-closeout规则；verify-design actualexit0，13文档/13BDD/4currenthash/Keep及原14-公开2-guard3-training2各scope结果、漂移历史对账。无运行中本线程句柄、无Maven/fulltype/sharedtarget/Git/实库/E2E。
- M06通知后保持两service/两tests源码稳定，没有新privateJava/Maven/sharedtarget/fulltype；只读receipt实际10:29:24+08 ended/exit1及freshlog。旧signature ElectronicSignatureServiceImplTest实际11/11PASS0F/E/skip；DCC testCompile首缺DccW08PreparedCreationDatabaseTest.java:407 TenantContextHolder导入，G02八类中的自身tests未业务执行。
- 缺导入文件在G06已授旧检入fixture范围，G02不越界修复/删测试/stub。M06不作为G02新业务RED或PASS；记录正式G02-am依然待运行，私有各限定scope结果不冒充该票通过。
- Root确认M06回收/无activeMaven后，本轮仅pre-M07来源复核，未重复旧14或培训2。pre-m07-training-source.json当前46编译源与public-training-01全部一致0漂移；两公共schema当前hash只读保存，无sharedschema改动。
- 实际分支int_qms、read对应规则；G01公开confirm仍本轮真实历史训练查询/producer/CAS/BPM代码，未退回旧ABSENT。当前没有本线程pending验证句柄或新增Java写入，四源码保持，正式联合M07由Root执行，来源一致不记新业务PASS。
- M07Root同八类-am已启动，本轮只读核importTenantContextHolder已存在、分支int_qms与日志DCCmain当前1300源编译阶段；M07receipt尚未结束。G02不并行Maven/privateJava/sharedtarget/fulltype或写四验证源码，不复制M06旧signature11或私有count替代M07目标freshXML。
- 实际M07已ended/exit1；只读freshXML时间窗核验，两G02 wholeclass10+11=21全PASS/0F/E/skip，Root正式确认软件授权范围接受。signature旧11PASS、DCC44中43PASS1ERROR，唯一V6图test路径Backend/Backend/config由G01负责；不越界改，不把整票exit1说G02failed。
- m07-formal-g02-evidence.json保存真实Rootreceipt、XMLhash/mtime/方法与counts、4sourcehash；生成前逐项核mtime在本轮实际起止窗口、4source同public-training-ready，无漂移。Root已稳定归档m07-junit两XML。
- g02-software-handback.json正式交还冻结4source，Root接收；无pending本线程验证句柄，不再private14/2/3/2重复或Java写入。后续只有Root重新分派才修，M08/最终freeze由Root统一。
- 再读task-closeout-rules“Git禁令实现验证完成记blocked”条款，本task工程状态blocked仅Git收尾未授权，软件已通过非业务故障；不引入许可问题、不Git、不completed，cleanupapply本阶段未执行。review-ready重写为当前接收/正式21/归还/边界，去掉旧等待误导。
- M08通知已接收，Root同八类-am正式复验，G01资源路径修复current source已Root核。G02无新privateJava/Maven/sharedtarget/fulltype/源码写入；只读日志当前541测试源testCompile，receipt尚未结束，保留pending而不伪PASS。四源码冻结交还不解除。
- 随后Root正式确认M08真实exit0/67秒/23reactorBUILD SUCCESS/8class55PASS0F/E/skip，M08已回收；不再保持旧等待。G02两类10+11=21仍全PASS，Root联合接收软件范围。
- 只读逐份8个Rootm08-junit归档核SHA等于原freshXML、原mtime在实际11:03:33—11:04:41+08窗口，suite合计55、各0F/E/skip；四G02冻结hash相同0漂移，全仓Root另3source动态变化如实记录。m08-formal-joint-evidence.json归档，handback更新latestM08，不将55扩大为custom/附件/全12/最终freeze/realports/E2E。
- 本轮没有Java/private重跑、Maven/sharedtarget/fulltype/source写入，工程Git未授权不completed；Root后续整体任务独立继续。四源码冻结交还保持。
- 最终verify-design实际exit0，13文档/13BDD/4frozenhash/M08八XML55-G02自身21归档/零F/E/skip对账通过；首次新状态文案marker不含旧词导致文档检查exit1，更正marker后通过，非业务失败。handoff当前formal_ticket=M08/wholeexit0、原M07exit1以history保存，消除旧pending/失败误读。
