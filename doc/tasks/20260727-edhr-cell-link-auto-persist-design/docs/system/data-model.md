# Data Model Design

## Purpose and Scope

本设计定义 eDHR 单元格链接自动落库涉及的数据实体、关系、状态和完整性约束。核心目标是让链接值写入正式执行记录值域，并保留字段审计链证据，而不是依赖前端本地草稿状态。

阶段一不要求新增表。若实现阶段发现现有字段审计元数据不足以表达“系统自动预填”来源，可通过字段审计 batch/item 的原因、备注或扩展字段记录规则元数据；新增列需另行提供正式迁移和 schema-backed E2E 验证。

## Evidence Reviewed

- `mes_pro_batch_record_cell_link_rule` 保存单元格链接规则，现有 VO 暴露 `ruleId`、`ruleVersion`、`sourceType`、`sourceFieldCode`、`targetCellKey`、`overwritePolicy`。
- `mes_pro_batch_record_execution` 保存执行记录、`cell_values_json`、`cell_values_hash`、`field_audit_revision`、`field_audit_head_hash`。
- 字段审计 batch/item 表保存字段级变更证据，字段审计服务负责生成哈希链和保存草稿证据。
- `mes_pro_work_order.batch_code` 是生产工单“生产批号”来源字段。
- `mes_pro_edhr_batch_execution.batch_code` 保存批次执行批号；本次问题中该值和工单批号均存在，目标执行记录 `cell_values_json=[]`。

## Entities

- `MesProBatchRecordCellLinkRuleDO`：链接规则实体。关键字段包括来源类型、来源字段、目标报表、目标单元格、规则版本、启用状态和覆盖策略。
- `MesProBatchRecordExecutionDO`：批记录执行记录。自动落库最终写入该实体的 `cellValuesJson`，并同步更新字段审计哈希字段。
- `MesProWorkOrderDO`：生产工单实体。`batchCode` 是本次生产批号来源。
- `MesProEdhrBatchExecutionDO`：eDHR 批次执行实体。用于定位批次、路线和任务上下文。
- `MesProEdhrBatchExecutionTaskDO`：批次工序任务实体。用于定位目标执行记录和打开任务的写边界。
- `MesProEdhrWorkTaskDO`：用户工作任务实体。打开填报任务时提供可写上下文和权限校验来源。
- `MesProBatchRecordExecutionFieldAuditBatchDO`：字段审计批次。自动预填应生成系统来源审计批次或等价证据。
- `MesProBatchRecordExecutionFieldAuditItemDO`：字段审计明细。每个自动落库单元格对应至少一条字段变更明细。
- `MesProBatchRecordExecutionSignature` 相关实体：用于草稿保存或签名证据。系统自动预填不使用人工密码，但需要明确系统证据类型。

## Relationships

- 一个 `MesProBatchRecordExecutionDO` 归属一个生产工单、一个批次执行、一个批记录报表和一个批记录版本。
- 一个目标执行记录可匹配多条启用的 `MesProBatchRecordCellLinkRuleDO`，匹配范围由版本、scope、目标报表和启用状态决定。
- `PRODUCTION_WORK_ORDER` 来源规则通过 `execution.workOrderId -> MesProWorkOrderDO.id` 读取来源字段。
- 自动落库写入 `MesProBatchRecordExecutionDO.cellValuesJson` 后，必须创建字段审计 batch/item 与该 execution 关联。
- eDHR 批次任务通过 `task.executionId -> execution.id` 让只读预览和执行页读取同一份 `cellValuesJson`。

## State Models

链接规则应用状态：

- `ENABLED_RULE_MATCHED`：规则启用且 scope、版本、目标报表匹配当前执行记录。
- `SOURCE_RESOLVED`：来源工单或来源执行记录已解析，来源值非空。
- `TARGET_EMPTY`：目标单元格在当前 `cell_values_json` 中不存在有效值。
- `APPLIED`：目标为空且来源有效，系统已写入目标单元格和字段审计链。
- `CONFLICT_TARGET_MANUAL`：目标已有值，因 `ONLY_WHEN_EMPTY` 不覆盖。
- `BLOCKED_SOURCE_MISSING`：生产工单来源值缺失，不能落库空值。
- `NO_CHANGE_ALREADY_APPLIED`：重复打开时幂等命中，无新审计写入。

执行记录状态影响：

- 仅 DRAFT 执行记录允许自动补齐链接值。
- 已提交、待复核、已归档或终态记录不允许自动变更单元格值；如存在空值，应返回显式冲突或阻断，避免事后改动历史记录。
- 预发布可编辑状态是否允许自动补齐需实现前单独确认；当前设计不把自动预填扩展到非 DRAFT。

## Migration Notes

- 阶段一可不新增表和列，优先复用字段审计 batch/item、签名草稿保存证据和 operation audit。
- 若需要持久化 `ruleId`、`ruleVersion`、`sourceType`、`sourceFieldCode`、`sourceExecutionId` 的结构化字段，必须新增正式迁移、DO/Mapper 字段、schema contract 测试和真实 E2E schema 证据。
- 不设计通过 SQL 直接回填历史 `cell_values_json`；历史空草稿优先在用户打开任务时按当前规则幂等补齐。
- 不把 `mes_pro_edhr_batch_execution.batch_code` 当生产工单批号的替代来源；生产工单来源规则必须读取 `MesProWorkOrderDO.batchCode`，以符合配置语义。

## Data Integrity Rules

- `cell_values_json` 与 `cell_values_hash` 必须始终匹配，写入后字段审计 hash verification 必须为 `VALID`。
- `field_audit_revision` 和 `field_audit_head_hash` 必须随自动预填单元格变更递增和更新。
- 目标单元格定位必须使用 `targetCellKey`、`targetRowIndex`、`targetColumnIndex` 和 execution snapshot 字段目录交叉校验；不可只按行列硬写。
- `overwritePolicy=ONLY_WHEN_EMPTY` 时，只要目标已有有效值即不覆盖，哪怕来源值不同。
- 来源值为空、空白字符串或无法转换成目标组件类型时不写入。
- 同一执行记录同一目标单元格不得在同一次自动落库中被多条规则写入。
- 自动预填写入的 field audit item 必须记录变更前值、变更后值、字段身份、单元格坐标和来源规则信息。
- 任何解析 JSON 失败、snapshot 缺字段或 schema 缺列都必须 fail-fast，禁止返回空数组冒充成功。

## Open Questions

- 是否需要新增结构化审计元数据列来保存规则来源，而不是把规则元数据放在原因文本或扩展 JSON 中。
- 是否要把自动预填结果同步展示到字段责任归属页面，显示为“系统根据单元格链接自动预填”。
- 是否需要配置级别区分“强制来源”和“可选来源”，用于跨表来源尚未产生时的阻断策略。

## Design Blockers

- 字段审计系统写入的证据形态必须先确定；如果现有字段审计表无法表达系统预填来源，实现不得退化为主表直接 update。
- 若需要新增审计元数据列，必须先完成数据库迁移设计和 schema-backed 测试计划。
