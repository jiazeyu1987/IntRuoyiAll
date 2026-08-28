# Task: 表单模板“编辑”按钮保持模板页并对齐右侧规则编辑

## Goal

把表单模板右侧的“编辑”按钮保留在表单模板页内，进入当前模板自身的规则编辑工作区；右侧“可填写/不可填写、字段名称、字段类型”等编辑交互与批记录表单右侧编辑一致，避免跳到批记录表单模块或进入空白 jimu 编辑页。

## Milestones

1. 复现并确认旧实现把表单模板“编辑”误导到批记录表单模块或错误编辑页。
2. 补齐失败前的回归测试，固定“留在表单模板页 + 模板自身编辑工作区”的真实路径。
3. 修正前端按钮与路由逻辑，让模板“编辑”进入当前模板自身 `templateMode=edit` 工作区。
4. 运行真实 E2E，验证左侧模板内容存在、点击规则单元格后右侧编辑控件出现。
5. 沉淀经验门禁并完成任务收尾。

## Expected Verification

- 相关静态合同先失败后通过。
- 真实 Playwright E2E 证明模板“编辑”留在 `/mdm/form-center/template?mode=designer&templateMode=edit`，左侧显示当前模板内容，点击规则单元格后右侧出现与批记录表单一致的规则编辑控件。
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

## Cleanup Keep

- doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-current-workspace.png

## 适用经验门禁

- `docs/frontend-development.md#表单模板三按钮领域边界门禁`：当前模板页已有“打开/编辑/填写”三按钮边界，“编辑”必须保留在表单模板页内，只对齐批记录表单的右侧规则编辑交互，不得跳到批记录表单模块。
- `docs/e2e-rules.md#官方登录前置与-admin-only-全量验证门禁`：真实 E2E 需使用本地真实登录态和真实页面路径，不得用 API-only 冒充进入效果。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：是
- 是否存在临时补丁或绕过：否
