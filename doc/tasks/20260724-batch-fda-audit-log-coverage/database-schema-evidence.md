# Database Schema Evidence

## Data

本任务目标是补齐批执行审计日志持久化覆盖；受影响实体复用现有 `mes_pro_edhr_operation_audit_event` 与 `mes_pro_edhr_release_transaction_event`。

## Migration

- 未新增迁移。
- 未修改生产表结构。
- 现有表已包含 requestId、objectType/objectId、batchExecutionId、executionId、workTaskId、operationType、actor、permissionDecision、resultStatus、before/after hash、metadataJson、occurredAt、previousAuditHash/auditHash 等审计字段。

## Safety

- 无破坏性 schema 变更。
- 无数据回填、删除、字段类型变更或索引变更。
- 新增内容以业务代码写入现有审计表和事件表，缺少登录用户或附件原因时 fail fast。

## Rollback

- 回滚方式为回滚本任务业务代码改动；数据库无需 down migration。
- 已有审计表结构和历史数据不受影响。

## BDD

- BDD: 复用现有审计表 -> Given 现有 operation audit/release event 表可记录 metadataJson 和 hash When 新增审计事件写入 Then 不需要新增 schema。
- BDD: 附件链路审计数据 -> Given 附件 file/hash/storage 字段可进入 metadataJson When 附件预登记、删除、入账 Then 追溯可还原 who/what/when/why 和前后 hash。

## RED/GREEN

- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> FAIL，审计写入覆盖不足。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\edhr-fda-operation-audit-coverage-static.spec.cjs` -> PASS，所有新增 operationType 均复用现有表写入。

## Verification

- PASS: `rg -n "mes_pro_edhr_operation_audit_event|mes_pro_edhr_release_transaction_event" IntRuoyiBackend\yudao-module-mes\src\test\resources\sql\create_tables.sql IntRuoyiBackend\sql IntRuoyiBackend\yudao-module-mes\src\main\resources` 定位到现有审计表和放行事件表。
- PASS: 未新增 `IntRuoyiBackend\sql\mysql\20260724*` 审计迁移。
- BLOCKED: Maven 编译被非本任务 `MesProRouteFlowConfigServiceImpl` 缺失方法阻塞，无法完成编译级 schema contract 回归。

## Blockers

- 无 schema 前置阻塞；全量编译阻塞来自非本任务 Java 文件。
