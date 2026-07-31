# Frontend Feature Evidence

## Feature Goal

将 eDHR 填写辅助模式卡片网格中所有卡片内对应文字字号提高为原来的 2 倍。

## Acceptance

- 卡片网格内字段标签、输入/占位、选择项、按钮、单位和校验提示文字均按原压缩字号提升为 2 倍。
- 页面数据来源、字段数量、输入控件、权限、保存/提交和错误处理链路不变。

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

- BDD: 卡片文字 2 倍字号 -> Given 用户在 eDHR 填写辅助模式查看黄色卡片网格，When 页面渲染卡片内标签、输入文字、占位文字和单位文字，Then 这些对应文字的 CSS 字号应为原样式的 2 倍且卡片数据、输入控件和交互不变。

## RED Command

- RED: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> FAIL，源码仍保留卡片网格半字号规则。

## GREEN Command

- GREEN: `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- 本次改动为样式字号调整；相邻结构契约 `edhr-assist-fill-mode-static` 与 `edhr-fill-workspace-hide-side-panels-static` 已通过，未改变按钮、输入框、错误提示、加载态、权限判断和空态逻辑。

## E2E Or Component Verification Path

- `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js` -> PASS。
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS。
- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Blockers And Follow-Up Skills

- 当前无 blocker；收尾前按项目规则执行 cleanup 与经验沉淀。
