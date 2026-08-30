# Task: 表单模板“编辑”按钮保持模板页并对齐右侧规则编辑

## Goal

把表单模板右侧的“编辑”按钮保留在表单模板页内，并在当前表单模板路由中打开 Jimu 编辑器；Jimu 编辑器加载当前模板自己的内容，编辑入口不得跳到批记录表单模块，也不得退回旧的自制规则编辑工作区。

补齐正式保存链路：表单模板 Jimu 编辑器内点击保存后，必须把 Jimu 最新画布同步回表单模板版本正式 `jimuSchemaJson/sheetLayoutJson`，且只允许写入草稿版本。

## Milestones

1. 复现并确认旧实现把表单模板“编辑”误导到批记录表单模块或错误编辑页。
2. 补齐失败前的回归测试，固定“留在表单模板页 + 模板自身编辑工作区”的真实路径。
3. 修正前端按钮与路由逻辑，让模板“编辑”进入当前模板自身 `reportMode=edit` 的 Jimu 工作区。
4. 运行真实 E2E，验证 URL 留在表单模板页，iframe 加载当前模板的 Jimu 编辑器且不是空白页。
5. 补齐 Jimu 原生保存回写表单模板版本的后端链路，只允许草稿版本落库。
6. 修正已发布版本点击编辑时的草稿切换链路，确保用户编辑的是可写草稿版本。
7. 运行静态、后端和真实 E2E 验证，确认保存后原表单模板内容会更新。
8. 沉淀经验门禁并完成任务收尾。

## Expected Verification

- 相关静态合同先失败后通过。
- 真实 Playwright E2E 证明模板“编辑”留在 `/mdm/form-center/template?mode=designer&reportMode=edit`，并携带当前模板虚拟 `reportId`，Jimu iframe 中显示当前模板内容。
- Jimu 编辑器保存请求走原生 `/jmreport/save` 后，模板版本正式 `jimuSchemaJson/sheetLayoutJson` 同步为最新画布 JSON。
- 非草稿版本的 Jimu 保存请求必须被明确拒绝，不允许写入正式模板版本。
- 前端类型检查、eslint 和 diff 检查通过。

## Cleanup Candidates

- E:\IntRuoyi\IntRuoyiFronted\batch-edit-check.png
- E:\IntRuoyi\IntRuoyiFronted\blank-check.png
- E:\IntRuoyi\IntRuoyiFronted\click-flow-15s.png
- E:\IntRuoyi\IntRuoyiFronted\encoded-template-edit.png
- E:\IntRuoyi\IntRuoyiFronted\login-inspect.png
- E:\IntRuoyi\IntRuoyiFronted\raw-template-edit.png
- E:\IntRuoyi\IntRuoyiFronted\template-edit-15s.png
- E:\IntRuoyi\IntRuoyiFronted\template-edit-2s.png
- E:\IntRuoyi\IntRuoyiFronted\template-route-direct-login.png
- E:\IntRuoyi\IntRuoyiFronted\template-route-direct.png
- doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-current-workspace.png
- doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-jimu-diagnostic.png
- doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-no-iframe-diagnostic.png
- output/tmp-jimureport-jar-inspect

## Cleanup Keep

- doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-jimu-editor.png
- doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-jimu-editor-iframe.png

## 适用经验门禁

- `docs/frontend-development.md#表单模板三按钮领域边界门禁`：当前模板页已有“打开/编辑/填写”三按钮边界，“编辑”必须保留在表单模板页内，只对齐批记录表单的右侧规则编辑交互，不得跳到批记录表单模块。
- `docs/backend-development.md#表单模板-jimu-保存回写正式版本门禁`：Jimu 原生保存必须在后端校验 `FORMTPL:*` 租户和草稿版本，并把最新画布同步回模板版本正式 `jimuSchemaJson.sheetLayoutJson`，不得只保存 Jimu 报表表。
- `docs/e2e-rules.md#官方登录前置与-admin-only-全量验证门禁`：真实 E2E 需使用本地真实登录态和真实页面路径，不得用 API-only 冒充进入效果。

## Current Status

completed - 表单模板页内 Jimu 编辑、草稿写保护和原生保存回写正式 `jimuSchemaJson.sheetLayoutJson` 已实现；静态合同、后端合同、类型检查、真实进入 E2E 和真实保存回写 E2E 均已通过。本任务临时产物已清理，经验门禁已沉淀。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：是
- 是否存在临时补丁或绕过：否
