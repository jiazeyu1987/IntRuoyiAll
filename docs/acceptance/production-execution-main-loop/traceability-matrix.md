# P0 生产执行主闭环追溯矩阵

## Purpose and Scope

本文档把 P0 审计问题拆成系统事实、来源表或服务、BDD 场景和 TDD 验证。矩阵用于避免后续开发只做页面展示而没有正式数据链路。

## Evidence Reviewed

- `docs/acceptance/production-execution-main-loop/bdd-scenarios.md`
- `docs/acceptance/production-execution-main-loop/tdd-plan.md`
- `docs/acceptance/production-execution-main-loop/e2e-plan.md`
- 当前工序池、生产组长、批记录回填和 trace 相关服务代码。

## Traceability Matrix

| 审计问题 | 必须返回的结构化事实 | 正式来源 | BDD 场景 | TDD 覆盖 |
| --- | --- | --- | --- | --- |
| 谁提交 | 实际员工 ID、姓名、设备登录账号、签名员工 | 工序池提交事件、电子签名记录 | 生产员工一次提交形成主事件 | P0-T02 |
| 在哪台设备 | 设备 ID、设备编号、工作站 ID | 工序池提交事件、班组设备配置 | 生产员工一次提交形成主事件 | P0-T02 |
| 做了哪个工序 | 路线 ID、路线工序 ID、MES 工序 ID、工序名称 | 工序池提交事件、路线工序 | 生产员工一次提交形成主事件 | P0-T02 |
| 做了多少 | 完成数量、损耗数量、PQC 检验数量、PQC 合格数量、确认数量、分配数量 | 原始 payload、PQC 明细、分配明细、订单工序完成 | FIFO 分配、组长确认后累计完成订单工序、PQC 合格数量不足阻塞 FIFO | P0-T04、P0-T07 |
| 质量结果怎样 | PQC 任务、规程版本、逐件明细、质量结论、质量可分配状态、合格数量覆盖结果、结构化生产提交绑定 ID | PQC 任务、PQC 明细、工序池 PQC 事件、正式绑定字段或关系表 | PQC 正式提交进入工序池质量链路、PQC 绑定不能只藏在 rawPayload、PQC 合格数量不足阻塞 FIFO | P0-T03、P0-T04、P0-T09A |
| 签名是谁 | 提交签名、PQC 签名、复核签名、签名员工和签名快照 | 电子签名字段和签名服务 | 生产员工提交、PQC 提交、班组长复核 | P0-T02、P0-T03、P0-T06 |
| 进入哪个生产工单 | 活跃订单 ID、生产工单 ID、生产工单编号、分配明细 | 活跃订单、分配明细 | 生产工单 FIFO 分配只消耗活跃订单 | P0-T07 |
| 班组长是否复核 | 复核状态、复核角色、是否强制、复核说明、复核人、复核时间、复核签名 | 提交复核表、复核角色配置 | 班组长复核必须签名且不改写原始提交、强制复核未完成不得进入 FIFO | P0-T05、P0-T06 |
| 如何进入批记录追溯 | 批记录执行 ID、正式报表 ID、定义 ID、版本 ID、字段审计 batch、字段审计明细、来源值、旧值、新值、单元格位置 | 批记录执行、字段审计、正式绑定、字段映射 | 活跃订单完成后统一回填正式资料 | P0-T08、P0-T10 |
| 一键闭环视图 | 以上所有节点、状态、阻塞原因和正式 ID | 统一 trace 服务 | 统一闭环 trace 回答 P0 审计问题 | P0-T09、P0-T10 |

## Missing-Link Rules

- 缺少任何一个关键节点时，trace 顶层必须返回 `complete=false`，对应分组返回 `status=BLOCKED` 或等价明确状态，并说明缺失对象和影响。
- 任一分组的 `sourceIds` 为空、只有展示名称、只有摘要文本或只有前端拼接 ID 时，该分组视为缺失。
- 若 PQC 任务存在但没有工序池 PQC 事件，质量链路视为未闭环。
- 若 PQC 工序池事件无法通过正式 ID 唯一绑定到当前生产提交事件，质量链路视为歧义并阻塞。
- 若 PQC 绑定只存在于 rawPayload、备注、扩展 JSON 或页面摘要，质量链路视为未正式绑定并阻塞。
- 若 `processPoolEventId` 指向 PQC 事件而无法解析到唯一 `PRODUCTION_SUBMIT` 生产提交根事件，trace 必须返回候选或阻塞，不能把 PQC 事件当完整闭环根。
- 若 PQC 合格数量、可分配数量或已消耗数量无法证明覆盖本次确认数量，质量和分配链路视为未闭环。
- 若复核记录存在但没有复核电子签名，复核链路视为未闭环。
- 若订单工序完成但批记录字段审计投影缺失，批记录追溯视为未闭环。
- 若批记录来源不是正式逐工序批记录表单绑定，trace 必须阻塞。
- 若按生产工单 + 工序查询命中多条事件，trace 必须返回候选列表或要求选择事件；不得合并成单条完成链路。
- `complete=true` 或等价完成状态只能在提交、质量、复核、分配、完成和批记录全部拥有正式结构化 ID 时返回；否则必须保持 `complete=false`。
- 若任一分组事实来自不同租户、生产工单、路线工序、MES 工序或当前操作者无权访问的数据，trace 必须阻塞或排除该事实，不得拼接成闭环。
- 若正式字段缺迁移、测试 schema、DO/Mapper、唯一约束或索引证据，该字段不得进入完成谓词。

## Section Completion Predicates

后端 trace 服务必须按以下分组谓词计算 `complete`，不得把页面显示状态、中文标签或空数组当完成：

| 分组 | 完成谓词 | 常见 blocker |
| --- | --- | --- |
| `submitEvent` | 有唯一 `eventType=PRODUCTION_SUBMIT` 的 `processPoolEventId`，且 `feedbackId`、记录本 ID、员工、设备账号、设备、工作站、提交签名齐备。 | `SUBMIT_EVENT_MISSING`、`SUBMIT_EVENT_TYPE_INVALID`、`DEVICE_CONTEXT_MISSING`、`SUBMIT_SIGNATURE_MISSING` |
| `quality` | 有正式 `pqcEventId`、`pqcTaskId`、规程版本、逐件明细摘要、检验数量、合格数量、可分配数量、已消耗数量、质量结论、PQC 签名，并能通过结构化字段或关系唯一绑定生产提交事件。 | `PQC_EVENT_MISSING`、`PQC_BINDING_MISSING`、`PQC_BINDING_AMBIGUOUS`、`QUALITY_NOT_ALLOCATABLE`、`QUALITY_QUANTITY_MISMATCH` |
| `review` | 所有强制复核角色的复核记录、复核人、复核时间、复核签名和来源事件齐备；非强制复核角色必须返回配置状态。 | `REVIEW_MISSING`、`REVIEW_SIGNATURE_MISSING`、`REVIEW_SCOPE_FORBIDDEN`、`MANDATORY_REVIEW_PENDING` |
| `allocation` | 分配明细均指向活跃订单，合计等于确认数量，且来源复核和来源事件齐备。 | `ACTIVE_ORDER_MISSING`、`ALLOCATION_QUANTITY_MISMATCH`、`QUALITY_GATE_BLOCKED` |
| `completion` | 订单工序完成记录保存目标数量、累计确认数量、最后来源事件、最后复核和完成时间。 | `ORDER_PROCESS_NOT_COMPLETE`、`COMPLETION_SOURCE_MISSING` |
| `batchRecord` | 正式批记录执行、报表/定义/版本、字段审计 batch/item、字段路径、单元格位置、来源值、旧值、新值和来源事件或分配 ID 齐备。 | `BATCH_RECORD_BINDING_MISSING`、`FIELD_AUDIT_MISSING`、`FIELD_AUDIT_VALUE_MISSING`、`FORM_BINDING_NOT_ALLOWED` |

`complete=true` 的计算公式固定为：六个分组全部满足完成谓词，顶层和分组 `blockers` 均为空，且没有未选择的 `candidateEvents`。任一分组 `BLOCKED`、`FAILED`、`AMBIGUOUS` 或 `MISSING` 都必须让顶层 `complete=false`。

## Trace Completion Contract

- trace 顶层必须包含 `processPoolEventId`、`complete`、`sections`、`blockers` 和 `candidateEvents`；每个 trace 分组必须包含 `status`、`sourceIds`、`blockers` 和 `lastUpdatedAt`；`status` 只能来自后端枚举，不得来自前端文案。
- trace 顶层 `processPoolEventId` 必须是生产提交根事件；若查询入参是 PQC 事件 ID，响应必须同时返回解析后的生产提交根事件 ID 或候选/阻塞原因。
- `sourceIds` 至少包含当前节点的正式 ID，并在适用时包含 `processPoolEventId`、`productionSubmitEventId`、`pqcEventId`、`reviewId`、`allocationId`、`orderProcessId`、`batchRecordExecutionId`、`fieldAuditBatchId`。
- `productionSubmitEventId` 和 `pqcEventId` 的关联必须来自结构化字段或关系；rawPayload 中的同名字段只能进入审计详情，不能进入完成谓词。
- `sourceIds` 必须同时携带或可反查 `tenantId`、`workOrderId`、`routeProcessId`、`processId` 和来源事件链；缺同源证据时分组必须 `BLOCKED`。
- 字段进入 `sourceIds` 或完成谓词前，必须能追溯到迁移脚本、测试 schema、DO/Mapper 和索引或唯一约束；只在接口层出现时不得计入完成。
- `blockers` 必须包含机器可读 `code`、用户可读 `message`、缺失对象类型和解除条件；不得只返回空数组或“暂无数据”。
- trace 接口必须只读；调用 trace 不得触发复核、分配、订单工序完成、批记录回填或任何补偿写入。
- `complete` 只能由后端按 `submitEvent`、`quality`、`review`、`allocation`、`completion`、`batchRecord` 六个分组同时为完成且 blocker 为空计算；前端不得覆盖或重算为 true。
- `candidateEvents` 只允许用于多事件选择；一旦 `candidateEvents` 非空且未选定唯一 `processPoolEventId`，顶层必须 `complete=false`。

## Trace Maturity Gates

- M3 initial GREEN 只证明统一 trace 入口、DTO 和基础分组存在；不能替代 P0 完成验收。
- `quality` 分组必须能证明 PQC 事件正式绑定目标生产提交事件或提交数量片段，并证明合格可分配数量覆盖本次确认数量；同工单、同工序、同员工、时间接近、rawPayload 解析、第一条记录或单独 `inspectionResult=SUCCESS` 都不是完成证据。
- `review` 分组必须聚合所有强制复核角色；只返回第一条复核、只返回中文角色名称或缺签名来源事件时必须 `BLOCKED`。
- `allocation` 与 `batchRecord` 分组必须暴露来源事件和来源分配 ID；批记录字段审计缺 batch/item、字段路径或单元格位置时不得完成。
- 按工单 + 工序查询命中多条主事件或质量候选时，`candidateEvents` 必须列出候选并让顶层 `complete=false`；空候选列表不得掩盖歧义。
- trace 成熟度测试必须覆盖 schema-backed 字段缺失、跨租户候选、跨工单候选和一对多明细聚合；只测 happy path 不算成熟。

## Reconciliation Checks

- 数量勾稽：提交完成数量、PQC 检验数量、PQC 合格数量、可分配质量数量、已消耗质量数量、确认数量、分配明细合计、订单工序累计确认数量和批记录回填数量必须可解释一致；差异必须返回 blocker。
- 身份勾稽：生产提交签名员工等于实际生产员工，PQC 签名员工等于实际 PQC 员工或正式授权人，复核签名员工等于复核人或正式授权复核人。
- 设备勾稽：生产提交和 PQC 事件必须保存正式设备账号、设备和工作站；缺设备上下文时不得回答“在哪台设备”。
- 批记录勾稽：字段审计的字段映射来源必须是 `PROCESS_POOL_REPORT` 和正式逐工序批记录表单绑定；出现 `formBindings`、默认 `MAIN` 或工序开始配置即阻塞。
- 幂等勾稽：重复生产提交、PQC 提交、复核或确认必须命中同一正式结果或明确重复拒绝；新增第二条有效终态即阻塞。
- schema 勾稽：新增字段必须在迁移 SQL、测试 schema、DO/Mapper 和索引或唯一约束中一致；任一漂移必须阻塞。
- 迁移勾稽：新增正式 SQL 必须通过 release migration policy gate；`NOT NULL`、唯一约束或索引收紧前必须证明历史未删除行已有正式来源 ID，或保留明确 backfill blocker。
- 租户权限勾稽：trace 和批记录只读核验必须证明当前用户只能看到授权租户、工单和工序范围内的事实。

## Closure Evidence Mapping

最终验收不得只看“trace 页面能打开”。必须把九个审计问题逐项映射到 trace 分组和正式来源，形成可复验的闭环证据包：

| 证据字段 | 对应审计问题 | 必须来自的 trace 分组 |
| --- | --- | --- |
| `answers.who` | 谁提交 | `submitEvent`、`signature` |
| `answers.device` | 在哪台设备 | `submitEvent`、`quality` |
| `answers.process` | 做了哪个工序 | `submitEvent`、`completion` |
| `answers.quantity` | 做了多少 | `submitEvent`、`quality`、`allocation`、`completion`、`batchRecord` |
| `answers.quality` | 质量结果怎样 | `quality` |
| `answers.signature` | 签名是谁 | `submitEvent`、`quality`、`review` |
| `answers.workOrder` | 进入哪个生产工单 | `allocation`、`completion` |
| `answers.review` | 班组长是否复核 | `review` |
| `answers.batchRecord` | 如何进入批记录追溯 | `batchRecord` |

每个证据字段必须包含 `value`、`sourceIds`、`section`、`sameSource=true` 或 blocker、以及只读复验入口。任一字段缺失或只由页面文案构造时，最终 trace 不得 `complete=true`，真实 E2E 不得 PASS。

## E2E Mapping

- E2E-P0-01：一线员工从真实前端入口提交生产执行事件，验证报工、记录本、提交签名和工序池主事件。
- E2E-P0-02：PQC 员工从真实前端入口提交质量结果，验证 PQC 任务、逐件明细、质量签名和工序池质量事件。
- E2E-P0-03：班组长从真实工作台复核并确认 FIFO 分配，验证复核签名、活跃生产工单、订单工序完成和活跃订单完成统一回填。
- E2E-P0-04：审计用户从 trace 入口按 `processPoolEventId` 查看闭环，验证所有 P0 审计问题均有正式来源或明确阻塞原因。

## Report Shape

统一 trace 必须至少返回以下分组：

- `submitEvent`：生产提交根工序池事件、事件类型、报工来源、记录本来源、原始 payload 摘要、提交签名。
- `quality`：PQC 子事件、PQC 任务、逐件明细摘要、检验数量、合格数量、可分配数量、已消耗数量、质量结论、可分配状态。
- `review`：生产组长 / PQC 组长复核状态、复核签名和说明。
- `allocation`：活跃订单、生产工单、FIFO 或手工分配明细、确认数量。
- `completion`：订单工序目标数量、累计确认数量、完成状态、完成时间。
- `batchRecord`：正式批记录执行、字段审计 batch、字段审计 item、字段映射来源。
- `blockers`：缺失正式前置、越权、质量不可分配、签名缺失、字段映射缺失。
- `schemaEvidence`：迁移脚本、测试 schema、DO/Mapper、索引或唯一约束验证状态；仅用于诊断和验收证据，不替代业务事实。
- `migrationPolicyEvidence`：release migration policy gate、迁移 metadata、dependsOn、历史断链 fail-fast 和运行态迁移核验状态；仅用于诊断和验收证据，不替代业务事实。
- `closureEvidence`：九个审计问题的脱敏答案、正式来源 ID、同源校验和只读复验入口；仅在全部答案完整时允许支撑最终 PASS。
