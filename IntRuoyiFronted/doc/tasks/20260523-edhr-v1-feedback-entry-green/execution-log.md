# Execution Log：eDHR V1 FeedbackForm 首入口 GREEN 实现

BDD: feedback form opens eDHR by dedicated context API -> Given 用户从 `FeedbackForm` 进入 eDHR 执行节点 When 前端判断是否打开或创建 eDHR 执行实例 Then 必须调用 eDHR 专用 `entry-context` 与 `open-or-create-by-context` 能力，而不是复用 `route-process` 查询或旧入口

BDD: feedback form exposes open eDHR action with full business context -> Given 用户已选定 `workOrderId`、`taskId`、`routeId`、`processId`、`workstationId`、`batchCode` 等业务上下文 When 用户点击 `打开 eDHR` Then 前端必须组织完整上下文并在缺失时立即报错

BDD: execution page has dedicated hidden route with active menu ownership -> Given 用户从 `FeedbackForm` 跳转 eDHR 执行页 When 前端注册路由 Then 必须提供独立隐藏路由并将 `activeMenu` 归属到 `/mes/pro/feedback`

BDD: execution page renders only from execution snapshot contract -> Given eDHR 执行页已拿到执行实例或入口返回结果 When 页面渲染执行节点 Then 只允许消费 `executionSnapshotJson`，缺少快照时必须直接报错，不得降级读取 `sheetLayoutJson` 或 `metaJson`

RED: `node --test scripts\\edhr-v1-feedback-entry.test.mjs` -> FAIL, `src/api/mes/pro/feedback/index.ts` 缺少 `entry-context` / `open-or-create-by-context`；`src/views/mes/pro/feedback/FeedbackForm.vue` 缺少 `打开 eDHR` 入口与最小上下文字段；`src/router/modules/remaining.ts` 缺少独立 eDHR 执行页隐藏路由与 `activeMenu: '/mes/pro/feedback'`；`src` 内不存在消费 `executionSnapshotJson` 的 eDHR 执行页或渲染器

GREEN: `node --test scripts\\edhr-v1-feedback-entry.test.mjs` -> PASS

INFO: `pnpm install --frozen-lockfile` -> PASS, 当前 worktree 原先缺少 `node_modules`，为运行本地 ESLint 与 `vue-tsc` 安装依赖

GREEN: `node node_modules/eslint/bin/eslint.js src/api/mes/pro/feedback/index.ts src/views/mes/pro/feedback/FeedbackForm.vue src/router/modules/remaining.ts src/views/mes/pro/edhr/ExecutionPage.vue src/views/mes/pro/edhr/ExecutionRenderer.vue` -> PASS

INFO: `pnpm ts:check` -> FAIL, Node 堆内存耗尽；`NODE_OPTIONS=--max-old-space-size=8192` 后重试仍 OOM，当前无法给出全仓类型检查 GREEN 结果

INFO: follow-up contract check against backend current implementation -> `entry-context` 仍为 `POST /mes/pro/batch-record-execution/entry-context`；`open-or-create-by-context` 当前返回字段名为 `executionId`

GREEN: `node --test scripts\\edhr-v1-feedback-entry.test.mjs` after follow-up contract repair -> PASS, 已锁定前端对 `executionId` 的读取并防止回退为猜测 `execution.id`

GREEN: `node node_modules/eslint/bin/eslint.js src/api/mes/pro/feedback/index.ts src/views/mes/pro/feedback/FeedbackForm.vue scripts/edhr-v1-feedback-entry.test.mjs src/views/mes/pro/edhr/ExecutionPage.vue src/views/mes/pro/edhr/ExecutionRenderer.vue src/router/modules/remaining.ts` after follow-up contract repair -> PASS

INFO: follow-up frontend scope expanded -> 新增独立 eDHR 执行列表页与列表/详情隐藏路由关系；详情主摘要优先显示 route/process/workstation/report 语义，模板字段降级为兼容信息
