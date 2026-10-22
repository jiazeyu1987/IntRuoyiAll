# G01 Changed Files

## 实际修改

初始SPEC_ONLY仅本任务目录；C02/A04/A06/A07/A11/A13/A16以后实际生产差异见下面实现清单，未写未授权共享owner文件。

- task.md：目标、里程碑、BDD、验证、授权/设计约束和状态。
- execution-log.md：实际命令、结果与边界。
- design.md：当前源码事实、最小合同与真实 TDD 方案。
- contract.md：同步 C01，精确 stage/status/meaning/六字段、模型和 G01/G02 单文件归属请求。
- integration-notes.md：Root 转交文件清单、锚点、必要差异/验收影响。
- review-ready.md：阶段结果与具体接入请求。
- verification-report.md：本轮证据及未执行项。
- source-baseline.json：只读源码基线 UTF-8/LF SHA256 与 HEAD；不包含秘密。
- delayed-upload-red.cjs：直接提取并执行当前生产 TS helper 的隔离 RED 探针；不复制冻结实现，不调用服务。
- verify-spec.cjs：UTF-8/结构/路径/锚点校验与只读源码指纹生成；默认复核而不静默重抓并行变更。

自有新文件创建前ABSENT；生产write-before为source-baseline/a06/a07 manifest。没有改公共测试fixture、Lifecycle/Finalization、Query/Guard、G05指派文件、详情父页。

## 当前生产实现清单（进行中）

- FE upload/index.vue、submitter.ts、approval-matrix.ts；detail/DetailApplicationPanel.vue；workflow/effective-facts.ts；新增DelayedEffectiveConfirmationPanel.vue；API workflow.ts/applicationRead.ts。
- BE ApplicationEffectiveSelection/WorkflowDatePolicy；文件DO/签名DO/SignatureEvidence(CreateReq)/RespVO；StageCode/StatusEnum；WorkflowDefinitionCatalog/BoundWorkflowContractResolver/LifecycleEffectiveContract(Resolver)/ApplicationExecutionContractService；FrozenApprovalSignatures；SignatureVerificationService(Impl)/SignatureEvidenceServiceImpl/ElectronicSignatureManagementServiceImpl/SignatureSubjectAdapter；WorkflowServiceImpl（提交变量/确认状态/禁止普通approve）、新ConfirmationEvidence/Service/Req/Resp、WorkflowController、WorkflowFileStateAudit；config/gxp-audit-policy.yaml仅新confirmation条目；新V6 upload/revision模型、新nullable迁移。
- 专属新增FE delayed submission/confirmation tests，已授旧ic05/draft文案断言与ic13新previewV2断言；BE专属Evidence/Hmac/Service/V6 tests。即时代码正在组合验证，不能按文件存在声称全闭环完成。

## 后续已授追加与阶段归还

- A23停止写main-handback.json列出的24文件及DetailApplicationPanel；后续G03/G06差异不作为G01独立实现证据。
- 当前G01详情owned追加：detail/index.vue、shared/ControlledFileBasicInfoPanel.vue、detail/ControlledFileBrowseRecords.vue、workflow/effective-facts.ts、workflow/DelayedEffectiveConfirmationPanel.vue、workflow/distribution-facts.ts与workflow-actions.ts只读mode联合、detail/application-round-context.ts（A25）、detail/native-approval-progress.ts（A27）。
- C05/A18签名跨模块已授：yudao-module-signature/api/ElectronicSignatureSubjectAdapter.java与service/ElectronicSignatureServiceImpl.java；新MICROS/default旧语义、服务端时间hook和immutableidentity核验。未改QueryServiceImpl既有SERVER_CLOCK历史验签逻辑；新MICROS证据前缀保证实际signedAt列参与verify。
- A28仅test Capturing新增方法严格抛UnsupportedOperationException，文件为IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/signature/policy/SignatureGovernancePolicyDccAdapterTest.java。
- 新专属FE：dcc-delayed-confirmation-round、dcc-delayed-progress、dcc-delayed-display、dcc-body-read-wrapper-g01、dcc-preview-time-parser-g01；新BE：DccDelayedEffectiveConfirmationEvidenceTest、DccDelayedEffectiveHmacTest、DccDelayedSignatureRoundtripTest、DccDelayedEffectiveConfirmationServiceTest、DccWorkflowV6BpmnTest。未改公共H2 schema（Root已授G05）。
- 该任务baseline哈希与归还hash分别保留；A27新helper缺单独写前hash持久json，前置实际源码读取及真实RED日志可查，不冒称已有该beforehash。后续current-source manifest仅为交接当前指纹，不伪称改前证据。
- A30 shared/lifecycle.ts及shared/stage-name.ts只新status/stage中文显示与statusunion；写前指纹a30-write-baseline.json，独立归还后指纹independent-handback.json。未改公共stage四条/withdraw允许/actionprojection。
- A31重新打开仅DccWorkflowV6BpmnTest.java的资源根定位；privateRunner同rootprop；新增自有A31ModelRootProbe.java/写前manifest。无生产Java/图内容修改，8条Flowable业务分支和断言保持。指纹修订后再次冻结给Root M08。

## 保留/忽略检查

git check-ignore -v -- doc/tasks/20261014-dcc-gap-delayed-effective/delayed-upload-red.cjs 返回 `.gitignore:103:doc/tasks/**/*.cjs`，退出 0。脚本已加入 task.md Cleanup Keep。以后若获 Git 授权，应精确 add -f 该文件；本轮不改 gitignore、不暂存。
