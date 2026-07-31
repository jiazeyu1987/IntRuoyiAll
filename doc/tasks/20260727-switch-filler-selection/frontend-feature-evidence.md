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

## Acceptance

- 弹窗标题保留 `选择当前工序填写人`。
- 弹窗标题右侧不再显示 `批处理表单 + 表单槽位`。
- 候选人行不再显示 `批处理表单` 或 `工艺路线表单槽位` 来源标签。
- 候选人行不再显示 `可填写` 状态标签。
- 候选人行继续显示候选填写人姓名和对应表单名称。

## Verification Plan

- RED: `node tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js`
- GREEN: `node tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js`
- REGRESSION: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js --format stylish`

## Verification Result

- RED: `node tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js` -> FAIL，标题右侧红框说明仍存在。
- GREEN: `node tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js` -> PASS。
- REGRESSION: `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js --format stylish` -> PASS。

## Checks

- Responsive: 未改布局容器和按钮结构，仅删除冗余文本节点。
- Accessibility: 保留弹窗主标题和候选按钮文本。
- Loading/Empty/Error: 未改加载、空态和错误提示分支。
- Permission: 未改候选项可选规则或权限判断。
- Blockers: 未执行真实页面 E2E；本次范围为截图红框展示清理，定向静态契约已覆盖。
