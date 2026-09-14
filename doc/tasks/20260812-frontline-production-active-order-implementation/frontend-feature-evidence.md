# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 一线生产顶部按活跃订单、工序、员工排列；提交明确归属选中订单；组长报工管理红色显示待调整数量并复用现有分配入口。
- Non-goals: 不限制或截断员工完成数量，不自动改选订单，不重做生产组长分配流程，不改变 PQC 业务口径。

## Requirements And Acceptance IDs

- FE-01: 生产活跃订单选择体验与 PQC 一致，覆盖搜索、loading、empty、error、ready。
- FE-02: 工序和员工选择保持现有功能，跨路线工序切换清除不再匹配的订单。
- FE-03: 正式提交必须从用户选中订单生成 `workOrderId`，没有订单或订单工序不一致时阻塞。
- FE-04: 组长页显示生产工单、红色待调整数量，并能进入现有订单分配。

## Entry Points And Owned Files

- Routes: `/mes/pro/feedback/edhr-batch-production-fill`、`/mes/pro/process-pool/production-leader`。
- Components: `FrontlineFixedTemplatePanel.vue`、`TeamLeaderWorkbenchPage.vue`、`frontlineDeviceEmployeeContext.ts`。
- API client: `src/api/mes/pro/feedback/index.ts`。

## API Contracts And Data States

- `GET /mes/pro/feedback/frontline/device-account/active-orders` 返回当前设备账号所属生产组长的正式活跃订单。
- 选择状态使用 `selectedActiveOrder`；提交的 feedback 与 process-pool 两份上下文都取同一 `workOrderId`。
- loading、空列表、筛选无结果和接口错误均有显式页面状态；接口错误不回退到默认订单。

## BDD Scenarios

- BDD: 生产顶部顺序 -> Given 生产页面加载 When 员工查看顶部 Then 依次显示活跃订单、工序、员工。
- BDD: 选中订单归属 -> Given 订单 A 与工序 P 匹配 When 员工选择并提交 Then 提交归属订单 A。
- BDD: 超报允许提交 -> Given 订单数量 100 且完成 200 When 员工提交 Then 页面不做数量上限拦截。
- BDD: 组长调整超报 -> Given 报工超出原订单可承接数量 When 组长查看报工管理 Then 显示红色待调整并可打开分配。

## RED And GREEN

- RED: 三个新增静态合同首次均失败，分别证明缺少订单入口、提交仍取运行态订单、组长页缺少红色待调整。
- RED: 只读真实页面首次发现活跃订单入口未纳入统一选择区域标识。
- GREEN: 三个新增静态合同、PQC 活跃订单回归、现有组长分配合同均通过。
- GREEN: `pnpm.cmd ts:check` 通过。

## Responsive Accessibility And States

- 1920x1080 真实页面截图确认订单弹框不溢出、搜索框和返回按钮可见。
- 三个顶部入口使用原生 button/明确 data 契约，订单搜索含 aria-label，选择弹框含 aria-label。
- loading、empty、error 和不可提交状态均显式呈现；未选订单时提交按钮阻塞。
- 权限沿用现有一线生产和生产组长路由权限，不新增前端越权入口。

## Verification And E2E Path

- `node tests/e2e/frontline-production-active-order-real-readonly.e2e.cjs` -> PASS；真实登录后验证订单弹框、顶部顺序和组长报工管理列，MES 写请求 0。
- 写入型超报路径因缺少可写测试租户和任务专用夹具而阻塞，未使用 admin 基线数据写入。

## Blockers And Follow-Up

- Blocker: 缺少 `TLW_*` 写入型 E2E 前置，浏览器未执行真实 200 件报工与分配调整。
- Downstream: 前置补齐后应复用正式组长工作台 E2E 数据治理执行一次写入闭环，不需要新增产品 fallback。
