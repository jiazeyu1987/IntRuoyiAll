# Execution Log

## User Intent

只做流程修复6代码审计、需求澄清和五份开发文档设计；不修改生产代码、数据库、服务或运行写入型 E2E。

## Evidence Reviewed

- `AGENTS.md`、任务收尾/经验/生产角色/后端正式来源、前端和 E2E 规则。
- 流程修复1、4、5、7、8、9、10、11 当前任务目录及其职责口径。
- `MesPqcProductionReleaseServiceImpl#approve`、`MesProductionReleaseBatchExecutionPortImpl#openOrCreate`、`MesProEdhrBatchExecutionServiceImpl`。
- 报告阶段初始化、管理者代表最终审批及四节点逻辑。

## Audit Findings (not TDD evidence)

BDD: M1 代码事实审计 -> Given 指定仓库和规则可读，When 检索建批、回填、材料和放行入口，Then 记录 PQC approve 先建批、入口前置不统一、旧批次迁移 blocker 和四节点实现。

AUDIT: 观察到 PQC approve 顺序为 `openOrCreate -> dossier.write -> reportStage.initialize`；这是代码事实，不是 RED 测试结果。

AUDIT: 手工、排产和申请入口各自建批，当前没有统一 completion/backfill receipt；旧批次缺有效关联时抛出 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`。

AUDIT: 当前 dossier 校验无条件要求损耗 evidence；与流程5的 `NO_LOSS` 语义不一致，列为实现 blocker。

## M1 - Contract Repair

BDD: M1 合同修订 -> Given 用户列出流程1/4/5/7/8/9/10/11职责和失败语义，When 修订五份文档，Then 字段、owner、幂等键、状态、错误码和依赖矩阵一致。

DOC-STRUCTURE: `task.md`、`development-plan.md`、`test-plan.md`、`execution-log.md`、`verification-report.md` -> PASS（文档已写入任务目录）。

## M2 - Transaction and Entry Design

BDD: M2 两阶段一致性 -> Given Tx-A 回填原子提交，When Tx-B 建批失败，Then immutable receipt payload 保持不变，独立 BatchProvisioningRecord 进入 retryable/blocked 并可用同 receipt 重试，不标记 `BATCH_READY`。

BDD: M2 Tx-A 成功建 receipt -> Given Tx-A 校验和三类回填均成功，When 本地事务提交，Then 流程4创建唯一不可变 `BACKFILL_SUCCEEDED` receipt，之后才允许流程6推进 `BATCH_*`。

BDD: M2 payload/provisioning 分离 -> Given Tx-A 已提交 immutable receipt，When 流程6 Tx-B 建批或流程7 Tx-C 映射，Then 仅流程6的 `BatchProvisioningRecord` 写入 `batchExecutionId`/`BATCH_*`，流程7只写 Origin/TraceLink/Manifest，receipt payload 不变。

BDD: M2 映射失败稳定码 -> Given Tx-B 已创建 batchExecutionId，When 流程7 Tx-C 映射失败或来源缺失，Then 对外统一返回 `TRACE_MAPPING_BLOCKED`，流程6不推进 `BATCH_READY`；这是后续实现测试计划，不是本次测试证据。

BDD: M2 材料错误码稳定性 -> Given 流程8材料门禁或入口绕过校验失败，When 门禁返回错误，Then 只使用冻结的 `RELEASE_MATERIAL_GATE_REQUIRED`、`MATERIAL_NODE_MISSING`、`MATERIAL_UPLOAD_INCOMPLETE`、`MATERIAL_FILE_NOT_VERIFIED`、`MATERIAL_VERSION_STALE`、`MATERIAL_HASH_MISMATCH`、`MATERIAL_VERSION_CONFLICT`、`MATERIAL_MANIFEST_CHANGED`、`MATERIAL_SOURCE_SNAPSHOT_CHANGED`、`RELEASE_ENTRY_GATE_BYPASS`、`IDEMPOTENCY_CONFLICT`，不输出 `MATERIAL_GATE_NOT_HARD`；这是后续实现测试计划，不是本次测试证据。

BDD: M2 Tx-A 失败不落 receipt -> Given Tx-A 任一校验或本地写入失败，When 完成命令执行，Then 所有回填回滚、无 `completionBackfillReceipt`/`BACKFILL_FAILED`，仅在回滚后追加失败尝试并返回 `BACKFILL_ATOMIC_ROLLBACK`；页面保持未完成且可重新发起。该行为是后续实现测试计划，不是本次测试证据。

DOC-STRUCTURE: Tx-A/Tx-B、活跃订单/独立入口、四材料门禁、零损耗和历史迁移边界 -> PASS（设计已记录）。

## M3 - Strict TDD Planning

RED: 后续实现阶段的 `CompletionBackfillReceiptTest` 等命令 -> NOT RUN（本次禁止实现和测试；代码审计不能冒充 RED）。

GREEN: 后续实现阶段的最小实现测试 -> NOT RUN（设计审阅不是生产 GREEN 证据）。

REGRESSION: 后续实现阶段全链回归 -> NOT RUN（无服务、无写入型 E2E）。

## M4 - Documentation Verification

DOC-STRUCTURE: 五份文件存在、包含目标态/事实/根因/边界/接口/数据/状态/BDD/RED/GREEN/REGRESSION/blocker/迁移回滚/跨线程合同 -> PASS。

## Change Boundary

- 仅修改本任务目录五份 Markdown 文档。
- 未修改 Java/TypeScript/SQL、未改数据库、未启动服务、未运行写入型 E2E、未提交生产实现。

## Remaining Blockers

- 流程4/5/7需冻结并实现 receipt、零损耗事实和完整映射。
- 流程8需确保四材料门禁不可被旧配置绕过。
- 流程9需为各独立入口落地正式等价凭证。
- 流程10/11需提供最终放行与总门禁实现证据；旧批次需先迁移或阻断。

## Current Status

completed（文档审计完成；生产实现和验证待后续）。

## 主流程合同同步记录（2026-08-22）

DOC-STRUCTURE: 已将 Tx-A immutable receipt 与流程6 `BatchProvisioningRecord` 分离、Tx-C 流程7映射顺序及稳定错误码 `TRACE_MAPPING_BLOCKED`、流程8冻结材料错误码集合、Tx-A 失败不落 receipt、`MATERIALS_PENDING/MATERIALS_READY/MATERIALS_RECHECK_REQUIRED` 门禁、完整领料/损耗字段、活跃完成链幂等键、独立凭证字段与服务端有效期、建批重试白名单、历史 dry-run 分类和独立追溯 `NOT_APPLICABLE` 写回五份流程6文档 -> PASS（结构核验，不是代码 GREEN）。

RED: 新增合同测试 -> NOT RUN（本次仍为文档任务）。
GREEN: 新增合同实现 -> NOT RUN。
