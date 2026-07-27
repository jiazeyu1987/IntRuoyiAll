# Backend API Design

## Purpose and Scope

后端设计目标是在表单中心模板池中提供稳定、租户隔离、可审计的批记录报表绑定，使前端 `打开 / 编辑 / 填写` 能以 `reportId` 调用批记录表单已有接口。范围包括表单中心模板响应、批记录报表绑定解析、错误模型和测试契约；不包括批记录设计器自身接口重写。

## Evidence Reviewed

- `FormCenterTemplateRespVO.java:12` 至 `40`：当前响应只有模板编号、名称、版本、状态、识别字段、`jimuSchemaJson` 和源文件名，缺少 `reportId`。
- `FormCenterRuntimeServiceImpl.java:101` 至 `105`：模板池直接从 `FormTemplateVersionDO` 转换响应。
- `MesProBatchRecordReportServiceImpl.java:1260` 至 `1282`：批记录分页已支持 `reportId` 精确过滤，并按批记录版本和产品展开。
- `IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts`：设计器路径、编辑路径、规则和签名接口都围绕 `reportId`。

## Modules

- BPM 表单中心模块负责暴露模板池和模板版本元数据。
- MES 批记录模块负责批记录报表元数据、Jimu 报表路径、规则、签名和模拟填写入口。
- 推荐新增一个清晰边界：表单中心只暴露已绑定的批记录报表摘要，不复制 MES 设计器业务逻辑。

## API Contracts

- 扩展 `GET /form-center/template-pool` 响应，每个模板版本增加批记录绑定摘要。
- 推荐响应字段：
  - `batchRecordReportId: string | null`
  - `batchRecordReportName: string | null`
  - `batchRecordName: string | null`
  - `batchRecordVersionNo: string | null`
  - `batchRecordFormSlotType: MAIN | LOSS_REPORT | PROCESS_INSPECTION | PARAMETER_RECORD | null`
  - `batchRecordBindingStatus: BOUND | UNBOUND | BROKEN`
  - `batchRecordBindingError: string | null`
- 如果选择独立详情接口，接口建议为 `GET /form-center/templates/{templateId}/versions/{versionNo}/batch-record-binding`，但列表页仍应可一次返回摘要，避免每行 N+1 请求。
- 不新增前端直接调用 `/mes/pro/batch-record-report/page` 按名称查找的设计；这是不稳定匹配。

## Error Model

- `UNBOUND`：模板版本没有批记录报表绑定，前端阻塞三按钮并提示管理员绑定。
- `BROKEN`：绑定存在但目标 `reportId` 在 MES 元数据或 Jimu 报表中不存在，后端返回明确状态和错误说明。
- `BOUND`：绑定存在且目标报表元数据可用，前端允许三按钮跳转。
- 后端不得把 `UNBOUND` 或 `BROKEN` 转换为空对象成功，也不得回退到模板 `jimuSchemaJson`。

## Transactions and Idempotency

- 查询模板池为只读，不创建、不修复、不迁移绑定。
- 导入或升级模板时如要创建绑定，必须在导入事务中原子写入模板版本与批记录报表关系。
- 重新导入同一模板版本不得生成多个有效绑定；唯一性由数据模型保证。
- 批记录报表被删除或作废时，绑定应进入 `BROKEN` 或被业务流程显式解绑，不允许静默指向最新同名报表。

## Open Questions

- 绑定应由表单中心导入动作创建，还是由批记录 Word 导入动作反向关联表单模板？
- 当前历史 `bpm_form_template_version` 是否有可迁移到批记录 `reportId` 的可靠源字段？
- 模板作废、停用、发布状态与批记录版本状态是否需要双向联动，还是只读展示绑定状态？

## Design Blockers

- 若没有正式数据关系，后端无法安全返回 `batchRecordReportId`。
- 若跨 BPM 与 MES 模块直接循环依赖不符合项目模块边界，需要新增 adapter 或 service facade，而不是在 BPM 模块硬引 MES mapper。
- 若历史数据无法唯一映射，必须先形成迁移规则和人工处理清单。

