# Frontend Feature Evidence

## Feature Goal

将 eDHR 填写辅助模式卡片网格中所有卡片内对应文字字号提高为原来的 2 倍。

## Non-Goals

- 不改变接口、路由、权限、保存/提交逻辑或数据来源。
- 不新增 fallback、mock、默认成功状态或错误吞噬。
- 不重做卡片布局或改变卡片数量、排序、状态颜色。

## UI Entry Points

- eDHR 填写页：`/mes/pro/feedback/edhr-execution/form`
- 组件：`IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`

## API Contracts And Data States

- 本次只调整 CSS 呈现，不改变 API 请求、响应字段、运行态数据解析或表单保存 payload。

## BDD Scenarios

- Given 用户在 eDHR 填写辅助模式查看黄色卡片网格，When 页面渲染卡片内标签、输入文字、占位文字和单位文字，Then 这些对应文字的 CSS 字号应为原样式的 2 倍且卡片数据、输入控件和交互不变。

## RED Command

- `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js`

## GREEN Command

- pending

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- 本次改动为样式字号调整；需确认不改变按钮、输入框、错误提示、加载态、权限判断和空态逻辑。

## E2E Or Component Verification Path

- 优先运行现有辅助卡片密度静态契约：`node tests/e2e/edhr-fill-workspace-card-density-static.spec.js`

## Blockers And Follow-Up Skills

- pending
