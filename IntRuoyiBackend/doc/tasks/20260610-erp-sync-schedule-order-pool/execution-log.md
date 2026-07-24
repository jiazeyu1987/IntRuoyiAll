# 20260610 ERP 生产订单同步与排产工单池完善执行日志

## BDD 场景

BDD: ERP 生产订单按工单编码幂等同步 -> Given ERP 最近一年内存在未完成生产订单 When 每天 2 点同步任务执行 Then 本地生产工单按工单编码新增或更新基础信息，不写入承诺交期。

BDD: ERP 完成状态不参与待排同步 -> Given ERP 工单状态为已完成 When 同步最近一年未完成工单 Then 不新增待排生产工单，也不作为排产工单生成来源。

BDD: ERP 变化只形成排产差异提示 -> Given 生产工单已经生成排产工单 When ERP 同一工单基础信息发生变化并同步 Then 排产工单不被自动覆盖，系统记录待处理差异。

BDD: 排产员生成排产工单 -> Given 生产工单来自 ERP 且未生成排产工单 When 排产员填写承诺交期并生成排产工单 Then 排产数量等于生产工单数量，排产工单获得清晰编号并生成工序快照。

BDD: 排产工单不允许拆分 -> Given 生产工单已经生成排产工单 When 排产员再次从同一生产工单生成排产工单 Then 系统拒绝并提示已经存在排产工单。

## 证据

- 2026-06-10 初始化任务文档，创建独立 worktree `erp_sync_schedule_order`。
- BDD: ERP 生产订单按工单编码幂等同步 -> Given ERP 最近一年内存在未完成生产订单 When 每天 2 点同步任务执行 Then 本地生产工单按工单编码新增或更新基础信息，不写入承诺交期。
- RED: `mvn -pl yudao-module-erp,yudao-module-mes "-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderSyncServiceImplTest" test` -> FAIL，`fetchUnfinishedProductionOrders(...)` 方法不存在。
- GREEN: `mvn -pl yudao-module-erp,yudao-module-mes "-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderSyncServiceImplTest" test` -> PASS，ERP client 使用最近一年日期窗口和 `FStatus <> '5'` 未完成过滤，同步服务按 `LocalDate.now().minusYears(1)` 到 `LocalDate.now()` 调用。
- BDD: 排产员生成排产工单 -> Given 生产工单来自 ERP 且未生成排产工单 When 排产员填写承诺交期并生成排产工单 Then 排产数量等于生产工单数量，排产工单获得清晰编号并生成工序快照。
- BDD: 排产工单不允许拆分 -> Given 生产工单已经生成排产工单 When 排产员再次从同一生产工单生成排产工单 Then 系统拒绝并提示已经存在排产工单。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest,KingdeeProductionOrderSyncJobTest" test` -> FAIL，`KingdeeProductionOrderSyncJob` 不存在，排产编码未包含来源工单编码。
- RED: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> FAIL，`kingdeeProductionOrderSyncJob` 与凌晨 2 点 `infra_job` SQL 不存在。
- GREEN: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> PASS，4 passed。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderSyncServiceImplTest,MesProScheduleOrderServiceImplTest,KingdeeProductionOrderSyncJobTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，目标测试 11 passed，聚合依赖模块构建通过。
- E2E: 本机 worktree `erp-sync-schedule-order`，前端 `http://127.0.0.1:8092`，后端 `http://127.0.0.1:48092`；测试租户 `测试租户/aoteman`；真实工单 `CODexERP20260610A`；`node tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS，生成 `scheduleOrderId=8`，承诺交期 `2026-06-30`，数量 `123`。
- SQL: `mes_pro_schedule_order.id=8` -> `code=SCH-CODexERP20260610A-20260610-0001`，`quantity=123.000000`，来源生产工单数量 `123.00`，`diff_status=0`。
- SQL: `infra_job.id=5600` -> `handler_name=kingdeeProductionOrderSyncJob`，`status=1`，`cron_expression=0 0 2 * * ?`。
- MERGE: 后端分支 `codex/erp-sync-schedule-order` 已合并回 `int_main`；前端分支无代码改动，合并结果为 already up to date。
- REGRESSION: `python -m pytest script/tests/test_mes_scheduling_closed_loop_sql.py` -> PASS，4 passed。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderSyncServiceImplTest,MesProScheduleOrderServiceImplTest,KingdeeProductionOrderSyncJobTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，目标测试 11 passed，聚合依赖模块构建通过。
- E2E: 合并后主运行态 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`；测试租户 `测试租户/aoteman`；真实工单 `CODexERP20260610B`；`node tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS，生成 `scheduleOrderId=9`，承诺交期 `2026-07-01`，数量 `124`。
