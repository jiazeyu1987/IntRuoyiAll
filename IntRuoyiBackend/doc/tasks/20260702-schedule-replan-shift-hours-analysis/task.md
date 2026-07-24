# 20260702 排产重排班时不影响计划完成日期修复

## 任务目标

分析并修复用户反馈：在排产工作台更换班时后，理论产能发生变化，但点击手动重排后，排产工单的计划完成日期没有变化。

## 经验门禁

- PowerShell / Windows shell / 中文编码陷阱：已先读取 `docs/powershell-memory.md`，后续中文文件读取使用 `Get-Content -Encoding utf8` 或 `python -X utf8`。
- 本轮用户明确要求真实 E2E；范围限定为本机 `http://localhost:8081` 前端与 `48081` 后端，使用测试租户 `测试租户/aoteman`，不访问测试服/正式服。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。缺少启用产线或班次配置时明确失败，不静默降级。
- 是否从根因和长期维护角度解决：是。工作台班时保存同步到手动重排实际读取的排程日历班次窗口和未来产能计划。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 工作台班时变更应影响手动重排计划完成日期 -> Given 排产工作台修改班时 / When 对相关排产工单执行手动重排 / Then 重排应使用最新可用产能并回写 `plannedEndTime`。

## 里程碑

1. 建立任务文档并读取经验门禁。状态：completed。
2. 追踪前端手动重排入口和计划完成字段来源。状态：completed。
3. 追踪后端重排应用、排产工单计划字段回写、班时保存和产能读取链路。状态：completed。
4. 输出根因、影响范围和后续修复建议。状态：completed。
5. 修复工作台班时保存与排程日历产能同步链路。状态：completed。
6. 补充回归测试并记录 RED/GREEN 证据。状态：completed。
7. 执行本机真实 E2E 验证班时变更后手动重排会改变计划完成时间。状态：completed。

## 预期验证

- 源码证据能证明排产工单列表“计划完成”来自 `plannedEndTime`。
- 源码证据能证明手动重排会执行 `replanApply -> applyInternal -> syncScheduleOrderPlanFields` 并回写 `plannedEndTime`。
- 源码证据能证明工作台保存“班时”原先只更新工作站 `shiftHours`，而手动重排的计划产能读取来自排程日历班次和 `mes_pro_capacity_plan.capacity_minutes`。
- 回归测试证明保存班时会调用排程日历刷新，并把 10.50 小时同步为一班结束时间 `18:30` 和未来产能计划 `630` 分钟。
- E2E 证明测试租户真实登录后，通过页面修改班时并执行手动重排，排产工单 `plannedEndTime` 会发生变化。

## 当前状态

- 状态：completed。
- 根因：工作台“班时”保存链路与手动重排产能链路不是同一数据源；工作台保存只更新工作站班时，未同步改排程日历班次时间或已生成的产能计划分钟数，因此手动重排仍按旧排程日历产能窗口计算，`plannedEndTime` 不变。
- 修复：`saveShiftHoursSetting` 事务内同步刷新排程日历，按启用且绑定排班计划的产线更新首个班次结束时间，并从当前模拟日期起刷新对应产线/班次的已启用产能计划分钟数。
- 验证：目标单测 `MesProSchedulerWorkbenchServiceImplTest#saveShiftHoursSetting_shouldUpdateAllWorkstationsAndReturnUnifiedSetting` 与 `MesProScheduleCalendarServiceImplTest#refreshPlanCapacityForShiftHours_shouldUpdatePrimaryShiftWindowAndFutureCapacityPlans` 已通过；新增启动修复回归 `MesProScheduleCalendarServiceImplTest#circularReferenceProneCollaborators_shouldUseLazyInjection` 已通过；本机真实 E2E 已通过，工单 `SMART-SCHED-20260630-RERUN5-MO` 在班时 `7.5 -> 9.5` 后执行手动重排，`plannedEndTime` 从 `1783562460000` 变为 `1783571280000`，预览生成任务 24 个、阻断问题 0 个，并恢复班时到本轮开始值 `7.5`。

## Current Status

completed
