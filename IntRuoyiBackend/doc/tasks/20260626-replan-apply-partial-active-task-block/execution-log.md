# 执行日志：MES 重排 latest-start 工单仍需生成活动任务

## 2026-06-26

- 初始化任务：根据用户反馈“我进行重排了，应该两个工单都有活跃任务才对”，创建本任务并限定在 MES 重排 latest-start 风险工单仍需生成活动任务。
- BDD: 混合重排中命中 latest-start 风险的工单仍应生成活动任务 -> Given 选中范围内两张工单都存在剩余报工量，其中一张计划开工晚于最晚开工时间 / When 用户执行重排预览并应用 / Then 系统仍为两张工单生成活动任务，并仅把 latest-start 工单标记为 LATEST_START warning。
- BDD: latest-start 风险在重排预览中继续只作为 warning 展示 -> Given 某工单计划开工晚于最晚开工时间 / When 用户执行重排预览 / Then 预览继续展示 LATEST_START warning，不额外补报 ACTIVE_TASK blocking。
- BDD: 普通自动排产的 latest-start 零任务阻断合同保持不变 -> Given 非重排自动排产范围内某工单仅因 latest-start 风险导致没有任何有效任务 / When 用户尝试应用自动排产 / Then 系统仍以 PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED 阻断发布。
- BDD: 受保护旧任务 + 剩余量续排场景不回退 -> Given 某工序已有受保护旧任务且 remainingQuantity 仍大于 0 / When 用户执行重排预览并应用 / Then 系统仍保留旧任务并为剩余量生成新的活动任务。
- INVESTIGATION: `mes_pro_schedule_order_operation_log` 最近一次 `REPLAN_APPLY` -> PASS，确认真实发布范围是 `scheduleOrderIds=[13,47]`、`workOrderIds=[903245,903200]`，而不是先前误认为的 `[1,13]`。
- INVESTIGATION: `mes_pro_task_schedule_ext` / `mes_pro_task` -> PASS，确认 `schedule_order_process_id=305` 有活动任务绑定，`schedule_order_process_id=1123` 没有任何活动任务绑定；本次应用重排只给 `903245` 生成了 `923132-923149` 任务。
- INVESTIGATION: `mes_pro_schedule_issue` -> PASS，确认 `work_order_id=903200` 在本次重排时命中 `LATEST_START` warning，并伴随一组 `MATERIAL` warning，但没有任何生成任务。
- INVESTIGATION: `MesProFeedbackImportRecordServiceImpl.getAttributionCandidates(...)` -> PASS，确认反馈归属候选严格依赖 `resolveActiveTaskByScheduleOrderProcessId(...)`；没有活动任务的排产工序不会出现在真实候选里。
- INVESTIGATION: `MesProAutoScheduleServiceImpl.scheduleTasks(...)` -> PASS，确认当前代码对普通自动排产与重排统一使用 `violatesLatestStartConstraint(...)` 分支；命中后直接 `continue`，导致 latest-start 风险工单在重排中被整单跳过。
- INVESTIGATION: `MesProFeedbackImportRecordServiceImpl.getAttributionCandidates(...)` 的单工单显示问题是重排零任务的下游结果，不是候选查询前端/后端筛选错误。
- RED: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanApply_shouldKeepLatestStartRiskOrderAsActiveTaskDuringReplan -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，修复前 latest-start 风险工单不会生成活动任务，`createdTaskIds` 仅覆盖单个工单。
- CHANGE: `MesProAutoScheduleServiceImpl.computeSchedule(...)` -> 增加 `replanMode` 上下文，仅在 `MesProAutoScheduleReplanReqVO` 场景启用“latest-start 记 warning 但继续生成任务”的调度策略。
- CHANGE: `MesProAutoScheduleServiceImpl.scheduleTasks(...)` -> 保留普通自动排产对 `latestStartRejectedPlans` 的拒排语义；重排场景命中 `LATEST_START` 时仅记录 warning，不再 `continue` 跳过整张工单。
- CHANGE: 移除本轮临时加入的 `validateApplyActiveTaskCoverage(computation)` 发布拦截，避免把用户预期的“两个工单都要有活跃任务”错误实现成“直接报错”。
- GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanApply_shouldKeepLatestStartRiskOrderAsActiveTaskDuringReplan -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN: `mvn --% -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldKeepLatestStartRiskOrderSchedulableDuringReplan+replanApply_shouldKeepLatestStartRiskOrderAsActiveTaskDuringReplan+replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity,MesProAutoScheduleAlgorithmContractTest#previewAndApply_shouldHoldScheduleOrderWhenPlanStartsAfterLatestStart" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，确认重排预览/应用与普通自动排产 latest-start 阻断合同同时成立。

## 2026-06-27

- GREEN: experience-preflight -> PASS，本轮仅执行本机后端重启与只读运行态核对；已先读取 `docs/release-backup-restore.md`，确认不涉及服务器发布、备份、恢复或跨环境写入。
- INVESTIGATION: `Get-NetTCPConnection 48081` / `Get-CimInstance Win32_Process` -> PASS，确认当前本机后端仍运行 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-showroom-translate-exec-20260626-210749.jar`，其 jar 时间 `2026-06-26 21:05:57` 早于 `MesProAutoScheduleServiceImpl.java` 修复时间 `2026-06-26 22:23:39`。
- INVESTIGATION: `mes_pro_schedule_order_operation_log` / `mes_pro_task_schedule_ext` / `mes_pro_task` -> PASS，确认用户刚执行的 `2026-06-27 00:45:09` 重排虽已成功落库，但 `createdTaskIds=[923150...923163]` 全部挂在 `workOrderId=903245 / scheduleOrderId=13`；`903200 / scheduleOrderId=47` 仍然没有任何新活动任务绑定，因此归属弹窗继续只显示一个工单。
- CHANGE: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS，重启本机 `48081` 后端到当前源码运行态。
- GREEN: `backend-runtime-control-20260627-005059.jar` / `backend-runtime-control-20260627-005059.out.log` -> PASS，确认新 runtime 已加载修复后源码并重新监听 `48081`；旧 `backend-showroom-translate-exec-20260626-210749.jar` 运行态已替换。
