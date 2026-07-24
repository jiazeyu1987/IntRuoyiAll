# Execution Log

- BDD: 当前工序展示 -> Given 一个排产工单包含多个未完成工序 / When 排产员工作台查询在制工序统计 / Then 只按排序最靠前的当前未完成工序聚合，工序编号和名称来自该排产工序快照。
- BDD: 当前工序合并 -> Given 多个排产工单当前工序相同 / When 工作台统计在制单数 / Then 同一当前工序合并展示订单数与订单 ID 列表，不把后续未开始工序计入。
- GREEN: experience-preflight -> PASS, 已读取 PowerShell、经验索引命中项、项目防错经验、前端样式、缺陷修复契约和后端接口契约；本任务不执行真实 E2E、不操作服务器、不修改数据库 schema。

## 当前进度

- M1 completed: 任务文档已创建。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseCurrentProcessSnapshotForWorkbenchWip" test` -> FAIL, expected 1 row but was 2; 当前实现把后续未完成工序也统计进工作台。
- M2 completed: 已新增失败回归测试，复现后续工序被统计导致工序展示不匹配。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseCurrentProcessSnapshotForWorkbenchWip" test` -> PASS, 1 test PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseCurrentProcessSnapshotForWorkbenchWip,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldAggregateCurrentUnfinishedEnabledProcessPerOrder,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExposeListMetricsForWorkbenchTable,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExposeNightShiftAndPlannedStartDateMixedState" test` -> PASS, 4 tests PASS。
- M3 completed: `getProcessWipStatistics` 改为先按每个排产工单解析当前未完成工序，再按当前工序聚合；响应编码和名称来自排产工序快照。
- GREEN: bug-regression evidence validation -> PASS。
- GREEN: backend-api evidence validation -> PASS。
- GREEN: `task_closeout.py --task-id 20260709-scheduler-workbench-current-process-wip --mode preview` -> PASS，计划保留 `task.md` 与 `execution-log.md`，清理附属 evidence 文件。
- M4 completed: 目标回归、证据校验和 closeout preview 均通过。
- M5 completed: 任务状态已标记完成，准备执行 closeout apply。
