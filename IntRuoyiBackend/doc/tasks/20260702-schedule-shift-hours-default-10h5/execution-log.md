# 执行日志：排产班次小时默认 10.5

BDD: 入池工位缺班次小时默认 10.5 -> Given 排产路线工序绑定的工位未设置班次小时 / When 从生产工单生成排产工单 / Then 工序快照 shiftHours 为 10.5 且 shiftCapacityTotal 按 10.5 计算，不为空。
BDD: 路线排产配置缺班次小时默认 10.5 -> Given 工艺路线配置存在有限小时产能但工位未设置班次小时 / When 查询路线排产配置 / Then 返回 shiftHours=10.5 且 standardShiftCapacity 按 10.5 计算。
BDD: 资源产能保存缺班次小时默认 10.5 -> Given 设备资源工位未设置班次小时 / When 保存设备标准小时产能 / Then 日/班产能按 10.5 计算，不因班次小时为空失败。

READONLY: docs/powershell-memory.md -> PASS，已按 UTF-8 门禁执行 PowerShell 读取。
READONLY: 相关源码定位 -> PASS，命中 `MesProScheduleOrderServiceImpl#requireShiftHours`、`MesProRouteScheduleConfigServiceImpl#resolveProcessShiftHours`、`MesProRouteResourceServiceImpl#saveMachineResource` 和 `MesProRouteProcessController#resolveProcessShiftHours`。
RED: mvn -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteResourceServiceImplTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderNoDefaultConfigContractTest" test -> FAIL，路线配置、资源保存、排产入池仍因缺少班次小时报错或缺少 `DEFAULT_SHIFT_HOURS` 契约。
IMPLEMENT: 默认班次小时 -> PASS，新增明确业务默认 `10.5`，用于排产入池资源快照、路线排产配置返回、资源产能保存和路线工序资源展示；人员数量、排产策略和资源缺失仍保持 fail-fast。
GREEN: mvn -pl yudao-module-mes clean test "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteResourceServiceImplTest,MesProScheduleOrderServiceImplTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderNoDefaultConfigContractTest" -> PASS，43 tests, 0 failures, 0 errors。
RESULT: 排产 1 班 X 工时未设置时后端统一使用 10.5，不再在入池快照、路线配置、资源保存或工序资源展示中留下空班次小时。
CLEANUP-PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260702-schedule-shift-hours-default-10h5 --mode preview -> PASS，keep task.md/execution-log.md，delete none，blocked none。
BLOCKER: git-commit -> 后端仓已有本任务开始前 staged 改动，包含 `ErrorCodeConstants.java`、`MesProScheduleOrderServiceImpl.java`、`MesProScheduleOrderAdmissionDiffServiceTest.java`、`MesProScheduleOrderServiceImplTest.java`；其中两份文件与本任务修改重叠。为遵守只提交当前任务改动的规则，本轮不提交，避免把其它任务改动带入本次 commit。
RESOLVED: git-commit-preflight -> PASS，重新检查 `ruoyi-vue-pro` 暂存区为空、`yudao-ui-admin-vue3` 无待提交改动，可只提交后端排产班次小时默认 10.5 相关文件。
