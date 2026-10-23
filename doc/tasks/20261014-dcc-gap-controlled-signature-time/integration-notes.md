# G02 接入与精确转交请求

## A07 当前生效状态

A07 已释放 BE 冻结，下面两生产文件/两专属测试当前 WRITE_GRANTED，不再重复请求授权。Maven 仍无票；实际 G01 Evidence/确认服务/V6 图仍 ABSENT，新 DO 字段/Rule 尚未接入。本轮完整逐方法接口组合、唯一 Evidence 消费矩阵及两窗口接入在 a07-interface-composition.md；历史 M01/M02 冻结文字保留为过程记录，不是当前阻塞。

当前仍只修改自有文档。C02 已明确授下面两生产文件及两专属测试，处于 WRITE_GRANTED_FROZEN，M01 释放前不写 BE；其他共享文件仍只是依赖，不构成写授权。以当前源码方法锚点为准，规范化指纹见 baseline-fingerprints.json 和 c01-followup-baseline.json；G01 方法签名/可执行合同由 Root 主动同步，Maven 仍须唯一票。

最新接续覆盖上述票号：M01 已回收，M02 GREEN/回归期间继续冻结 G02 BE。唯一 DccDelayedEffectiveConfirmationEvidence.java 与六字段 VerifiedConfirmation 已核准；M02 正式释放后只消费实际 G01 callable 合同，不另 resolver/DTO。

## 请求下一阶段由 G02 接收的生产文件（C02 已授，M01 冻结中）

|完整仓库相对路径|方法锚点|必要最小差异|验收影响|
|---|---|---|---|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileLifecycleService.java|completeControl / activateDue / activateLocked / dueVersions|消费唯一 G01 确认合同；新延迟先验正式签名/完成/身份并写 controlledTime=S；publishedTime/事件处理时间独立；精确 E 到期前不得 ACTIVE/下发；重放不改 S|S!=T、无/错/伪确认拒绝、同日未来时刻、回滚重试；保持 Master -> File 锁、旧版、立即及精确历史合同。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java|finalizeOrdinaryApproval / finalizeRevision / requirePublicEnd / requireCompletedNativeTask / requireNativeFinalizationActors / requireRetryApprovalEvidence / requirePublicCompletedReplay|按冻结新图和模式接入确认 stage/status、最终 End actor 与真实命令人；未确认在盖章前拒绝；重试/重放核验同一确认；文控及培训证据继续完整|真实确认 End 可走到受控且未签不能走到盖章；不把文控权限强加给合法编制人、不放宽技术重试。|

## 请求的专属新测试文件（C02 已授，M01 冻结中，尚未创建）

- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccG02ControlledSignatureTimeTransactionTest.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccG02DelayedFinalizationTransactionTest.java

若需抽测试支持，仅新增 G02 自有支持文件并先登记路径；不改公共 BaseDbUnitTest、application-unit-test、pom.xml 或共享 sql fixture。

## 仍由 G01/Root 处理或显式转交的共享依赖

|完整仓库相对路径|锚点/必要差异|G02 所需验收结果|
|---|---|---|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java|effectiveDate/effectiveMode/effectiveTime 与新确认字段；区分 E、S、实际生效及确认引用|正式精确时刻/签名/task/process/申请轮次可以读取且不可串版。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationEffectiveSelection.java|isComplete/requireSubmission；延迟上传可空 E|同一正式生效选择合同，不创建 G02 平行模式。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowDatePolicy.java|requireEffectiveConfiguration/requireSubmittedEffectiveConfiguration/now；确认 E>=S 精确时间|G01 冻结后复用，日期与精确时刻不得混算。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLifecycleEffectiveContract.java|Rule 与 exact identity|新延迟图有正式可区分语义，不按模式文本推历史。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLifecycleEffectiveContractResolver.java|resolve|新图经唯一 Bound resolver 正式识别，保留 legacy。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccBoundWorkflowContractResolver.java|resolve / BoundExecution|具体定义、deployment/digest、属性轮次与 G01 新图完整，禁止另建 resolver。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowDefinitionCatalog.java|FlowRule / 正式图登记|只新增合法冻结新图，不重写旧图或历史申请。具体新 BPMN/迁移路径由 G01/Root 登记，G02 不臆造路径。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java|approveTaskWithProcessDefinitionKey / G01 新确认命令与提交、重提|完整真实确认动作、原编制人资格、签名及字段绑定、事务完成/回滚/重放。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccFrozenApprovalSignatures.java|requireComplete|原全部冻结审批证据保持；新确认唯一义务在新图中正式消费，不能因新增节点跳过原部门义务。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccSignatureVerificationServiceImpl.java|resolveMeaningCode / createSignature|G01 正式 meaning/action 及 E 载荷绑定，S 同一正式服务端时间。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowFileStateAudit.java|snapshot / recordControl / recordActivation|新确认字段纳入真实快照；不把延迟确认当文控 standardization-effective；若需新 operationId，Root/G01 同步正式策略与定向回归。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledContentAdapter.java|recordNativeActivation|新延迟的审计 reason 表达正确，保持既有平台正式转换和版本身份。|

Controller/VO、前端公共 workflow.ts/applicationRead.ts、上传/详情父页、迁移与公共 fixture 仍未授权。本阶段不请求整体后端独占；只请求上面两生产文件和两专属测试的限定转交。若合同需要以上依赖由 G02 接入，Root 应逐路径转交后才实施。

## 接入顺序与必须冻结的决定

1. G01/Root 冻结新模式、精确 E 类型/时区/精度、确认字段名称、stage/status/meaning、正式 definition digest 和原编制人来源。
2. 冻结同事务 End 或已提交确认后的独立处理窗口、最终 event actor/operation actor 以及重试资格。对无培训/有培训/立即/延迟分别明确。
3. Root 复核上述共享接入后转交 G02 Lifecycle/Finalization；G02 重采 hash 并写 RED 可达 fixture。
4. Root 唯一 Maven 验证票指定 RED/GREEN/REGRESSION 时段、来源及范围；回收票后交还文件。
5. 不改变未知第 8 项规则；Root 需在总体 review 明确实际生效/盖章/下发序列与已收到需求一致。

## C02 接续后的剩余合同与实现顺序

- stage/status/meaning/四文件投影/签名两字段/V2/V6/new payload 已批准；同秒精度旧文字不再作为 S 精度依据，实际签名不截断。
- 本线程只消费 G01 唯一 ConfirmationEvidence 服务器验证方法。其参数需能携带本轮精确 bound 与文件身份，其返回证据需支持 S/E/signatureId/process/task/attributeRound 和已验证原编制人来源；具体签名由 Root 同步，不自行实现重名验证器。
- 同步 End：事件 actor=原 R、当前命令 actor=R，历史任务可能处于 preflush；真实已完成确认任务须由唯一证据方法识别。已提交重放/技术重试须走 completed-history 分支，不能以当前活动 task 代替。
- 当前 DCC createSignature 在统一 sign 前取整秒时间，而统一 sign 另取含小数时间；G01 须按 C02 收口确认真实 S/可信来源与 HMAC/schema，G02 不改全局签名模块。新 v5 payload 的 subject adapter 必须仍绑定 process/确认 stage。
- Root M01 释放后先核对可执行依赖和源 hash，再写两专属 Java 测试；获得唯一票后执行到真实受控/End 的 RED。测试设计细节在 c01-end-event-test-design.md。

A07 已满足源码写入释放；当前 G01 需同步真实 callable 方法和当前源依赖，G02 不自行声明虚构 Java 方法。逐方法已准备，字段/新图/source尚未可执行时编译失败不得记 RED；可私有 javac/JUnit，必须编译本轮受影响源、无共享 target。正式 Maven 仍按 Root 唯一票。
