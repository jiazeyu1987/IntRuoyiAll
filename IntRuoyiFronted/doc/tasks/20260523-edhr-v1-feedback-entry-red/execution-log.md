# Execution Log：eDHR V1 FeedbackForm 首入口 RED 测试

BDD: feedback form opens eDHR by dedicated context API -> Given 用户从 `FeedbackForm` 进入 eDHR 执行节点 When 前端判断是否打开或创建 eDHR 执行实例 Then 必须调用 eDHR 专用 `entry-context` 与 `open-or-create-by-context` 能力，而不是用 `route-process` 查询接口决定入口

BDD: feedback form exposes open eDHR action with full business context -> Given 用户已经选定报工单关联任务和工艺上下文 When 页面提供 `打开 eDHR` 入口 Then 传递上下文至少包含 `workOrderId`、`taskId`、`routeId`、`processId`、`workstationId`、`batchCode`

BDD: execution page has dedicated hidden route with active menu ownership -> Given 用户从 `FeedbackForm` 跳到 eDHR 执行页 When 路由注册到前端 Then 必须存在独立 eDHR 执行页隐藏路由，并明确 `activeMenu` 归属

BDD: execution renderer consumes execution snapshot contract -> Given eDHR 执行页需要渲染执行快照 When 前端定义页面或渲染器契约 Then 必须依赖 `executionSnapshotJson`，而不是旧的 `sheetLayoutJson` 或 `metaJson`

RED: `node --test scripts\\edhr-v1-feedback-entry.test.mjs` -> FAIL, `src/api/mes/pro/feedback/index.ts` 缺少 `entry-context` / `open-or-create-by-context`；`src/views/mes/pro/feedback/FeedbackForm.vue` 缺少 `打开 eDHR` 入口与最小上下文字段；`src/router/modules/remaining.ts` 缺少独立 eDHR 执行页隐藏路由与 `activeMenu: '/mes/pro/feedback'`；`src` 内不存在消费 `executionSnapshotJson` 的 eDHR 执行页或渲染器

INFO: first run exposed a Windows-only test support path bug in the new script; after switching to `fileURLToPath(import.meta.url)`, the second run failed only on missing eDHR functionality, not on the test harness itself
