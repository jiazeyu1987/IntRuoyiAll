# Frontend Feature Evidence

## Feature Goal

Reduce first-screen load work for the MES 工艺流程 list/page by moving hidden route dialogs and route flow designer dependencies out of the initial list bundle.

## Non-goals

- Do not change backend API contracts.
- Do not change menu permissions, tenant data, or route business behavior.
- Do not hide or downgrade API errors.
- Do not redesign the page.

## Requirements And Acceptance

- A1: 工艺流程列表首屏 should render without synchronously loading hidden dialog shells or the flow graph designer.
- A2: Route create/detail/import actions should still load their dialogs on demand.
- A3: Route edit flow tab should still load the designer and graph data when the user opens it.

## UI Entry Points

- Route list: `/mes/pro/route`
- Route edit: `/mes/pro/route/edit/:id`
- Components under review:
  - `IntRuoyiFronted/src/views/mes/pro/route/index.vue`
  - `IntRuoyiFronted/src/views/mes/pro/route/RouteForm.vue`
  - `IntRuoyiFronted/src/views/mes/pro/route/RouteFormContent.vue`
  - `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`

## API Contracts And Data States

- Existing route APIs remain unchanged.
- Loading, empty, and error states remain owned by existing components.

## BDD Scenarios

- BDD: 工艺流程列表首屏按需加载重型弹窗 -> Given 用户首次进入工艺流程列表页面, When 页面渲染首屏列表, Then 首屏入口不应同步导入新增/详情弹窗、Excel 导入弹窗或流转关系图设计器的大组件，只有用户触发对应操作时才加载。
- BDD: 工艺流程编辑页保持流转关系图可用 -> Given 用户从列表进入工艺流程编辑页并打开流转关系图, When 编辑页加载路线数据和图设计器, Then 原有图加载、自动布局、保存和返回行为保持可用且错误仍显式暴露。

## Verification Plan

- RED: add a static contract that fails while `route/index.vue` and `RouteForm.vue` synchronously import hidden heavy route components.
- GREEN: make hidden route components async-loaded and rerun the contract.
- REGRESSION: run the narrow frontend static contract and, if feasible, a targeted TypeScript check for the touched files.

## Verification Evidence

- RED: `node tests/e2e/mes-route-first-screen-defer-static.spec.js` -> FAIL on static `RouteForm` import.
- GREEN: `node tests/e2e/mes-route-first-screen-defer-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- REGRESSION BLOCKER: `pnpm build:local` timed out after 604s; no build success claimed.

## Blockers

- Final closeout commit/push is blocked by unrelated concurrent dirty changes and ahead commits in the shared workspace.
