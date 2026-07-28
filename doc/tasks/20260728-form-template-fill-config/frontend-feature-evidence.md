# Frontend Feature Evidence

## Feature Goal

新增表单中心模板预览区“填写配置”入口，复用批记录填写配置的单元格规则和辅助行配置体验，但保存到模板自身 `jimuSchemaJson`。

## Non-Goals

- 不新增后端接口。
- 不把表单中心模板绑定到批记录 `reportId`。
- 不扩展表单中心运行态分派/审批人执行语义。

## UI Entry Points

- `/mdm/form-center/template`
- 右侧 `.form-template-preview__actions`

## API Contracts

- 读取：`TemplateApi.getTemplatePool` / `TemplateApi.getTemplateVersion` 返回 `jimuSchemaJson`。
- 保存：`TemplateApi.saveTemplateJimuSchema(templateId, versionNo, jimuSchema)`。
- 候选人：`getSimpleUserList()` / `getSimpleRoleList()`。

## BDD Scenarios

- 见 `execution-log.md`。

## Verification

待补充。
