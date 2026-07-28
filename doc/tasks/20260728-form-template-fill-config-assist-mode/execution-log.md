# Execution Log

## User Intent

用户反馈“表单模板”页签下的“填写配置”逻辑应与“批记录表单”里的填写配置逻辑一致，但当前无法切换到辅助表单映射模式。

## BDD Scenarios

BDD: 表单模板填写配置可切换辅助表单映射模式 -> Given 管理员在表单模板页签打开一个可编辑模板的填写配置 When 点击填写配置中的模式切换入口 Then 弹窗应进入辅助表单映射模式并显示辅助表格控制区。

BDD: 表单模板辅助映射仍保存模板自身配置 -> Given 表单模板填写配置处于辅助表单映射模式 When 用户调整辅助表格、填写人和原表单元格映射后保存 Then 保存请求应写回模板自身的 `fillAssignments` 和 `assistRows`，不得依赖批记录表单链路。

BDD: 表单模板与批记录填写配置保持交互一致 -> Given 批记录填写配置支持原表配置与辅助表单映射两种模式 When 表单模板打开填写配置 Then 应展示同等模式入口、辅助表格状态和右侧控制栏结构。

## TDD Evidence

- Gate: 已读取 `docs/experience-index.md` 并命中 `docs/frontend-development.md#前端静态契约隔离门禁`、`docs/frontend-development.md#表单模板三按钮领域边界门禁`。
- RED: `node tests\e2e\form-template-fill-config-assist-mode-static.spec.js` -> FAIL, expected reason: `FormTemplateFillConfigDialog.vue` 缺少 `activeConfigMode` 和辅助表单映射模式入口。
- GREEN: `node tests\e2e\form-template-fill-config-assist-mode-static.spec.js` -> PASS。
