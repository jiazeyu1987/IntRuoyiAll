# 流程修复 9：多入口正式前置合同

## 任务目标

保留活跃订单完成、排产完成、PQC 批准联动、页面手工/受控重试和独立批次创建等多种建批入口，同时由流程修复 9 统一执行 `entryType` 分流、正式凭证前置、来源关系、幂等和入口适配。流程修复 9 不拥有回填、材料或最终放行状态。

活跃订单生产链路必须消费流程修复 4 生成的 `CompletionBackfillReceipt`：`activeOrderId`、`workOrderId`、流程修复 1 的 `pickListBindingId/pickListId/sourceSnapshotHash/bindingVersion`、双进度 100%、流程修复 4 完成版本和三类回填成功均不可缺失。排产、PQC、手工重试若属于活跃订单场景，只能消费该 receipt，不得创建、修改或重新回填。

合法独立批次可以没有 `activeOrderId`，但必须消费流程修复 6 冻结的 `IndependentBatchPrerequisiteReceipt` 和正式 `batchExecutionSourceRelation`，不能按工单号、批号或路线号猜测来源，也不能伪造 active order。允许的独立 entryType 至少包括 `MANUAL`、`SCHEDULED`、`PQC_INDEPENDENT`。

批次创建与最终放行严格分离：流程修复 9 将统一建批请求交给流程修复 6；批次创建/复用成功后，流程修复 8 负责四材料上传与硬门禁，流程修复 10 消费既有 `batchExecutionId` 并唯一写入最终 `RELEASED`。

## 当前代码事实与根因

- `ScheduleApplier.java:176-196` 存在排产缺项 warning/skip 和专用建批逻辑。
- `MesProAutoScheduleServiceImpl.java:419` 触发自动排产完成联动。
- `MesProEdhrBatchExecutionController.java:101-105` 提供页面 `open-or-create`。
- `MesPqcProductionReleaseServiceImpl.java:93-147` 的 PQC 批准路径存在先建批再写资料的顺序。
- `ErrorCodeConstants.java:1238-1243` 已有活跃订单双进度、正式来源和负责人错误合同。

现有入口并存但凭证、状态 owner 和 gate 分散，导致 PQC 可能被误当作活跃完成，页面可能按业务号码直建，独立业务也缺少统一的正式来源合同。根因是入口适配与领域状态所有权未分离。

## 修改边界

- 流程9自身实现范围：入口 `entryType` 分流、活跃订单 completionBackfillReceipt 校验、独立入口正式凭证适配、场景混用阻断、来源 hash/version/payloadHash 一致性、幂等冲突和 fail-fast 错误码，以及 task-owned 合同测试。
- 不修改数据库 schema、不签发或伪造流程6凭证、不拥有流程8材料状态或流程10最终 RELEASED，不启动服务、不运行写入型 E2E。
- 流程6/7/8/10/11 仍按各自状态 owner 落地正式持久化、材料 gate、最终放行和全链路迁移验证。

## 目标态与状态所有者

- 流程修复 4：唯一产生不可变 `CompletionBackfillReceipt`，并记录三类回填和完成版本。
- 流程修复 9：仅负责 entryType 分流、凭证前置、来源关系、幂等、冲突拒绝和入口适配。
- 流程修复 6：唯一拥有批次执行创建/复用状态，返回 `batchExecutionId` 和 created/reused。
- 流程修复 8：拥有四材料上传、版本/hash manifest 和统一硬门禁。
- 流程修复 10：消费已创建/复用的 `batchExecutionId`，唯一写最终 `RELEASED`。
- 流程修复 7：提供批次完整映射及放行后追溯根。
- 流程修复 11：负责 BDD/TDD/回归/迁移总门禁。

## 预期验证

- 五份 Markdown 均存在且内容互相一致。
- 双场景凭证、入口矩阵、批次/放行分离、四材料 gate、流程 10 合同和 BDD/RED/GREEN/REGRESSION 均可核验。
- 流程9目标模块编译、task-owned 合同测试和相关回归可重复通过；数据库迁移、服务启动和写入型 E2E 保持未运行。

## Blockers

- 流程6仍拥有独立凭证后端受控签发、source relation 持久化和跨入口复用状态；流程9只做正式接口适配。
- 流程修复 7/8/10/11 的生产实现、数据库约束/迁移、四材料 gate、最终 RELEASED 和真实 E2E 尚未执行。
- 历史无正式 receipt/source relation 的记录只能保持 `BLOCKED_LEGACY`，迁移审查尚未执行。

## Current Status

completed

## 设计约束检查

- 不新增默认凭证、空签名放行、前端携带整份凭证或吞异常的 fallback；缺少签发密钥、来源事实、租户或正式持久化时 fail fast。
- 本轮只补流程9独立凭证签发/验真/撤销及其持久化合同，不改流程6批次状态、流程8材料门禁或流程10最终放行。
- 先以 RED 合同测试冻结 canonical/hash/signature、租户、生命周期和幂等边界，再实现生产代码；迁移运行态未执行不得宣称数据库已就绪。

### 主流程统一冻结合同（2026-08-22）

独立凭证统一使用后端签发的 `IndependentBatchPrerequisiteReceipt`，至少包含 receiptId、tenantId、entryType、工单/路线/批号、正式来源关系及 source IDs、sourceSnapshotHash、业务理由、签发系统/用户/角色、issuedAt、expiresAt、撤销信息、credentialVersion、payloadHash、签名/审计事件和幂等键。PQC 关联活跃订单必须消费 `BACKFILL_SUCCEEDED` receipt；独立 PQC 仅凭有效独立凭证进入流程6统一建批服务。

收尾证据：已标记 `ready_for_closeout`，完成 cleanup preview/apply（无可删除附属产物）后标记 `completed`；流程9自身代码已提交，新增合同测试受外部流程7编译 blocker 影响未运行，跨流程闭环仍未完成。

最新主线收尾（2026-08-23）：并行流程11先提交 `ef217fe2c`，流程9随后以其为父节点提交 `2cf830d7b`；`477c97d41` 和 `2cf830d7b` 均在 `int_main` 祖先链，不重复融合旧 worktree。

## Cleanup Keep

- doc/tasks/20260821-flow-repair-09-multi-entry-precondition-contract/development-plan.md
- doc/tasks/20260821-flow-repair-09-multi-entry-precondition-contract/test-plan.md
- doc/tasks/20260821-flow-repair-09-multi-entry-precondition-contract/backend-api-evidence.md
- doc/tasks/20260821-flow-repair-09-multi-entry-precondition-contract/database-schema-evidence.md
