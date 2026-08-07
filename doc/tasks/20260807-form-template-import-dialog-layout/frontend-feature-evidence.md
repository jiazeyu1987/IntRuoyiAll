# Frontend Feature Evidence

## Feature Goal

优化导入表单模板弹窗，使桌面与窄屏下的字段、上传区域、文件列表和底部操作保持清晰对齐且不横向溢出。

## Non-goals

- 不修改导入 API、请求字段、升版审批逻辑、模板候选加载或错误处理。
- 不改造 FormCenter 其它弹窗或全局 Dialog 组件。

## Requirements And Acceptance IDs

- AC-01：字段标签位于输入控件上方，表单信息按单列纵向层级排列。
- AC-02：上传区域占满弹窗内容宽度，不受横向 label 宽度计算影响。
- AC-03：已选长文件名在弹窗内容区内截断或换行，不越过边界。
- AC-04：弹窗具备视口宽度约束，窄屏不产生横向滚动。
- AC-05：现有正式导入交互和 API 合同保持不变。

## UI Entry Points And Owned Files

- Entry：FormCenter 表单模板列表的“导入”操作。
- Component：`IntRuoyiFronted/src/views/form-center/template/components/TemplateImportDialog.vue`。
- Test：`IntRuoyiFronted/tests/e2e/form-template-import-dialog-layout-static.spec.js`。

## API Contracts And Data States

- 模板候选：`TemplateApi.getTemplatePool`，保留 loading 与明确错误提示。
- 正式导入：`TemplateApi.importTemplateDoc(FormData)`，保留 CREATE/UPGRADE 成功提示和失败抛出。
- 空文件：保留现有前端校验；不新增 mock、默认成功或静默错误。

## BDD Scenarios

- Given 桌面端打开弹窗并选择长文件名，When 弹窗完成渲染，Then 字段单列对齐、上传区满宽、文件名不溢出、底部操作对齐。
- Given 窄屏打开弹窗，When 可用宽度小于默认弹窗宽度，Then 弹窗随视口收缩且内容保持可操作。
- Given 用户提交有效表单，When 点击导入，Then 仍走 `importTemplateDoc` 正式链路。

## TDD Evidence

- RED：待执行。
- GREEN：待执行。

## Responsive And Accessibility Checks

- Responsive：待检查桌面与窄屏宽度。
- Accessibility：保留 Element Plus 表单 label、校验、键盘和上传控件语义；待验证。
- Loading：保留模板候选 loading 与提交按钮 loading。
- Empty：保留未选择文件的明确校验。
- Error：保留模板池加载和导入失败的明确错误提示及异常抛出。
- Permission：本次不变更路由或权限。

## E2E Or Component Verification Path

- 专用静态合同锁定组件级布局契约。
- 相邻 `form-center-static.spec.js` 锁定正式导入和版本号契约。
- Playwright 在真实 FormCenter 页面检查桌面与窄屏布局。

## Blockers And Follow-up Skills

- 当前无功能前置缺失；Git 基线提交按项目规则先处理。
