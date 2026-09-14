# Bug Regression Evidence

## Bug

测试服务器排产班时设置为 7 小时或 14 小时后再重排，计划完成时间没有明显变化。

## Expected

同一排产范围、同一产能口径下，班时应参与任务持续时间和跨日排程计算；7 小时班时相对 14 小时班时应让计划完成时间显著后移。

## Reproduction

- RED: 静态数据链路复现，测试服问题路线的工序未绑定工作站，历史排产工序快照保留旧 `shift_hours=10.5`；用户在工作台保存 `7` 或 `14` 小时后，原重排刷新逻辑仍可能沿用该旧快照。

## Root Cause

重排入口会先调用 `refreshScheduleOrderProcessesFromRouteConfig()` 刷新排产工序的最新路线配置。原逻辑在工序绑定工作站时读取工作站 `shiftHours`，但对无工作站绑定的 `FINITE_HOURLY` / `MANUAL_OVERRIDE` 工序，会从旧 `MesProScheduleOrderProcessDO.shiftHours` 取值。这导致测试服这种“路线工序未绑定工作站”的数据下，工作台保存后的 7 小时或 14 小时没有进入重排工序快照，计划完成时间仍按旧 10.5 小时计算。

## GREEN:

- Code: `MesProAutoScheduleServiceImpl` 已改为对无工作站的手工/小时产能工序读取当前统一工作台班时；缺少统一有效班时时抛出 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`，不再静默沿用旧快照。
- Test: `MesProAutoScheduleServiceImplTest` 已加入回归覆盖：旧快照 10.5 小时在工作台班时 7 小时时应刷新为 7 小时；工作台班时缺失时必须失败。

## Verification

- Static: `git diff --check` 已通过，当前改动无空白错误。
- Static: 调用链确认 `applyLatestPublishedCapacitySnapshot()` 写入刷新后的 `shiftHours` / `shiftCapacityTotal`；`CapacityWindowAllocator` 和 `SchedulePlanner` 后续用这两个字段计算日历窗口、日产能和任务持续时间。
- Not run to completion: 本地 Maven 目标 JUnit 因 Windows 构建卡住未取得明确 PASS；用户随后要求不发布到服务器、仅做静态分析，因此测试服发布和真实页面回归未执行。

## Blockers

- 用户要求本轮静态分析-only，不能把测试服行为标记为已验证。
- 本地 Maven 环境存在构建卡住/残留 `target` 产物问题，后续若要放行发布需先跑出明确目标 JUnit PASS。
