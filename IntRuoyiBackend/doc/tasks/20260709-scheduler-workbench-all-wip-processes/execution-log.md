# 执行日志：排产员工作台显示全部在排工序

BDD: 工作台工序列表显示全部在排工序 -> Given 多个排产单存在多个未完成且启用的排产工序 When 查询排产员工作台工序列表 Then 返回所有在排工序聚合结果，而不是只返回某一天或单个当前工序。
BDD: 工作台工序显示排产快照编码名称 -> Given 工序主数据缺失或与排产快照不一致 When 查询排产员工作台工序列表 Then 工序编号和名称来自排产工序快照。

RED: mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldAggregateAllUnfinishedEnabledProcesses,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseProcessSnapshotForAllWorkbenchWip" test -> FAIL, 旧逻辑只返回每个排产单当前工序，两个回归断言均得到 1 而不是 2。
GREEN: mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldAggregateAllUnfinishedEnabledProcesses,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseProcessSnapshotForAllWorkbenchWip" test -> PASS，2 个回归测试通过。
REGRESSION: mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExcludeFrozenScheduleOrders,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldAggregateAllUnfinishedEnabledProcesses,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseProcessSnapshotForAllWorkbenchWip,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExposeListMetricsForWorkbenchTable,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExposeNightShiftAndPlannedStartDateMixedState" test -> PASS，5 个 WIP 相关测试通过。
GREEN: validate_bug_regression.py --evidence doc/tasks/20260709-scheduler-workbench-all-wip-processes/bug-regression-evidence.md -> PASS。
GREEN: validate_backend_api.py --evidence doc/tasks/20260709-scheduler-workbench-all-wip-processes/backend-api-evidence.md -> PASS。
GREEN: task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260709-scheduler-workbench-all-wip-processes --mode preview -> PASS，delete 为 backend-api-evidence.md 与 bug-regression-evidence.md，blocked/warnings 均为 <none>。
