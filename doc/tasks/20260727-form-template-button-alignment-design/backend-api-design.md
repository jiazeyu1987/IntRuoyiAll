# Backend API Design

## Purpose and Scope

本设计纠正 BPM FormCenter 模板池曾错误暴露批记录绑定摘要的问题。表单模板三个按钮只需要当前模板自身数据，因此后端保持 FormCenter 模板契约，不新增 BPM 到 MES 的数据关系或运行时依赖。

## Evidence Reviewed

- `FormCenterTemplateRespVO` 的正式职责是返回模板编号、名称、版本、状态、识别字段、`jimuSchemaJson` 和源文件信息。
- `FormTemplateVersionDO` 对应 FormCenter 模板版本持久化，不需要保存 MES 批记录报表摘要。
- `FormCenterRuntimeServiceImpl#toTemplateResp` 负责从模板版本组装模板池响应。
- 纠偏前新增的七个 `batchRecord*` 字段和映射仅为支撑错误按钮跳转，没有独立业务来源。

## Modules

- BPM FormCenter 模块继续拥有模板导入、版本、模板池、规则和模板生命周期。
- MES 批记录模块继续独立拥有批记录报表和批次执行。
- 两个模块可复用通用前端渲染组件或规则结构，但本任务不建立 BPM -> MES 服务调用、数据库外键或 DTO 绑定。

## API Contracts

- `GET /form-center/template-pool` 保持 FormCenter 模板响应。
- 响应不包含：
  - `batchRecordReportId`
  - `batchRecordReportName`
  - `batchRecordName`
  - `batchRecordVersionNo`
  - `batchRecordFormSlotType`
  - `batchRecordBindingStatus`
  - `batchRecordBindingError`
- 三个按钮使用既有模板字段，不需要新增接口。
- 本次不修改 MES 批记录接口。

## Error Model

- 模板池查询继续使用既有权限、租户和请求错误模型。
- 未绑定批记录表单不是 FormCenter 模板错误状态，后端不得构造 `UNBOUND/BROKEN` 阻断信息。
- 模板自身数据缺失或解析失败时，按现有 FormCenter 错误链路返回真实失败，不切换 MES 数据源、不返回默认模板。

## Transactions and Idempotency

- 模板池查询为只读，不创建、修复或推断批记录绑定。
- 模板规则保存继续以 `templateId + versionNo` 定位当前模板版本，并沿用既有事务边界。
- 删除错误响应字段不会产生数据写入，也不需要兼容双写或回填。

## Open Questions

- 本次没有 API 待确认项。
- 若未来新增正式跨域转换能力，需重新定义独立 API、权限、事务和审计，不得恢复当前已移除字段作为隐式兼容。

## Design Blockers

- 当前实现无 blocker。
- 本地数据库可能仍存在此前误加的冗余列，但代码不读取这些列，不阻塞当前 API 行为；物理清理由单独迁移审计决定。
