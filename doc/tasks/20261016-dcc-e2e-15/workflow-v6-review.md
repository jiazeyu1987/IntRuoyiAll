# V6 正式模型只读技术 review

日期：2026-10-16；实际分支：int_qms。范围为仓库现有 upload/revision XML、catalog、digest、准备/绑定服务、正式模型维护入口和测试源码。未运行 Maven、部署、DB 查询/写入、服务操作或 E2E；下列测试均为建议 Root 执行，不能记为本轮 PASS。

## 结论

1. 新上传和新升版的预检已经明确要求 `PUBLIC_MATRIX_NATIVE_V2`。两份 V6 XML 是现有正式候选；V5 即使仍在 catalog 中有合法 digest，也不满足新预检的 V2 契约。
2. `EXECUTION_CONTRACT_UNREGISTERED_DEFINITION` 不足以独自断言“库里没有 V6”。它表示 contract/key/完整图 digest 组合未登记，或提交时 expectedDigest 与实际图不符；Root 仍须核对实际租户、定义 ID、部署 ID、当前选中定义、完整图 digest 和预检请求。不能放宽 catalog 或改历史字段消除错误。
3. 上传与升版应作为同一 V2 上线范围分别核对、分别部署、分别回读验证。仅部署上传可以解除 NEW 上传预检，但不会更新 revision，也不能证明后续升版已可用。若某 key 实际已经是登记 V6，则只读证明后保留，不重复部署。
4. 两份 XML 无需为本次解除阻塞而改源码。当前仓库已有正式“导入流程图”及“发布”入口；不能用编辑设计器后随手保存、直接覆盖历史字节、改流程名或替换 candidate 策略来追求 digest 通过。

## 正式身份与摘要

| 文件 | process key / process name | catalog contract | catalog 完整图 digest |
| --- | --- | --- | --- |
| IntRuoyiBackend/config/dcc-workflow-v6/upload.bpmn20.xml | dcc-controlled-file-upload / DCC受控文件上传审批 | PUBLIC_MATRIX_NATIVE_V2 | 36bc2379b56d0ada0571fccd573024fb05f951632e0963cbcd9ab1702af7b2c2 |
| IntRuoyiBackend/config/dcc-workflow-v6/revision.bpmn20.xml | dcc-controlled-file-revision / DCC受控文件升版审批 | PUBLIC_MATRIX_NATIVE_V2 | 84d039f7fab6c81159fb049637a0ff70b6eb3e57d6dfc6b47a64475e421935d5 |

两项 `FlowRule` 均为 `PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM`，来源为 `DccWorkflowDefinitionCatalog.java:15`、`:16`。本轮文件原始字节 SHA-256（用于冻结输入，不能当作 Flowable 图 digest）：

```text
upload.bpmn20.xml   444E4450DC28EF111F71E9A29508E7127F65408344CD7A17EF157C1C363BA33B
revision.bpmn20.xml BBCEE56F4BEEB8440C497DDADA581E770E34216C2E9200279624C6CC04AD219A
```

`DccLifecycleEffectiveContractResolver.definitionDigest`（同名类第 61 行）对 **实际 BpmnModel** 调用 `BpmnModelUtils.getBpmnXml`/Flowable `BpmnXMLConverter.convertToXML`，规范化 CRLF/CR 为 LF 后按 UTF-8 做 SHA-256。不是直接 hash 原始 XML 文件，也不是只比节点集合；流程名、节点名、扩展配置等经序列化后可能改变结果。上表为源码登记值，本轮没有另跑 JVM 重新计算或核对实际部署图。

`DccApplicationExecutionContractService.preparePublicPreview` 第 300 行先按当前租户/key 取 newest active definition，然后调用 `verify`；非 OBSOLETE 一律要求 V2。`verify` 第 339 行起核对租户、精确 ID、key、deploymentId、单一主流程、完整 digest 及 catalog。`BpmProcessDefinitionServiceImpl.getActiveProcessDefinition` 第 85 行使用 `latestVersion().active()`，最新定义挂起时不会静默选回旧 active 版。

因此实际活动 V5 的 `PUBLIC_MATRIX_NATIVE_V1` digest 即便合法登记，拿来按 V2 校验仍会得到该错误；同 key 未登记改图也会得到同错误。若没有可选 definition，则应为不同错误 `EXECUTION_CONTRACT_NEW_DEFINITION_NOT_DEPLOYED`。

## 两份模型的关键流程

upload 与 revision 仅 process ID/name 和开始事件名称不同，业务图一致：

```text
StartEvent -> SUPERVISOR_REVIEW -> MATRIX_REVIEW -> MATRIX_APPROVAL
           -> DOC_CONTROL_REVIEW -> NeedTrainingGateway
             needTraining=true  -> TRAINING(receiveTask) -> EffectiveModeGateway
             needTraining=false ------------------------> EffectiveModeGateway
             effectiveMode=AFTER_STANDARDIZATION_SIGNATURE -> EndEvent
             effectiveMode=DELAYED -> DELAYED_EFFECTIVE_CONFIRM -> EndEvent
```

- `SUPERVISOR_REVIEW`：candidateStrategy=35（START_USER_SELECT），approveMethod=1；不是模型自行按部门推断主管，正式 DCC 启动端提供冻结主管候选。
- `MATRIX_REVIEW`：candidateStrategy=34（APPROVE_USER_SELECT），approveMethod=2、approveRatio=100；并行多实例数来自 `PROCESS_DCC_TASK_OBLIGATION_IDS["MATRIX_REVIEW"].size()`，完成条件是 `nrOfCompletedInstances == nrOfInstances`，不能改成任一通过。
- `MATRIX_APPROVAL`、`DOC_CONTROL_REVIEW`：candidateStrategy=34、approveMethod=1，先批准负责人再文控。
- 培训在文控之后；`TRAINING` 是 receiveTask，不能以伪完成审批任务代替。无培训则直接进生效方式网关。
- `DELAYED_EFFECTIVE_CONFIRM`：candidateStrategy=35、approveMethod=1。真实 `DccControlledFileWorkflowServiceImpl.java:1788` 写入 `effectiveMode`、`dccOriginalRequesterId`，`:1799` 显式将该节点候选固定为原 requester；仅 XML 本身不能证明运行账号正确。
- V6 只有立即/延迟两种分支，没有 PRESET_DATE fallback；`DccBoundWorkflowContractResolver` 另核验正式申请轮次、prepared identity、原 requester 候选及 needTraining/effectiveMode 变量，不能只改模型绕过这些约束。

## 已有正式部署入口与约束

- 正式路由：`/approval-center/manager/model`，菜单为“审批中心 -> 流程管理 -> 流程模型”，对应 `IntRuoyiFronted/src/router/modules/remaining.ts:345`。`/bpm/manager/model` 是隐藏别名，不能用手拼地址替代账号菜单验收。
- 模型行已存在“导入流程图”：`IntRuoyiFronted/src/views/bpm/model/index.vue:208`，组件 `BpmModelXmlImport.vue`。文件输入 `data-testid=bpm-model-xml-file`，保存按钮“保存流程图”。组件校验同 key 的唯一 process、BPMN type=10、当前元数据未变化、无外部 XML 声明。
- `bpmn-import-contract.ts` 返回 `{ ...current, bpmnXml: xml }`，不改 XML，不丢弃现有 form/manager/description。保存前再次读取当前 model 防止并行覆盖；“保存流程图”后单独点击同一行“发布”（`index.vue:1864`），不是保存即部署。
- 自然页面链路是 `BpmModelController.updateModel` -> `BpmModelServiceImpl.updateModel/saveModel` -> 存 editor source，之后 `deployModel` 校验模型管理人、BPMN、表单及候选策略，再通过 `BpmProcessDefinitionServiceImpl.createProcessDefinition` 创建新定义。
- 所需权限为 `bpm:model:query`、`bpm:model:update`、`bpm:model:deploy`，服务端还执行 `validateModelManager`。不能以按钮可见替代实际模型管理人和表单完整性；缺管理人时先由 Root 核对既有“认领管理人”流程及范围，不能任意抢占已有管理人。
- 部署使用现有 XML 原字节，随后要求 `definition.key == model.key` 且 `definition.name == model.name`。Root 必须提前核对模型名称分别为表中两个 process name；如果不一致是单独前置，不能为匹配显示名改 V6 XML，否则可能破坏 catalog digest。普通表单须有存在的 formId，自定义表单须有 create/view path。
- `deployModel` 第 232 行挂起模型原 deployment 的定义；`updateProcessDefinitionState` 使用 `suspendProcessDefinitionById(id, false, null)`，不挂起在途实例、不改历史 XML。两份模型的部署分别为事务，现有入口没有“双模型原子发布”承诺，必须分开记录精确新定义 ID/digest/状态。
- 文档依据：`IntRuoyiBackend/config/dcc-workflow-v5/README.md` 说明 config 资料不自动部署、通过既有模型服务保留表单/管理人，旧实例按原定义处理；`docs/dcc-three-workflows/technical-design.md:206` 要求正式部署，禁止直接改 ACT_GE_BYTEARRAY。V6 目录目前只有两份 XML，无自己的 README/部署 manifest；不能把 V5 的三份模型或其迁移闭包原样当 V6 执行清单。

## 定向测试：精确类名与证明边界

下面路径均已确认实际存在，Maven 统一由 Root 执行。

| 测试类 | 关键覆盖 / 限制 |
| --- | --- |
| cn.iocoder.yudao.module.dcc.DccWorkflowV6BpmnTest | `actualUploadAndRevisionModelsWaitForOriginalCompilerOnlyAfterDocumentAndNecessaryTraining`：真实 Flowable 私有引擎部署两份生产 XML，覆盖 upload/revision × 有无培训 × 两种生效方式共 8 条路径；验证实际会签多实例和延迟任务原编制人候选。输出 `G01_V6_CANONICAL_DIGEST upload/revision=...`。**源码只打印这两个 digest，没有直接 assert catalog.require**，Root 必须逐项比对上表；候选目录为隔离端口，不是实际租户 E2E。 |
| cn.iocoder.yudao.module.dcc.service.file.DccG02ControlledSignatureTimeTransactionTest | 真实 V6 upload 解析后调用 `DccWorkflowDefinitionCatalog.require("PUBLIC_MATRIX_NATIVE_V2", key, digest)`（第 275 行）；验证签名时刻 S、同日未来时间 E、HMAC、防篡改、受控与激活、立即模式。此类的模型 fixture 为 upload，不单独证明 revision catalog digest。 |
| cn.iocoder.yudao.module.dcc.service.file.DccG02DelayedFinalizationTransactionTest | 复用 V6 fixture，覆盖真实确认签名命令/BPM 结束/Finalization，`requesterConfirmationEndControlsWithRealSAndFutureEKeepsOldVersion`、晚失败回滚重试、原作者/日期/轮次拒绝，以及必要培训 receive/原上传记录；外部端口有隔离替身。 |
| cn.iocoder.yudao.module.dcc.service.file.DccApplicationExecutionContractServiceTest | 缺 prepared/未登记改图/冒称 legacy 拒绝；精确定义不随 latest 改变；OBSOLETE 固定图独立验证。主要是 V4/V5 守卫回归，不能称其独立证明 V6 两图。 |
| cn.iocoder.yudao.module.dcc.service.file.DccBoundExecutionGraphContractTest | 真实 runtime/history 的冻结图、prepared ID/digest、tenant/businessKey/round/属性来源等拒绝；不以 latest 替换绑定历史。 |
| cn.iocoder.yudao.module.dcc.service.file.DccLifecycleEffectiveContractResolverTest | 同 key 改图被拒绝、旧图跨部署版本仍可验证、历史已结束实例按原图解析、revision legacy 完整摘要；主要为历史保留回归。 |
| cn.iocoder.yudao.module.dcc.service.file.DccPreparedExecutionInheritanceDatabaseTest | `controlledUploadRequiresAnExplicitExactRevisionTransitionAndKeepsOldHistory`，以及缺精确跨 key 注册、重复映射/错 digest/未知图拒绝；保留原历史且零错误回填。 |
| cn.iocoder.yudao.module.bpm.service.definition.BpmLatestProcessDefinitionTest | 多 active 版本只取最新；最新挂起不能回退旧版；跨租户拒绝；现有实例保留原定义。 |
| cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceExactDefinitionStartTest | `frozenIdWinsOverNewDeploymentAndPreservesBusinessFacts`；显式 ID、tenant/key、挂起、权限和候选校验，缺精确 ID 不 fallback。 |

核心推荐命令（工作目录 `IntRuoyiBackend`；本轮未运行）：

```powershell
mvn.cmd -pl yudao-module-dcc,yudao-module-bpm -am "-Dtest=DccWorkflowV6BpmnTest,DccG02ControlledSignatureTimeTransactionTest,DccG02DelayedFinalizationTransactionTest,DccApplicationExecutionContractServiceTest,DccBoundExecutionGraphContractTest,DccLifecycleEffectiveContractResolverTest,DccPreparedExecutionInheritanceDatabaseTest,BpmLatestProcessDefinitionTest,BpmProcessInstanceExactDefinitionStartTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dfile.encoding=UTF-8" test
```

`-am` 使跨模块依赖按当前源码编译；`surefire.failIfNoSpecifiedTests=false` 仅处理 reactor 依赖模块不含这些指定类，Root 仍须核对目标九类实际测试数、fail/error/skip，不能仅看 exit 0。`DccWorkflowV6BpmnTest` 从 Maven basedir 定位真实仓库；非 Maven runner 必须显式绝对 `-Ddcc.repository.root`。

如需复核现有前端导入合同，工作目录 `IntRuoyiFronted`：

```powershell
node --test tests/unit/dcc-bpmn-import-contract.test.cjs
```

该测试只证明元数据保留、错 key/多 process/非 BPMN/并发变化拒绝和正式页面入口合同，不能代替真实导入/发布。若模型确实缺管理人，另有 `BpmUnmanagedModelClaimServiceTest` 覆盖正式认领边界；不应无条件执行认领。

## 同步与部署后的 Root 核验点

- upload/revision 各记录 tenant、model ID、key/name、原 form/manager、发布前后 definition ID/deployment ID、版本、状态、**实际完整图** digest；与 catalog 对照，保留旧定义和实例身份。
- 若原上传/升版申请已经冻结 prepared ID/digest，不得 SQL 改为新定义或强行重写；重新打开页面获取正式预检/新申请按既有状态规则办理，历史继续按旧图。新普通上传预检恢复只代表该阶段 GREEN。
- 跨上传/升版或旧 V1 到新 V2 的新动作准备，还可能要求 `dcc.application-execution.definition-transitions` 的精确 sourceDefinitionId/sourceDigest -> targetDefinitionId/targetDigest。`DccExecutionDefinitionTransitions` 默认空表，部署新模型不会自动登记映射；若命中 `EXECUTION_CONTRACT_EXACT_ACTION_TRANSITION_REQUIRED` 应作为独立前置，不用最新 key 推断。普通全新 NEW 上传无需凭空增加映射。
- 若目标 E2E 依赖未来时刻自动激活，须由 Root 核对正式 activation job；两份 V6 BPMN 只把确认节点接至 End，并不含到期定时器。不能把流程结束/发布成功当生效完成。
- 本轮未接触实际运行定义、库或页面，不能据源码判断 01/03/05 的实际 digest，亦未宣布部署或 E2E PASS。
