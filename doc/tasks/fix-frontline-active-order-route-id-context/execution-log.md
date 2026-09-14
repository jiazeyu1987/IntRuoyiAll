# 执行日志：一线选择员工保留活跃订单路线身份

BDD: 选择员工后保留活跃订单路线身份 -> Given 一线生产运行态 activeOrder 返回正式 `routeId` / When 用户在“选择员工”弹框切换实际填写员工 / Then 前端正式提交上下文仍包含 `productionSubmitContext.activeOrder.routeId`，不得提示缺少该字段。

BDD: 缺少正式路线身份仍 fail fast -> Given 一线生产运行态 activeOrder 本身没有正式路线身份 / When 用户尝试正式提交 / Then 前端继续暴露缺失上下文错误，不使用 URL query、旧值或默认值补齐。

BDD: 同路线多活跃订单按正式任务唯一解析 -> Given 当前负责组长同一路线下存在多个 ACTIVE 活跃订单 / When 一线运行态加载当前路线工序 / Then 后端按工单任务的路线、工序和正式工作站唯一选择目标活跃订单，并返回该工单的正式提交上下文。

- INFO: task-start -> 用户截图显示“选择员工”弹框内提示 `一线提交身份上下文缺少必填字段：productionSubmitContext.activeOrder routeId=922119`。
- INFO: skill -> 使用 `bug-regression-fix-loop`，按 RED/GREEN 修复。
- INFO: rules -> 已读取 task closeout、PowerShell/编码、frontend、backend、E2E 相关规则。
- INFO: root-cause-scope -> 截图错误来自后端 `MesFrontlineRuntimeConfigServiceImpl.requireSingleActiveOrder`，不是前端本地提交上下文断言。当前实现先按 `leaderUserId + routeId` 要求活跃订单唯一，导致同一路线多个活跃订单时在任务匹配前误 fail-fast。
- INFO: red-fixture-correction -> 首次新增测试夹具因候选携带 deviceId 但无班组设备绑定，先命中 `frontline runtime deviceId=7001` 范围门禁；已移除无关设备干扰，仅保留路线、工序、工作站身份。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_resolvesActiveOrderByMatchingProductionTaskWhenRouteHasMultipleActiveOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 当前实现先按 `leaderUserId + routeId` 要求活跃订单唯一，实际抛出 `productionSubmitContext.activeOrder routeId=101`。
- INFO: implementation -> `MesFrontlineRuntimeConfigServiceImpl` 改为先收集当前负责组长同路线活跃订单，再逐个读取工单任务并按 `routeId + processId + workstationId` 匹配；唯一匹配时返回对应 activeOrder/task，缺失或多匹配仍 fail fast。

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_resolvesActiveOrderByMatchingProductionTaskWhenRouteHasMultipleActiveOrders" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 10, Failures: 0, Errors: 0, Skipped: 0。
- REGRESSION: `git diff --check` -> PASS, exit code 0；仅有既有 CRLF warning，无 whitespace error。
- INFO: experience -> 已将同路线多活跃订单必须按 `routeId + processId + workstationId` 任务身份唯一解析的门禁合并到 `docs/backend-development.md`，并补充 `docs/experience-index.md` 关键词。
- INFO: reports -> 已生成 `verification-report.md` 与 `bug-regression-evidence.md`。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-frontline-active-order-route-id-context\bug-regression-evidence.md` -> PASS, Bug regression evidence is valid。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id fix-frontline-active-order-route-id-context --mode preview` -> PASS, keep 4, delete none, blocked none, warnings none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id fix-frontline-active-order-route-id-context --mode apply` -> PASS, deleted_paths none。
- INFO: final-status -> task.md 已标记 completed；未执行 Git 提交/推送。
