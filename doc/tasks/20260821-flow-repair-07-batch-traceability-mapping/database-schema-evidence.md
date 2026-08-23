# 流程7数据库 Schema 实施证据

## Goal and Affected Entities

目标是为批次执行增加不可篡改的 Origin、TraceLink、Manifest 关系，并支持按正式来源 ID/hash 读取详情、列表和历史迁移分类。

## Engine and Migration

项目现有迁移为 MySQL 脚本，实际运行库未在本任务中连接或修改。任何新表/列/唯一索引必须先由流程1、4、5、6、8、9、10冻结字段和 schema owner，再按 release-migration 元数据提交。

## Required Constraints

- Origin 唯一键必须区分 `ACTIVE_ORDER_COMPLETION` 与 `PQC_INDEPENDENT`、`MANUAL`、`SCHEDULED` 等入口。
- TraceLink 和 sealed Manifest 只能追加；纠错使用 CORRECTION/VOID_REFERENCE，不更新或物理删除历史关系。
- Origin 必须持久化 hasActualLoss；活跃订单的 sourceSnapshotHash 必须与领料正式来源快照 hash 绑定，不能仅以非空值通过。
- 活跃订单主键必须是 `activeOrderId + completionTransactionId`，不能以 `releaseApplicationId` 代替。
- 同一来源 ID 的不同 snapshot hash 必须阻断；无损耗只保存 NO_LOSS 事实，不创建损耗单行。

## RED Evidence

`RED: schema contract scan -> FAIL, current mes_pro_edhr_batch_execution schema has no activeOrderId/completion receipt/pick-list/source relation columns or Origin/TraceLink/Manifest tables.`

## BDD

`BDD: immutable trace relation -> Given a batch has a formal source snapshot When a second capture uses a different hash Then the unique relation remains unchanged and the request is blocked.`

## GREEN and Migration Verification

`GREEN: migration execution NOT RUN; the authored Flow7 migration slice is structurally present and passed static scan only (3 tables, 6 append-only triggers, 7 SIGNALs). Formal pick-list, completion, loss and material-manifest owner contracts still block execution.`

`REGRESSION: NOT RUN, no database connection or migration execution was authorized.`

## Data Safety and Rollback

本任务不执行 DDL/DML，不触碰真实业务数据。实施前必须提供 dry-run 迁移分类、备份、影响范围、可逆脚本和已放行历史的人工复核策略；缺任一项即阻断。

## Safety

本轮不连接真实数据库，不执行 DDL/DML，不修改既有批次或历史关系。

## Rollback

正式实施前必须提供 dry-run 分类、备份、影响范围、可逆脚本和已放行历史人工复核策略；缺任一项即阻断。

## Verification

已完成迁移切片静态扫描；未执行或应用 schema，未宣称运行时数据库已修复。

## 2026-08-22 Slice Evidence

已新增但未执行 20260822_mes_edhr_batch_traceability.sql：三张 Origin/TraceLink/Manifest 表、正式 ID/hash 字段、has_actual_loss 损耗决策字段、入口/幂等唯一键、manifest hash 链和 UPDATE/DELETE 阻断触发器。流程6 BATCH_PROVISION_RECEIPT 使用现有 TraceLink 字符串类型保存正式凭证 ID/hash，不需要另加列；未连接数据库、未执行 DDL/DML，回滚演练和上游 schema owner 仍为 blocker。

## Blockers

正式领料、完成、损耗、材料 manifest 和入口凭证字段尚未冻结，无法安全产生新迁移。

当前关系语义补充：TraceLink 使用 `NO_LOSS_CONFIRMED` 保存流程5的正式无损耗事实；`LOSS_FACT` 与 `LOSS_REPORT_RECEIPT` 仅用于 `has_actual_loss=1`。数据库仍由 append-only 触发器保护，manifest hash 链和关系状态由服务读取门禁复核；迁移尚未执行。
- 来源唯一身份由服务按 linkType/sourceObjectType/sourceObjectId/sourceLineId/sourceEventId 生成并写入 source_identity_key；sourceVersion 不参与唯一键。调用方提供的 sourceIdentityKey 若非空必须与 canonical 值一致，否则服务在入库前返回 TRACE_SOURCE_CONFLICT。
- 读取详情时服务重新校验 TraceLink 的 source_identity_key 与 snapshot_hash，防止数据库侧篡改绕过 append-only 约束；不一致返回 TRACE_MAPPING_BLOCKED。

## 2026-08-23 Tx-C Schema and Verification Boundary

- Tx-C adds the append-only `mes_pro_edhr_batch_trace_outbox_event` table with tenant/event/idempotency keys, batch/origin/link IDs, event/status/error/reason, source and manifest hashes, payload hash and retryability. Two outbox UPDATE/DELETE triggers join the six Origin/TraceLink/Manifest append-only guards.
- Main-workspace Maven compile/testCompile and the 29 Flow7 focused tests (17 validator + 12 service contract) passed; the post-commit invocation copied testResources normally. This remains application-slice evidence only and does not prove a real migration/runtime.
- Real MySQL migration, append-only trigger execution, Mapper queries, permission runtime and rollback rehearsal remain `NOT RUN`; the authored SQL remains un-applied.
- `validate_database_schema.py -> PASS`; static SQL shape scan is `PASS (tables=3, append-only triggers=6, SIGNAL guards=7)`. This does not prove that MySQL accepted or executed the migration.
