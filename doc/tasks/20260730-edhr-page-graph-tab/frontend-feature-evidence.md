# Frontend Feature Evidence

## Feature Goal

新增 eDHR “批记录页面关系图”页签和页面，用节点/连线展示批记录相关页面之间的操作关系。

## Non-Goals

- 不新增后端接口。
- 不修改工艺路线流转关系图。
- 不新增排产、工序池或审核副本业务写入能力。
- 不用 `formBindings` 或“工序开始”推断正式批记录表单。

## Requirements And Acceptance

- 新增共享页签“批记录页面关系图”。
- 新增前端路由和页面组件。
- 页面节点代表页面，不代表工序。
- 已有正式路由节点可点击；未有正式路由节点明确显示待接入。

## UI Entry Points, Routes, Components, Owned Files

- Entry: `EdhrBatchRecordTabs.vue`
- Route: `/mes/pro/feedback/edhr-batch-page-graph`
- Component: `BatchPageGraphPage.vue`
- Test: `tests/e2e/edhr-batch-page-graph-tab-static.spec.js`

## API Contracts And Data States

- 第一版不新增 API，使用静态页面关系定义。
- 后续可替换为批记录页面关系图聚合接口，当前页面不吞掉后端错误，因为没有新增后端请求。

## BDD Scenarios

- See `execution-log.md`.

## RED / GREEN Evidence

- Pending.

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Pending.

## Blockers And Follow-Up Skills

- 真实节点状态、数量徽标和权限投影需要后端聚合接口后再扩展。
