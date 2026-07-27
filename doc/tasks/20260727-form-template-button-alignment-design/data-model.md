# Data Model Design

## Purpose and Scope

本数据模型设计用于支撑表单中心模板版本到 MES 批记录报表的稳定映射。核心原则是可追溯、唯一、租户隔离、禁止模糊匹配。文档不直接执行迁移，只定义后续实现应采用的数据关系和完整性规则。

## Evidence Reviewed

- `bpm_form_template_version` 对应 `FormTemplateVersionDO`，当前字段包含 `templateId`、`tenantId`、`templateName`、`versionNo`、`status`、源文件、识别 JSON 和 `jimuSchemaJson`。
- 批记录报表元数据由 MES 模块维护，前端和服务层以 `reportId` 作为设计器、编辑器、规则、签名和模拟填写入口。
- 批记录分页已经支持 `reportId` 精确过滤，说明 `reportId` 是现有批记录表单行为的稳定主键。

## Entities

- `FormTemplateVersion`
  - 主体：表单中心模板版本。
  - 关键字段：`templateId`、`tenantId`、`versionNo`、`status`。
- `BatchRecordReport`
  - 主体：MES 批记录报表元数据和 Jimu 报表定义。
  - 关键字段：`reportId`、`reportName`、`batchRecordName`、`versionNo`、`formSlotType`。
- `FormTemplateBatchRecordBinding`
  - 主体：模板版本与批记录报表的正式关系。
  - 推荐字段：`id`、`tenantId`、`templateId`、`templateVersionNo`、`batchRecordReportId`、`bindingSource`、`bindingStatus`、`createdTime`、`updatedTime`。

## Relationships

- 一个 `FormTemplateVersion` 在同一租户下最多绑定一个当前有效 `BatchRecordReport`，除非产品确认一对多。
- 一个 `BatchRecordReport` 可以被一个模板版本引用；是否允许多个模板版本引用同一 `reportId` 需产品确认。
- 绑定关系不得依赖 `templateName == reportName`、`sourceFileName == sourceFileName` 或 `versionNo == versionNo`。

## State Models

- `BOUND`：绑定存在，目标批记录报表元数据存在且可被当前租户访问。
- `UNBOUND`：模板版本没有绑定批记录报表。
- `BROKEN`：绑定存在，但目标批记录报表不存在、不可访问或租户不一致。
- `OBSOLETE`：如后续需要，可表示模板或批记录版本已作废但历史关系保留。

## Migration Notes

- 新增映射表是推荐方案，因为它避免污染 `bpm_form_template_version`，也避免 BPM 模块直接理解 MES 报表内部字段。
- 如果选择扩展 `bpm_form_template_version` 增加 `batch_record_report_id`，必须同步处理历史数据唯一性、软删除和租户边界。
- 历史模板不能自动按名称绑定；需要生成待处理清单，由管理员或正式迁移规则填充。
- 迁移前必须核对真实库表结构和现有迁移脚本；不得只根据 DO 类推断字段。

## Data Integrity Rules

- 唯一约束：`tenant_id + template_id + template_version_no` 只能有一个有效绑定。
- 外部完整性：`batch_record_report_id` 必须对应当前租户可见的批记录报表。
- 删除策略：批记录报表删除前必须检测绑定；若业务允许删除，则绑定进入 `BROKEN` 或被显式解绑。
- 审计规则：绑定创建、修改、解绑必须记录操作来源，不能由查询接口隐式修复。

## Open Questions

- 是否需要支持一个表单模板版本绑定多个表单槽位或多个产品版本？
- 已发布模板绑定变更是否需要审批，或只允许草稿模板绑定变更？
- 是否已有批记录导入结果能可靠回写 `templateId + versionNo -> reportId`？

## Design Blockers

- 缺少正式唯一关系时，不能安全实施前端三按钮对齐。
- 历史数据若存在多个同名批记录报表，任何名称匹配都会产生误跳转风险，必须阻塞。

