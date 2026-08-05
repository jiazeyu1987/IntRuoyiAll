# Frontend Feature Evidence

## Feature Goal

生产组长页面按功能模块拆分为独立 Tab，至少覆盖人员管理、报工管理、损耗管理。

## Non-Goals

- 不修改后端 API 契约。
- 不新增 mock 数据。
- 不改变已有权限、路由或模块业务逻辑。

## Requirements And Acceptance

- AC1: 页面存在功能模块 Tab。
- AC2: 人员管理、报工管理、损耗管理分别是独立 Tab。
- AC3: 切换 Tab 时仅展示对应模块内容。

## UI Entry Points

- 待定位。

## API Contracts And Data States

- 待定位；预期保持现有契约不变。

## BDD Scenarios

- BDD: 生产组长模块按 Tab 展示 -> Given 用户进入生产组长页面, When 页面加载完成, Then 人员管理、报工管理、损耗管理等功能模块以独立 Tab 展示。
- BDD: Tab 切换不改变模块契约 -> Given 生产组长页面已有各功能模块, When 用户切换不同 Tab, Then 当前 Tab 只展示对应模块内容，现有数据请求、事件和组件职责保持不变。

## RED

- 待执行。

## GREEN

- 待执行。

## Responsive Accessibility Loading Empty Error Permission

- 待验证。

## E2E Or Component Verification Path

- 任务专用静态合同；如运行态前置齐全，再补真实页面验证。

## Blockers And Follow-Up Skills

- 暂无。
