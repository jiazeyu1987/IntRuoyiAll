# Execution Log

## BDD Scenarios

BDD: 夜间自动重排保护已报工任务 -> Given 测试租户存在未完成排产工单且其中部分任务已有报工 When 夜间重排 Job 执行 Then 系统只重排未开始/可替换任务并保留已报工任务。

BDD: 工作台解释今天产能与瓶颈 -> Given 今天存在排产任务、维修设备、资源未配置或剩余工序 When 排产员打开工作台 Then 能看到今日计划产能、可用产能、瓶颈工序、阻塞原因和处理入口。

BDD: 工作台解释报工偏差 -> Given 今天有报工数量与计划任务数量不一致 When 排产员查看工作台 Then 能看到偏差数量、偏差说明和报工页面入口。

## TDD Evidence

RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProNightlyReplanServiceImplTest,MesProSchedulerWorkbenchServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `MesProNightlyReplanServiceImpl` 不存在。

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProNightlyReplanServiceImplTest,MesProSchedulerWorkbenchServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 新增夜间重排服务与工作台汇总增强测试 3 passed。

REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProNightlyReplanServiceImplTest,MesProSchedulerWorkbenchServiceImplTest,MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 自动排程重排既有保护测试与新增测试共 23 passed。

FRONTEND RED: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> FAIL, 工作台页面缺少快捷入口、夜间自动重排、报工偏差、瓶颈建议。

FRONTEND GREEN: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。

FRONTEND GREEN: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> PASS。

SQL RED: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> FAIL, `mesProNightlyReplanJob` 和 02:30 cron 缺失。

SQL GREEN: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> PASS, `infra_job.id=5601` 使用 `handler_name=mesProNightlyReplanJob`，`cron_expression=0 30 2 * * ?`。

E2E: `node tests/e2e/mes-pro-scheduler-workbench-real-flow.e2e.js` with `MES_SCHEDULER_WORKBENCH_E2E_BASE_URL=http://127.0.0.1:8094` and test tenant `测试租户/aoteman` -> PASS，真实登录后打开工作台，验证快捷入口、夜间自动重排、瓶颈建议、报工偏差、今日可用产能，并点击 7 个主要入口到达真实页面。

MERGE REGRESSION: backend `int_main` -> PASS, `mvn -pl yudao-module-mes -am "-Dtest=MesProNightlyReplanServiceImplTest,MesProSchedulerWorkbenchServiceImplTest,MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，23 passed。

MERGE REGRESSION: SQL `int_main` -> PASS, `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py`，5 passed。

MERGE REGRESSION: frontend `int_main` -> PASS, `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js`。

MERGE REGRESSION: frontend typecheck `int_main` -> PASS, `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192`。

MERGE E2E: `node tests/e2e/mes-pro-scheduler-workbench-real-flow.e2e.js` with `MES_SCHEDULER_WORKBENCH_E2E_BASE_URL=http://127.0.0.1:8081` and test tenant `测试租户/aoteman` -> PASS，合并后主入口真实工作台与 7 个主要导航入口可用。
