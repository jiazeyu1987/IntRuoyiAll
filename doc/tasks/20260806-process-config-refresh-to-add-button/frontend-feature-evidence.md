# Frontend Feature Evidence

## Feature Goal

将生产组长工作台“工序配置”模块头部按钮从“刷新”改为“新增”。

## Non-Goals

- 不新增后端接口。
- 不改变当前按钮点击绑定的 `loadProcessConfigRows` 行为。
- 不改动其它模块的刷新按钮。

## Requirements And Acceptance

- A1：工序配置模块头部按钮显示“新增”。
- A2：按钮继续使用 `processConfigLoading` 和 `loadProcessConfigRows`。
- A3：静态合同可防止该按钮退回“刷新”。

## UI Entry Points

- 页面：生产组长工作台。
- 组件：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 静态合同：`IntRuoyiFronted/tests/e2e/production-leader-function-tabs-static.spec.js`。

## API Contracts And Data States

本次不改 API；按钮仍触发现有工序配置列表加载逻辑。

## BDD Scenarios

- BDD: 工序配置按钮文案 -> Given 生产组长进入“工序配置”模块；When 页面渲染模块头部操作按钮；Then 右上角按钮显示“新增”，并继续绑定原列表加载方法和 loading 状态。

## RED

待记录。

## GREEN

待记录。

## Responsive Accessibility Loading Empty Error Permission

- 文案调整不改变按钮布局、loading、空状态、错误提示或权限边界。

## E2E Or Component Verification Path

- 使用目标静态合同验证按钮文案与绑定关系。

## Blockers And Follow-Up Skills

- 暂无。
