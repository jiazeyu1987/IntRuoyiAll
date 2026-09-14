# Verification Report

## Result

修复已完成：点击重排时，当前工艺路线剩余工序若只缺班次小时，会按用户确认的默认 `10.5` 小时计算，不再因为 `workstationId=980008` 的班次小时为空而报 `排产资源缺少班次小时配置`。其它正式资源缺失仍保持 fail-fast。

## Acceptance

- AC1 PASS：新增回归用例复现 `workstationId=980008` 缺少班次小时的重排路径旧报错。
- AC2 PASS：手动重排刷新当前路线工序时，缺班次小时默认 `10.5`。
- AC3 PASS：当前路线排产配置中的小时产能继续作为产能来源，未回退到旧报工或旧任务资源快照。
- AC4 PASS：未放宽工作站缺失、产能缺失等正式资源校验。

## Verification

- RED: `workdir=E:\IntRuoyi\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`，错误参数包含 `routeProcessId=3, workstationId=980008`。
- GREEN: `workdir=D:\IntRuoyiWorktree\verify-replan-shift-hours-default-20260806\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `workdir=D:\IntRuoyiWorktree\verify-replan-shift-hours-default-20260806\IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing,MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing,MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseWorkbenchShiftHoursForUnboundFiniteHourlyConfig" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- VALIDATOR: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-replan-shift-hours-default-regression/bug-regression-evidence.md` -> PASS。
- VALIDATOR: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260806-replan-shift-hours-default-regression/backend-api-evidence.md` -> PASS。
- DIFF CHECK: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImplTest.java doc/tasks/20260806-replan-shift-hours-default-regression` -> PASS，仅 CRLF working-copy 提示。
- CLEANUP: `task_closeout.py --task-id 20260806-replan-shift-hours-default-regression --mode preview` -> ready，blocked none，warnings none。
- CLEANUP: `task_closeout.py --task-id 20260806-replan-shift-hours-default-regression --mode apply` -> applied，deleted only temporary evidence files。
- EXPERIENCE: `rg -n "排产资源缺少班次小时配置|defaultShiftHoursWhenMissing|默认10.5小时|缺班次小时" docs\experience-index.md docs\backend-development.md` -> PASS。

## Notes

- Main workspace Maven target was busy because of unrelated parallel Maven writes, so GREEN was run in a detached verification worktree with the exact task patch applied.
- The detached verification worktree did not start frontend/backend services and did not reserve runtime ports.
- The detached verification worktree `D:\IntRuoyiWorktree\verify-replan-shift-hours-default-20260806` was removed after PASS; remaining worktree list no longer contains it.
- Project experience was merged into existing backend development rules and experience index; no new long-term memory file was created.
- Implementation commit: `c76f3fec0` (`修复重排班次小时默认值回归`).

## Blockers

- No blocker remains.
