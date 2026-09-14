# Verification Report

## Result

BLOCKED。功能已融合进当前 `int_main`，融合后的后端目标测试、相邻服务完整测试、前端合同、类型检查、后端打包和只读真实页面验证均通过。用户复现的生产活跃订单接口“请求地址不存在”已修复：当前 `48081` 运行 Jar 包含生产活跃订单控制器方法，未登录请求进入鉴权，登录态请求返回 HTTP 200、业务码 0 和 3 条活跃订单候选；真实页面点击一线生产活跃订单卡片也触发同一接口并通过。订单数量 100、报工 200 仍成功并归属用户选中订单，已由后端服务单测覆盖；生产组长红色待调整和现有分配入口已由前端合同覆盖。当前唯一阻塞是写入型真实 E2E 缺少任务专用前置，因此不能完成最终清理并标记任务完成。

## Passed Evidence

- Backend targeted tests: 23 passed，0 failures，0 errors。
- Current `int_main` integration tests: 26 passed，0 failures，0 errors；相邻 `MesTeamLeaderActiveOrderServiceTest` 33 passed。
- Backend package: 30/30 Maven reactor modules passed。
- Frontend type check: passed。
- Frontend feature contracts: 生产订单选择、提交归属、组长超报标识全部 passed。
- Regression: PQC 活跃订单切换和组长现有分配合同 passed。
- Real read-only E2E: 工作树 `8100/48100`，订单弹框和生产组长报工管理真实页面 passed，MES 写请求 0，页面错误 0。
- Merge check: 功能提交 `efa04e3653c36f83eb32754a6f405d1f29ecdc23` 是当前 `int_main` 提交 `a386dc0daf00aabba0494e64f0439ea2630e4e10` 的祖先；`git ls-files -u` 与暂存区均为空；5 个原 `UU` 文件无冲突标记。
- Runtime port guard: `int_main` 的前端 8081、后端 48081 配置通过。
- Runtime 404 regression: `backend-runtime-control-20260813-103152.jar` SHA256 `953235563528C6FAEC1C2C8777A95522C96948EB29CCB0F0053A614CFECFC466`；内嵌 MES 控制器包含 `getProductionActiveOrders`，`/active-orders` 常量 2 次且 PQC 映射仍存在。
- Runtime API verification: 未登录 `/admin-api/mes/pro/feedback/frontline/device-account/active-orders` 返回业务码 401；`芋道源码/admin` 登录态返回 HTTP 200、业务码 0、3 条候选且不包含“请求地址不存在”。
- Real page trigger verification: `verify-runtime-active-orders-real.cjs` 在本机 `8081/48081` 登录后点击一线生产活跃订单卡片，接口 HTTP 200、业务码 0、3 条候选，页面错误 0。

## Real E2E Evidence

- `output/playwright/20260812-frontline-production-active-order/production-active-order-picker.png`
- `output/playwright/20260812-frontline-production-active-order/team-leader-report-workbench.png`
- `output/playwright/20260812-frontline-production-active-order/result.json`
- `output/playwright/20260812-frontline-production-active-order/production-active-order-runtime-8081.png`

## Blocked Evidence

写入型真实 200 件报工与组长调整未执行。当前环境未注入可写测试租户账号及任务自有订单、任务、路线、工序、物料、员工、设备、记录本、签名、审批人和报工类型等 `TLW_*` 前置。按项目数据安全规则未使用 `芋道源码/admin` 基线数据执行写入，也未用 API-only 或 mock 冒充真实 E2E。

## Residual Risk

浏览器级写入链路仍需在任务专用夹具恢复后补跑；核心服务行为、数据归属、幂等和页面交互已有自动化覆盖。
