# 活跃订单放行资料 M0 契约冻结

## 1. 结论与适用范围

本文件是 V4 后续 A1-A6 的唯一 M0 输入。M0 已冻结接口、writer、fixture、运行时顺序、事务和来源快照契约；A1-A6 可以在不重新解释业务来源的前提下进入 BDD/TDD 开发。

本次只冻结契约，不修改生产代码、数据库或运行环境。当前代码中尚未实现的 writer、映射、事务拆分和 DTO 扩展属于 A1-A5 的明确交付，不是用替代来源绕过的理由。

术语必须保持三条链路独立：

- `工序开始`：上传人、附件负责人或开始动作配置，不提供放行资料正文。
- `批记录表单`：工序设置中逐工序正式 BATCH 绑定，目标为正式生产批记录。
- `formBindings/表单槽位`：动态表单目标链路。它不得替代 `MAIN` 批记录；对于本合同明确的 `PROCESS_INSPECTION/form_template_id=28` 和 `LOSS_REPORT/form_template_id=25`，可作为各自正式目标载体，但业务内容仍必须来自 PQC/QA 或已签名生产损耗事实。

代码中的 `formSlotType=PROCESS_INSPECTION/LOSS_REPORT` 同时用于区分传统报表绑定和动态表单绑定。A4/A5 必须按路线冻结的唯一绑定类型执行：传统绑定走 `batchRecordReportId` execution；上述指定动态绑定走已发布 FormCenter template version、精确字段映射和正式 instance 提交。不得在两条目标链路之间猜测或静默降级。

## 2. 适用经验门禁

依据 `docs/experience-index.md`，本任务应用以下项目门禁：

- `docs/backend-development.md#edhr-放行负责人来源门禁`：展示、候选人和授权统一读取路线级 `RELEASE_APPROVE`；`CLOSE`、当前登录人、静态角色和 `stageOwnerRole` 均不得替代。
- `docs/backend-development.md#活跃订单申请放行资料必须只使用正式来源`：双 100%、逐工序 BATCH 绑定、已确认 PQC 汇集、正式损耗承载、负责人及幂等均由后端权威校验。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：PQC 事实来自发布 QA 规程、结构化 `itemResults`、正式逐件明细和确认汇集，不得以 raw payload 或单一状态代替。
- `docs/frontend-development.md#前端写入成功与列表刷新失败分层门禁`：申请写成功后刷新失败必须保留成功事实并提示“申请已提交，但列表刷新失败”，不得允许无条件重复写入。

## 3. M0-01 接口契约

### 3.1 HTTP 入口与权限

- 方法：`POST /admin-api/mes/pro/process-pool/team-leader/active-order/release/apply`
- Controller 相对路径：`POST /mes/pro/process-pool/team-leader/active-order/release/apply`
- 权限：`mes:pro-process-pool-team-leader:release-apply`
- 当前正式入口：`MesProcessPoolTeamLeaderController.applyActiveOrderRelease(...)`
- 前端 wrapper：`applyTeamLeaderActiveOrderRelease(...)`

不新增第二个申请接口，不新增 `clientRequestId`，不允许前端生成批次、资料或待办 ID。

### 3.2 请求

| 字段 | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| `activeOrderId` | `Long` | 是 | 正数；必须是当前登录生产组长负责且状态为 ACTIVE 的活跃订单 |
| `idempotencyKey` | `String` | 是 | 去首尾空白后非空，最大 128；同一活跃订单内稳定唯一 |
| `applyRemark` | `String` | 否 | 去首尾空白，最大 500；只作为申请/提交说明，不参与正式来源证明 |

前端只发送以上三个字段。

### 3.3 响应

| 字段 | 类型 | 可空 | 说明 |
| --- | --- | --- | --- |
| `applicationId` | `Long` | 否 | 申请记录 ID |
| `activeOrderId` | `Long` | 否 | 活跃订单 ID |
| `workOrderId` | `Long` | 否 | 生产工单 ID |
| `workOrderCode` | `String` | 是 | 生产工单号快照 |
| `status` | `String` | 否 | 仅 `BLOCKED` 或 `PENDING_RELEASE_APPROVAL` |
| `statusName` | `String` | 否 | `资料生成阻塞` 或 `待生产负责人放行` |
| `batchExecutionId` | `Long` | 是 | 成功提交时必填；预写阻塞时为空 |
| `releaseTransactionId` | `Long` | 是 | 成功提交时必填；预写阻塞时为空 |
| `releaseApprovalWorkTaskId` | `Long` | 是 | 仅 `PENDING_RELEASE_APPROVAL` 必填 |
| `dossierSummary` | `Object` | 否 | 三类资料计数、签名证据计数和来源哈希 |
| `blockers` | `Array` | 否 | 无 blocker 返回空数组，不返回 `null` |
| `appliedAt` | `LocalDateTime` | 否 | 服务端申请记录时间 |

`dossierSummary` 首版字段固定为：

- `batchRecordCount`
- `processInspectionFormCount`
- `lossReportFormCount`
- `signatureEvidenceCount`
- `sourceSnapshotHash`

成功条件必须满足三个资料计数均大于 0、`signatureEvidenceCount > 0`、三个关联 ID 均存在。首版不新增必填 `generatedDocuments[]`。

### 3.4 blocker

每个 blocker 固定包含：

- 必填：`blockerType`、`objectType`、`objectId`、`objectCode`、`reason`、`suggestion`
- 可选定位：`routeProcessId`、`processId`、`fieldCode`、`cellKey`

确定性业务缺失返回 HTTP 成功响应且 `status=BLOCKED`，用于让生产组长修复配置或正式来源。认证、越权、请求格式错误、数据库/事务/解析/持久化异常必须 fail fast，不能包装成假 `BLOCKED` 成功。

首版 blocker type 至少冻结为：

| 类型 | 触发条件 |
| --- | --- |
| `ABNORMAL_OPEN` | 生产工单仍有未关闭异常 |
| `BATCH_RECORD_BINDING_REQUIRED` | 缺逐工序正式 BATCH 批记录绑定或绑定快照不合法 |
| `PRODUCTION_HISTORY_REQUIRED` | 缺正式生产提交、历史表单、分配完成或组长确认 |
| `PRODUCTION_SIGNATURE_REQUIRED` | 缺生产填写/组长确认签名证据 |
| `PQC_CONFIRMED_AGGREGATE_REQUIRED` | PQC 任务未 CONFIRMED 或缺结构化汇集明细 |
| `PQC_QA_REGULATION_REQUIRED` | 缺当前产品/工序发布 QA 规程版本 |
| `PQC_QA_ITEM_MISMATCH` | 汇集项目、方法、上下限、设备、判定与发布版本不一致 |
| `PQC_SIGNATURE_REQUIRED` | 缺 PQC 填写或 PQC 组长复核签名证据 |
| `PROCESS_INSPECTION_REPORT_BINDING_REQUIRED` | 缺唯一有效过程检验目标绑定 |
| `PROCESS_INSPECTION_DYNAMIC_FORM_AUTOWRITE_REQUIRED` | 已识别 template 28 正式目标，但 FormCenter 自动写入/提交链路尚未接通 |
| `PROCESS_INSPECTION_MAPPING_REQUIRED` | 缺 PQC 汇集到目标单元格的启用映射 |
| `LOSS_SOURCE_REQUIRED` | 缺可追溯生产损耗事实或总量/明细不一致 |
| `LOSS_REPORT_BINDING_REQUIRED` | 缺唯一有效损耗目标绑定 |
| `LOSS_REPORT_DYNAMIC_FORM_AUTOWRITE_REQUIRED` | 已识别 template 25 正式目标，但 FormCenter 自动写入/提交链路尚未接通 |
| `LOSS_REPORT_MAPPING_REQUIRED` | 缺生产损耗到目标单元格的启用映射 |
| `ZERO_LOSS_CONFIRMATION_UNSUPPORTED` | 无损耗但目标模板没有正式无损耗确认字段及映射 |
| `DOSSIER_SIGNATURE_INCOMPLETE` | 任一资料填写/审核签名或时间不完整 |
| `DOSSIER_COMPLETENESS_BLOCKED` | 三类资料必填、来源追溯、模板或审计不完整 |
| `RELEASE_OWNER_REQUIRED` | 路线缺启用 `RELEASE_APPROVE` 或候选池为空/无效 |
| `RELEASE_PRECHECK_BLOCKED` | eDHR 正式 precheck 返回失败或阻塞项 |

定位字段能确定时必须填写，不得只返回无法定位的汇总文案。

### 3.5 状态与最终放行事实

申请表首版只持久化：

- `BLOCKED`：本次来源快照无法生成完整资料，没有放行待办。
- `PENDING_RELEASE_APPROVAL`：完整资料已生成，precheck 通过，已创建/复用负责人待办。

负责人最终 `RELEASED/REJECTED` 由 `mes_pro_edhr_release_transaction.release_status` 和正式事件表权威表达，不复制到申请表。生产负责人页面和 G-10 验收读取 eDHR 放行事务；活跃订单申请状态仅表达“申请是否成功进入负责人待审批”。

### 3.6 双幂等

- 请求幂等：`tenantId + activeOrderId + idempotencyKey`。完全重复请求返回原 `applicationId` 及原快照，不重复生成资料或待办。
- 业务幂等：`tenantId + activeOrderId + businessIdempotencyKey`，其中业务键固定为 `AO_RELEASE_SOURCE_V1|workOrderId|routeVersionId|sourceSnapshotHash`。
- 相同来源快照使用新请求键时返回同一业务申请；正式来源变化后生成新 hash，必须使用新请求键创建新申请。
- 同一请求键不能因来源后来变化而改写旧申请；前端刷新失败或响应不确定时应先用活跃订单列表中的稳定申请事实确认，不能直接重试生成新键。
- 唯一键冲突只允许重新读取对应正式申请并严格核对业务键；不允许吞冲突后返回默认成功。

## 4. M0-02 writer 端口契约

### 4.1 公共约束

A2 只依赖三个小边界：`BatchRecordWriter`、`ProcessInspectionWriter`、`LossReportWriter`。可以使用接口、内部组件或现有服务适配器，但禁止 no-op、默认成功和第二套大抽象。

公共输入：

- `tenantId`、`activeOrderId`、`workOrderId`、`routeId`、`routeVersionId`、`productId`、`batchCode`
- 当前 eDHR `batchExecutionId`
- 该批次中与正式报表绑定匹配的 `batchExecutionTaskId`
- 已冻结的活跃订单工序快照
- `sourceSnapshotHash` 和申请人 ID（申请人只用于审计，不得冒充资料签名人）

公共输出：

- `documentType`
- `batchRecordExecutionIds[]`
- `fieldAuditIds[]`
- `sourceObjectIds[]`
- `sourceValueHashes[]`
- `signatureEvidence[]`，每项包含 `role/sourceType/sourceId/signatureId/userId/signedAt/evidenceHash`
- `blockers[]`

writer 必须先执行无副作用 plan/validate。存在 blocker 时返回 blocker 且不写目标资料；写入阶段任一异常直接抛出并使整段生成事务回滚。

### 4.2 BatchRecordWriter

正式来源：

- 活跃订单逐工序快照及完成记录。
- 正式生产提交事件、报工/历史表单、数量分配及组长确认。
- 事件上的实际填写员工、服务端提交时间和电子签名；组长复核记录上的审核人、审核时间和审核签名。
- 工序设置逐工序 `useType=BATCH`、`recordCategory=BATCH_RECORD`、非 `PROCESS_INSPECTION/LOSS_REPORT` 的正式批记录绑定。

正式目标：

- `mes_pro_batch_record_execution`，report/definition/version 必须来自上述逐工序绑定。
- execution 必须关联本次 `batchExecutionId` 和对应 `batchExecutionTaskId`。
- 写值必须留下 `mes_pro_batch_record_execution_field_audit` 或等价现有正式字段审计。

实现边界：A3 首选复用 `MesTeamLeaderBatchRecordBackfillServiceImpl.backfillCompletedProcess(...)` 的映射、审计和幂等逻辑，通过小适配补齐当前批次/任务上下文。历史完成时产生但不属于本次 eDHR 批次任务的 execution 只能作为来源证据，不能冒充本次输出。

完成条件：每个要求批记录的工序都有本批次 execution、必填单元格、来源对象、值 hash、填写和组长确认签名证据；输出 execution/审计 ID 非空。

禁止来源：`formBindings`、默认 `MAIN`、工序开始上传人、当前登录申请人、空绑定、仅完成状态。

### 4.3 ProcessInspectionWriter

正式来源：

- `mes_pqc_inspection_task.task_status=CONFIRMED`。
- `mes_pqc_process_inspection_aggregate_detail` 结构化汇集明细，且 event、record、task、routeProcess、process、轮次和租户一致。
- 正式 PQC 提交记录/事件的检验员、签名 ID、签名用户、服务端提交时间。
- PQC 组长 APPROVED 复核记录的审核人、审核时间、审核签名。
- 与产品、路线版本和工序匹配的当前 PUBLISHED QA 规程版本、项目、方法、标准、上下限、单位、精度、项目级设备要求。

正式目标：

- 路线上的唯一正式目标绑定：传统目标要求 `batchRecordReportId`、definition/version/snapshot 有效；动态目标要求 `formSlotType=PROCESS_INSPECTION`、`form_template_id=28`、非空稳定 binding key、已发布 template version 和 binding snapshot hash 完整。
- 传统目标写 `mes_pro_batch_record_execution`；动态目标写当前 batch/task 的 `ROUTE_FORM` task 所关联 FormCenter instance，并提交到正式生效状态。
- 当前代码正式类别沿用 `recordCategory=INTERNAL_RECORD`、`validationProfile=INTERNAL_TRACE`、负责人角色 `QUALITY`；不得因此改写正式来源规则。

字段映射：扩展现有 `mes_pro_batch_record_cell_link_rule`，sourceType 固定为 `PQC_AGGREGATE_DETAIL`。传统目标按 report/version 读取；动态目标按 `scope_type=FORM_TEMPLATE_VERSION` 与 `target_report_id=FORMTPL:<publishedTemplateVersionId>` 读取，并由识别 schema 的稳定 fieldCode 精确定位。每个必需 QA 项目/元数据/填写审核字段必须存在启用映射，禁止根据中文标签猜字段。

完成条件：QA 项目集合完全匹配，实测值和判定一致，设备约束满足，所有必填目标格有审计，填写人=PQC 检验员、审核人=PQC 组长，签名时间来自正式记录。

禁止来源：raw payload、仅 `SUBMITTED`/`CONFIRMED` 状态、前端显示值、默认上下限、当前用户/当前时间。FormCenter instance 只能作为正式目标载体，不能反向冒充 PQC/QA 来源。

### 4.4 LossReportWriter

正式来源：

- `MesProFeedbackDO` 的产出、合格、不合格/损耗总量、分类数量及损耗原因快照。
- 与反馈关联的正式生产提交事件及其签名、实际员工、服务端提交时间。
- 结构化生产提交损耗明细 `lossDetails[]`，只作为已签名正式事件的一部分读取，并必须与反馈主表损耗总量、原因快照精确对账。
- 生产组长确认记录及审核签名。

正式目标：

- 路线上的唯一正式目标绑定：传统目标要求 `batchRecordReportId`、definition/version/snapshot 有效；动态目标要求 `formSlotType=LOSS_REPORT`、`form_template_id=25`、非空稳定 binding key、已发布 template version 和 binding snapshot hash 完整。
- 传统目标写 `mes_pro_batch_record_execution`；动态目标写当前 batch/task 的 `ROUTE_FORM` task 所关联 FormCenter instance，并提交到正式生效状态。
- 当前代码正式类别沿用 `recordCategory=INTERNAL_RECORD`、`validationProfile=INTERNAL_TRACE`、负责人角色 `PRODUCTION`。

字段映射：扩展现有 `mes_pro_batch_record_cell_link_rule`，sourceType 固定为 `PRODUCTION_LOSS`。传统目标按 report/version 读取；动态目标按 `scope_type=FORM_TEMPLATE_VERSION` 与 `target_report_id=FORMTPL:<publishedTemplateVersionId>` 读取。至少覆盖产品、批号、工序、产出、损耗总量、分类数量、逐项原因、填写/审核和日期字段；禁止按标签猜字段。

零损耗规则：只有目标模板存在明确“无损耗确认”正式字段且存在启用映射时才能生成无损耗确认。当前 `MesProBatchRecordLossReportNormalizer` 不能证明这一字段，因此首个成功 fixture 必须使用大于 0 且原因完整的损耗；零损耗按 `ZERO_LOSS_CONFIRMATION_UNSUPPORTED` 阻塞，不能写空损耗单。

完成条件：反馈总量、事件明细和目标值精确一致，原因完整，必填格及字段审计完整，填写/组长审核签名完整。

禁止来源：只有 raw payload、只看 `unqualifiedQuantity` 而不对账、空原因、当前用户/当前时间。FormCenter instance 只能作为正式目标载体，不能反向冒充生产损耗来源。

### 4.5 完成性检查

A5 提供只校验不创建待办的完成性组件，A2 在三个 writer 全部成功后调用。必须校验：

- 三类目标 execution 均属于当前 `batchExecutionId`，且绑定 report/version 与路线快照一致。
- 每个必填单元格有值、有字段审计、有 source object 和 value hash。
- 填写人、审核人、签名 ID、签名用户、签名时间和证据 hash 完整且角色正确。
- PQC 项目与发布 QA 版本一致；损耗总量/明细/原因一致。
- `sourceSnapshotHash` 能覆盖本次生成使用的全部来源。
- 路线级 `RELEASE_APPROVE` 规则和解析后候选人有效。

任何缺失返回具体 blocker；A5 不调用 precheck、`submitForApproval` 或负责人放行。

## 5. M0-03 fixture manifest 与可执行入口

### 5.1 manifest

A6 成功造数后必须写出机器可读 UTF-8 JSON，字段完整且数组稳定排序：

```json
{
  "testPrefix": "",
  "tenantId": 0,
  "productId": 0,
  "routeId": 0,
  "routeVersionId": 0,
  "workOrderId": 0,
  "workOrderCode": "",
  "activeOrderId": 0,
  "productionSubmitEventIds": [],
  "productionHistoryFormIds": [],
  "productionLeaderConfirmIds": [],
  "pqcTaskIds": [],
  "pqcSubmissionIds": [],
  "pqcHistoryFormIds": [],
  "pqcLeaderReviewIds": [],
  "pqcAggregateDetailIds": [],
  "lossSourceIds": [],
  "batchRecordExecutionIds": [],
  "processInspectionFormIds": [],
  "lossReportFormIds": [],
  "sourceFormIds": [],
  "sourceEventIds": [],
  "sourceValueHashes": [],
  "expectedFillerUserIds": [],
  "expectedReviewerUserIds": [],
  "expectedSignatureTimes": [],
  "signatureEvidenceCount": 0,
  "applicationId": 0,
  "batchExecutionId": 0,
  "releaseTransactionId": 0,
  "releaseApprovalWorkTaskId": 0
}
```

所有 ID 必须来自正式写入回执或只读查询；`signatureEvidenceCount` 必须等于去重后的有效签名证据条数且大于 0。manifest 不得含密码、token 或签名口令。

### 5.2 配置/造数入口

| 数据 | 唯一允许入口 | 完成证据 |
| --- | --- | --- |
| 产品、工单、任务自有前缀 | 现有正式页面/API/领域 service 创建；禁止直接 SQL | 正式 ID、编码及租户 |
| 路线、版本、工序及产品绑定 | `/mes/pro/route/*`、`/mes/pro/route-version/*`、`/mes/pro/route-process/*`、`/mes/pro/route-product/*` 的正式发布链路 | ACTIVE/发布版本和快照 ID |
| 逐工序批记录、过程检验、损耗目标绑定 | `/mes/pro/route/flow-config/batch-record/save`；MAIN 必须保存非空 `batchRecordReportId`；PI/LOSS 可保存传统 report 或本合同指定动态 template，类型和快照必须准确 | routeProcess、绑定类型、report 或 published template version、snapshot hash |
| QA 规程 | `POST /mes/qa/inspection-regulation/draft` 后 `POST /publish` | PUBLISHED version、items、equipment |
| `RELEASE_APPROVE` | eDHR 路线放行负责人正式配置 `/route-release-approval-rule` | rule ID、候选来源及非空候选快照 |
| 账号、角色、人员归属和签名 | 现有系统管理/人员/电子签名正式服务；使用任务专用测试账号 | user ID、角色、人员范围、有效签名配置 |
| 活跃订单 | 生产组长正式入口 `POST /team-leader/active-order/add` 或同一正式 service | `activeOrderId` 及工序快照 |
| 生产历史和损耗 | 一线生产真实页面，正式提交 API `POST /mes/pro/feedback/frontline/submit` | feedback/event/form/signature ID |
| 生产组长确认 | 组长页面，`POST /team-leader/submission/review`，`leaderType=PRODUCTION` | APPROVED review/confirm/signature ID |
| PQC 历史 | 一线 PQC 真实页面，`POST /mes/pro/feedback/frontline/device-account/pqc/submit` | task/submission/event/form/signature ID |
| PQC 组长复核与汇集 | 组长页面，`POST /team-leader/submission/review`，`leaderType=PQC` | APPROVED review、CONFIRMED task、aggregate IDs |

若任何正式入口、权限、账号、签名、模板或映射不可用，A6 必须 fail fast 记录 blocker；不得改用 SQL、mock、直接改进度或 API-only 冒充真实页面路径。

### 5.3 必须通过的真实页面路径

1. 一线生产真实填写页提交产量、设备参数和正损耗明细。
2. 生产组长工作台历史列表和历史表单看到该提交并正式确认。
3. 一线 PQC 真实填写页按发布 QA 项目提交。
4. PQC 组长工作台历史列表和历史表单看到该提交并复核，随后只读证明 task=CONFIRMED 且 aggregate detail 非空。
5. 生产组长活跃订单页自然显示生产/PQC 双 100%，点击“申请放行”。
6. eDHR 批次/正式表单页面只读核验三类 execution、字段审计和签名。
7. 生产负责人从正式放行待办/放行页面批准或驳回，不直接调用 API 代替页面操作。
8. `GET /mes/pro/edhr-release/get`、check items、events 和资料只读 API 仅用于页面操作后的最终核验。

前端已存在页面组件：`ProductionLeaderWorkbenchPage.vue`、`PqcLeaderWorkbenchPage.vue`、`FrontlineFixedTemplatePanel.vue`、`edhr-release/ReleasePage.vue`。实际菜单权限和动态路由必须由 A6 在运行环境确认，静态文件存在不能代替页面可达证据。

## 6. M0-04 运行时、事务和 hash 契约

### 6.1 固定顺序

申请编排顺序不得调整为先建待办后补资料：

1. 校验申请人是该 ACTIVE 活跃订单生产组长。
2. 锁定并读取活跃订单、工单、产品、发布路线版本和工序快照。
3. 从正式生产完成/确认与 PQC CONFIRMED 汇集重新计算双 100%，不信任前端进度。
4. 读取三类正式来源、目标绑定、字段映射和签名，构造 `AO_RELEASE_SOURCE_V1` hash。
5. 完成三个 writer 的无副作用 plan/validate；有 blocker 时直接保存 BLOCKED 申请。
6. 创建或复用 eDHR 批次执行。
7. 调用 BatchRecordWriter。
8. 调用 ProcessInspectionWriter。
9. 调用 LossReportWriter。
10. 执行三类资料完成性检查。
11. 执行 release precheck。
12. 调用 `submitForApproval` 创建或复用 `RELEASE_APPROVE` 待办。
13. 保存 `PENDING_RELEASE_APPROVAL` 申请回执。

这里在 V4 原 11 步中显式增加了“writer 无副作用预校验”和“保存申请回执”，不改变业务顺序。

### 6.2 双 100% 事实

- 生产 100%：所有活跃订单工序快照均有正式完成记录，完成数量满足目标，完成记录的回填状态、来源事件/分配和组长确认完整。
- 检验 100%：每个要求 PQC 的工序/任务均为 `CONFIRMED`，且存在结构化 aggregate detail、正式提交和 APPROVED 组长复核。
- 当前实现把 PQC `SUBMITTED` 计入完成不符合冻结契约，A2 必须以 `CONFIRMED + aggregate detail` 修正。

### 6.3 `sourceSnapshotHash`

算法固定为：`SHA-256(UTF-8(canonical JSON))`，版本前缀 `AO_RELEASE_SOURCE_V1`。规范化要求：对象 key 按字典序、数组按稳定业务键排序、时间用秒精度 ISO-8601、decimal 去无意义尾零但保留精确值、null 与空数组不混用。

hash 至少覆盖：

- tenant、active order、work order、product、batch、route/version 及排序后的工序快照身份。
- 生产完成/分配/回填记录、正式提交事件、历史表单、反馈数量、设备参数、损耗总量和明细、组长确认及各自 value hash。
- PQC task/record/event/review/aggregate detail、QA regulation version/item/equipment 及各自 value hash。
- 三类正式目标 binding 的 routeProcess、目标类型、report/definition/version 或 formTemplate/publishedTemplateVersion/bindingKey、snapshot hash、record category 和 formSlotType。
- 命中的字段映射 rule ID、sourceType/sourceField、target cell、启用状态和版本/更新时间。
- 所有填写/审核签名的 sourceType/sourceId/signatureId/userId/signedAt/evidenceHash。
- 路线 `RELEASE_APPROVE` rule ID、候选来源、候选用户稳定快照。

hash 明确排除：申请时间、当前时间、`applyRemark`、当前 blocker 列表、生成后的 application/batch/execution/transaction/task ID。当前实现只拼 ID 和 blocker type 的 hash 不满足此契约，A2 必须替换为规范化值快照。

### 6.4 事务边界

当前单个 `@Transactional apply(...)` 在 writer 产生预期 blocker 后直接返回会留下部分资料，因此不能沿用为最终边界。冻结方案：

- 读取/plan 阶段无副作用。预写 blocker 由独立短事务保存 `BLOCKED` 申请。
- batch open/create、三个 writer 写入、完成性检查、release precheck、`submitForApproval` 和 `PENDING_RELEASE_APPROVAL` 申请回执处于一个生成事务。
- 生成事务内任一 writer、完整性、precheck、负责人解析或持久化失败，必须回滚该事务内新批次、资料、审计、release transaction 和待办，不留下部分成功。
- 对可修复的确定性 blocker，生成事务回滚后由外层 facade 通过 `REQUIRES_NEW` service 或显式 `TransactionTemplate` 保存 BLOCKED 快照；不得依赖同类 self-invocation 触发新事务。
- 数据库、JSON、签名校验器、候选解析器或基础设施异常继续抛出，不转成 BLOCKED。
- 并发唯一键冲突后只允许回读并严格验证 request/business key 对应同一正式快照。

### 6.5 `RELEASE_APPROVE`

唯一负责人来源是路线级启用 `TASK_TYPE_RELEASE_APPROVE` assignment rule。`MesProEdhrWorkTaskServiceImpl.createReleaseApprovalTaskAfterSubmit(...)` 已按 release transaction 业务范围创建/复用待办并冻结候选快照，A2 继续复用。

缺规则、候选为空、用户/角色无效时为 `RELEASE_OWNER_REQUIRED`，不得用 `CLOSE`、工序开始上传人、当前登录人、stage owner 或静态角色替代。只有完成性和 precheck 均通过后才能调用 `submitForApproval`。

## 7. 当前实现差距与 Agent 归属

| 差距 | 归属 | M0 冻结处理 |
| --- | --- | --- |
| blocker DTO 只有六个基础字段 | A1/A2 | 增加四个可选定位字段并同步前后端合同 |
| apply 未调用三类 writer，`signatureEvidenceCount=0` | A2-A5 | 接入真实 writer，成功必须大于 0 |
| PQC `SUBMITTED` 被计为检验完成 | A2 | 改为 CONFIRMED + aggregate detail |
| source hash 只覆盖部分 ID 且含 blocker type | A2 | 按 `AO_RELEASE_SOURCE_V1` 重做 canonical hash |
| 当前单事务可能保留部分生成物 | A2 | facade + 生成事务 + 独立 BLOCKED 持久化 |
| 批记录 backfill 未显式携带当前 batch/task | A3 | 小适配复用现有 writer，输出必须属于当前批次任务 |
| 无过程检验动态目标自动写入/映射 | A4/P7 | 支持 template 28 FormCenter 正式目标和 `PQC_AGGREGATE_DETAIL` 精确映射；未接通前返回专用 blocker |
| 无损耗动态目标自动写入/映射 | A5/P7 | 支持 template 25 FormCenter 正式目标和 `PRODUCTION_LOSS` 精确映射；未接通前返回专用 blocker |
| 当前模板不能证明无损耗确认字段 | A5/A6 | 零损耗阻塞；首个成功 fixture 使用正损耗 |
| V4 指定前端静态测试路径不存在 | A1/A6 | 复用现有 static spec 或新增 V4 指定路径，禁止把不存在命令记 PASS |
| 真实 fixture/manifest 尚未提供 | A6 | 按本文件入口和 manifest 实现并真实页面核验 |

以上都是后续实现任务，不允许通过放宽正式来源或默认成功消除。

## 8. M0 放行判定

- 接口字段、状态、blocker 和幂等：已冻结。
- 三类 writer 正式来源、目标、输入、输出、完成条件和禁止替代来源：已冻结。
- fixture manifest、配置/业务入口和真实页面路径：已冻结。
- 双 100%、canonical hash、事务边界、完成性和 `RELEASE_APPROVE`：已冻结。
- 未启动 A1-A6，未修改生产代码或数据库。

结论：M0 通过。后续必须先由 A2 写出“当前 apply 未调用 A3/A4/A5”的 RED，再按 V4 顺序实施；任一 Agent 不得自行改写本契约中的正式来源和事务规则。

## 9. 代码事实索引

- 申请 Controller/VO/Service：`MesProcessPoolTeamLeaderController`、`MesTeamLeaderActiveOrderReleaseApplyReqVO`、`MesTeamLeaderActiveOrderReleaseApplicationServiceImpl`
- 前端入口：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`、`TeamLeaderWorkbenchPage.vue`
- 申请表：`20260808_mes_active_order_release_application.sql`、`MesProcessPoolActiveOrderReleaseApplicationDO`
- 批记录回填：`MesTeamLeaderBatchRecordBackfillServiceImpl`、`MesTeamLeaderOrderProcessCompletionService`
- PQC 汇集：`MesPqcProcessInspectionAggregationServiceImpl`、`MesPqcProcessInspectionAggregateDetailDO`
- 正式报表任务：`MesProEdhrBatchExecutionServiceImpl`、`MesProBatchRecordExecutionService`
- 放行事务/负责人：`MesProEdhrReleaseServiceImpl`、`MesProEdhrWorkTaskServiceImpl`
- 损耗来源：`MesProFeedbackDO`、`MesProProcessPoolEventDO`、`MesProFrontlineFeedbackPayloadSplitter`
- 字段映射：`MesProBatchRecordCellLinkRuleDO`、`MesProBatchRecordCellLinkServiceImpl`
