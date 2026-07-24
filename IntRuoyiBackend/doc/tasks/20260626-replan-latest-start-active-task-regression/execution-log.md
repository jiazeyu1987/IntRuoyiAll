# 执行日志：重排预览 latest-start 暂缓误报 ACTIVE_TASK 修复

## 2026-06-26

- 初始化任务：创建独立任务包，聚焦 latest-start 暂缓工单被 ACTIVE_TASK 误报阻断的问题。
- BDD: 多工单重排中 latest-start 暂缓工单不应误报 ACTIVE_TASK -> Given 同一批重排中一张工单可正常排程，另一张工单因计划开工晚于最晚开工时间只保留分析结果 / When 用户执行重排预览 / Then 暂缓工单只暴露 LATEST_START warning，不应再叠加 ACTIVE_TASK blocking。
- BDD: latest-start 单工单空结果发布仍需 fail-fast -> Given 单张工单因计划开工晚于最晚开工时间且预览中没有任何有效任务 / When 用户尝试发布自动排产 / Then 系统仍以 `PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED` 阻断发布，不得因本次修复而放行。
- INVESTIGATION: `Invoke-RestMethod POST http://localhost:48081/admin-api/mes/pro/auto-schedule/replan/preview` with `scheduleOrderIds=[13,47]` -> PASS，真实返回 `881MO090880` 命中 `LATEST_START x1 + ACTIVE_TASK x24`，而 `workOrderAnalyses` 已给出完整 24 道工序分析，说明问题不是排程分析缺失，而是 latest-start 暂缓后又被覆盖校验误升级。
- INVESTIGATION: `docker exec int-ruoyi-mysql mysql ... SELECT ... FROM mes_pro_schedule_order WHERE id IN (13,47);` -> PASS，确认 `881MO090880.latest_start_time = 2026-06-20 09:49:00`，本次预览请求 `startTime = 2026-06-26 16:07:06` 已明显晚于最晚开工时间。
- INVESTIGATION: `MesProAutoScheduleServiceImpl.scheduleTasks(...)` + `validateAttributableProcessActiveTaskCoverage(...)` 代码走查 -> PASS，确认 latest-start 暂缓工单不会写入 `finalSteps`，但后置覆盖校验未识别该状态，仍按 `remainingQuantity` 为全部工序追加 `ACTIVE_TASK` 阻断。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldNotAddActiveTaskBlockingForLatestStartDeferredWorkOrder -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL（修复前），latest-start 暂缓工单仍被附加 `ACTIVE_TASK` 阻断，无法满足“只保留 warning”合同。
- FIX: `MesProAutoScheduleServiceImpl.validateAttributableProcessActiveTaskCoverage(...)` -> PASS，在 `latestStartRejectedPlans` 命中时跳过后置活动任务覆盖校验，避免 latest-start warning 被误升级成 blocking。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldNotAddActiveTaskBlockingForLatestStartDeferredWorkOrder -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleAlgorithmContractTest#previewAndApply_shouldHoldScheduleOrderWhenPlanStartsAfterLatestStart -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，确认 latest-start 单工单空结果发布仍由 `PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED` fail-fast 阻断。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldKeepFinishedTaskAndGenerateNewActiveTaskForRemainingQuantity+replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，确认“存在受保护旧任务且仍有剩余报工量”场景未回退。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，43 个定向/合同回归全部通过。
