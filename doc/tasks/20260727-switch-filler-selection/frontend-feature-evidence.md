# Frontend Feature Evidence

## Feature Goal

- 删除“切换填写人”弹窗截图红框内的冗余表单类型与状态标签。

## Non-Goals

- 不修改后端接口、权限、填写人候选数据来源或真实切换提交逻辑。
- 不调整弹窗标题、候选人姓名、表单名称和取消按钮。

## Entry Point

- 页面组件：`IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- 弹窗区域：`data-assist-switch-menu="filler"`

## BDD

- BDD: 删除切换填写人弹窗红框标签 -> Given 当前工序存在多个填写人候选项 When 用户打开“切换填写人”弹窗 Then 弹窗不展示标题右侧表单类型说明、候选行来源标签和 `可填写` 状态标签，并继续展示填写人姓名与表单名称。

## Verification Plan

- RED: `node tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js`
- GREEN: `node tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js`
- REGRESSION: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js --format stylish`
