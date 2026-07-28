# Frontend Feature Evidence

## Feature Goal

新增表单中心模板预览区“填写配置”入口，复用批记录填写配置的单元格规则和辅助行配置体验，但保存到模板自身 `jimuSchemaJson`。

## Non-Goals

- 不新增后端接口。
- 不把表单中心模板绑定到批记录 `reportId`。
- 不扩展表单中心运行态分派/审批人执行语义。

## UI Entry Points

- `/mdm/form-center/template`
- 右侧 `.form-template-preview__actions`，按钮顺序为“填写” -> “填写配置” -> “下载”。

## Owned Files

- `IntRuoyiFronted/src/views/form-center/template/index.vue`
- `IntRuoyiFronted/src/views/form-center/template/components/FormTemplateFillConfigDialog.vue`
- `IntRuoyiFronted/tests/e2e/form-template-fill-config-static.spec.js`

## API Contracts

- 读取：`TemplateApi.getTemplatePool` / `TemplateApi.getTemplateVersion` 返回 `jimuSchemaJson`。
- 保存：`TemplateApi.saveTemplateJimuSchema(templateId, versionNo, jimuSchema)`。
- 候选人：`getSimpleUserList()` / `getSimpleRoleList()`。
- 禁止：表单中心填写配置不调用批记录 `cell-rules`、`save-by-report`、批记录路由或批记录 `reportId`。

## Data States

- `DRAFT`：允许保存填写配置。
- 非 `DRAFT`：允许打开只读查看，提示“只有草稿版本可以保存填写配置。”。
- 作废或审批锁定：不显示“填写配置”按钮，与现有交互按钮状态一致。

## Verification

- PASS: `node tests/e2e/form-template-fill-config-static.spec.js`
- PASS: `node tests/e2e/form-template-button-interaction-parity-static.spec.js`
- PASS: `node tests/e2e/form-template-independent-button-actions-static.spec.js`
- PASS: `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- PASS: `pnpm ts:check`
- BLOCKED/UNRELATED: `node tests/e2e/form-center-static.spec.js` fails on existing route `activeMenu: '/mdm/form-center/policy'` expectation.
- BLOCKED: Real E2E not run because local backend `127.0.0.1:48081` is not listening.

## Acceptance

- 表单中心模板预览区显示“填写配置”，位置在“填写”和“下载”之间。
- “填写配置”按钮使用 `form:template:update` 权限，且跟随非作废/非审批锁定动作状态显示。
- 弹窗提供单元格填写规则、辅助行配置和辅助行填写人配置。
- 保存仅写入模板自身 `jimuSchemaJson`，并保留 `sheetLayoutJson`、`cellRules`、`signatureCellMarkers`、`assistRows`、`fillAssignments` 和未知字段。
- 非草稿模板只读查看并提示“只有草稿版本可以保存填写配置。”。

## BDD:

- BDD: 模板自身填写配置 -> Given 用户选中非作废且无审批锁定模板；When 点击“填写配置”；Then 打开批记录式配置弹窗并使用模板自身 `jimuSchemaJson`。
- BDD: 草稿保存约束 -> Given 模板状态为 `DRAFT`；When 保存填写配置；Then 通过 `saveTemplateJimuSchema` 保存合并后的模板 schema，且不调用 MES 批记录保存接口。
- BDD: 非草稿只读约束 -> Given 模板状态不是 `DRAFT`；When 打开“填写配置”；Then 弹窗只读并禁用保存。

## RED:

- RED: `node tests/e2e/form-template-fill-config-static.spec.js` -> FAIL, expected reason: 缺少 `FormTemplateFillConfigDialog.vue` 和预览工具栏入口。

## GREEN:

- GREEN: `node tests/e2e/form-template-fill-config-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Blockers

- `node tests/e2e/form-center-static.spec.js` 当前失败在相邻路由 `activeMenu: '/mdm/form-center/policy'` 断言，不属于本任务新增按钮/弹窗/模板保存链路。
- 真实 E2E 被本地后端 `127.0.0.1:48081` 未监听阻塞；前端 `127.0.0.1:8081` 可达，未使用 API-only 替代。
