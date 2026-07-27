# Backend API Design

## Purpose and Scope

后端设计目标是在表单中心模板池中提供稳定、租户隔离、可审计的批记录报表绑定，使前端 `打开 / 编辑 / 填写` 能以 `reportId` 调用批记录表单已有接口。范围包括表单中心模板响应、批记录报表绑定解析、错误模型和测试契约；不包括批记录设计器自身接口重写。

## Evidence Reviewed

- `FormCenterTemplateRespVO.java:12` 至 `40`：当前响应只有模板编号、名称、版本、状态、识别字段、`jimuSchemaJson` 和源文件名，缺少 `reportId`。
- `FormCenterRuntimeServiceImpl.java:101` 至 `105`：模板池直接从 `FormTemplateVersionDO` 转换响应。
- `MesProBatchRecordReportServiceImpl.java:1260` 至 `1282`：批记录分页已支持 `reportId` 精确过滤，并按批记录版本和产品展开。
- `IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts`：设计器路径、编辑路径、规则和签名接口都围绕 `reportId`。

## Modules

- BPM 表单中心模块负责暴露模板池、模板版本元数据和已持久化的批记录绑定摘要。
- MES 批记录模块继续负责批记录报表元数据、Jimu 报表路径、规则、签名和模拟填写入口。
- 已采用清晰边界：表单中心只暴露已绑定的批记录报表摘要，不复制 MES 设计器业务逻辑，也不引入 BPM -> MES 运行时依赖。

## API Contracts

- 扩展 `GET /form-center/template-pool` 响应，每个模板版本增加批记录绑定摘要。
- 响应字段：
  - `batchRecordReportId: string | null`
  - `batchRecordReportName: string | null`
  - `batchRecordName: string | null`
  - `batchRecordVersionNo: string | null`
  - `batchRecordFormSlotType: MAIN | LOSS_REPORT | PROCESS_INSPECTION | PARAMETER_RECORD | null`
  - `batchRecordBindingStatus: BOUND | UNBOUND | BROKEN`
  - `batchRecordBindingError: string | null`
- 本次不新增独立详情接口；列表页一次返回摘要，避免每行 N+1 请求。
- 不新增前端直接调用 `/mes/pro/batch-record-report/page` 按名称查找的设计；这是不稳定匹配。

## Error Model

- `UNBOUND`：模板版本没有批记录报表绑定，前端阻塞三按钮并提示管理员绑定。
- `BROKEN`：绑定写入链路确认目标 `reportId` 不可用、不可访问或租户不一致，前端阻塞三按钮并显示 `batchRecordBindingError`。
- `BOUND`：绑定写入链路确认目标报表元数据可用，前端允许三按钮跳转。
- 模板池查询只读取 BPM 已持久化摘要，不实时查询 MES 表、不隐式修复绑定；后端不得把 `UNBOUND` 或 `BROKEN` 转换为空对象成功，也不得回退到模板 `jimuSchemaJson`。

## Transactions and Idempotency

- 查询模板池为只读，不创建、不修复、不迁移绑定。
- 导入或升级模板时如要创建绑定，必须在导入事务中原子写入模板版本与批记录报表关系。
- 重新导入同一模板版本不得生成多个有效绑定；当前采用模板版本行内字段承载一组有效绑定摘要。
- 批记录报表被删除或作废时，绑定应进入 `BROKEN` 或被业务流程显式解绑，不允许静默指向最新同名报表。

## Decisions And Remaining Scope

- 已决定通过扩展 `bpm_form_template_version` 持久化批记录绑定摘要，避免新增 BPM -> MES 依赖和名称匹配。
- 本次只读展示并驱动三按钮行为；绑定写入来源仍需由导入链路或正式绑定流程后续补齐。
- 模板作废、停用、发布状态与批记录版本状态暂不双向联动；后续若产品要求联动，需单独设计事务和权限规则。

## Verification Gates

- 合同测试必须断言 `FormCenterTemplateRespVO`、`FormTemplateVersionDO` 均包含七个绑定字段。
- 合同测试必须断言 `FormCenterRuntimeServiceImpl#toTemplateResp` 只从 `FormTemplateVersionDO` 映射字段，不引用 MES 类、不查询 MES 表、不按名称或源文件猜测。
- 真实运行态验收前必须确认当前后端 jar 已加载这些字段，且数据库已应用新增列迁移。
