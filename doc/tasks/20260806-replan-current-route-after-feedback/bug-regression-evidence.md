# Bug Regression Evidence

## Bug Summary

手动重排时，已有第三方报工的历史任务会被标记为受保护任务；如果该旧任务没有 `workstationId`，旧逻辑会直接报 `受保护任务未绑定工作站` 并阻断排产。

## Expected Behavior

历史报工/已完成任务只用于计算已完成量和剩余量。点击排产后，剩余未完成部分必须按当前最新工艺路线重新选择工序、工作站、产线和产能。`FEEDBACK`/`FINISHED` 历史任务缺旧工作站或旧产线不应阻断；真正需要校验的是当前工艺路线剩余工序是否绑定可用工作站、产线和产能。

## Reproduction Command

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Root Cause

重排保护逻辑没有区分“进度事实型保护任务”和“未来资源约束型保护任务”。第三方报工产生的 `FEEDBACK` 任务、已完成的 `FINISHED` 任务本应只参与进度扣减，却被拿来补水旧工作站、强制旧产线、占用产线可用性和工序日产能，因此旧任务缺工作站会错误阻断后续剩余量按当前路线重排。

## Regression Test

- Added: `MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation`
- Updated: `MesProAutoScheduleServiceImplTest#replanPreview_shouldNotReserveFeedbackProtectedRouteProcessCapacityWithoutLineKey`

## RED Evidence

RED:

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: FAIL。失败原因符合预期：预览返回阻断问题 `受保护任务未绑定工作站`。

## GREEN Evidence

GREEN:

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: PASS。

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation+replanPreview_shouldNotReserveFeedbackProtectedRouteProcessCapacityWithoutLineKey" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: PASS，2 tests。

## Verification

目标回归已通过，且 `git diff --check` 已通过。完整类回归的既有基线阻断记录在 `verification-report.md`。

## Risk And Regression Scope

- Covered: 第三方报工反馈任务缺旧工作站时，剩余量仍按当前工艺路线资源生成。
- Covered: `FEEDBACK` 保护任务不再占用未来路线工序日产能，避免旧任务容量影响新排产。
- Preserved: `IN_PROGRESS`、`LOCKED`、`MANUAL` 等真实受保护任务仍按既有资源保护语义校验。
- Risk: 完整 `MesProAutoScheduleServiceImplTest` 当前存在基线夹具漂移失败，已单独记录在 `verification-report.md`，不作为本次修复引入。

## Blockers And Follow-Up

- Blocker: full-class 回归受既有最新工艺路线配置测试夹具缺少生产数量系数/班次小时阻断。
- Follow-up: 需要独立修复最新发布工艺路线配置相关测试夹具，使完整类回归重新成为可用门禁。
