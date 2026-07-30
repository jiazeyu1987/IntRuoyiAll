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

- BDD: 批记录页面关系图页签 -> Given 用户打开 eDHR 批记录页签栏, When 查看页签, Then 能看到“批记录页面关系图”并可进入独立页面。
- BDD: 页面节点关系图 -> Given 用户进入批记录页面关系图, When 页面渲染, Then 节点代表页面/业务入口，连线表达页面数据关系，且不使用工艺路线流转配置。
- BDD: 节点跳转边界 -> Given 某个节点已有正式路由, When 点击节点, Then 跳转到对应页面；Given 节点尚无正式路由, Then 显示待接入且不执行假跳转。

## RED / GREEN Evidence

- RED: `node tests/e2e/edhr-batch-page-graph-tab-static.spec.js` -> FAIL，原因：`BatchPageGraphPage.vue must exist`。
- GREEN: `node tests/e2e/edhr-batch-page-graph-tab-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS。
- TYPE CHECK: `pnpm ts:check` -> PASS。

## Verification: Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Responsive: 页面关系图使用 4 列、2 列和单列断点，节点文本在按钮内换行。
- Accessibility: 页面有 `aria-label`，节点使用原生 `button`，未接入节点设置 `disabled` 和 `aria-disabled`。
- Loading: v1 不新增 API，无异步加载态。
- Empty: v1 使用静态页面节点定义，不存在空数据接口状态。
- Error: v1 不新增后端请求，不新增吞异常或默认成功路径；路由跳转错误按 Vue Router 原始行为暴露。
- Permission: 新路由复用 `mes:pro-edhr-batch-execution:query`，不新增业务写入权限。
- Real E2E: 官方登录预检 PASS；从批次执行真实点击页签进入关系图 PASS。
- Real E2E: 10 个目标节点、11 条关系、6 个待接入禁用节点 PASS。
- Real E2E: 生产填写、PQC填写、正式批记录节点路由跳转 PASS，MES 写请求数为 0。

## Blockers And Follow-Up Skills

- 真实节点状态、数量徽标和权限投影需要后端聚合接口后再扩展。
- 下游 blocker: 生产填写与 PQC填写页面缺少设备账号工艺路线绑定来源，无法加载一线报工上下文，并出现相关 502 resource error。
- Final E2E status: `GRAPH_PASS_DOWNSTREAM_BLOCKED`。
- Closeout blocker: 当前分支领先 `origin/int_main` 且含非本任务提交，未执行最终 closeout push。
