# 20260709 排产员工作台当前工序在制统计修复

## 任务目标

修复排产员工作台“工序列表”中工序编号、工序名称不对应当前正在排产工序的问题。工作台应按每个排产工单当前未完成工序聚合，并展示该当前工序快照里的编码和名称。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；后续命令显式设置 UTF-8，不使用 `&&`。
- 项目级防错 / 智能排产统计口径：已读取 `docs/agent-memory/project-error-prevention.md`；排产统计必须区分“任务/工单条数”和“生产数量”，并从权威业务明细汇总。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本任务不改前端样式。
- 缺陷修复：已读取 `bug-regression-fix-loop` 与证据契约；先写失败回归，再做最小修复。
- 后端接口：已读取 `backend-api-delivery` 与证据契约；不改接口权限、不改数据库 schema、不引入 fallback。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。统计口径从“所有未完成工序”收敛为“每个排产工单的当前未完成工序”，并优先使用排产工序快照字段展示编码和名称。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 当前工序展示 -> Given 一个排产工单包含多个未完成工序 / When 排产员工作台查询在制工序统计 / Then 只按排序最靠前的当前未完成工序聚合，工序编号和名称来自该排产工序快照。
- BDD: 当前工序合并 -> Given 多个排产工单当前工序相同 / When 工作台统计在制单数 / Then 同一当前工序合并展示订单数与订单 ID 列表，不把后续未开始工序计入。

## 里程碑

- [completed] M1：创建任务目录，记录任务目标、经验门禁、BDD 与设计约束。
- [completed] M2：补充失败回归测试，复现工作台统计后续工序和空主数据导致名称编码错误。
- [completed] M3：最小修复后端当前工序统计口径。
- [completed] M4：运行目标回归与证据校验。
- [completed] M5：记录收尾状态并按混合工作区提交边界处理。

## 预期验证

- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseCurrentProcessSnapshotForWorkbenchWip" test`
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldUseCurrentProcessSnapshotForWorkbenchWip,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldAggregateCurrentUnfinishedEnabledProcessPerOrder,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExposeListMetricsForWorkbenchTable,MesProScheduleOrderServiceImplTest#getProcessWipStatistics_shouldExposeNightShiftAndPlannedStartDateMixedState" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ruoyi-vue-pro/doc/tasks/20260709-scheduler-workbench-current-process-wip/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence ruoyi-vue-pro/doc/tasks/20260709-scheduler-workbench-current-process-wip/backend-api-evidence.md`

## 当前状态

completed

COMPLETED：当前工序统计口径已修复，新增 RED 回归已转 GREEN，相关在制工序统计回归 4 tests PASS；缺陷证据、后端接口证据和 closeout preview 均已通过。

## Current Status

completed

## 完成记录

- 根因：工作台在制统计按所有未完成启用工序聚合，并从工序主数据取编码和名称，导致后续工序也进入列表且主数据缺失时显示“无工序编码 / 未命名工序”。
- 修复：`getProcessWipStatistics` 先按排产工单解析当前未完成工序，再按当前工序聚合；编码和名称来自排产工序快照。
- 验证：目标 RED 已复现并转 GREEN；相关在制工序统计 4 tests PASS；证据契约校验 PASS。
- 收尾：`task_closeout.py --task-id 20260709-scheduler-workbench-current-process-wip --mode preview` PASS，待 apply 清理附属证据文件。
