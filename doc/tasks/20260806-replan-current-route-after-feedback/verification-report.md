# Verification Report

## Scope

修复手动重排中历史反馈/已完成任务错误提供未来资源约束的问题。历史报工只扣减已完成量和剩余量，剩余未完成任务按当前最新工艺路线重新选择工作站、产线和产能。

## RED

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: FAIL，旧逻辑返回 `受保护任务未绑定工作站`，与用户确认的业务规则冲突。

## GREEN

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: PASS。

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation+replanPreview_shouldNotReserveFeedbackProtectedRouteProcessCapacityWithoutLineKey" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: PASS，2 tests。

```powershell
git diff --check
```

Result: PASS。

## Full-Class Regression

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: BLOCKED by baseline fixture drift。实际结果为 `Tests run: 96, Failures: 0, Errors: 7, Skipped: 0`。完整类回归中的最新发布工艺路线配置用例缺少必要测试夹具，错误集中在 `工艺流程排产配置生产系数必须大于 0` 和 `排产资源缺少班次小时配置`：

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldRefreshDailyCapacityLimitFromLatestPublishedRouteConfigWhenSnapshotCapacityIsStale" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Observed failure: `ServiceException ... 工艺流程排产配置生产系数必须大于 0，routeProcessId=3`。

Examples affected by the same baseline area include:

- `replanPreview_shouldKeepLatestPublishedUnboundRouteProcessWhenCurrentRouteIsBoundToWorkstation`
- `replanPreview_shouldKeepSearchingRouteProcessWindowsForLargeOrderAfterPriorRouteProcessCapacityUse`
- `replanPreview_shouldRefreshNightShiftFromRouteConfigWhenScheduleOrderSnapshotIsStale`
- `replanPreview_shouldRefreshDailyCapacityLimitFromLatestPublishedRouteConfigWhenSnapshotCapacityIsStale`
- `refreshScheduleOrderProcessesFromRouteConfig_shouldUseLatestPublishedRouteVersionAndProcessIdentity`
- `replanPreview_shouldUseLatestPublishedUnboundRouteProcessCapacityInsteadOfCurrentProcessWorkstation`
- `replanPreview_shouldExtendRouteProcessVirtualWindowsWhenPriorOrdersConsumeDailyCapacity`

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按保护原因区分进度事实任务和未来资源约束任务。
- 是否存在临时补丁或绕过：否。
