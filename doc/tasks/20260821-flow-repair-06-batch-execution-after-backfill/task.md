# 流程修复6：统一回填成功后创建或复用批次执行

## Task Goal

本任务只做代码审计、需求澄清和开发文档设计，不修改生产代码、数据库、服务或运行写入型 E2E。目标是冻结“活跃订单完成 -> 三类正式回填 -> 创建或复用批次执行 -> 四份材料齐套 -> 统一放行”的可实现合同，同时允许合法独立批次入口。

## Target State

1. 生产组长加入活跃订单时消费流程修复1的正式领料单绑定：`pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId`。
2. 一线生产、一线 PQC 的提交及生产组长、PQC 组长复核由其所属流程产生正式事件；本任务只消费事件，不生成或推断人员/签名 ID。
3. 活跃订单双进度均为 100% 后，点击完成在一个业务节点内完成批记录、过程检验单和“有实际损耗才生成”的损耗单回填。三类回填是原子阶段，成功后提交不可变 `completionBackfillReceipt`。无损耗只记录 `hasActualLoss=false`、`lossDecision=NO_LOSS`，不生成损耗单，也不要求 loss evidence ID。
4. receipt 提交后，流程6在独立的 `BatchProvisioningRecord` 中创建或复用批次执行。该记录而非 receipt 持有 `BATCH_*` 状态、`batchExecutionId`、错误码和重试元数据；失败不得伪造 `BATCH_READY`，使用同一 receipt 幂等重试。
5. 流程6创建/复用 `batchExecutionId` 后，流程7在独立 Tx-C 建立 Origin/TraceLink/Manifest；流程6仅消费其成功事件并把自己的 provisioning 记录推进为 `BATCH_READY`。映射完成前不得上传四份材料。
6. `BATCH_READY` 后才可上传来料检报告、灭菌报告、成品检报告、成品检记录。流程8统一输出 `MATERIALS_PENDING`、`MATERIALS_READY` 或 `MATERIALS_RECHECK_REQUIRED`；只有 `MATERIALS_READY` 才允许任何合法入口请求流程10最终放行。流程7提供完整映射和放行后追溯，能够反查活跃订单、生产工单、领料单、一线生产、一线 PQC、复核和回填来源。

## Current Status

in_progress（流程6局部实现、主线融合和定向验证已完成；跨流程真实闭环、迁移和运行验证仍待完成）。

## Current Code Facts

- `MesPqcProductionReleaseServiceImpl#approve` 当前先调用 `openOrCreate`，再写 dossier，再初始化报告阶段，仍是“先建批后写资料”。
- `MesProductionReleaseBatchExecutionPortImpl#openOrCreate` 以 `PQC_RELEASE:<applicationId>` 复用申请级批次；没有有效 release 关联的旧批次会触发 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`。
- `MesProEdhrBatchExecutionServiceImpl` 的手工 `openOrCreate`、`openOrCreateFromScheduleCompletion`、`openOrCreateFromProductionRelease` 各自校验上下文，尚无统一 completion/backfill receipt。
- 报告阶段初始化和管理者代表审批已有四节点逻辑，但所有入口仍需汇聚到同一材料门禁；流程修复8拥有门禁，流程修复10拥有最终状态。
- `requireDossierWrite` 当前无条件要求损耗 evidence；与流程修复5已冻结的零损耗语义冲突，必须改为 `hasActualLoss=true` 才要求损耗单。

## Root Cause

建批服务被多个入口直接调用，PQC approve 把建批放在 dossier 之前；完成节点没有不可变回填 receipt，导致“完成/回填成功”和“批次已就绪”混用。原设计又把 immutable receipt 与可变 `BATCH_*`/`batchExecutionId` 混在一起，并让流程6和流程7同时声称映射写入权。手工、排产、PQC 入口缺少统一前置合同，且历史批次没有有效完成回填关联时无法安全补链。

## Scope

- 设计 receipt、统一建批服务、入口适配器、四材料门禁和最终放行的接口/数据/状态/幂等/追溯合同。
- 明确 Tx-A 回填原子阶段与 Tx-B 建批后继阶段、失败后的页面状态和完成按钮行为。
- 显式对接流程修复1、4、5、7、8、9、10、11；原始专项要求的1、4、7、9、11均保留。

## Non-Scope

- 不实现生产代码、数据库迁移、服务启动或写入型 E2E。
- 不替流程修复1生成领料单绑定，不替流程修复4/5决定回填字段，不替流程修复7定义材料映射，不替流程修复8/10拥有放行状态。

## Consistency Model

### Tx-A：完成与三类回填原子阶段

完成编排命令锁定活跃订单和 `completionVersion`，校验双进度 100%、正式领料绑定及提交/复核事件，然后在同一数据库事务写入完成事件、批记录回填、过程检验单回填，以及有实际损耗时的损耗单。无损耗写入 `hasActualLoss=false`/`NO_LOSS` 事实，不写损耗单。任一校验或本地写入失败全部回滚，不提交 `completionBackfillReceipt`，也不产生 `BACKFILL_FAILED`；事务回滚后在 Tx-A 之外追加可审计的失败尝试和稳定错误码，返回 `BACKFILL_ATOMIC_ROLLBACK`，用户可重新发起完成。只有 Tx-A 成功时才提交不可变 receipt，状态为 `BACKFILL_SUCCEEDED`。

### Tx-B：receipt 后继建批阶段

编排器可在 Tx-A 成功后立即调用统一建批服务，但该调用拥有独立事务边界。流程6以 `completionBackfillReceiptId + sourceSnapshotHash + completionVersion` 建立或锁定独立 `BatchProvisioningRecord`，在其中幂等创建/复用批次，写入 `batchExecutionId` 和 `BATCH_PROVISIONING`；它绝不修改 immutable receipt，也不写流程7映射。建批失败只更新该 provisioning 记录为 `BATCH_PROVISIONING_RETRYABLE` 或 `BATCH_PROVISIONING_BLOCKED` 并持久化错误码。重试必须复用同 receipt，不重新回填。

### Tx-C：流程7映射与流程6状态回执

Tx-B 成功后以 `batchExecutionId + sourceSnapshotHash` 发送幂等受控事件。流程7在独立 Tx-C 创建 Origin/TraceLink/Manifest，不创建批次；成功后返回 mapping-completed 事件。流程6消费该事件，仅推进自己的 `BatchProvisioningRecord` 到 `BATCH_READY`，其语义为“批次已创建且必需来源映射已完成，可进入材料阶段”。Tx-C 失败时流程6保持 `BATCH_PROVISIONING` 或按稳定错误码转为 `BATCH_PROVISIONING_BLOCKED`，不得进入材料或放行阶段。

页面在 Tx-A 成功、Tx-B 失败时显示“已完成，批次执行待重试”，完成按钮对同一版本禁用，提供单独“重试创建/复用批次”动作；不得再次执行完成回填。Tx-B 成功而 Tx-C 映射未完成时显示“批次已创建，正在建立来源映射”，禁用材料上传和放行；仅 Tx-C 成功、流程6记录 `BATCH_READY` 后开放材料上传。

Tx-A 失败时页面显示“完成未提交，请修正来源后重试”，活跃订单不进入完成态，不显示批次重试动作，完成按钮保持可重新发起状态；不得把失败尝试作为 receipt 或投影为任何 `BATCH_*` 状态。

## Entry Classification

| 入口 | 正式前置合同 | 是否需要 active-order receipt | 统一后续 |
|---|---|---:|---|
| 活跃订单完成/生产放行申请/PQC 关联入口 | 流程修复1绑定 + 流程修复4 `BACKFILL_SUCCEEDED` receipt | 是 | 流程6建批 -> 流程7映射 -> 流程8材料门禁 -> 流程修复10放行 |
| 手工独立批次 | 独立 `IndependentBatchPrerequisiteReceipt`：工单、批号、路线/版本、正式来源快照、责任人、来源关系 | 否 | 同一统一建批服务、同一材料门禁、同一放行状态 |
| 排产完成独立批次 | 排产系统正式完成凭证及工单/批号/路线版本快照，生成独立 receipt | 否 | 同上 |

独立入口不是绕过前置条件，也不能被一律判定为不可放行；它必须有自己的正式凭证、来源关系、幂等键和追溯链。所有入口均不得绕过四份材料门禁。

## Cross-thread Contracts

字段级合同见 `development-plan.md#Cross-thread Contracts`；状态和错误码在本任务中冻结：

| 线程 | 输入 | 输出 | owner | 幂等键 | 状态/失败码 |
|---|---|---|---|---|---|
| 流程修复1 | activeOrderId、pickListId、sourceSnapshotHash | `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId` | 流程修复1 | activeOrderId+bindingVersion | `BOUND`/`PICK_LIST_BINDING_REQUIRED`、`PICK_LIST_SNAPSHOT_CHANGED` |
| 流程修复4 | 双100%、提交/复核事件、流程1绑定、回填输入 | immutable receipt：三类回填成功事实、订单级 `hasActualLoss`、每工序损耗决定/`lossReportStatus`、正式来源快照/hash；失败尝试审计记录 | 流程修复4 | activeOrderId+completionVersion | 成功 `BACKFILL_SUCCEEDED`；失败无 receipt，返回 `BACKFILL_ATOMIC_ROLLBACK` |
| 流程修复5 | 实际损耗事实及数量 | 有损耗：`hasActualLoss=true`、`lossRecordId`、`lossQuantity`、`lossReportStatus`；无损耗：`hasActualLoss=false`、`lossQuantity=0`、`lossDecision=NO_LOSS/NOT_REQUIRED`、零损耗确认快照 | 流程修复5 | activeOrderId+completionVersion | `CREATED`/`NOT_REQUIRED`、`LOSS_SOURCE_INVALID` |
| 流程修复6 | receipt/独立正式凭证、来源 hash | `BatchProvisioningRecord`、`batchExecutionId`、provision 状态/错误码 | 流程修复6 | 活跃订单：completionBackfillReceiptId+sourceSnapshotHash+completionVersion | `BATCH_PROVISIONING`/`BATCH_READY`、retryable/blocked |
| 流程修复7 | receipt、流程1绑定、batchExecutionId、来源快照 | Tx-C Origin/TraceLink/Manifest、放行后追溯查询 | 流程修复7 | batchExecutionId+sourceSnapshotHash | `MAPPED`/`TRACE_MAPPING_BLOCKED` |
| 流程修复8 | batchExecutionId、四材料当前版本 | 四材料齐套门禁结果 | 流程修复8 | batchExecutionId+materialVersionSetHash | `MATERIALS_PENDING`/`MATERIALS_READY`/`MATERIALS_RECHECK_REQUIRED`；对外失败码为 `RELEASE_MATERIAL_GATE_REQUIRED`、`MATERIAL_NODE_MISSING`、`MATERIAL_UPLOAD_INCOMPLETE`、`MATERIAL_FILE_NOT_VERIFIED`、`MATERIAL_VERSION_STALE`、`MATERIAL_HASH_MISMATCH`、`MATERIAL_VERSION_CONFLICT`、`MATERIAL_MANIFEST_CHANGED`、`MATERIAL_SOURCE_SNAPSHOT_CHANGED`、`RELEASE_ENTRY_GATE_BYPASS`、`IDEMPOTENCY_CONFLICT` |
| 流程修复9 | entryType、来源凭证、业务键 | 入口合同校验结果、标准化 provision request | 流程修复9 | entryType+sourceReference+sourceVersion | `ACCEPTED`/`ENTRY_PREREQUISITE_MISSING`、`ENTRY_SOURCE_INVALID` |
| 流程修复10 | gate receipt、四材料门禁、审批人快照 | 唯一最终 release 状态及审计事件 | 流程修复10 | batchExecutionId+releaseVersion | `RELEASED`/`RELEASE_GATE_BLOCKED`、`RELEASE_SNAPSHOT_MISMATCH` |
| 流程修复11 | 上述合同、BDD/TDD、迁移证据 | 总门禁结论 | 流程修复11 | taskId+contractVersion | `PASS`/`MIGRATION_BLOCKED`、`TDD_EVIDENCE_MISSING` |

## Milestones

| 里程碑 | 交付物 | 状态 |
|---|---|---|
| M1 | 当前代码事实与根因 | completed |
| M2 | 一致性模型、入口合同、状态/错误码 | completed |
| M3 | BDD/TDD/REGRESSION、迁移/回滚边界 | completed |
| M4 | 文档结构核验与意见关闭 | completed |

## Expected Verification

- 五份文件均存在且包含目标态、当前事实、根因、边界、接口/数据/状态、BDD、RED/GREEN/REGRESSION、blocker、迁移/回滚和跨线程合同。
- `RED`/`GREEN` 仅作为后续实现计划，当前均 `NOT RUN`；本次只能证明文档结构。
- 代码符合性结论仅为审计发现，不作为生产 GREEN 证据。

## Blockers

1. 旧批次若没有有效完成回填 receipt、流程1绑定和流程7完整映射，必须先迁移或人工阻断：`LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`；不得静默补链。
2. 流程4/5/7需在实现前冻结 immutable receipt 字段、订单级/工序级损耗事实、流程7 Tx-C 映射事件和正式来源快照；缺失时 `BACKFILL_CONTRACT_NOT_FROZEN`。
3. 流程8硬门禁当前可能被旧配置开关绕过；对外必须返回 `RELEASE_ENTRY_GATE_BYPASS`，不得暴露未冻结的内部错误码。
4. 流程9独立入口若缺正式等价凭证，返回 `ENTRY_PREREQUISITE_MISSING`；不因缺 active-order 关系而误拒绝合法独立批次。
5. 流程10/11未提供最终状态和总门禁证据前不得进入实现 GREEN：`RELEASE_CONTRACT_NOT_FROZEN`、`TDD_EVIDENCE_MISSING`。

## Migration and Rollback Boundary

迁移只允许把能证明来源快照、流程1绑定、完成/回填事实和流程7映射的旧批次转为可追溯 receipt；无法证明的记录保持阻塞并出具清单。Tx-A 失败按事务回滚；Tx-B 失败只回滚建批尝试，保留 immutable receipt 并按同 receipt 重试。已放行记录不得删除或重写来源，回滚只能阻止新放行并保留审计事件。

## Current Status

in_progress（流程6实现已形成 task-owned commit 并快进融合到 int_main；流程4/7/9正式闭环、迁移和运行验证仍未完成）。

## Coding Verification Update (2026-08-24)

- `e539e8a2c` is the Flow 6 task-owned implementation commit: task-gate overload plus legacy fixture correction.
- Targeted Flow 6 suite: 37 tests passed; 24-module MES reactor compile passed; `git diff --check` passed.
- Flow 4/7/9 formal closure, migration, and main-line verification remain blockers.

## 主流程统一冻结合同（2026-08-22）

### Tx-A 本地事务边界

Tx-A 只锁定活跃订单/完成版本，读取已提交的生产、PQC、领料和损耗事实本地快照，重新校验本地版本号与 `sourceSnapshotHash`，然后原子写入完成事件、批记录、过程检验、条件损耗单或 `NO_LOSS` 事实及 immutable receipt。receipt 只含回填成功事实、订单级 `hasActualLoss`、每工序损耗决定/`lossReportStatus`、正式来源快照/hash 和 `receiptHash`，不含 `BATCH_*` 或 `batchExecutionId`。远程签名中心、ERP、文件服务等必须在 Tx-A 前预校验并落成本地可验证快照；版本/hash 不一致立即返回 `SOURCE_SNAPSHOT_MISMATCH`，不远程重查、不使用默认值。一线提交和两类组长复核只形成来源事实，不启动 Tx-A。

### 状态 owner 与错误白名单

流程4是 `completionBackfillReceipt` 唯一 owner；仅在 Tx-A 成功提交时创建不可变 `BACKFILL_SUCCEEDED` receipt。Tx-A 失败不持久化 receipt，也不存在 `BACKFILL_FAILED` receipt 状态；仅在回滚完成后以独立审计追加写入 `CompletionBackfillFailureAttempt`（完成版本、来源快照 hash、幂等/关联键、错误码和时间），返回 `BACKFILL_ATOMIC_ROLLBACK`，用户可重新发起完成。流程6是独立 `BatchProvisioningRecord` 唯一 owner，记录 `receiptId`、`batchExecutionId`、`BATCH_PROVISIONING`、`BATCH_PROVISIONING_RETRYABLE`、`BATCH_PROVISIONING_BLOCKED`、`BATCH_READY`、错误码和重试元数据；流程7独占 Tx-C 映射写入，流程6只消费映射完成事件后推进自己的状态。仅 `BATCH_DB_DEADLOCK`、`BATCH_LOCK_TIMEOUT`、`BATCH_TRANSIENT_DB_UNAVAILABLE`、`BATCH_IDEMPOTENT_QUERY_TIMEOUT`、`BATCH_PROVISIONING_OUTCOME_UNKNOWN` 可重试；其它前置、来源、幂等、权限、唯一约束、迁移或映射错误必须阻断并持久化稳定错误码。

### 独立凭证与 PQC 分流

`IndependentBatchPrerequisiteReceipt` 至少包含 `receiptId`、租户、`entryType`、工单/路线/批号、正式来源关系及 source IDs、`sourceSnapshotHash`、业务理由、签发系统/用户/角色、`issuedAt`、`expiresAt`、撤销信息、`credentialVersion`、`payloadHash`、签名/审计事件和幂等键；只能由后端签发，服务端按入口生成有效期。关联活跃订单的 PQC 申请必须消费 `BACKFILL_SUCCEEDED` receipt 和流程1绑定；独立 PQC 只能凭流程9允许的有效独立凭证进入统一建批服务。

### 材料、迁移与独立追溯

四节点有效条件为状态 `COMPLETED`、文件已持久化、元数据和 SHA-256 已校验、当前版本且来源 hash 一致；节点已有批准字段时还必须 `APPROVED`，无批准字段不得伪造批准。流程8门禁状态只使用 `MATERIALS_PENDING`、`MATERIALS_READY`、`MATERIALS_RECHECK_REQUIRED`；节点替换、版本/hash/来源变化使门禁进入 `MATERIALS_RECHECK_REQUIRED`，放行前置只能是 `MATERIALS_READY`。历史批次先 dry-run 分类，缺 receipt/绑定/hash/损耗决策/关系或有冲突则 `INCOMPLETE_OR_AMBIGUOUS`，已放行且来源不全则 `ALREADY_RELEASED_REVIEW_REQUIRED`。独立批次追溯显示 `originType`、凭证、工单/路线/批号、来源快照、适用事实、三类回填、四材料版本/hash、放行和审计；不适用关系返回 `relationStatus=NOT_APPLICABLE` 及原因码，不返回空字符串。
