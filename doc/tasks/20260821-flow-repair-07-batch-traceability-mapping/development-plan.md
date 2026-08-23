# 批次执行完整映射和放行后追溯开发设计

## Latest Implementation Boundary (2026-08-23)

The task-owned Flow7 slice now covers Origin/TraceLink/Manifest persistence, canonical source identity checks, `hasActualLoss`/`NO_LOSS` mapping, traceability reads, post-release `RELEASE_DECISION` TraceLink lookup, and a formal Tx-C producer/outbox path. Release-decision commands require an explicit `originId`; list/detail resolution must use persisted TraceLinks and must not select an arbitrary origin.

Main-workspace Maven 3.9.16 compile, testCompile and the focused 29 tests passed (validator 17 + service contract 12, zero failures/errors, `BUILD SUCCESS`). The linked-worktree flatten/compiler ACL failures are historical context only. This does not establish upstream Flow1/4/5/6 formal-receipt adapters, real database migration/append-only trigger/Mapper/permission/runtime behavior, Flow8 four-material gating, Flow10 final `RELEASED`, full regression, service startup, or write-enabled E2E. Those remain `NOT RUN`/blockers, so the task status remains `partial / blocked`.

### Tx-C producer boundary (authoritative)

`POST /mes/pro/edhr-batch-execution/traceability/tx-c` accepts only `batchExecutionId`, event/idempotency keys, and optional witness hashes. At the persistence boundary it reads the successful Flow6 `OPEN` audit, Flow1 binding header/items, and immutable Flow4/2/3/5 evidence from the current tenant. It then performs the source precheck and a second read before commit. Any missing receipt/source evidence, tenant mismatch, canonical identity mismatch, hash/version change, or ambiguous provision relation returns `TRACE_MAPPING_BLOCKED`; source mutation after the first read is recorded as `SOURCE_CHANGED_AFTER_PRECHECK`.

The success `TransactionTemplate` commits Origin/TraceLink/Manifest and `mes_pro_edhr_batch_trace_outbox_event` together, then publishes `FLOW7_TRACE_MAPPING_SUCCEEDED` after commit. Failure rolls the graph transaction back and uses a separate transaction to commit `FLOW7_TRACE_MAPPING_FAILED_RETRYABLE` or `FLOW7_TRACE_MAPPING_FAILED_FINAL`; Flow6 alone consumes the success event and owns `BATCH_READY`. Duplicate event/idempotency requests return the existing immutable outbox record. Real database/Mapper/outbox runtime evidence remains `NOT RUN` because the formal upstream sourceEvidence and receipt owners are not yet available.

## Milestones

### 里程碑 1：规则与现状审计

目标：冻结正式来源规则、当前代码事实、根因和流程2/3/4/5/6/7/8/9/10/11职责边界。

涉及文件：
- `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/task.md`
- `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/backend-api-evidence.md`

交付物：
- 现状代码事实和正式来源 blocker 清单

### 里程碑 2：目标态与数据/API设计

目标：定义 Batch Origin、TraceLink、TraceManifest、状态 owner、事务、幂等、权限、历史迁移和跨线程接口契约。

涉及文件：
- `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/prd.md`
- `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/development-plan.md`
- `IntRuoyiBackend/sql/mysql/20260822_mes_edhr_batch_traceability.sql`

交付物：
- 关系模型、接口合同、迁移/回滚边界

### 里程碑 3：BDD/TDD与验收计划

目标：定义完成回填、建批、来源映射、无损耗、四份材料、后置放行和独立入口的 Given/When/Then、RED/GREEN/REGRESSION门禁。

涉及文件：
- `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/test-plan.md`
- `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/execution-log.md`

交付物：
- 可执行测试矩阵和失败 blocker 记录

### 里程碑 4：文档与证据结构验证

目标：验证任务必需文档、章节、职责表、接口契约和证据文件结构完整且保持一致。

涉及文件：
- `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/verification-report.md`
- `doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/database-schema-evidence.md`

交付物：
- 文档结构检查和 API/数据库证据校验结果

### 里程碑 5：流程7实现切片与验证

目标：在已存在 batchExecutionId 和正式上游 receipt 的前提下，实现并验证 Origin/TraceLink/Manifest、canonical source identity、损耗事实和 RELEASE_DECISION 后置追溯。

涉及文件：
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord`

交付物：
- Flow7 task-owned API/service/SQL/test slice
- 29项定向测试和完整链路 blocker 证据

## 1. 目标态与修改边界

### 1.1 目标态

批次执行是由明确业务触发建立的可审计容器。生产/PQC 提交及各自组长复核先形成流程 2/3 正式来源事实；活跃订单双进度达到 100% 后由流程 4 唯一完成 owner 在 Tx-A 统一回填三类适用表单并提交不可变 completionBackfillReceipt；流程 6 在 receipt 成功后以 Tx-B 创建或复用 batchExecutionId。流程 7 只消费流程 6 返回的 batchExecutionId、流程 4 receipt、流程 1 领料绑定及流程 2/3/5 正式来源，在后继事务或可验证的幂等事件消费中建立 Origin、TraceLink、Manifest 和放行后追溯。releaseApplicationId 只有在流程 10 后续确实产生最终放行决定时才能追加 RELEASE_DECISION 关系，不能作为活跃订单建批或流程 7 映射前置。生产工单、路线/版本、批号、正式领料单及分录、生产/PQC/损耗事实、三类回填收据均必须被冻结并可验证。独立/手工/排产等其它批次创建途径各自拥有正式来源凭证、状态所有者、幂等键和追溯关系，不得强行复用活跃订单放行申请。

不在本专项实现范围内：一线生产签名、PQC 签名、双进度计算、三类表单实际填报、文件存储适配器和管理者代表角色实现。本专项只消费、冻结、校验、显示和追溯这些正式事实。

### 1.2 禁止方式

- 不能由工单、批号、路线推断活跃订单或领料单。
- 不能由 formBindings、工序开始配置、默认 MAIN、当前设备、当前登录用户或当前路线补齐来源。
- 不能把审计事件、页面显示、原始 JSON、空附件或 HTTP 成功当作来源关系成立。
- 不能为历史批次静默回填、自动复用或删除；来源不全必须阻断。

## 2. 当前代码事实与根因

| 范围 | 已有事实 | 缺口/根因 |
| --- | --- | --- |
| 批次实体 | MesProEdhrBatchExecutionDO 有工单、批号、产品、路线/版本、路线快照、状态、aggregate hash 与关闭审计 | 无 activeOrderId、申请/完成交易、领料单头/分录、生产/PQC/损耗来源及冻结来源包 |
| 批次唯一性 | Mapper 按工单、批号、路线查询 | 不含活跃订单和完成申请，可能混淆不同业务完成 |
| 申请记录 | 活跃订单申请有活跃订单、工单、路线、批号、批次 ID、资料 hash、幂等键 | 不是批次第一类来源；无领料分录及签名/损耗明细 |
| 资料回填 | MesPqcReleaseDossierPortImpl 读取生产事件、分配/复核、PQC 汇集且拒绝 formBindings 冒充传统绑定 | 未读取正式领料单/分录；零损耗规则与目标冲突 |
| 批次 API | 有详情、列表、工作台、上传、关闭、驳回、时间线 | 请求/响应/时间线无来源图；人工打开只传工单、批号、路线、备注 |

根因：批次仍以工单执行上下文设计，活跃订单放行申请只是旁路关联；来源图未成为批次的事务性持久模型和 API 合同。

## 3. 数据与关系设计

| 逻辑实体 | 必填身份 | 职责 |
| --- | --- | --- |
| BatchExecutionOrigin | batchExecutionId、originType、originKey、activeOrderId、completionTransactionId、backfillReceiptId、pickListBindingId、sourceSnapshotHash、capturedAt、releaseApplicationId? | 每批次一条主来源。ACTIVE_ORDER_COMPLETION 以订单完成/回填和领料绑定为主；releaseApplicationId 只能作为后续放行阶段的可选追加关联。PQC_INDEPENDENT、MANUAL、SCHEDULED 等入口必须有各自正式前置。 |
| BatchExecutionTraceLink | batchExecutionId、linkType、sourceObjectType、sourceObjectId、sourceLineId、sourceEventId、snapshotJson、snapshotHash、capturedAt、capturedBy | 不可更新的逐来源关系。 |
| BatchExecutionTraceManifest | batchExecutionId、manifestVersion、previousManifestHash、manifestJson、manifestHash、sealedAt、sealReason | 追加后重算清单 hash，形成 hash 链；封存项不可改。 |
| BatchExecutionDossierRequirement | batchExecutionId、documentType、required、status、fileId、fileHash、uploadedBy、uploadedAt | 四项独立资料要求。 |

TraceLink 类型至少包括 ACTIVE_ORDER、WORK_ORDER、MATERIAL_ISSUE、MATERIAL_ISSUE_LINE、PRODUCTION_SUBMIT、PRODUCTION_SIGNATURE、PRODUCTION_LEADER_REVIEW、PQC_TASK、PQC_SUBMISSION、PQC_SIGNATURE、PQC_LEADER_CONFIRMATION、PQC_AGGREGATE_DETAIL、LOSS_FACT、LOSS_REPORT_RECEIPT、BATCH_RECORD_RECEIPT、PROCESS_INSPECTION_RECEIPT、DOSSIER_FILE、RELEASE_DECISION。

四项资料类型固定为 INCOMING_INSPECTION_REPORT、STERILIZATION_REPORT、FINISHED_PRODUCT_INSPECTION_REPORT、FINISHED_PRODUCT_INSPECTION_RECORD，不能把两项成品检合并。

### 3.1 约束和 hash

- ACTIVE_ORDER_COMPLETION 主来源唯一键：tenant_id + active_order_id + completion_transaction_id；同一订单完成交易只允许一个批次 origin。流程 1 的 pickListBindingId 与 sourceSnapshotHash 必须进入来源快照并参与幂等校验。后续放行申请若实际产生，再以 tenant_id + release_application_id 追加唯一 RELEASE_DECISION 关系，不回写为建批前置。
- 同一批次的 sourceObjectType + sourceObjectId + sourceLineId + linkType 唯一；相同来源快照可幂等返回，不同 hash 返回 TRACE_SOURCE_CONFLICT。
- 领料单必须唯一、已审核、正式关联工单；每个引用物料有稳定 sourceEntryId。DCC、路线产品、工单、工序、物料或分录不一致时返回 MATERIAL_ISSUE_SOURCE_INVALID。
- LOSS_FACT 仅在实际损耗大于零时允许。无损耗只保存 NO_LOSS_CONFIRMED 快照，不能创建损耗单收据或零损耗报告。
- snapshotJson 需含稳定业务号、版本/状态、签名/人员/时间、数量、来源 ID、前一层 hash。snapshotHash 为 canonical JSON 的 SHA-256，序列化规则必须被冻结并跨端测试。

### 3.2 关系图

流程 1 绑定正式领料单 -> 流程 2 一线生产提交、生产组长复核/驳回重提并形成分配事实；流程 3 一线 PQC 提交、PQC 组长复核确认并汇集事实 -> 活跃订单双进度均为 100% -> 流程 4 唯一 owner 点击完成，在 Tx-A 统一回填批记录、过程检验单及（流程 5 判定有实际损耗时）损耗单，提交不可变 completionBackfillReceipt -> 流程 6 在 receipt 成功后以 Tx-B 创建/复用 batchExecutionId 并提交 batch provision receipt/status -> 流程 7 在后继 Tx-C 或可验证幂等事件消费中消费 batchExecutionId、receipt 和流程 1/2/3/5 正式来源，建立 Origin、TraceLinks、Manifest -> 流程 8 上传四份资料并执行硬门禁 -> 流程 10 消费既有 batchExecutionId 和流程 8 gate，写最终放行状态/签名/审计 -> 放行后完整追溯。流程 9 只定义多入口的 entryType、正式凭证、来源关系和幂等前置；流程 11 负责 BDD/TDD、回归、迁移和总门禁。

独立/手工/排产入口：流程 9 先校验各自 entryType、正式 sourceCredential、幂等键和来源关系 -> 流程 6 或对应创建 owner 取得 batchExecutionId -> 流程 7 才能建立该批次的 Origin、TraceLinks、Manifest。入口没有活跃订单时不得生成 activeOrderId，也不得伪造 releaseApplicationId 关系；不适用的订单/领料/完成关系必须显式记录 NOT_APPLICABLE。

## 4. 状态、所有者和事务

| 动作/状态 | 唯一状态所有者 | 前置条件 | 同一事务输出 |
| --- | --- | --- | --- |
| ACTIVE_ORDER_COMPLETED | 流程 4 活跃订单完成服务，唯一 owner | 流程 1 领料绑定、流程 2 生产正式事实、流程 3 PQC 正式事实、流程 5 损耗决定、双 100% | Tx-A 三类适用回填、不可变 completionBackfillReceipt；不创建批次、不建立 batchExecutionId 映射 |
| BATCH_PROVISIONED | 流程 6 批次 provision 服务，唯一 owner | 流程 4 receipt 成功且三类适用回填可验证 | Tx-B batchExecutionId、created/reused、batch provision receipt/status、sourceSnapshotHash |
| TRACE_CAPTURED | 流程 7 批次来源映射服务，唯一 owner | 已有 batchExecutionId、流程 6 成功 receipt、流程 4 receipt 及流程 1/2/3/5 正式来源 ID/快照/hash | 后继 Tx-C 或幂等事件消费写入 Origin、TraceLinks、初始 Manifest；不完成、不回填、不建批 |
| DOSSIER_INCOMPLETE/DOSSIER_COMPLETE | 批次资料服务，流程 8 | 四项类型和文件校验 | 文件链接/hash、manifest 追加 |
| RELEASED | 管理者代表最终放行服务，流程 10 唯一 owner | 既有 batchExecutionId、流程 8 四项 gate、正式候选/权限、签名 | 最终放行状态/签名/审计、release decision、manifest 封存；可追加 RELEASE_DECISION 关系 |

流程 2 只写一线生产提交、生产组长复核/驳回重提和分配事实；流程 3 只写一线 PQC 提交、PQC 组长复核确认和汇集事实；流程 5 只写实际损耗判定、损耗单条件和 NO_LOSS 事实；流程 6 只写批次 provision 状态；流程 7 只写批次映射与追溯投影；流程 8 只写四份材料和 gate；流程 10 只写最终 RELEASED。完成来源事实由流程 4 的 Tx-A receipt 冻结，批次 provision 由流程 6 的 Tx-B 完成，流程 7 的映射必须在 batchExecutionId 成功返回后通过后继 Tx-C 或可验证幂等事件消费写入，不能把映射写入订单完成命令同事务，也不能反查当前投影、最新记录或名称猜测成功。

## 5. 接口设计

### 5.1 内部同步端口

流程 6 的建批命令为 openOrCreateFromActiveOrderCompletion(command)，由流程 6 持有。命令必须含 activeOrderId、completionTransactionId、workOrderId/code、routeId/versionId、batchCode、expectedActiveOrderVersion、completionVersion、sourceVersion、pickListBindingId、sourceSnapshotHash、正式领料单头与分录、流程 2/3/5 正式来源 ID/快照/hash、流程 4 completionBackfillReceiptId/hash、三类适用回填结果、sourceBundleHash、idempotencyKey。不得要求 releaseApplicationId。

流程 7 的映射命令为 captureBatchExecutionTrace(command)，不得创建或复用批次。命令必须含已存在的 batchExecutionId、batchProvisionReceiptId、batchProvisionStatus（created/reused）、activeOrderId（仅 ACTIVE_ORDER_COMPLETION）、completionTransactionId、completionVersion、completionBackfillReceiptId/hash、流程 1 的 pickListBindingId/pickListId/bindingVersion/sourceSnapshotHash 及领料头/分录快照、流程 2/3/5 正式来源 ID/版本/签名或设备快照/hash、sourceBundleHash、idempotencyKey。缺少 batchExecutionId 或流程 6 成功 receipt 返回 BATCH_PROVISION_REQUIRED；来源 hash 冲突返回 TRACE_SOURCE_CONFLICT；映射缺失时状态保持 TRACE_MAPPING_BLOCKED，不得允许追溯完成或放行。

入口合同：

| 入口 | 正式前置/凭证 | 幂等键 | 追溯边界 |
| --- | --- | --- | --- |
| ACTIVE_ORDER_COMPLETION | 流程 4 completionBackfillReceipt + 流程 6 batch provision receipt；receipt 内含 activeOrderId + completionTransactionId + 三类适用回填 + pickListBindingId/sourceSnapshotHash | tenant + activeOrderId + completionTransactionId | 流程 7 只在 batchExecutionId 已存在后回链订单、工单、领料、生产/PQC/损耗和回填；放行申请可后置追加 |
| PQC_INDEPENDENT | 流程9签发的独立 PQC `IndependentBatchPrerequisiteReceipt`、路线/版本、批号和该入口 own sourceCredential | tenant + originType + sourceCredential | 只追溯该入口正式凭证，不显示活跃订单或放行申请 |
| MANUAL | 受授权人工创建理由、工单/路线/批号和人工来源凭证 | tenant + originType + sourceCredential | 只显示人工来源和后续资料；不得冒充活跃订单 |
| SCHEDULED | 排产单/生产任务及正式批号、路线快照 | tenant + originType + scheduleSourceId | 回链排产和工单；没有活跃订单时不得补算 activeOrderId |

HTTP JSON 中 Long ID 一律字符串。业务失败须返回稳定 blockerCode、blockerScope、missingIds、retryable，不允许成功码包失败或仅以中文 message 分支。

### 5.2 读取接口

| 接口 | 目的 | 最小返回 |
| --- | --- | --- |
| GET /mes/pro/edhr-batch-execution/{id}/traceability | 批次追溯图 | origin、订单、工单、领料分录、生产/PQC/损耗、回填、四项资料、放行、manifest hash、权限结果 |
| GET /mes/pro/edhr-batch-execution/page | 列表筛选 | activeOrderId、workOrderCode、materialIssueCode、releaseApplicationCode、放行和资料状态、摘要 hash |
| GET /mes/pro/active-order/{id}/release-traceability | 从订单反查 | 完成申请、批次、资料状态、放行状态和 blocker |
| GET /mes/pro/edhr-batch-execution/{id}/traceability/manifest | 合规审计 | manifest 版本、hash 链、封存状态 |

人工、独立、排产创建批次必须明确各自 originType 与正式来源凭证。若标注为 ACTIVE_ORDER_COMPLETION，强制走订单完成端口；不能用旧的工单、批号、路线、备注接口冒充，也不能把 releaseApplicationId 作为所有入口的通用身份。

## 6. 权限和不可篡改

- 生产组长查看自己负责订单和由其完成申请产生的摘要，不能放行。
- PQC 组长查看自身确认事实，不能改写生产、领料或放行来源。
- 管理者代表候选只能来自正式角色；xujianhai 是首版环境授权要求，不能成为代码默认用户。
- 审计/质量权限可见完整来源图，但签名密文、附件和个人信息仍按对象级 scope 脱敏。
- TraceLink 和封存 manifest 禁止更新/删除；纠错只能追加 CORRECTION 或 VOID_REFERENCE，保留 prior hash、理由、审批人、时间。放行后通过正式撤销/重放行，不得直接编辑。

## 7. 前端设计边界

批次详情增加业务追溯入口，调用独立 traceability API，分组展示活跃订单、工单、领料单/分录、生产、PQC、损耗、三类回填、四项资料、放行。列表按来源编号和资料齐套状态筛选。不能用通用时间线、备注或文件数量替代关系；缺失时展示后端 blocker 并禁用放行。复用详情页时 query scope 必须区分追溯与签核。

## 8. 历史、回滚和实施 blocker

### 历史和迁移

先只读盘点。ACTIVE_ORDER_COMPLETION 历史批次仅当 activeOrderId、completionTransactionId、三类回填证据、pickListBindingId/sourceSnapshotHash、source bundle hash、幂等键和批次 ID 都可验证时，才建立受审计历史 Origin/links；releaseApplicationId 不是历史建批必需字段，仅在已有放行申请时追加。独立/手工/排产历史批次按其自身 originType/sourceCredential 核验。缺订单、领料分录、完成交易或回填关联的订单批次，以及缺自身正式凭证的其它入口批次，统一标记 LEGACY_TRACEABILITY_MIGRATION_REQUIRED，不认领、不复用、不删除。迁移必须另行审批、演练、备份和回滚。

### 回滚

未放行且当前事务失败时，回滚本次申请、回填收据、links、批次和资料要求，保留来源事实。已放行不得物理回滚或改写，使用正式撤销放行和纠错引用。

### Blocker

1. 尚未确认流程 1 提供的 pickListBindingId、sourceSnapshotHash 与生产工单到领料单头/分录的正式实体、唯一审核状态及稳定 sourceEntryId。
2. 现有零损耗报告合同与目标冲突，必须先由资料/回填所有者统一。
3. 完成交易 ID、三类回填收据与 source bundle canonical JSON 尚未冻结。
4. 四份文件的类型、版本、hash、对象权限尚未提供可消费合同。
5. 管理者代表角色和 xujianhai 授权尚待流程 10/环境确认。

## 9. 与流程修复线程的接口契约

| 线程 | 提供给本专项的正式输入 | 本专项输出/限制 |
| --- | --- | --- |
| 1 | pickListBindingId、pickListId、bindingVersion、sourceSnapshotHash、领料头/分录冻结快照 | 只拥有领料绑定；流程7按正式 ID/版本/hash 消费，不猜测来源 |
| 2 | productionFactEventId、reviewEventId、allocationEventId、submission/review/allocationVersion、payloadHash、signatureSnapshot | 只拥有生产提交/复核/驳回重提/分配事实；不完成、不回填 |
| 3 | PQC 提交/确认/汇集事件 ID、版本、设备快照和 hash | 只拥有 PQC 正式事实；不完成、不回填 |
| 4 | completionTransactionId、completionVersion、completionBackfillReceiptId/hash、三类回填结果 | 唯一拥有双100%点击完成和 Tx-A 回填；流程7只消费 receipt |
| 5 | hasActualLoss、NO_LOSS 或正式 lossRecord/lossReceipt | 只拥有损耗判定；仅实际损耗时产生损耗单，流程7不补写 |
| 6 | batchExecutionId、created/reused、batch provision receipt/status、sourceSnapshotHash | 只拥有 receipt 成功后的 Tx-B 批次 provision；流程7不得创建/复用批次 |
| 7 | 无上游新增业务事实；消费流程6 batchExecutionId、流程4 receipt、流程1/2/3/5正式来源 | 只建立 Origin/TraceLink/Manifest 和追溯投影；不拥有完成、回填、建批、资料或最终放行 |
| 8 | 四份材料当前有效版本、文件 hash、manifest/gate 结果 | 只拥有批次创建后的材料上传和硬门禁 |
| 9 | 多创建/放行入口的 entryType、正式 sourceCredential、来源关系、幂等前置、NOT_APPLICABLE 规则 | 不拥有批次状态或追溯投影，不按工单号/批号/当前投影猜测来源 |
| 10 | 既有 batchExecutionId、流程8 gate、releaseDecision、放行签名和最终 manifest 封存结果 | 唯一写最终放行状态/签名/审计，可追加 RELEASE_DECISION，不反向成为建批前置 |
| 11 | BDD/TDD、真实回归、迁移/回滚和总门禁证据 | 只读核验并输出总门禁/blocker，不自动认领或修复 |

任一线程改变字段、错误码、Long ID JSON 类型、事务或幂等策略，必须先更新本专项提供方/消费方合同测试。

### 主流程冻结核验
流程4独占 receipt，流程6独占 BATCH_*；流程7只拥有完整映射/trace graph。独立批次不适用关系返回 NOT_APPLICABLE+reasonCode，应有关系缺失返回 MISSING/BLOCKED；历史迁移先 dry-run 分类 INCOMPLETE_OR_AMBIGUOUS 或 ALREADY_RELEASED_REVIEW_REQUIRED。
