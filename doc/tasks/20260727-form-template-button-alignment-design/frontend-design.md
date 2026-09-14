# Frontend Design

## Purpose and Scope

本设计覆盖 `表单模板` 页面预览区红框内 `打开 / 编辑 / 填写` 三个按钮。三个按钮始终操作当前 FormCenter 表单模板，不依赖批记录表单、`reportId` 或 MES 路由：

- `打开`：查看当前模板。
- `编辑`：进入当前模板规则编辑工作区。
- `填写`：进入当前模板模拟填写工作区。

范围不包含导入、发布、停用、作废、下载和签名等其他操作，也不调整批记录表单页面自身行为。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/form-center/template/index.vue` 已包含 `TemplateViewDialog`、`rulesDialogVisible` 和 `fillDialogVisible` 三套当前模板工作区。
- 当前模板由 `selectedTemplate` 提供 `templateId`、`versionNo`、`jimuSchemaJson`、识别字段和布局数据。
- 纠偏前的实现通过 `batchRecordBindingStatus + batchRecordReportId` 阻断三个按钮，并跳转 MES 批记录页面，这是“交互对齐”被误解为“数据绑定”的根因。
- `IntRuoyiFronted/tests/e2e/form-template-independent-button-actions-static.spec.js` 已锁定三个按钮的独立行为。

## Pages and Routes

- 页面入口保持 `/mdm/form-center/template`。
- `打开`调用 `templateViewDialogRef.open(selectedTemplate)`，不改变当前路由。
- `编辑`调用 `openSelectedTemplateAction('edit')`，仅在当前路由写入模板操作查询参数，并打开 `.form-template-rules-dialog`。
- `填写`重置当前模板模拟值后打开 `.form-template-fill-dialog`，不跳转其他业务模块。
- 禁止三个按钮跳转 `/mes/pro/batch-record-form-list` 或 `/mes/pro/feedback/edhr-batch-execution/template-simulate`。

## Components

- `TemplateViewDialog`：展示当前模板元数据和只读表单效果。
- `.form-template-rules-dialog`：读取当前模板规则和布局，提供模板规则编辑。
- `.form-template-fill-dialog`：使用 `EdhrExecutionTemplateEditableForm` 与 `EdhrExecutionReadonlyForm` 完成当前模板模拟填写和同步预览。
- 批记录共享的规则渲染工具可以作为纯 UI/规则工具复用，但不得因此建立 FormCenter 模板与批记录报表的数据关系。

## State and Data Flow

1. 模板池加载完成后，`selectedTemplateKey` 选中一条当前模板。
2. `selectedTemplate` 作为三个按钮的唯一业务上下文。
3. `打开`把当前模板对象传给查看弹窗。
4. `编辑`使用当前模板的 `templateId + versionNo` 装载和保存 `jimuSchemaJson`。
5. `填写`基于当前模板布局重置 `templateFillValues`，只在前端模拟，不创建批记录执行数据。
6. 前端类型 `FormTemplateListItemVO` 不包含 `batchRecordReportId`、`batchRecordBindingStatus` 等批记录绑定字段。

## Error States

- 未选择模板时不执行操作；正常页面中按钮仅在存在 `selectedTemplate` 时展示。
- 当前模板布局或规则解析失败时，使用现有模板工作区的明确错误状态，不切换到批记录表单。
- 接口失败继续由现有请求错误链路暴露，不吞异常、不返回默认成功。
- 页面不得再显示“当前模板未绑定批记录表单，无法执行该操作”。
- 不保留“有绑定走 MES、无绑定走 FormCenter”的条件 fallback。

## Accessibility and Responsive Behavior

- 保留可读按钮文字 `打开 / 编辑 / 填写`，不改为仅图标入口。
- 三个工作区继续使用现有 Element Plus Dialog 键盘关闭、焦点和响应式宽度能力。
- 当前任务不调整页面布局；桌面和移动端继续沿用现有模板页适配规则。

## Open Questions

- 当前行为没有待确认产品问题。
- 若未来确需让某类 FormCenter 模板生成批记录表单，应作为独立跨域集成需求设计，不能复用本任务已删除的隐式绑定字段。

## Design Blockers

- 当前实现与验证无 blocker。
- 真实 E2E 需要本机前端、后端、登录身份和 Chrome/Edge 可用；本次已满足。
