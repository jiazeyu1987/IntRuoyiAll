# 20260608-mes-route-process-machinery-column

## 任务目标

在 MES 工艺路线详情的“组成工序”表格中，将展示用的“下一道工序”列替换为“设备”列。设备列显示当前工序需要使用的设备数量，数量按设备资源绑定 `quantity` 求和；点击数量展示该工序设备列表，点击设备编码打开现有设备台账详情弹窗。

## 前置任务状态

- 已检查最近前端任务 `20260608-runtime-console-test-root-cleanup`，状态为 completed。
- 当前工作区存在既有未跟踪 `runtime/`，本任务不触碰、不提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；前端只展示后端真实返回的设备数量和列表，接口失败按现有请求链路暴露。
- `是否从根因和长期维护角度解决`：是；复用现有 route-process 详情接口和设备台账详情弹窗，不新增重复页面或 mock 数据。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 设备数量求和展示 -> Given 工艺路线某工序绑定多个设备资源 / When 打开工艺路线详情 / Then “设备”列显示这些资源 `quantity` 求和。
- BDD: 点击数量展示设备列表 -> Given 工序存在设备资源 / When 点击设备数量 / Then 弹窗展示每个工作站设备绑定的编码、名称和数量。
- BDD: 点击设备编码打开设备详情 -> Given 设备列表弹窗已打开 / When 点击设备编码 / Then 打开对应设备台账详情弹窗。
- BDD: 无设备工序显示空态 -> Given 工序没有设备资源 / When 查看工艺路线详情 / Then “设备”列显示 `-` 且不可点击。

## 里程碑

- [x] M1：创建任务文档，记录 BDD 与设计约束。
- [x] M2：写前端 RED 契约测试。
- [x] M3：实现设备列、设备列表弹窗和设备详情打开。
- [x] M4：运行前端静态测试、真实 E2E 与相关回归。
- [ ] M5：更新执行证据，运行 task-closeout-cleanup 预览，仅提交本任务相关改动。

## 预期验证

- `node tests/e2e/mes-pro-route-process-machinery-column.spec.js`
- `node tests/e2e/mes-route-resource-table-real-flow.e2e.js`
- Playwright 使用测试租户登录 `http://localhost:8081`，打开工艺路线详情验证设备数量、设备列表和设备详情弹窗。

## 当前状态

blocked: 前端实现、静态契约、类型检查、资源大表回归已通过；真实页面在测试租户 `aoteman` 下已验证设备数量和设备列表，但点击设备编码打开现有设备台账详情时，现有 `/mes/dv/machinery/get` 接口返回 `Access Denied`。管理员账号只读验证已完整通过；按 no-fallback 策略不在前端伪填充设备详情，暂不提交。

## 最终验证结果

- PASS: `node tests/e2e/mes-pro-route-process-machinery-column.spec.js`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- PASS: `node tests/e2e/mes-route-resource-table-real-flow.e2e.js`
- BLOCKED: `MES_ROUTE_PROCESS_MACHINERY_E2E_BASE_URL=http://127.0.0.1:18081 node tests/e2e/mes-pro-route-process-machinery-column-real-flow.e2e.js` 使用 `测试租户/aoteman` 失败；设备列和列表已通过，设备详情接口权限不足。
- PASS: 同一真实 E2E 使用 `芋道源码/admin` 只读验证通过。

## Cleanup Keep

- `doc/tasks/20260608-mes-route-process-machinery-column/frontend-feature-evidence.md`
