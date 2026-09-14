# 验证报告：一线运行态按正式任务解析活跃订单

## 结论

通过。已修复同一组长同一路线存在多个活跃订单时，后端运行态仅按 `leaderUserId + routeId` 要求唯一导致误报 `productionSubmitContext.activeOrder routeId=...` 的问题。

## 变更摘要

- `MesFrontlineRuntimeConfigServiceImpl`：运行态生产提交上下文先收集当前负责组长同路线活跃订单，再按各工单生产任务的 `routeId + processId + workstationId` 唯一匹配 activeOrder/task。
- `MesFrontlineRuntimeConfigServiceTest`：新增多活跃订单同路线回归用例，断言只返回任务匹配当前工序工作站的工单。
- `docs/backend-development.md` / `docs/experience-index.md`：沉淀一线生产正式提交 activeOrder 解析门禁。

## RED

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_resolvesActiveOrderByMatchingProductionTaskWhenRouteHasMultipleActiveOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: FAIL
- Expected reason: 当前实现先按 `leaderUserId + routeId` 要求活跃订单唯一，抛出 `productionSubmitContext.activeOrder routeId=101`。

## GREEN / Regression

- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_resolvesActiveOrderByMatchingProductionTaskWhenRouteHasMultipleActiveOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS，Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。
- Command: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: PASS，Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。
- Command: `git diff --check`
- Result: PASS，退出码 0；仅输出既有 CRLF warning，无 whitespace error。

## 未执行项

- 未启动本地前后端或真实 Playwright E2E；本次根因和修复点在后端运行态解析，已用后端单元回归覆盖员工切换相邻链路和正式提交服务相邻链路。

## Cleanup

- `task_closeout.py --mode preview`：PASS，keep 4，delete none，blocked none，warnings none。
- `task_closeout.py --mode apply`：PASS，deleted_paths none。
- 任务状态已标记 `completed`；未执行 Git 提交/推送。
