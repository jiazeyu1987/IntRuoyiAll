# 执行日志：修正排产工作台报工偏差为当次排产口径

- BDD: 总偏差按当次排产工单计算 -> Given 工作台展示当前有效排产工单 / When 查看报工偏差 / Then 总偏差必须来自有效排产工单层的实际报工数量与排产数量差值，而不是当天任务段 quantity 汇总。
- BDD: 工序明细按排产工序快照计算 -> Given 有效排产工单存在工序快照 / When 打开报工偏差明细 / Then 每条工序显示 planned/reported/deviation，且总偏差不重复累计每道工序。
- BDD: 已完成/已取消排产工单不计入当前排产偏差 -> Given 历史已完成或已取消排产工单存在报工记录 / When 计算当前工作台报工偏差 / Then 不应混入当前当次排产总偏差。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchServiceImplTest" test` -> FAIL，旧实现仍断言 `currentScheduleScopeText` 为“当天任务段”口径，且 summary 契约缺少当前排产总量与工序偏差明细。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchServiceImplTest" test` -> PASS。
- RED: 用户指出 `completed_quantity=20.791668` 是进度折算小数，不是实际报工数量；报工偏差不应出现这种小数。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchServiceImplTest,MesProSchedulerWorkbenchMapperXmlTest" test` -> PASS，实际报工数量改为按排产工单取工序快照/真实报工单最大报工量，不再使用 `completed_quantity` 或 `progress_percent`。
