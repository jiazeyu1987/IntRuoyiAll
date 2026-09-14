# Bug Regression Evidence：一线运行态 activeOrder routeId 误报

## Bug

一线员工在选择员工/加载提交上下文时出现 `一线提交身份上下文缺少必填字段：productionSubmitContext.activeOrder routeId=922119`。

## Expected

期望行为是：当当前负责组长同一路线存在多个活跃订单时，后端运行态继续按正式生产任务的 `routeId + processId + workstationId` 唯一解析目标 activeOrder/task；只要正式任务唯一匹配，就返回该工单的 `productionSubmitContext`，不得因同一路线多活跃订单提前失败。

## Reproduction command or path

`mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_resolvesActiveOrderByMatchingProductionTaskWhenRouteHasMultipleActiveOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`MesFrontlineRuntimeConfigServiceImpl.requireSingleActiveOrder` 先按 `leaderUserId + routeId` 过滤活跃订单并要求结果数量等于 1，然后才读取生产任务。真实场景同一负责组长可在同一路线下存在多个活跃订单，导致还没用当前工序/工作站任务身份消歧就误抛 `productionSubmitContext.activeOrder routeId=...`。

## Regression test added or updated

新增 `MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_resolvesActiveOrderByMatchingProductionTaskWhenRouteHasMultipleActiveOrders`：构造同一组长同一路线两张 ACTIVE 活跃订单，第一张工单任务工作站不匹配，第二张工单任务的路线、工序、工作站匹配当前候选；断言运行态返回第二张工单与任务。

## RED: command and expected failure

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_resolvesActiveOrderByMatchingProductionTaskWhenRouteHasMultipleActiveOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL
- Expected failure: `productionSubmitContext.activeOrder routeId=101`，证明当前实现按路线提前要求 activeOrder 唯一。

## GREEN: command and passing result

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_resolvesActiveOrderByMatchingProductionTaskWhenRouteHasMultipleActiveOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。

## Verification and regression scope

- 后端仍保留 fail-fast：没有 activeOrder、单工单任务不唯一、多 activeOrder 匹配不到唯一任务、任务 ID/工作站/物料/记录本缺失时继续抛正式上下文错误。
- 已复跑 `MesFrontlineRuntimeConfigServiceTest,MesFrontlineEmployeeSwitchServiceTest` 和 `MesProFrontlineFeedbackSubmitServiceTest`，覆盖运行态配置、选择员工相邻链路和正式提交服务相邻链路。

## Blockers and follow-up actions

无阻塞。真实页面 E2E 未在本次执行，因为修复点是后端运行态解析，且未启动本地服务；如需验证截图路径，可在确认本地前后端运行态与测试账号后补跑真实 Playwright。
