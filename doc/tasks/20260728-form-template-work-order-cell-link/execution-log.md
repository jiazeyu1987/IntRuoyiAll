# Execution Log

## Intent

用户要求在表单中心模板预览红框位置增加“链接”按钮，功能与批记录表单下“链接”一致，可以把生产工单列值链接到表单。

## Baseline

- `713ef9fb`：baseline existing dirty workspace。
- `06fabade`：baseline residual dirty workspace。
- `d2b09536`：baseline form template fill config edits。
- `288b3f83`：baseline additional residual workspace edits。
- `47e1d2a0`：baseline residual task docs。
- Baseline 后当前工作区可用于本任务实现；当前分支 `int_main` 已 ahead origin。

## BDD

- BDD: 表单模板链接入口 -> Given 已选中可交互表单模板 When 用户点击预览工具栏“链接” Then 进入批记录单元格链接工作台并携带 `templateId + versionNo`。
- BDD: 生产工单字段链接到模板单元格 -> Given 工作台选择生产工单字段和模板目标单元格 When 保存链接规则 Then 规则以 `FORM_TEMPLATE_VERSION` 作用域保存并可重新加载。
- BDD: 动态表单实例预填 -> Given 模板版本存在启用的生产工单字段链接规则 When MES 创建该模板的表单中心实例 Then 对应单元格写入生产工单字段值。
- BDD: 缺失正式前置 fail fast -> Given 模板版本不存在、模板无可链接单元格或生产工单字段不存在 When 加载/保存/预填 Then 返回明确错误且不返回默认成功。

## Evidence

- GREEN: experience-preflight -> PASS，已读取 `docs/experience-index.md` 并摘录表单模板、单元格链接、E2E 与 Git/PowerShell 适用门禁。
