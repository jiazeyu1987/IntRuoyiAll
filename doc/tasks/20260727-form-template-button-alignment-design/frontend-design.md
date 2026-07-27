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

- `表单模板` 页面入口为 `/mdm/form-center/template`，保留现有列表、预览、导入、下载、停用、作废功能。
- `打开` 必须跳转到批记录表单同源设计器预览路径：`/mes/pro/batch-record-form-list?mode=designer&reportId=<reportId>&reportMode=preview`。
- `编辑` 必须跳转到批记录表单同源设计器编辑路径：`/mes/pro/batch-record-form-list?mode=designer&reportId=<reportId>&reportMode=edit`。
- `填写` 必须跳转到 `/mes/pro/feedback/edhr-batch-execution/template-simulate`，携带 `reportId`、`reportName`、`batchRecordName`、`returnTo`、`returnLabel=返回表单模板`。
- 表单模板页实现与 `batchrecordformlist/index.vue` 同契约的路由函数；不得再调用本页 `TemplateViewDialog`、规则编辑弹窗或本页模拟填写弹窗作为红框三按钮路径。

## Components

- `FormCenterTemplate` 读取 `selectedTemplate` 上的显式批记录绑定摘要字段。
- `TemplateViewDialog` 不再作为红框 `打开` 按钮入口；若仍有其他入口依赖，保留组件但不作为对齐按钮路径。
- 本页 `fillDialogVisible` 相关模拟填写弹窗不再作为红框 `填写` 按钮入口；若后续产品确认完全废弃，应另起清理任务删除。
- `EdhrExecutionTemplateEditableForm` 在本页的模拟填写用途不再参与三按钮对齐验收。

## State and Data Flow

- 模板列表加载后，每行必须获得稳定批记录绑定信息，字段已明确为：
  - `batchRecordReportId`
  - `batchRecordReportName`
  - `batchRecordName`
  - `batchRecordVersionNo`
  - `batchRecordFormSlotType`
  - `batchRecordBindingStatus`
  - `batchRecordBindingError`
- 前端点击三按钮时只接受后端返回的 `batchRecordReportId`；不得用 `templateName`、`versionNo`、`sourceFileName` 再请求批记录分页做猜测匹配。
- `selectedTemplate` 切换时只更新当前行绑定状态，不触发隐式迁移、隐式补绑定或旧弹窗回退。
- 跳转前必须同时校验 `batchRecordBindingStatus === 'BOUND'` 且 `batchRecordReportId` 非空；否则显示明确错误：`当前模板未绑定批记录表单，无法执行该操作`。

## Error States

- `batchRecordBindingStatus` 非 `BOUND` 或缺少 `batchRecordReportId`：按钮可保持可见但点击 fail fast；也可禁用并用 tooltip 展示同一阻塞原因。
- 批记录设计器路径接口失败：沿用批记录表单设计器 wrapper 的错误显示，不在表单模板页吞异常。
- 模拟填写页缺少必要参数：跳转前阻塞，不传空字符串。
- 权限不足：沿用后端 403 和现有权限指令，不做前端默认成功。

## Accessibility and Responsive Behavior

- 保留按钮文字 `打开 / 编辑 / 填写`，不要仅用图标替代。
- 禁用态必须保留可读说明，优先使用 `el-tooltip` 或页面错误提示。
- 跳转后 `returnTo` 应包含当前表单模板查询条件，返回时恢复用户上下文。

## Decisions

- 红框三按钮只按批记录表单行为执行，不保留旧弹窗作为降级路径。
- 历史未绑定模板保持 fail fast，不自动按名称、文件名或版本号匹配批记录报表。
- 本次只切换红框三按钮行为；本页旧预览、规则编辑和模拟填写组件是否清理，另起任务处理。

## Verification Gates

- 静态合同必须断言三按钮使用 `batchRecordReportId`、`reportMode=preview/edit` 和模板模拟填写页。
- 静态合同必须断言三按钮同时要求 `batchRecordBindingStatus === 'BOUND'` 和 `batchRecordReportId` 非空。
- 真实 E2E 必须在运行态存在已绑定模板数据后，通过 `/mdm/form-center/template` 页面逐个点击 `打开 / 编辑 / 填写` 验证。
