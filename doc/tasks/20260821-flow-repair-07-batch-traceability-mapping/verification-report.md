# 流程修复 7 文档验证报告

## Latest Authoritative Verification (2026-08-23)

Flow7 implementation slice: **PASS for the Flow7 task-owned scope (fresh current-int_main verification)**. Current `int_main` Maven 3.9.16 clean compile passed at `2026-08-23T15:39:37+08:00` across 24 reactor modules (MES 2857 main sources); 29 focused tests (17 validator + 12 service contract) passed at `2026-08-23T15:40:52+08:00` with zero failures/errors/skips (`BUILD SUCCESS`). Test resources were copied normally. The verified slice covers task-owned Origin/TraceLink/Manifest contracts, formal Tx-C producer/outbox handling, source-precheck resolver, explicit `originId`, persisted `RELEASE_DECISION` TraceLink resolution, canonical source identity mismatch blocking, loss-fact mapping, `FLOW8_SOURCE_PRECHECK_STALE` after a post-precheck source mutation, and `FLOW8_TRACE_LINK_ORIGIN_MISMATCH` for a batch/origin relation mismatch. Tx-C maps source mutation to stable `TRACE_MAPPING_BLOCKED` with `SOURCE_CHANGED_AFTER_PRECHECK`. The linked-worktree flatten/compiler ACL failures are historical context only.

Full target: **BLOCKED**. Formal Flow1 pick-list binding, Flow4 completionBackfillReceipt, Flow5 loss receipt, and Flow6 batch-provision receipt adapters/owners/fixtures are not all connected; real database migration/append-only trigger/Mapper/permission/runtime verification and service startup are `NOT RUN`; Flow8 four-material gate, Flow10 final `RELEASED`, full regression and write-enabled E2E are also `NOT RUN`. Flow7 does not own those states.

提交交付：**PASS（task-owned selective commits）**。当前 `int_main` 已包含 `0767b1fa5` 以及后续验证/文档提交；未使用 `git add -A`，未重复提交已跟踪 DTO，未混入流程6/8/10或其它 dirty/untracked。提交不改变流程6/8/10状态 owner。

Tx-C 交付边界：生产入口为 `POST /mes/pro/edhr-batch-execution/traceability/tx-c`；成功事务提交四张 Flow7 表并在提交后发布 `FLOW7_TRACE_MAPPING_SUCCEEDED`，失败在独立失败事务提交 `TRACE_MAPPING_BLOCKED` outbox 事件且不保留半成品。来源预检后变化使用 `SOURCE_CHANGED_AFTER_PRECHECK` 证据 fail-fast。该合同可供流程6消费，但流程6的正式 sourceEvidence/receipt 持久化适配尚未提供。

## 验证范围

当前范围包含流程7 task-owned 实现切片、主工作区模块编译/测试和文档证据；真实服务启动、数据库迁移/Mapper/权限运行时、跨流程回归、流程8/10 集成及写入型 E2E 仍未运行。

## 文档交付

| 文件 | 结果 | 覆盖 |
| --- | --- | --- |
| task.md | PASS | 目标、里程碑、状态、预期验证、设计约束、符合性结论 |
| development-plan.md | PASS | 当前事实、根因、边界、数据/API/状态、权限、不可篡改、迁移/回滚、跨线程契约 |
| test-plan.md | PASS | BDD、严格 RED/GREEN、回归、E2E 前置与 blocker |
| execution-log.md | PASS | 用户意图、审计证据、里程碑、BDD、未执行 RED/GREEN 原因与 blocker |
| verification-report.md | PASS | 文档验证范围、结论、未解决 blocker |

## 代码审计结论

FAIL（相对于目标态）：当前实现只能可靠追溯生产工单和路线，尚未提供活跃订单完成/回填、正式领料单/分录、一线生产、一线 PQC、损耗和后续放行到批次执行的完整一等关系。资料规划未消费领料单来源；批次复用条件不能按订单完成交易区分；批次详情、列表和时间线没有来源图 API。当前零损耗报告合同与无损耗不回填规则冲突。原设计中将 releaseApplicationId 作为建批前置的错误已在本次修订中移除：它只能在后续实际放行时追加 RELEASE_DECISION 关系。

## 未解决 Blocker

- 正式领料单和分录的准确实体、审核状态和 sourceEntryId 解析契约尚未确认。
- 零损耗报告规则必须先消除冲突。
- 四份材料的文件模型、版本/hash、权限和管理者代表授权需由流程修复 8、10 提供冻结合同。
- 历史批次缺完成申请/回填/幂等关联时必须走单独、获批准的迁移方案；本任务不创建迁移。
- 各入口正式凭证尚未冻结：ACTIVE_ORDER_COMPLETION 需 activeOrderId/completionTransactionId/backfillReceipt/pickListBindingId/sourceSnapshotHash，PQC_INDEPENDENT、MANUAL、SCHEDULED 需各自 sourceCredential；不能用 releaseApplicationId 统一覆盖。流程9的 canonical 独立入口集合为 `PQC_INDEPENDENT`、`MANUAL`、`SCHEDULED`，旧 `INDEPENDENT_WORK_ORDER` 不得作为兼容旁路。

## 流程职责复核

| 流程 | 结论 |
| --- | --- |
| 2 | 一线生产提交、生产组长复核/驳回重提、分配事实；不拥有完成或回填 |
| 3 | 一线 PQC 提交、PQC 组长复核/确认、PQC 汇集事实；不拥有完成或回填 |
| 4 | 活跃订单双 100% 点击完成的唯一 owner；Tx-A 统一回填三类适用表单并提交不可变 completionBackfillReceipt |
| 5 | 实际损耗判定、损耗单条件写入和 NO_LOSS 事实；不拥有 PQC 事实 |
| 6 | 消费流程4 receipt 后 Tx-B 创建/复用 batchExecutionId，拥有 batch provision 状态；不拥有三类回填 |
| 7 | 消费既有 batchExecutionId、流程4 receipt、流程1/2/3/5正式来源，在后继 Tx-C/幂等事件中建立 Origin/TraceLink/Manifest 和放行后追溯；不拥有完成、回填、建批、四材料或最终 RELEASED |
| 8 | 批次创建后上传四份材料并执行硬门禁 |
| 9 | 多创建/放行入口的 entryType、正式凭证、来源关系、幂等前置；不拥有批次状态或追溯投影 |
| 10 | 消费既有 batchExecutionId 和流程8 gate，唯一写最终放行状态/签名/审计 |
| 11 | BDD/TDD、回归、迁移和总门禁；不自动认领旧批次 |

上述职责复核通过设计一致性检查，但未运行生产代码或 E2E。

## 下一步实施门槛

实施前先关闭 blocker，并按 test-plan.md 顺序记录真实 RED、GREEN、REGRESSION 证据。任一项缺失都不得创建不完整批次、提前放行或用旧批次冒充成功。
