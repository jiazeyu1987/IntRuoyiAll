# G01 精确接入与文件转交请求

仅为请求清单，不构成当前写权限。当前授权仍仅自有 doc/tasks/20261014-dcc-gap-delayed-effective/。Root 请按下面分组顺序登记 owner；文件不同锚点也不并行写。

## 路径前缀

- DCC = IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/
- BPM = IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/
- FE = IntRuoyiFronted/src/views/dcc/controlled-file/
- API = IntRuoyiFronted/src/api/dcc/controlledFile/
- JT = IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/

## A：核心合同/提交接入（Root 核准后 G01）

| 文件精确路径（展开前缀） | 锚点与最小差异 | 依赖/不接入的验收影响 |
|---|---|---|
| DCC/service/file/DccApplicationEffectiveSelection.java | isComplete/requireSubmission：按显式新定义合同允许 DELAYED/null；旧 PRESET_DATE 只保留历史正式合同路径 | 公共 VO、revision 依赖；否则无日期仍拒绝 |
| DCC/service/file/DccWorkflowDatePolicy.java | 增加提交意图/延迟确认 datetime 校验，严格分新旧契约 | G01→G02 顺序；否则只能按天或误拒旧轮 |
| DCC/service/file/DccControlledFileWorkflowService.java | 声明正式确认 read/command 服务边界或只代理专属新 service | Root 统一接口；不能平行旁路 |
| DCC/service/file/DccControlledFileWorkflowServiceImpl.java | createApprovalProcess、提交校验/幂等 hash、syncStatusAfterApprove、uploadTrainingRecord；从绑定新合同产生 frozen mode/requester map、确认状态、正式委托 | G03/G06 后续；否则模型不消费/培训提前 End |
| DCC/service/file/DccWorkflowDefinitionCatalog.java | 添加新不可变模型 digest/FlowRule PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM；保留所有旧条目 | G02 共用；digest 要由真实模型序列化生成，不猜 |
| DCC/service/file/DccApplicationExecutionContractService.java | preparePublicPreview/requireOrdinaryPublic/继承准备：PUBLIC_MATRIX_NATIVE_V2 分支，显式历史→新申请过渡 | 不能把矩阵 derivationVersion 一并改成 V2 |
| DCC/service/file/DccBoundWorkflowContractResolver.java | resolve/roundFlow：识别新精确图/变量/原编制人，仍四审批来源；确认任务不是第五矩阵来源 | 保留历史、零 latest 推断 |
| DCC/service/file/DccLifecycleEffectiveContract.java | Rule 新增显式延迟确认合同 | 与 G02 冻结 |
| DCC/service/file/DccLifecycleEffectiveContractResolver.java | resolve：new graph→new Rule，禁止 V5/旧 null 当新延迟 | 与 G02 冻结 |
| DCC/enums/DccControlledFileStageCodeEnum.java | 新增 DELAYED_EFFECTIVE_CONFIRM | 阶段/签名身份不可借文控 |
| DCC/enums/DccControlledFileStatusEnum.java | 新增 PENDING_DELAYED_EFFECTIVE_CONFIRM 投影 | UI/待办/撤回锁同步 |
| DCC/service/file/DccOfflineTrainingRecordService.java | requireWaiting/canUpload：新图证明仍按文控后培训，不假完成 | 保留原上传人培训责任 |
| DCC/service/file/DccControlledFileRevisionServiceImpl.java | createRevision/校验与新候选复制：保留所选 DELAYED/null，清确认事实，按精确 definition transition 接入 | G06 后续 owner；否则升版/驳回沿旧定义 |
| DCC/controller/admin/file/vo/DccControlledFileSubmitReqVO.java | isEffectiveSelectionValid：新申请 mode/null；同步精确提示 | 公共 VO Root 保留 |
| DCC/controller/admin/file/vo/DccControlledFileSubmitIterationReqVO.java | isEffectiveSelectionValid：同上 | 初始草稿/升版无时间可送审 |
| DCC/controller/admin/file/vo/DccWorkingApplicationSaveReqVO.java | 草稿形状验证：保存模式，不接受上传阶段确认字段 | 不默认值、不覆盖实际值 |
| DCC/controller/admin/file/DccControlledFileWorkflowController.java | 新正式 delayed-effective-confirm GET/POST，原权限及精确身份复核 | Root 授权后才能 API 接线 |
| DCC/controller/admin/file/vo/DccControlledFileDelayedEffectiveConfirmReqVO.java（新增） | 精确 task/process/attributeRound/definition/digest/time/password/reason/幂等键 | 禁止客户端签名事实 |
| DCC/controller/admin/file/vo/DccControlledFileDelayedEffectiveConfirmRespVO.java（新增） | 服务端 now/zone、精确任务/文件/轮次与已提交事实 | 前端只消费正式 decoder |
| DCC/service/file/DccDelayedEffectiveConfirmationService.java（新增） | 唯一确认资格、真实签名、CAS、审计与 BPM 同事务命令 | 本文件新增也须 Root WRITE_GRANTED |
| DCC/service/file/DccDelayedEffectiveConfirmationEvidence.java（新增） | 新确认签名/所选时间/轮次/任务的唯一只读证据验证器，返回 VerifiedConfirmation | G02 消费，不另造 lifecycle 平行验签 |
| DCC/dal/dataobject/file/DccControlledFileDO.java | 增加 design.md 四个 nullable 投影 | 不能借 effectiveTime 表达未来所选时间 |
| DCC/controller/admin/file/vo/DccControlledFileRespVO.java | 四投影只读字段/正式权限与办理投影 | G07 Query 真正映射需交 owner |
| API/workflow.ts | DccEffectiveMode、新执行合同、确认 GET/POST 类型与 wrapper、raw date 联合类型 | 当前 Root 保留 |
| API/applicationRead.ts | new flowRule/contract/mode strict decoder/保存合同 | G11/G06 也依赖，单 owner |
| IntRuoyiBackend/config/dcc-workflow-v6/upload.bpmn20.xml（新增） | 四审批+training+effective-mode gateway+真实确认节点 | 不改 v5 文件 |
| IntRuoyiBackend/config/dcc-workflow-v6/revision.bpmn20.xml（新增） | 同上对应升版 key | 作废模型不改 |
| IntRuoyiBackend/sql/mysql/20261014_dcc_delayed_effective_confirmation.sql（新增） | 文件四列、签名两列，nullable/no default/no backfill，严格 schema 漂移检查/迁移 metadata | 迁移执行未授权；只开发文件需 Root 批准 |

## B：签名合同（Root 转交后 G01；G02 消费）

| 文件 | 锚点与最小差异 | 验收影响 |
|---|---|---|
| DCC/service/file/DccSignatureVerificationService.java | 新延迟签名命令，接受已验证精确 selected time/attribute round | 不能用通用 APPROVE 密码签名忽略业务选择 |
| DCC/service/file/DccSignatureVerificationServiceImpl.java | resolveMeaningCode/label、createSignature/persist：新 meaning、新 payload，真实 selected time/round 入签名快照 | 不吞签名失败；旧 signatureTime 不改 |
| DCC/service/file/DccControlledFileSignatureEvidenceCreateReq.java | 两个字段 | 严格不能伪造签名时间 |
| DCC/service/file/DccControlledFileSignatureEvidence.java | 新 evidence snapshot 字段 | 统一 subjectAdapter 消费 canonical 不绕过 |
| DCC/service/file/DccControlledFileSignatureEvidenceServiceImpl.java | 新 payload 分支、canonical selected time/round；旧 v4 字节不改 | 所选时间篡改应 INVALID |
| DCC/dal/dataobject/file/DccControlledFileSignatureDO.java | 两个新 nullable 不可变签名字段 | 验签不能取可变文件值 |
| DCC/service/file/DccElectronicSignatureManagementServiceImpl.java | verifySignature/hasCompleteVerifiableEvidence/buildCanonicalPayload/new payload allowlist/image/workflow 判定 | 不能只扩 allowlist、不重算新字段 |
| DCC/service/file/DccWorkflowFileStateAudit.java | capture/记录确认事实；reason 从真实用户输入，签名引用 DCC 投影与 HMAC | 审计 append 失败回滚 |
| IntRuoyiBackend/config/gxp-audit-policy.yaml | 新操作 dcc.controlled-file.delayed-effective-confirm，REQUIRED；Root 可自行编辑并交还证据 | Root 保留，未写；缺正式 policy fail fast |

通用 BPM DccSignedTaskActionGuard.java 不需要放宽；它已按正式 task-local 签名凭证保护全部 DCC upload/revision key。DccWorkflowSignoffAssignmentService.java:authorizeSignedAction 已提供所需持久 task-local 写入，可由确认 service 调用，G05 当前 owner 无需并写。若后续真实测试显示新 stage 资格接口仍阻断，必须提交具体路径/锚点由 Root 单独转交，不擅自改 G05 文件。

## C：前端页接线（等待 G12、G10/G08）

| 文件 | 锚点/差异 | 前置 owner |
|---|---|---|
| FE/upload/index.vue | 生效单选改为两条精确业务文案，普通延迟不展示日期；rules/preflight/草稿/确认保持 mode/null，外部送审原日期合同独立 | G12 交还后 G01 |
| FE/upload/submitter.ts | buildUploadEffectiveChoice/buildSubmitPayload/buildUploadConfirmationSummary：新 mode/null、不要求具体时间、确认文案 | G12 交还后 G01；当前 RED 所在 |
| FE/detail/DetailApplicationPanel.vue | 初始化/恢复/保存/升版确认/submit helpers，草稿依保存 mode | Root 交后 G01，再 G03/G06 |
| FE/detail/index.vue | 当前用户精确待办绑定专属确认 panel；版本申请选择、历史只读展示四投影 | G10→G08→G01；不能与父页 owner 并写 |
| FE/workflow/DelayedEffectiveConfirmationPanel.vue（新增） | 真实 datetime/reason/password、提示、服务器合同、命令及错误保留 | 新增需正式授予，不提前造平行生产 |
| FE/workflow/effective-facts.ts | 新 mode 展示与历史模式区分；所选/签名/实际启用各字段 | G02 联合，当前旧 mapper 拒绝 DELAYED |
| FE/shared/ControlledFileBasicInfoPanel.vue | 生效方式和新所选时刻投影 | G10→G08→G01 |
| DCC/approval/DccApprovalTaskAdapter.java | toSummary/详情办理路由：原编制人确认任务可见、新命令可办理；普通 APPROVE 不能跳过 time | Root 转交，批准意见门禁保持 |

## D：G02 与其他 owner 的明确依赖

- G01 先冻结 signature meaning/payload/四文件投影、新 V6 flowRule 和 task 身份；G02 再接 DccControlledFileLifecycleService.java、DccControlledFileFinalizationServiceImpl.java、相关 native control audit/到期调度消费。Finalization 目前只接受旧文控/READY 状态，应验证真实确认 End 来源、签名及训练前置，而不是宽泛增加一个状态。
- 已依 Root C01 指示明确请求拆分：LifecycleService/Finalization 两文件由 G02 专属写；G01 无写需求。G01 自有新 Evidence verifier 是双方唯一确认签名读合同；具体 stage/status/meaning/字段见 contract.md。
- G02 必须证明 delayedEffectiveConfirmedAt=不可变确认 projection.signedAt、controlledTime 同源，selected time 不早于该 signedAt；签名 S、处理 T、目标 E 三值分离。datePolicy/contract resolver/file audit 若先由 G01修改，Root 复核再整体交 G02，禁止交叉并写。
- G07 的 Query/DetailAuthorizationGuard/Controller 内容上下文：确认页有合法精确 task 内容读取需求，只向 owner 提供 DELAYED_EFFECTIVE_CONFIRM identity；不得默认日常获取正文。新增详情字段映射请求为 DccControlledFileQueryServiceImpl.java:response mapping，只由当前 owner/Root 接入。
- G08 版本显示与 G01 新节点无耦合，按当前 helper 展示精确版本，request 仍传 id；详情父页必须依顺序交还。
- G03 不改公共矩阵；PUBLIC_MATRIX_NATIVE_V2 是执行合同，不等于自定义矩阵版本，G01 实施后归还公共申请文件供其接入。
- G06 继续保留附件票据/版本事务和培训原上传人；G01 新确认字段不得复制到新升版，也不把普通附件作为 TRAINING 完成事实。
- 模型真正部署、历史→新定义 transition 配置、正式新签名岗位/操作审计许可需要 Root 后续明确授权；本阶段不联系外部系统/业务库、不声明运行验收。

## 测试文件/fixture 边界

建议专属新增 JT/service/file/DccDelayedEffectiveSubmissionTest.java、DccDelayedEffectiveConfirmationServiceTest.java、DccDelayedEffectiveConfirmationTransactionTest.java、JT/DccWorkflowV6BpmnTest.java、IntRuoyiFronted/tests/unit/dcc-delayed-effective-confirmation.test.cjs 和 dcc-delayed-effective-submission.test.cjs。隔离 DB schema 为专属新测试类/文件，不先改公共 dcc_b_gxp_audit_tables.sql 或全局 BaseDbUnitTest。

需修改的已有专属 G01 相邻测试：IntRuoyiFronted/tests/unit/dcc-upload-ic05-application-options.test.cjs、dcc-detail-application-ic05.test.cjs、dcc-upload-effective-activation-times.test.cjs 及 JT/service/file/DccWorkflowDatePolicyTest.java。其它共享 Java 回归先只运行；如需变更过期 fixture，应列具体 hunk 经 Root 接收后再写。
