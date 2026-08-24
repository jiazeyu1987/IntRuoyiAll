# 流程修复 4 执行记录

## 2026-08-21 任务启动

- 用户意图：只做代码审计、需求澄清和开发文档设计；不修改生产代码、数据库、环境，不启动服务，不执行写入型 E2E。
- 已读取门禁：`AGENTS.md`、`docs/task-closeout-rules.md`、`docs/experience-index.md`、`docs/product/production-role-system-operations.md`、`docs/backend-development.md` 的正式来源章节、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`。
- 经验门禁：应用 `活跃订单申请放行资料必须只使用正式来源`。

## 里程碑 1：只读代码审计

状态：完成。

- 审计事实：当前正式入口为 `/active-order/release/apply`，没有正式的双100订单完成命令。
- 审计事实：申请服务先要求各冻结工序 `COMPLETED + BACKFILL_SUCCESS`，随后创建 `PQC_RELEASE_PENDING`。
- 审计事实：`MesTeamLeaderOrderProcessCompletionService` 在工序完成时调用 `completeAndBackfill`；批记录回填服务使用独立事务。
- 审计结论：现有实现会在订单完成前物化资料，且没有订单级回滚、完成回执或同步建批保障。

## 里程碑 2：需求澄清与接口设计

状态：完成。

BDD: 双100完成统一回填 -> Given 正式生产/PQC/工单/领料/损耗来源完整且双进度100%，When 当前生产组长提交完成命令，Then Tx-A 原子回填三类资料并一次写入不可变 completionBackfillReceipt，返回 provisionHandoff=PENDING_FLOW6；receipt 不持有 batchExecutionId 或流程6批次状态，交给流程6后继 Tx-B 消费。
BDD: 完成前禁止物化 -> Given 任一进度不足或任一来源不完整，When 生产提交、PQC提交或任一组长复核，Then 不写最终三类资料、completionBackfillReceipt 和批次执行。
BDD: Tx-A原子失败 -> Given 三类来源有效，When 任一三类回填失败，Then 三类资料、订单状态和 completionBackfillReceipt 整体回滚；流程6不会被触发。
BDD: 损耗字段与分支 -> Given 流程5给出逐工序 REQUIRED/NO_LOSS/BLOCKED；When 完成命令校验损耗事实，Then REQUIRED 必须 hasActualLoss=true、lossQuantity>0、lossRecordId 存在并生成损耗单且 receipt lossReportStatus=SUCCESS，NO_LOSS 必须有正式零损耗快照并保存 false/0/NOT_REQUIRED，不生成损耗单或零损耗报告，BLOCKED 或缺失 lossRecordId 不生成成功 receipt。
BDD: Tx-B失败保留receipt -> Given Tx-A 已提交不可变 completionBackfillReceipt，When 流程6建批失败，Then 由流程6独占 BATCH_PROVISIONING_RETRYABLE 或 BATCH_PROVISIONING_BLOCKED，receipt 不更新、不写入 batchExecutionId，后续重试不能重复三类回填。
BDD: pre-release与放行顺序 -> Given 流程6或流程9合法入口创建/复用批次执行，When 流程7 pre-release 映射并校验 Origin/TraceLink 来源 hash/version，Then 必须先映射生产工单、正式领料单、批记录、过程检验、适用损耗或 NO_LOSS 事实，再由流程8校验四份材料并返回 MATERIALS_READY，最后由流程10唯一最终放行；放行后由流程7 post-release追溯。
BDD: 幂等与历史执行 -> Given 已完成回执或遗留完成前批次执行，When 重试完成命令，Then 仅可返回同键同证据的原 completionBackfillReceipt，其他情况冲突或迁移 blocker；流程6建批按自身唯一键幂等。

## 严格 TDD 计划（未执行）

RED: <待实现阶段确定的后端测试命令> -> NOT_RUN, 当前任务禁止实现和运行测试。
GREEN: <待实现阶段确定的后端测试命令> -> NOT_RUN, 当前任务禁止实现和运行测试。
REGRESSION: <待实现阶段确定的完整回归命令> -> NOT_RUN, 当前任务禁止启动服务或执行写入型 E2E。

## 里程碑 3：文档产出

状态：完成（复核修订）。

- 文档校验：DOCUMENT_CHECK=PASS；五份必需文档均存在，Node UTF-8 读取无替换字符，设计必需段落均已覆盖。
- 未运行构建、服务、数据库操作或写入型 E2E，符合用户限定范围。
- 已按复核意见更正流程6建批、流程8材料、流程10最终放行、流程5条件损耗、流程7追溯和流程11总门禁职责。
- 任务状态：ready_for_closeout（待主线程完成最终只读一致性检查和收尾）。

## Flow6 receipt read-contract freeze (2026-08-24)

BDD: Flow6 consumes an immutable receipt -> Given a tenant-scoped receiptId, When Flow6 reads the Flow4 handoff, Then only a persisted BACKFILL_SUCCEEDED receipt is returned; missing, tampered, cross-tenant, incomplete, or non-success receipts fail fast and Flow6 must not rebuild from production/PQC facts.
BDD: Three backfill statuses are explicit -> Given a successful Tx-A receipt, When Flow6 validates the handoff, Then batchRecordStatus=SUCCESS and processInspectionStatus=SUCCESS; lossReportStatus=SUCCESS is required only for hasActualLoss=true and positive lossQuantity, otherwise hasActualLoss=false, lossQuantity=0, lossReportStatus=NOT_REQUIRED, a zero-loss snapshot is required, and no loss record is exposed.
BDD: Receipt identity is frozen -> Given a valid receipt, When Flow6 consumes it, Then receiptId remains Long, tenantId is matched in the query and rechecked on the row, completionVersion, sourceSnapshotHash, and receiptHash are non-empty/frozen, and no batchExecutionId or BATCH_* field is part of the handoff.

RED: & $env:MAVEN_HOME\\bin\\mvn.cmd -pl yudao-module-mes -Dtest=MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, expected missing DTO status getters; the same compile also reported pre-existing unrelated MesIndependentBatchPrerequisiteReceiptServiceTest errors.
GREEN: Same targeted Maven command after DTO/port mapping -> BLOCKED before test execution by pre-existing unrelated MesIndependentBatchPrerequisiteReceiptServiceTest missing getVerifiedByReceiptId(...); no Flow4 production compile error remained.
REGRESSION: & $env:MAVEN_HOME\\bin\\mvn.cmd -pl yudao-module-mes -am -DskipTests compile -> to be run after the main-line parallel compile blocker is cleared.

- 已计划产物：`task.md`、`development-plan.md`、`test-plan.md`、`execution-log.md`、`verification-report.md`。
- 下一步：执行只读结构/UTF-8 验证；不执行构建、服务或写入测试。

## 已知实施 blocker（合同已冻结）

1. 流程5、6/9、7、8、10、11 的职责、字段、状态和接口合同已在任务文档中冻结；生产代码尚未实现，尚无实现级测试、真实数据或迁移证据。
2. 流程6 Tx-B receipt 消费、createOrReuse 幂等、BATCH_PROVISIONING/BATCH_PROVISIONING_RETRYABLE/BATCH_PROVISIONING_BLOCKED/BATCH_READY 状态、provision relation 和 batchExecutionId 尚未实现或验证。
3. 流程5 REQUIRED/NO_LOSS/BLOCKED、hasActualLoss、lossQuantity、lossReportStatus 与零损耗确认快照尚未实现或验证；流程7 pre-release hash/version 校验、流程8 MATERIALS_READY、流程10唯一放行和流程7 post-release 追溯尚无实现证据。
4. 流程11的 BDD/TDD、回归和迁移总门禁尚未运行；真实任务自有数据、签名账号、四份材料及共享 schema 迁移证据缺失，本任务不启动服务或执行写入型验证。



## 文档验证结果

- DOCUMENT_CHECK=PASS；五份必需文档均存在，Node UTF-8 读取无替换字符，设计必需段落均已覆盖。
- 未运行构建、服务、数据库操作或写入型 E2E，符合用户限定范围。
- 任务状态：ready_for_closeout（待主线程完成最终只读一致性检查和收尾）。
