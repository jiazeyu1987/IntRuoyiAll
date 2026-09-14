# Backend API Design

## Purpose and Scope

本设计把 eDHR 批记录单元格链接从“前端 DRAFT 页面临时预填”改为“创建/打开执行记录时后端自动落库预填”。目标是让生产工单批号等链接值成为执行记录正式 `cell_values_json` 的一部分，并与字段审计哈希链、批次详情只读预览、执行页详情保持一致。

范围包含 MES 后端服务边界、内部服务接口、事务、幂等、错误模型和与现有接口的响应契约。范围不包含本阶段代码实现、数据库迁移执行、测试执行或生产数据修复。

## Evidence Reviewed

- `MesProBatchRecordCellLinkServiceImpl#getPrefill(Long targetExecutionId, Long workTaskId)` 已按启用规则计算可预填项和冲突项，生产工单来源字段包含 `batchCode`。
- `MesProBatchRecordExecutionServiceImpl#openOrCreateByContext` 创建执行记录时初始化 `cellValuesJson("[]")` 和 genesis 字段审计链，未调用预填落库。
- `MesProEdhrBatchExecutionServiceImpl#openOrBindTraditionalBatchRecordExecution` 在打开批次工序任务时会绑定或创建传统批记录执行记录，具备“打开任务”写边界。
- `MesProBatchRecordExecutionFieldAuditServiceImpl#saveChanges` 已维护字段审计哈希链、幂等键、字段变更明细和草稿保存证据，但公共命令要求 `workTaskId` 和人工填写上下文。
- 前端 `ExecutionPage.vue` 当前只在 DRAFT 状态请求 `/prefill` 并 hydrate 本地草稿；只读组件只消费已保存的 `cellValuesJson`。

## Modules

- `MesProBatchRecordCellLinkService`：保留现有规则配置、来源字段枚举和预填计算能力；建议抽取“解析适用规则与冲突”的纯计算方法，供预览和自动落库复用。
- `MesProBatchRecordCellLinkAutoPersistService`：新增后端内部服务边界，负责按执行记录、触发点、可选工作任务上下文自动落库链接值。
- `MesProBatchRecordExecutionService`：在 `openOrCreateByContext` 新建执行记录后触发创建态自动落库；返回响应可携带自动落库摘要。
- `MesProEdhrBatchExecutionService`：在 `/task/open` 的写边界中，对历史空草稿或首次打开任务触发补齐，保证“打开执行记录”也落库。
- `MesProBatchRecordExecutionFieldAuditService`：建议新增内部系统预填写入方法，复用哈希、字段解析、审计 batch/item 生成逻辑，但不要求人工密码。
- `MesProEdhrOperationAuditService`：记录自动落库成功、冲突和失败的操作审计事件，便于问题追踪。

## API Contracts

外部接口尽量不新增入口，主要增强现有写路径返回信息。

`POST /mes/pro/edhr-batch-execution/task/open`

- 请求保持 `EdhrBatchExecutionTaskOpenReqVO`：`batchExecutionId`、`taskId`、`workTaskId`。
- 响应建议在 `EdhrBatchExecutionTaskOpenRespVO` 增加 `cellLinkAutoPersist`：
  - `executionId`
  - `trigger`: `TASK_OPEN`
  - `appliedCount`
  - `conflictCount`
  - `items[]`: `ruleId`、`ruleVersion`、`targetCellKey`、`sourceType`、`sourceFieldCode`、`status`
  - `headHashAfter` 和 `fieldAuditRevisionAfter` 可选，用于前端调试和 E2E 断言。

`MesProBatchRecordExecutionService#openOrCreateByContext`

- 内部响应 `MesProBatchRecordExecutionOpenOrCreateByContextRespVO` 建议增加同结构 `cellLinkAutoPersist`，触发点为 `EXECUTION_CREATE` 或 `EXECUTION_OPEN_OR_CREATE_EXISTING`。
- 新建执行记录后立即调用自动落库服务；若是已存在 DRAFT 且目标链接单元格为空，允许在明确触发点下补齐。
- 不建议让 `GET /mes/pro/batch-record-execution/get` 执行写入，以避免只读详情接口产生隐式副作用；直接打开详情前应通过批次任务 `task/open` 写边界完成补齐。

内部服务命令：

```java
class BatchRecordCellLinkAutoPersistCommand {
    Long executionId;
    Long workTaskId;
    String trigger; // EXECUTION_CREATE, TASK_OPEN
    boolean failOnProductionWorkOrderMissingValue;
    String idempotencyNamespace;
}
```

内部服务结果：

```java
class BatchRecordCellLinkAutoPersistResult {
    Long executionId;
    int appliedCount;
    int conflictCount;
    List<Item> items;
    Long fieldAuditRevisionAfter;
    String fieldAuditHeadHashAfter;
}
```

状态口径：

- `APPLIED`：来源值有效、目标为空、已写入 `cell_values_json` 并生成审计证据。
- `NO_CHANGE_ALREADY_APPLIED`：同规则版本和同来源值已经落库，本次幂等无变更。
- `TARGET_ALREADY_MANUAL`：目标已有值且规则为 `ONLY_WHEN_EMPTY`，不覆盖。
- `SOURCE_VALUE_MISSING`：来源字段为空；生产工单批号场景必须阻断，不写空值。
- `SOURCE_EXECUTION_MISSING`：跨表来源执行记录缺失；阶段一作为显式冲突返回，不伪造值。
- `RULE_NOT_APPLICABLE`：目标报表、scope、版本不匹配，不写入。

## Error Model

- 目标执行记录不存在：抛出现有执行记录不存在错误，不返回空结果。
- 生产工单不存在或执行记录缺 `workOrderId`：抛出现有单元格链接工单缺失错误，阻断创建/打开。
- 生产工单 `batchCode` 为空且存在启用规则：抛出新增业务错误 `PRO_BATCH_RECORD_CELL_LINK_AUTO_PERSIST_SOURCE_VALUE_MISSING`，错误信息包含目标执行记录、规则 ID、来源字段和目标单元格。
- 目标单元格已有人工值：不抛错，作为显式冲突返回并写操作审计，不覆盖。
- 字段审计基线不匹配、哈希链冲突、幂等请求 hash 冲突：沿用字段审计错误模型，事务回滚。
- 目标字段在 execution snapshot 中不存在或组件不支持普通值写入：抛出配置错误，阻断该执行记录打开，要求修复规则或模板。
- 不允许吞异常后继续展示空单元格；前端收到错误时显示阻断状态。

## Transactions and Idempotency

- 自动落库必须在一个后端事务内执行，读取目标执行记录时使用 `selectByIdForUpdate` 或等价锁，避免并发打开重复写入。
- 自动落库必须按当前执行记录的 `cell_values_json`、`cell_values_hash`、`field_audit_revision`、`field_audit_head_hash` 作为基线。
- 每个目标单元格的幂等键建议为 `CELL_LINK_AUTO_PREFILL:{trigger}:{executionId}:{ruleId}:{ruleVersion}:{targetCellKey}:{sourceValueHash}`。
- 同一幂等键和同一请求 hash 已存在时返回既有结果，不追加审计 batch/item。
- 同一目标单元格如已存在相同值但缺少本次自动预填审计证据，设计建议按 `TARGET_ALREADY_MANUAL` 处理，不反向认定为系统已应用，避免篡改责任来源。
- 变更写入必须同时更新 `cell_values_json`、`cell_values_hash`、`field_audit_revision`、`field_audit_head_hash`；禁止直接 update `cell_values_json`。
- 多条规则指向同一目标单元格时应 fail-fast 为配置冲突，除非现有规则保存层已禁止该形态；自动落库阶段不得随机选择一条。

## Open Questions

- 是否需要在规则表增加 `required_on_open` 之类字段，用于跨表来源缺失时区分阻断和显式冲突。阶段一可先对生产工单来源批号强制阻断，对跨表来源只显式冲突。
- 是否需要为历史 DRAFT 执行记录提供一次性管理端 backfill 命令。当前设计优先在任务打开时补齐，避免批量改历史数据。
- 自动预填审计原因分类使用现有枚举还是新增 `SYSTEM_AUTO_PREFILL`，实现前需核对字段审计原因分类常量。

## Design Blockers

- 实现前必须确认字段审计内部系统写入方法的责任人和签名证据模型，不能绕过哈希链直接写主表。
- 实现前必须确认 `task/open` 是所有可编辑打开路径的入口；若存在直接进入执行页的正式路径，需补显式写边界而不是让 GET 详情隐式写库。
