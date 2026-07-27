# Data Model Design

## Purpose and Scope

本数据模型设计确认 FormCenter 表单模板与 MES 批记录表单不存在直接数据关系。当前任务移除代码和未发布迁移中的错误绑定契约，不新增关系表、外键、摘要字段或绑定状态。

## Evidence Reviewed

- `bpm_form_template_version` 由 `FormTemplateVersionDO` 映射，核心数据是模板版本、源文件、识别结果和 `jimuSchemaJson`。
- MES 批记录报表使用独立数据模型和 `reportId`，其生命周期不属于 FormCenter 模板版本。
- 错误迁移 `20260727_bpm_form_template_batch_record_binding.sql` 曾新增七个绑定列和索引，但未发现进入正式发布记录。
- 本地数据库已经存在这些冗余列和索引；当前任务没有破坏性数据库变更授权。

## Entities

- `FormTemplateVersion`
  - 标识：`templateId + versionNo`。
  - 职责：保存模板名称、状态、源文件、识别结构和模板规则/布局。
- `BatchRecordReport`
  - 属于 MES 批记录领域。
  - 不作为 `FormTemplateVersion` 的父实体、子实体或绑定实体。

## Relationships

- 本任务定义两者之间没有持久化关系。
- FormCenter 模板三个按钮不查询、推断或保存 `BatchRecordReport.reportId`。
- 前端复用批记录样式或规则渲染工具不构成数据关系。

## State Models

- FormCenter 模板继续使用既有 `DRAFT`、发布、停用、作废等模板生命周期状态。
- 不存在 `BOUND`、`UNBOUND`、`BROKEN` 等批记录绑定状态。
- 三个按钮是否可见继续由模板自身状态和既有权限决定。

## Migration Notes

- 删除未发现正式发布引用的错误迁移 `IntRuoyiBackend/sql/mysql/20260727_bpm_form_template_batch_record_binding.sql`。
- 删除该迁移的旧专用测试 `test_form_template_batch_record_binding_sql.py`。
- 新增独立性合同，确保错误迁移和旧测试不再进入发布内容。
- 本次不执行 `DROP COLUMN`、`DROP INDEX`、数据回填或远端数据库操作。
- 本地已存在的七个冗余列和索引保持惰性，代码不再映射或读取。

## Data Integrity Rules

- FormCenter 模板版本只保存其自身领域数据。
- 不得根据模板名、源文件名、版本号或 UI 位置推断批记录 `reportId`。
- 不得保留条件双路径或把空绑定字段当作业务状态。
- 未来如需物理删除本地/目标环境冗余列，必须先审计迁移是否曾发布、目标环境使用情况、备份和回滚方案。

## Open Questions

- 冗余列是否需要在后续版本物理清理，需单独确认实际发布历史和目标环境 schema。

## Design Blockers

- 冗余列物理清理当前被授权范围和迁移历史审计阻塞。
- 该阻塞不影响本次代码解耦和三个按钮正常执行。
