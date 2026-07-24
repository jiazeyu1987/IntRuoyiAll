# Execution Log

BDD: 设备数量求和展示 -> Given 工艺路线某工序绑定多个设备资源 / When 打开工艺路线详情 / Then “设备”列显示这些资源 `quantity` 求和。

BDD: 点击数量展示设备列表 -> Given 工序存在设备资源 / When 点击设备数量 / Then 弹窗展示每个工作站设备绑定的编码、名称和数量。

BDD: 点击设备编码打开设备详情 -> Given 设备列表弹窗已打开 / When 点击设备编码 / Then 打开对应设备台账详情弹窗。

BDD: 无设备工序显示空态 -> Given 工序没有设备资源 / When 查看工艺路线详情 / Then “设备”列显示 `-` 且不可点击。

RED: `node tests/e2e/mes-pro-route-process-machinery-column.spec.js` -> FAIL, 工艺路线详情组成工序表格仍展示“下一道工序”列，且尚未实现设备列、设备列表弹窗和设备详情打开。

GREEN: `node tests/e2e/mes-pro-route-process-machinery-column.spec.js` -> PASS, “下一道工序”展示列已移除，设备列、设备列表弹窗和 `MachineryForm.open('detail', machineryId)` 调用契约存在。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS, Vue/TypeScript 类型检查通过。普通 `pnpm ts:check` 曾因 Node 默认堆内存不足退出，未改代码降级处理。

GREEN: `node tests/e2e/mes-route-resource-table-real-flow.e2e.js` -> PASS, 资源大表真实 UI 回归通过。

BLOCKED: `MES_ROUTE_PROCESS_MACHINERY_E2E_BASE_URL=http://127.0.0.1:18081 node tests/e2e/mes-pro-route-process-machinery-column-real-flow.e2e.js` with `测试租户/aoteman` -> FAIL, 页面已显示 `B010` 工序 `5 台`，点击后设备列表展示 `A03190/A03197/A03214/A03383/A03389` 和数量；点击 `A03190` 后现有设备详情接口 `/mes/dv/machinery/get` 返回 `Access Denied`，业务码 `500`，无法验证测试租户下设备台账详情内容。

GREEN: `MES_ROUTE_PROCESS_MACHINERY_E2E_TENANT=芋道源码 MES_ROUTE_PROCESS_MACHINERY_E2E_USERNAME=admin MES_ROUTE_PROCESS_MACHINERY_E2E_PASSWORD=admin123 node tests/e2e/mes-pro-route-process-machinery-column-real-flow.e2e.js` -> PASS, 管理员只读路径完整验证设备数量、设备列表和现有设备台账详情弹窗。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-mes-route-process-machinery-column --mode preview` -> PASS, keep `task.md`、`execution-log.md`、`frontend-feature-evidence.md`；delete/blocked/warnings 均为空。

Status: Frontend implementation completed; repository commit is blocked until the test-tenant `mes:dv-machinery:query` permission prerequisite is resolved or explicitly accepted by the user.
