# Frontend Feature Evidence

## Feature Goal

将表单中心模板“填写配置”弹窗调整为左右双栏：左侧主区域承载只读表单预览并扩大到截图黄框宽度，右侧蓝框侧栏承载字段配置、辅助行配置、填写人配置和操作按钮。

## Non-Goals

- 不修改表单中心模板 API、后端接口、路由或权限判断。
- 不引入批记录 `reportId`、MES 批记录保存接口或批记录路由依赖。
- 不新增 mock 数据、fallback、默认成功或静默吞异常。

## Acceptance

- 预览区按钮仍打开表单中心模板自身“填写配置”弹窗。
- 弹窗默认最大化，右上角保留最大化/恢复按钮。
- 表格预览放入 `batch-record-cell-rules-editor__main-panel` 左侧主区域。
- 字段类型、必填、说明、下拉选项、辅助行、辅助行填写人和保存按钮放入 `data-fill-config-panel="template-config-sidebar"` 右侧侧栏。
- 关闭、重新读取、保存填写配置按钮不再使用全宽 `<template #footer>`，而固定在右侧蓝框底部。

## Entry Points And Owned Files

- Route: `/mdm/form-center/template`
- Component: `IntRuoyiFronted/src/views/form-center/template/components/FormTemplateFillConfigDialog.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/form-template-fill-config-static.spec.js`
- Protected: API wrappers, backend routes/services, database schema, mock/seed data, batch-record report save path.

## API Contracts And Data States

继续使用表单中心模板自身 `templateId + versionNo + jimuSchemaJson` 数据上下文保存填写配置；本次只调整视觉布局和静态合同，不改变保存 payload、权限、只读草稿判断或错误展示。

## BDD Scenarios

BDD: 填写配置双栏布局 -> Given 用户打开最大化的“填写配置”弹窗；When 弹窗展示表格预览和配置内容；Then 表格预览应占据左侧黄框主区域，字段类型、必填、说明、辅助行和底部保存按钮等其他内容应集中在右侧蓝框侧栏。

BDD: Vue 模板结束标签完整 -> Given Vite HMR 编译 `FormTemplateFillConfigDialog.vue`；When 填写配置弹窗模板包含左侧 `main` 和右侧 `aside`；Then Vue SFC 模板必须能完成编译，不得出现 `Element is missing end tag` overlay。

## RED Evidence

RED: Vite HMR overlay（用户提供）-> FAIL, expected reason: `FormTemplateFillConfigDialog.vue:10:7` 报 `Element is missing end tag`，指向新增的 `<main class="batch-record-cell-rules-editor__main-panel">`。

RED ATTEMPT: `pnpm exec eslint --ext .vue src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` -> TIMEOUT, parser 阶段未在 124s 内返回；未把超时当通过。

## GREEN Evidence

GREEN: `node tests/e2e/form-template-fill-config-static.spec.js` -> PASS.

GREEN: Vite 同源 `@vue/compiler-sfc` parse + template compile for `FormTemplateFillConfigDialog.vue` -> PASS.

GREEN: `pnpm exec eslint --ext .vue src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` -> PASS.

GREEN: `pnpm ts:check` -> PASS.

## Verification

- 静态布局合同断言左侧主区域、右侧配置侧栏、侧栏滚动区、侧栏底部操作按钮和禁止全宽 footer。
- Vue SFC 编译合同通过 `@vitejs/plugin-vue` 使用的同源 `@vue/compiler-sfc` 解析并编译目标组件，防止结束标签错误回归。
- 相邻回归命令已通过：`batch-record-cell-rule-default-fullscreen-static`、`edhr-visual-fill-config-static`、`form-center-static`、`form-template-button-interaction-parity-static`、`form-template-independent-button-actions-static`。
- 真实页面截图验证未通过登录前置：`scripts/preflight/login-preflight.mjs` 等待 `/admin-api/system/auth/login` 响应超时，按真实 E2E 门禁记录 blocker，未用 API-only 冒充通过。

## State Checks

- Responsive: 组件保留 `@media (max-width: 1180px)` 单列堆叠，避免窄屏挤压。
- Accessibility: 操作按钮保留 Element Plus 原按钮语义；未新增隐藏交互。
- Loading/Error: 保留 `v-loading`、只读草稿提示和 `errorMessage` 错误展示。
- Permission/Readonly: 保留 `readonlyMode` 和 `canConfirmRules` 原判断。

## Blockers

- Real browser page verification is blocked by local login response timeout on `/admin-api/system/auth/login`; impact: cannot claim final screenshot parity from the running app in this turn.
- Cleanup, commit, and push are not completed because the shared `int_main` workspace has many unrelated concurrent dirty changes; no unrelated files were staged, reverted, or overwritten by this task.
