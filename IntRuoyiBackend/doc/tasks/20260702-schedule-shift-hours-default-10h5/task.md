# 任务：排产班次小时默认 10.5

- Task ID: `20260702-schedule-shift-hours-default-10h5`
- Created: 2026-07-02
- Current Status: completed

## 任务目标

修复用户反馈：排产 1 班 = X 工时，若 X 未设置，则 X 必须默认 10.5，不能在排产快照、路线配置或资源产能中为空。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令、中文输出和文档读写显式 UTF-8，不使用 `&&`。
- 命中 no-fallback 特别说明：本轮用户明确要求“X 未设置时默认 10.5”，因此仅在班次小时未设置或非正数时使用明确业务默认值 `10.5`；不吞其它资源、产能、人员数量或路线配置缺失错误。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：是。用户明确要求班次小时缺失时使用业务默认 `10.5`；触发条件仅限 `shiftHours == null` 或 `shiftHours <= 0`；风险是历史缺配置数据会按 10.5 入池，需要后续通过页面/数据治理补齐正式配置；移除策略为所有工位班次小时完成显式配置后删除默认常量和相关契约。
- 是否从根因和长期维护角度解决：是。统一后端排产班次小时解析入口，避免页面返回空、入池快照为空或保存产能时报错。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 入池工位缺班次小时默认 10.5 -> Given 排产路线工序绑定的工位未设置班次小时 / When 从生产工单生成排产工单 / Then 工序快照 shiftHours 为 10.5 且 shiftCapacityTotal 按 10.5 计算，不为空。`
- `BDD: 路线排产配置缺班次小时默认 10.5 -> Given 工艺路线配置存在有限小时产能但工位未设置班次小时 / When 查询路线排产配置 / Then 返回 shiftHours=10.5 且 standardShiftCapacity 按 10.5 计算。`
- `BDD: 资源产能保存缺班次小时默认 10.5 -> Given 设备资源工位未设置班次小时 / When 保存设备标准小时产能 / Then 日/班产能按 10.5 计算，不因班次小时为空失败。`

## 里程碑

1. 建立任务文档并记录经验门禁。状态：completed。
2. 补充 RED 回归测试覆盖缺班次小时默认 10.5。状态：completed。
3. 实现后端统一默认班次小时逻辑。状态：completed。
4. 运行目标测试和回归验证。状态：completed。
5. 记录最终验证并提交本次直接改动。状态：completed。

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteResourceServiceImplTest,MesProScheduleOrderServiceImplTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderNoDefaultConfigContractTest" test`

## 当前状态

- 状态：completed。
- 修复：排产班次小时解析统一改为缺失或非正数时使用明确默认值 `10.5`，覆盖排产入池快照、路线排产配置返回、设备资源产能保存和工序资源展示。
- 保留：人员数量、排产策略、资源配置等其它缺失仍按原业务规则明确失败，不用默认值掩盖。
- 验证：`mvn -pl yudao-module-mes clean test "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteResourceServiceImplTest,MesProScheduleOrderServiceImplTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderNoDefaultConfigContractTest" test` 通过，43 tests。
- 提交：本轮重新确认 `ruoyi-vue-pro` 暂存区为空，前端仓无待提交改动；后端本任务直接改动可单独提交。
