# Data Model Design

## Purpose and Scope

本数据模型设计用于支撑表单中心模板版本到 MES 批记录报表的稳定映射。核心原则是可追溯、租户隔离、禁止模糊匹配。当前实现采用在 `bpm_form_template_version` 上增加显式绑定摘要字段的方案，不新增跨模块映射表。

## Evidence Reviewed

- `bpm_form_template_version` 对应 `FormTemplateVersionDO`，当前字段包含 `templateId`、`tenantId`、`templateName`、`versionNo`、`status`、源文件、识别 JSON 和 `jimuSchemaJson`。
- 批记录报表元数据由 MES 模块维护，前端和服务层以 `reportId` 作为设计器、编辑器、规则、签名和模拟填写入口。
- 批记录分页已经支持 `reportId` 精确过滤，说明 `reportId` 是现有批记录表单行为的稳定主键。

## Entities

- `FormTemplateVersion`
  - 主体：表单中心模板版本。
  - 关键字段：`templateId`、`tenantId`、`versionNo`、`status`。
  - 新增绑定摘要字段：`batchRecordReportId`、`batchRecordReportName`、`batchRecordName`、`batchRecordVersionNo`、`batchRecordFormSlotType`、`batchRecordBindingStatus`、`batchRecordBindingError`。
- `BatchRecordReport`
  - 主体：MES 批记录报表元数据和 Jimu 报表定义。
  - 关键字段：`reportId`、`reportName`、`batchRecordName`、`versionNo`、`formSlotType`。

## Relationships

- 一个 `FormTemplateVersion` 行内只承载一组当前有效批记录绑定摘要。
- 一个 `BatchRecordReport` 可以被模板版本引用；当前实现不在 BPM schema 内建立跨 MES 外键。
- 如果未来需要一个模板版本绑定多个产品、版本或槽位报表，必须另起设计引入正式关系表，不能在当前七个摘要字段上叠加分隔符或 JSON 兼容写法。
- 绑定关系不得依赖 `templateName == reportName`、`sourceFileName == sourceFileName` 或 `versionNo == versionNo`。

## State Models

- `BOUND`：绑定存在，目标批记录报表元数据存在且可被当前租户访问。
- `UNBOUND`：模板版本没有绑定批记录报表。
- `BROKEN`：绑定存在，但目标批记录报表不存在、不可访问或租户不一致。
- `OBSOLETE`：如后续需要，可表示模板或批记录版本已作废但历史关系保留。

## Migration Notes

- 已新增迁移 `IntRuoyiBackend/sql/mysql/20260727_bpm_form_template_batch_record_binding.sql`。
- 迁移为 additive schema change：只新增七个 nullable 字段和 `tenant_id, batch_record_report_id, deleted` 索引，不删除、不回填、不更新历史业务数据。
- 历史模板不能自动按名称绑定；未绑定模板保持 `UNBOUND`/空字段，并由前端三按钮 fail fast。
- 迁移前必须核对真实库表结构和现有迁移脚本；不得只根据 DO 类推断字段。

## Data Integrity Rules

- 行内约束：同一 `bpm_form_template_version` 只能保存一组当前绑定摘要。
- 查询索引：`tenant_id + batch_record_report_id + deleted` 支撑按绑定报表定位模板版本。
- 外部完整性：`batch_record_report_id` 必须由正式写入链路确认对应当前租户可见的批记录报表；BPM 查询接口不实时补查 MES 表。
- 删除策略：批记录报表删除前必须检测绑定；若业务允许删除，则绑定进入 `BROKEN` 或被显式解绑。
- 审计规则：绑定创建、修改、解绑必须记录操作来源，不能由查询接口隐式修复。

## Remaining Scope

- 绑定写入来源需由表单模板导入链路、批记录 Word 导入链路或正式绑定管理流程后续补齐。
- 已发布模板绑定变更是否需要审批，或只允许草稿模板绑定变更，仍需产品规则确认后另起实现。
- 多槽位或多报表绑定不在当前字段模型内支持。

## Verification Gates

- SQL 契约必须断言迁移文件存在、包含 fail-fast 缺表检查、只做 additive 字段和索引变更。
- 本地或目标环境应用迁移后，必须用 `information_schema.columns/statistics` 只读核对新增字段和索引。
- 任何历史数据自动回填方案都必须先提供唯一映射证据；没有证据时保持未绑定并阻塞三按钮。
