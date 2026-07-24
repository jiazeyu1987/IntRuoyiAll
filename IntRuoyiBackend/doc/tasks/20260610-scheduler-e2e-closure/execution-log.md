# 20260610 排产目标 7 执行日志

## BDD 场景

BDD: 目标 7 测试租户排产闭环回归 -> Given 测试租户存在 ERP 同步生产工单、排产工单、工序快照、报工归属样本和夜间重排配置；When 排产员登录工作台并按顺序查看生产订单、排产工单池、工艺路线资源、排程日历、生产任务、生产报工；Then 系统必须展示完整闭环数据、解释产能/瓶颈/偏差/重排状态，并且不得使用芋道源码/admin 写数据。

## TDD 证据

- RED: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\worktrees\scheduler_e2e_closure\yudao-ui-admin-vue3\docs\qa\mes-scheduler-e2e-closure-evidence.md` -> FAIL，缺少 Scope/Matrix/Test/RED/GREEN/Verification/Blockers 标记。
- RED: `node tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js` -> FAIL，`归属状态` 文本同时匹配表单 label 和表头，Playwright 严格模式要求选择器唯一。
- RED: `node tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js` -> FAIL，同登录态最终校验未带前端 `Authorization`/`tenant-id` 头。
- RED: `node tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js` -> FAIL，token 缓存值带包裹引号导致后端 401。
- RED: `node tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js` -> FAIL，工作台瓶颈字段名应为 `bottlenecks`。
- GREEN: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\worktrees\scheduler_e2e_closure\yudao-ui-admin-vue3\docs\qa\mes-scheduler-e2e-closure-evidence.md` -> PASS。
- GREEN: `node --check tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest,MesProScheduleOrderServiceImplTest,MesProNightlyReplanServiceImplTest,MesProSchedulerWorkbenchServiceImplTest,MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，33 个测试通过。
- GREEN: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> PASS，5 个测试通过。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js; node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js` on `http://127.0.0.1:8095` with `测试租户/aoteman` -> PASS；样本 `CODexERP20260610E`，排产单 `12`，路线版本 `ROUTE-ROUTE-XLSX-00001-20260610-0003`，工序快照 `24` 条，已归属导入记录 `135`，今日可用产能 `586711.950243`，瓶颈项 `10`。
- RED: `node tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js` on merged `int_main` `http://127.0.0.1:8081` -> FAIL，登录页没有显式租户输入时脚本仍尝试处理租户下拉，导致登录请求未触发。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest,MesProScheduleOrderServiceImplTest,MesProNightlyReplanServiceImplTest,MesProSchedulerWorkbenchServiceImplTest,MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` on merged `int_main` -> PASS，33 个测试通过。
- REGRESSION: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` on merged `int_main` -> PASS，5 个测试通过。
- REGRESSION: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js; node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` on merged `int_main` -> PASS。
- REGRESSION: `node tests/e2e/mes-scheduler-target7-closed-loop-real-flow.e2e.js` on merged `int_main` `http://127.0.0.1:8081` with `测试租户/aoteman` -> PASS；样本 `CODexERP20260610E`，排产单 `12`，路线版本 `ROUTE-ROUTE-XLSX-00001-20260610-0003`，工序快照 `24` 条，已归属导入记录 `135`，今日可用产能 `586711.950243`，瓶颈项 `10`。
