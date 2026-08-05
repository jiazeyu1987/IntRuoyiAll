# Feature

## Feature Goal

修复班组长工作台报工列表首屏读取多维筛选 `state` 的渲染崩溃。

## Non-Goals

- 不修改后端 API。
- 不改变筛选字段或查询语义。
- 不引入可选链、空对象或静默降级掩盖初始化问题。

## Acceptance

- AC1: 页面首屏渲染时 `submissionMultiFilter` 已初始化。
- AC2: 模板可读取正式 `state`，多维筛选事件仍绑定正式 hook 方法。
- AC3: 相邻人员管理、报工和班组长静态合同通过。

## BDD

- BDD: 班组长工作台首屏可渲染多维筛选 -> Given 用户进入班组长工作台, When 报工列表首屏渲染, Then 页面不会因读取多维筛选 `state` 崩溃。

## RED

- `node tests/e2e/team-leader-multifilter-render-state-static.spec.js` -> FAIL，现有模板仍使用 `submissionMultiFilter.state/updateState/removeCondition`。

## GREEN

- 待执行。

## Verification

- 待执行。

## Blockers

- 暂无。
