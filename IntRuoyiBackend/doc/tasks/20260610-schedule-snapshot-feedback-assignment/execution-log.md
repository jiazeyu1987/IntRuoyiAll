# 执行日志

BDD: 排产工单固化工艺路线与资源快照 -> Given 排产员从 ERP 生产工单生成排产工单, When 当前产品已配置工艺路线与设备/人工资源, Then 排产工单必须保存路线版本、工序列表、设备/人工来源、班次小时、小时产能与班次产能快照。

BDD: 已生成排产工单不受后续路线变更影响 -> Given 已生成排产工单, When 后续修改工艺路线或资源配置, Then 已生成排产工单的快照不应随之变化。

BDD: 报工导入候选只显示可归属排产工序 -> Given 班组长导入 MES Excel 报工, When 查看归属候选, Then 只能看到未完成且剩余数量足够的排产工单工序。

BDD: 报工归属推进排产工序进度 -> Given 班组长选择一个候选排产工单工序, When 确认归属, Then 系统创建正式报工并增加该工序已报工数量、减少剩余数量。

BDD: 报工归属不自动兜底 -> Given 导入报工没有足够剩余数量或没有已生成任务, When 班组长确认归属, Then 系统必须失败并给出明确错误，不自动兜底到其他工序。

RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderServiceImplTest,MesProFeedbackImportRecordServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`MesProScheduleOrderProcessDOBuilder` 缺少 `processCode/processName`，暴露工序快照未固化工序编码名称。

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderServiceImplTest,MesProFeedbackImportRecordServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 tests。

GREEN: `python -m pytest script\tests\test_mes_scheduling_closed_loop_sql.py` -> PASS，4 tests。

GREEN: `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS。

GREEN: `node tests\e2e\mes-pro-schedule-order-pool-real-flow.e2e.js` with `MES_SCHEDULE_ORDER_E2E_WORK_ORDER_CODE=CODexERP20260610D` -> PASS，测试租户生成排产工单 `id=11`，数量 `127`，承诺交期 `2026-07-03`。

GREEN: `node tests\e2e\mes-pro-feedback-import-attribution-real-flow.e2e.js` with `MES_FEEDBACK_ATTRIBUTION_E2E_WORK_ORDER_CODE=CODexERP20260610D` -> PASS，导入记录 `id=134` 手动归属到排产工单 `11` 工序 `241`，创建报工 `id=134`。

GREEN: 只读 SQL -> PASS，`mes_pro_schedule_order_process.id=241` 的 `process_code=B010`、`reported_quantity=5.000000`、`remaining_quantity=122.000000`。

GREEN: 融合后 `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderServiceImplTest,MesProFeedbackImportRecordServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 tests。

GREEN: 融合后 `python -m pytest script\tests\test_mes_scheduling_closed_loop_sql.py` -> PASS，4 tests。

GREEN: 融合后 `node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS。

GREEN: 融合后 `node tests\e2e\mes-pro-schedule-order-pool-real-flow.e2e.js` with `MES_SCHEDULE_ORDER_E2E_WORK_ORDER_CODE=CODexERP20260610E` -> PASS，主运行时 `8081/48081` 测试租户生成排产工单 `id=12`。

GREEN: 融合后 `node tests\e2e\mes-pro-feedback-import-attribution-real-flow.e2e.js` with `MES_FEEDBACK_ATTRIBUTION_E2E_WORK_ORDER_CODE=CODexERP20260610E` -> PASS，导入记录 `id=135` 手动归属到排产工单 `12` 工序 `265`，创建报工 `id=135`。

GREEN: 融合后只读 SQL -> PASS，`mes_pro_feedback_import_record.id=135` 为 `ATTRIBUTED`；`mes_pro_feedback.id=135` 关联排产工单 `12` 工序 `265`；`mes_pro_schedule_order_process.id=265` 的 `reported_quantity=6.000000`、`remaining_quantity=122.000000`。
