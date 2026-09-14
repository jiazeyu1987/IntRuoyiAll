# Development Plan

## Purpose and Design Principle

把“损耗是否需要建单”建模为流程修复 4 活跃订单完成节点中的条件分支，而不是 dossier writer 的固定资料要求。流程修复 5 只按冻结工序校验正式一线生产事实、形成损耗决策并在有实际损耗时写入损耗单；它不拥有完成命令、批次执行、四份材料或最终放行。

## Evidence Reviewed

- docs/product/production-role-system-operations.md：完成节点统一回填；有损耗才回填损耗单；无损耗不生成损耗单。
- docs/backend-development.md：活跃订单申请放行资料必须只使用正式来源章节，要求零损耗不得生成空损耗单或零损耗报告。
- MesPqcProductionReleaseServiceImpl、MesPqcReleaseDossierPortImpl、MesTeamLeaderActiveOrderReleaseLossReportWriterImpl：当前申请阶段先建批、固定要求损耗 evidence、零损耗返回 blocker。
- 流程修复 4 设计：同一个活跃订单完成节点统一启动三类回填，并在 receipt 中保存损耗分支结果。
- 流程修复 6/7/8/10/11：分别拥有回填成功后建批、来源映射、四材料硬门禁、最终放行状态/角色/审计、BDD/TDD/回归/迁移总门禁。

## Milestone 1: Loss Fact Contract

### Formal source

每个订单冻结工序必须从已签名且已由生产组长批准的正式生产反馈读取，并消费流程修复 1 冻结的领料绑定快照：

- activeOrderId、workOrderId、pickListBindingId、pickListId、bindingVersion、batchPickListRelationId、routeId、routeVersionId；
- routeProcessId、processId 及订单工序快照；
- feedbackId、eventId、allocationId、reviewId；
- feedbackQuantity、qualifiedQuantity、unqualifiedQuantity；
- laborScrapQuantity、materialScrapQuantity、otherScrapQuantity；
- 结构化 lossDetails：reasonId/code/name 快照、明细数量；
- 一线填写签名 ID/用户/时间和组长复核签名 ID/用户/时间；
- sourceValueHash、sourceSnapshotHash、completionVersion。

领料关系五字段 `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId` 必须作为同一正式绑定快照原样传入并逐字段校验。流程修复 5 只读取、校验和固化该快照，不创建绑定、不补建领料单、不猜测工单/批次关系；`materialPickListId` 不能作为唯一字段，也不能替代这五字段合同。

禁止从 formBindings、默认 MAIN、当前登录人、QA 最新设备/原因主数据、备注或前端输入补齐。

### Normalization and positive-loss rule

1. 所有数量必须存在、为非负 Decimal，按正式业务精度规范化后比较。
2. unqualifiedQuantity = labor + material + other，且 feedbackQuantity = qualified + unqualified；分配数量必须与反馈总量一致。
3. 规范化值大于 0 才是 HAS_LOSS；等于 0 才是 NO_LOSS；负数、空值、精度冲突或守恒失败是 blocker，不得归零。
4. HAS_LOSS 要求每条明细数量大于 0，原因 ID、编码、名称快照全部存在且与反馈原因一致，明细合计等于损耗总量。
5. NO_LOSS 要求正式反馈存在明确无损耗确认事实，并保存 `hasActualLoss=false`、`lossQuantity=0`、`zeroLossConfirmed=true`、确认来源 ID、签名和快照；没有该事实返回 NO_LOSS_FACT_REQUIRED，不能把缺失字段转换为 false。

## Milestone 2: Process Loss Decision and NO_LOSS Semantics

### Decision output

流程修复 5 按工序返回一个 `ProcessLossDecision`：

routeProcessId、processId、decisionVersion、status、hasActualLoss、lossQuantity、categoryTotals、lossReasonSnapshot、lossDetails、sourceObjectIds、signatureEvidence、pickListBindingSnapshot、sourceValueHash、sourceSnapshotHash。

status 仅允许 `REQUIRED`、`NO_LOSS`、`BLOCKED`；工序及订单完成 receipt 使用唯一字段 `lossReportStatus`，只允许 `SUCCESS`、`NOT_REQUIRED`，不得用缺少损耗单 ID 推断状态。工序的 `NO_LOSS` 必须映射为 receipt 的 `lossReportStatus=NOT_REQUIRED`，不得把 `NOT_REQUIRED` 当作工序决策状态。

- REQUIRED：`hasActualLoss=true` 且损耗数量大于 0；必须返回正式损耗单映射、目标报表版本和写入数据。
- NO_LOSS：`hasActualLoss=false`、损耗数量严格等于 0 且正式无损耗事实已确认；只返回 `NO_LOSS` 来源快照，不返回损耗单 ID 或 evidence ID。完成 receipt 对该工序记录 `lossReportStatus=NOT_REQUIRED`。不能因为 `lossRecordId` 缺失就推断 `hasActualLoss=false`。
- BLOCKED：来源、数量、签名、原因、映射或版本不完整；不得写入。

BDD: 条件损耗回填 -> Given 流程 4 在双100完成节点提供完整五字段绑定快照，When 流程 5 评估工序，Then 正数事实输出 REQUIRED/`hasActualLoss=true`/`lossQuantity>0` 并建损耗单，正式零损耗事实输出 NO_LOSS/`hasActualLoss=false`/`lossQuantity=0` 且 receipt `lossReportStatus=NOT_REQUIRED`，缺失事实输出 BLOCKED；BLOCKED 不产生成功 receipt，也不驱动流程 6 建批。

### NO_LOSS receipt fact

无实际损耗时只把结构化事实交给流程修复 4 写入完成 receipt：

- lossReportStatus=NOT_REQUIRED；
- lossDecision=NO_LOSS；
- hasActualLoss=false；
- lossQuantity=0、分类合计均为 0；
- sourceFeedbackId/eventId/reviewId、无损耗确认字段、签名快照、来源哈希、完成版本；
- 五字段领料绑定快照及其 `sourceSnapshotHash`；
- lossReportId、lossEvidenceIds 必须为空且不可由下游生成。

不得创建零数量损耗单，不得生成任何无损耗报告或伪造报告编号。下游只能读取 receipt 中的 `NO_LOSS` 事实。

流程 4 的订单级完成 receipt 必须同时保存订单级 `hasActualLoss`、逐工序 decisions、逐工序 `lossReportStatus` 和订单级 `lossReportStatus`：`hasActualLoss` 等于所有工序 `hasActualLoss` 的逻辑或；全部工序明确 `NO_LOSS` 时各工序及订单 receipt 使用 `lossReportStatus=NOT_REQUIRED`，任一工序 `REQUIRED` 且写入成功时对应工序及订单 receipt 使用 `lossReportStatus=SUCCESS`。任一工序 `BLOCKED` 时不提交成功 receipt，也不得驱动流程 6 建批。

### Partial-process semantics

- 订单有多个工序时，每个工序独立输出 REQUIRED、NO_LOSS 或 BLOCKED，并携带 `hasActualLoss=true/false` 和该工序的 `lossReportStatus`；流程 4 receipt 再汇总为订单级 `lossReportStatus=SUCCESS` 或 `NOT_REQUIRED`。
- 至少一个工序为 REQUIRED 时，只为这些工序建损耗单；其它工序保持 `NO_LOSS`、`lossReportStatus=NOT_REQUIRED` 和 `hasActualLoss=false`，订单级 `hasActualLoss=true`。
- 任一工序 BLOCKED，整个完成回填事务失败，不能出现部分损耗单、部分回执或批次执行。
- 不允许以订单级总损耗抵消某工序缺失，也不允许把一个工序的损耗原因复制到另一个工序。

## Milestone 3: API and Transaction Design

### Complete command

流程修复 5 不新增对外完成接口。流程修复 4 的完成入口在双进度 100% 后调用本专项内部端口；生产/PQC 提交和两类组长复核都不得调用该端口。完成请求不得接受损耗数量、损耗单 ID、无损耗标记或来源哈希。

本专项结果按工序返回 REQUIRED、NO_LOSS 或 BLOCKED，并必须返回 `hasActualLoss` 和 `lossQuantity`。流程修复 4 在任一工序有损耗且全部写入成功时保存订单级/逐工序 `lossReportStatus=SUCCESS`；全部工序无损耗时保存订单级/逐工序 `lossReportStatus=NOT_REQUIRED`、订单级 `hasActualLoss=false` 和逐工序 `NO_LOSS`。任一工序 BLOCKED 时不生成成功 receipt，流程 6 不得建批。部分工序损耗通过逐工序 decisions 表达。批次执行 ID 不属于本专项输出。

### Internal ports

- `ProcessLossDecisionEvaluator.evaluate(frozenCompletionContext)`：只读流程 1 五字段领料绑定快照和正式一线生产来源，输出逐工序 REQUIRED/NO_LOSS/BLOCKED、`hasActualLoss` 和 `lossQuantity`。
- `ConditionalLossRecordWriter.write(requiredDecisions, completionVersion)`：只接收 REQUIRED，返回损耗单 ID 和来源证据；空集合返回 `NO_LOSS`、`lossReportStatus=NOT_REQUIRED`、`hasActualLoss=false`、`lossQuantity=0` 且零写入。
- `CompletionBackfillOrchestrator`（流程修复 4）：唯一完成编排 owner，在同一活跃订单完成节点启动批记录、过程检验和本条件分支，并提交包含订单/工序 `lossReportStatus` 的统一 receipt。
- `BatchExecutionProvisioner`（流程修复 6）：只消费已提交的三类回填 receipt，负责创建/复用批次执行。
- 流程修复 7 的映射/追溯只消费损耗单来源或 receipt 中的 `NO_LOSS` 快照及五字段领料绑定快照，不得由流程修复 5 生成或猜测批次关系。

### Error model

稳定 blocker 至少包括：`LOSS_SOURCE_FACT_REQUIRED`、`LOSS_SOURCE_PICK_LIST_BINDING_REQUIRED`、`LOSS_SOURCE_PICK_LIST_BINDING_SNAPSHOT_CHANGED`、`LOSS_QUANTITY_INVALID`、`LOSS_QUANTITY_CONSERVATION_FAILED`、`LOSS_HAS_ACTUAL_LOSS_REQUIRED`、`LOSS_HAS_ACTUAL_LOSS_CONFLICT`、`LOSS_REASON_SNAPSHOT_REQUIRED`、`LOSS_DETAIL_REQUIRED`、`LOSS_SIGNATURE_REQUIRED`、`NO_LOSS_FACT_REQUIRED`、`LOSS_REPORT_MAPPING_REQUIRED`、`LOSS_SOURCE_SNAPSHOT_CHANGED`、`LOSS_IDEMPOTENCY_PAYLOAD_CONFLICT`、`ACTIVE_ORDER_VERSION_CONFLICT`、`LEGACY_LOSS_DECISION_MIGRATION_REQUIRED`。错误必须 fail fast，保留 blocker data，不以成功码包失败。缺失事实不得转换为 `NO_LOSS` 或 `hasActualLoss=false`。

## Milestone 4: State, Idempotency and Concurrency

### State ownership

| 状态 | 唯一所有者 | 说明 |
| --- | --- | --- |
| ProcessLossDecision.REQUIRED/NO_LOSS/BLOCKED + hasActualLoss | 流程修复 5 | 由流程 1 五字段绑定快照和正式来源计算，不由前端、放行申请或批次服务改变 |
| 损耗单 PENDING/SUCCEEDED | 条件损耗 writer | 只存在于 REQUIRED 工序 |
| 完成 receipt | 流程修复 4 | 固化订单级 hasActualLoss、逐工序 decision/lossQuantity/lossReportStatus、订单级 lossReportStatus、三类回填结果、五字段绑定快照和来源 hash，不含本专项生成的批次状态 |
| 批次执行 | 流程修复 6 | 仅消费已提交的成功 receipt |
| 损耗来源映射/追溯 | 流程修复 7 | 只消费正式引用，不猜测批次映射 |
| 四材料及放行硬门禁 | 流程修复 8 | 不补建损耗单或改写 NO_LOSS |
| 最终放行状态、角色、审计 | 流程修复 10 | 不拥有损耗判定 |
| BDD/TDD、回归、迁移总门禁 | 流程修复 11 | 汇总验证和迁移放行条件 |

### Idempotency

- 唯一键：tenantId + activeOrderId + completionVersion + routeProcessId（decision）以及 tenantId + activeOrderId + completionVersion（完成回执）。
- 同键同 requestPayloadHash + 领料五字段绑定快照 + sourceSnapshotHash 返回原回执和原损耗单；不得重复建单。
- 同键不同 payload、来源 hash、工序集合或零损耗标记返回冲突。
- REQUIRED 与 NO_LOSS 决策变化必须提升 completionVersion，旧 receipt 保持不可变；不能把已建损耗单改成无损耗。
- 流程修复 4 锁定活跃订单/完成版本并管理三类回填事务。本专项任一判断或写入失败时抛出稳定错误，由流程修复 4 回滚该完成节点全部回填；流程修复 6 的后继建批失败不得反向重跑本专项。

## Milestone 5: Cross-thread Contracts

| 流程修复 | 本专项输入/输出 | 接口约束 |
| --- | --- | --- |
| 4：活跃订单完成统一回填 | 输入双 100%、流程 1 五字段领料绑定快照、正式来源与完成版本；调用本专项；输出订单级/逐工序 hasActualLoss、decisions、lossReportStatus 和条件损耗证据 | 修复 4 是完成节点和三类回填事务 owner；提交/复核只形成来源事实，不触发回填；不得从缺少 lossRecordId 推断 false |
| 6：回填后批次创建/复用 | 输入流程 4 已提交且 `lossReportStatus=SUCCESS|NOT_REQUIRED` 的 receipt、订单级/逐工序 hasActualLoss、五字段绑定快照、来源 hash、实际损耗单 ID（可空） | `NOT_REQUIRED + NO_LOSS + hasActualLoss=false + lossQuantity=0` 不要求损耗 evidence；`BLOCKED` receipt 不得建批；流程 6 独占批次状态和幂等 |
| 7：批次来源映射和追溯 | 消费损耗单来源或 `NO_LOSS` 来源快照、hasActualLoss 和五字段绑定快照 | 只消费和映射，不由本专项猜测批次、批号或关联关系；缺少显式 false 不得推断无损耗 |
| 8：四材料上传及硬门禁 | 消费批次与既有来源映射 | 本专项不上传、不校验四材料，也不拥有放行门禁 |
| 10：最终放行状态/角色/审计 | 消费流程 8 门禁和流程 7 追溯关系 | 本专项不创建放行申请、不改变最终状态、不分配放行角色 |
| 11：BDD/TDD、回归和迁移总门禁 | 消费本专项状态、错误码、幂等和历史分类 | 覆盖有损耗、无损耗、部分工序、重复、回滚和追溯；历史缺事实保持阻塞 |

## Migration and Rollback

1. 上线前只读扫描历史完成/放行记录，按正式反馈、零损耗确认、签名、复核和损耗单关系生成候选清单。
2. 可证明正损耗的记录绑定 REQUIRED + `lossReportStatus=SUCCESS` + `hasActualLoss=true` + lossReportId；可证明零损耗且有正式零损耗字段、五字段领料绑定快照的记录绑定 NO_LOSS + `lossReportStatus=NOT_REQUIRED` + `hasActualLoss=false` + lossQuantity=0；其它记录 BLOCKED_LEGACY。
3. 迁移不创建空损耗单、不重算历史数量、不按名称或时间猜测原因。
4. 代码回滚保留完成回执和损耗事实；只能停用新入口/读模型，不能删除已写损耗单或用删除制造无损耗。

## Verification Gate

BDD 已在本计划和 test-plan.md 固化；流程5实现、主线 compile、核心21项 JUnit、diff-check 和 runtime guard 均已通过。数据库迁移、服务和真实写入型 E2E 仍 NOT_RUN。

## Design Blockers

- 流程5已实现五字段绑定快照只读校验、正/零/阻塞条件分支、显式 `hasActualLoss` 与 `lossReportStatus`，并通过核心测试。
- 流程4订单级 receipt、流程6消费、流程7映射以及流程8/10/11 下游门禁仍是跨线程验证项；流程5不猜测或拥有这些实现。
- 当前主线组合中的 `MesFrontlineRuntimeConfigProcessScopeTest` 静态断言失败，属于前线运行时 owner，不改变流程5核心21项 PASS。
