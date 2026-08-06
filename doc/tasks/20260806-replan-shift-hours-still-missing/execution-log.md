# Execution Log

## User Intent

- 用户反馈：重启到最新后端后，点击重排仍提示 `排产资源缺少班次小时配置，routeProcessId=926632, workstationId=980008`。
- 期望行为：工作站班次小时未配置时，按默认 `10.5` 小时计算；不允许再以缺班次小时阻断手动重排。

## BDD

- BDD: 当前路线工作站缺班次小时默认 10.5 -> Given 当前工艺路线剩余工序绑定可用工作站且工作站 `shift_hours` 为空 / When 点击手动重排刷新排产工序 / Then 后端不抛 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`，并把该工序 `shiftHours` 写为默认 `10.5`。

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `ServiceException ... 排产资源缺少班次小时配置，routeProcessId=3, workstationId=980008` thrown from `MesProAutoScheduleServiceImpl.resolveUnifiedWorkbenchShiftHoursOrNull`.

## GREEN

- GREEN: same command -> PASS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.

## REGRESSION

- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing+refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing+refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`.
- Root cause: 未绑定工作站的手动重排会用所有工作站班次小时推导“统一工作台班次小时”；当工作站班次小时值不一致时，旧逻辑仍抛 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`，没有按默认 `10.5` 规则继续计算。
- Fix: `resolveUnifiedWorkbenchShiftHoursOrNull` 在无法推导单一统一班次小时（存在不一致值）时，返回 `scheduleDefaultCompatibilityPolicy.defaultShiftHoursWhenMissing()`，与缺失班次小时默认规则保持一致。

## Verification

- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260806-replan-shift-hours-still-missing\bug-regression-evidence.md` -> PASS, Bug regression evidence is valid.
- GREEN: `git diff --check -- IntRuoyiBackend\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\schedule\MesProAutoScheduleServiceImpl.java IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\schedule\MesProAutoScheduleServiceImplTest.java doc\tasks\20260806-replan-shift-hours-still-missing` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, `frontend 8081, backend 48081`.
- EXPERIENCE: 更新 `docs/backend-development.md#第三方报工直报正式链路门禁` 与 `docs/experience-index.md`，补充多个当前可用工作站班次小时不一致时也必须按默认 `10.5` 小时计算。
- GIT: 主工作区存在大量并行任务脏改动；本任务提交只选择性暂存排产修复源码、测试、任务记录和本次经验 hunk，未暂存并行任务文件。

## Blockers

- None.
