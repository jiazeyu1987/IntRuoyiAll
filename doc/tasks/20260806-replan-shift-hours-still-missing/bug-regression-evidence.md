# Bug Regression Evidence

## Bug Summary

点击重排仍提示 `排产资源缺少班次小时配置，routeProcessId=926632, workstationId=980008`。

## Expected Behavior

当前工艺路线工作站 `shift_hours` 为空或非正数时，手动重排按默认 `10.5` 小时计算；缺班次小时本身不是 blocker。

## Reproduction

- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

- 未绑定工作站的重排路径调用 `resolveUnifiedWorkbenchShiftHoursOrNull`，用全工作站班次小时推导统一值；当已有工作站的班次小时值不一致时，旧逻辑抛 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`，导致 `workstationId=980008` 这类场景仍被误判为缺班次小时配置。

## Regression Test

- `MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer`

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ServiceException ... 排产资源缺少班次小时配置，routeProcessId=3, workstationId=980008`。

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 目标测试断言 `shiftHours=10.5`、`hourlyCapacityTotal=25.714286`、`shiftCapacityTotal=270.0000030`。

## Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing+refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenAnyWorkstationMissing+refreshScheduleOrderProcessesFromRouteConfig_shouldDefaultWorkbenchShiftHoursWhenWorkbenchValuesDiffer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。

## Risk And Regression Scope

- 手动重排当前路线资源计算。
- 排产工序 `shiftHours`、`shiftCapacity`、`hourlyCapacity` 写回。

## Blockers And Follow-Up

- None.
