# Frontend Design

## Purpose and Scope

本设计覆盖 `表单模板` 页面预览区红框内 `打开 / 编辑 / 填写` 三个按钮。目标是让这三个按钮与 `批记录表单` 页面同名按钮使用同一用户路径、同一 `reportId` 参数和同一批记录接口链路。范围不包含 `下载 / 停用 / 作废 / 签名 / 规则 / 链接 / 重命名 / 删除` 等其他按钮。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/form-center/template/index.vue:150` 至 `176`：表单模板按钮显示在预览区，当前 `打开 / 编辑 / 填写` 分别绑定本页函数。
- `IntRuoyiFronted/src/views/form-center/template/index.vue:899` 至 `912`：`打开` 进入 `TemplateViewDialog`，`填写` 打开本页模拟填写弹窗。
- `IntRuoyiFronted/src/views/form-center/template/index.vue:1039` 至 `1075`：`编辑` 保存时调用表单中心 `jimu-schema` 链路。
- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue:204` 至 `206`：批记录表单 `打开 / 编辑 / 填写` 使用 `reportId` 进入设计器和模拟填写。
- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue:1976` 至 `1998`：批记录打开/编辑路由和填写路由的目标参数已明确。

## Pages and Routes

- `表单模板` 页面保留现有列表、预览、导入、下载、停用、作废功能。
- `打开` 必须跳转到批记录表单同源设计器预览路径：`/mes/pro/batch-record-form-list?mode=designer&reportId=<reportId>&reportMode=preview`。
- `编辑` 必须跳转到批记录表单同源设计器编辑路径：`/mes/pro/batch-record-form-list?mode=designer&reportId=<reportId>&reportMode=edit`。
- `填写` 必须跳转到 `/mes/pro/feedback/edhr-batch-execution/template-simulate`，携带 `reportId`、`reportName`、`batchRecordName`、`returnTo`、`returnLabel=返回表单模板`。
- 允许复用 `batchrecordformlist/index.vue` 中的 `openDesigner` 与 `openSimulate` 行为，但不要复制分叉逻辑；建议抽出共享 helper 或在表单模板页实现同契约路由函数。

## Components

- `FormCenterTemplate` 需要新增对 `selectedTemplate.batchRecordReport` 或等价字段的读取。
- `TemplateViewDialog` 不再作为红框 `打开` 按钮入口；若仍有其他入口依赖，保留组件但不作为对齐按钮路径。
- 本页 `fillDialogVisible` 相关模拟填写弹窗不再作为红框 `填写` 按钮入口；若后续产品确认完全废弃，应另起清理任务删除。
- `EdhrExecutionTemplateEditableForm` 在本页的模拟填写用途不再参与三按钮对齐验收。

## State and Data Flow

- 模板列表加载后，每行必须获得稳定批记录绑定信息，最小字段建议为：
  - `batchRecordReportId`
  - `batchRecordReportName`
  - `batchRecordName`
  - `batchRecordVersionNo`
  - `batchRecordFormSlotType`
  - `batchRecordBindingStatus`
- 前端点击三按钮时只接受后端返回的 `batchRecordReportId`；不得用 `templateName`、`versionNo`、`sourceFileName` 再请求批记录分页做猜测匹配。
- `selectedTemplate` 切换时只更新当前行绑定状态，不触发隐式迁移、隐式补绑定或旧弹窗回退。
- 跳转前校验 `batchRecordReportId`，缺失则显示明确错误：`当前模板未绑定批记录表单，无法按批记录表单方式打开。`

## Error States

- 缺少 `batchRecordReportId`：按钮可保持可见但点击 fail fast；也可禁用并用 tooltip 展示同一阻塞原因。
- 批记录设计器路径接口失败：沿用批记录表单设计器 wrapper 的错误显示，不在表单模板页吞异常。
- 模拟填写页缺少必要参数：跳转前阻塞，不传空字符串。
- 权限不足：沿用后端 403 和现有权限指令，不做前端默认成功。

## Accessibility and Responsive Behavior

- 保留按钮文字 `打开 / 编辑 / 填写`，不要仅用图标替代。
- 禁用态必须保留可读说明，优先使用 `el-tooltip` 或页面错误提示。
- 跳转后 `returnTo` 应包含当前表单模板查询条件，返回时恢复用户上下文。

## Open Questions

- 表单中心模板与批记录报表的正式关系是一对一，还是一个模板版本可能绑定多个产品/版本/槽位报表？
- 未绑定批记录报表的历史模板是否需要批量迁移，还是保持不可打开并提示管理员处理？
- 表单模板页是否继续保留原本“规则编辑/模拟填写”作为非红框入口，还是在后续任务删除？

## Design Blockers

- 当前 `FormTemplateListItemVO` 没有批记录 `reportId` 字段；在后端提供稳定映射前，前端不得对齐三按钮到批记录接口。
- 若后端只能按模板名或文件名模糊匹配批记录报表，本设计不允许实施；必须先补正式数据关系。

