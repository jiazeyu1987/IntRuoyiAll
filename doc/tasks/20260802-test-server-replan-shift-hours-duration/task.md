# 测试服重排班时不影响完成时间修复

## Task Goal

分析并修复测试服务器中“排产班时设置为 7 小时或 14 小时后，手动重排计划完成时间没有明显变化”的问题，确保重排计算真正使用用户设置的班时。

## Milestones

- [x] 建立任务审计目录，记录目标、BDD/TDD 和验证口径。
- [x] 复现/确认测试服 7 小时 / 14 小时重排完成时间不变化的静态数据特征。
- [x] 定位班时配置保存、快照读取和重排计算链路。
- [x] 增加回归测试用例并实施最小正式修复。
- [x] 绑定测试服 `ROUTE-XLSX-00002` 路线工序工作站并通过真实后端接口执行重排。
- [ ] 验证本地回归和测试服 7 小时 / 14 小时真实页面重排结果。

## Expected Verification

- RED：同一测试服排产工单在只改班时 7 / 14 的情况下，重排预览或应用的计划完成时间没有随班时变化。
- GREEN：修复后同一排产样本的有效班时参与持续时间计算，7 小时班时相对 14 小时班时计划跨度应显著变长。
- REGRESSION：真实页面保存班时后再重排，后端预览/应用结果和列表计划完成时间一致变化。

## Current Status

test_server_replan_after_binding_verified

用户再次要求绑定测试服当前 `ROUTE-XLSX-00002` 路线工序并执行重排。2026-08-02 21:10 只读复核显示该路线当前 26 道工序又全部处于未绑定工作站状态，且每道工序均可按相同 `process_id` 唯一匹配到一个有效工作站；候选工作站班时均为 `15.00` 小时。本轮仍不发布代码，写入范围限定为芋道源码租户 `tenant_id=1`、`ROUTE-XLSX-00002` 的 26 条未绑定路线工序。2026-08-02 21:13 已完成绑定并复核 `null_ws_count=0`、`distinct_ws_count=26`、`invalid_binding=0`；2026-08-02 21:26 已通过真实后端 `replan/preview` + `replan/apply` 执行重排，6 个目标排产工单完成时间均已提前，排产工单工序班时快照已刷新为 `15.00`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少正式班时来源时必须暴露，不用默认固定班时伪造成功。
- `是否从根因和长期维护角度解决`：是。将从保存、快照、计算三个环节定位根因。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 重排计算必须使用真实配置，不得用固定常量或旧快照覆盖用户刚保存的班时。
- 测试服验证必须通过真实页面或真实接口路径，不用 API-only 代替最终行为验收。
- 涉及 SQL 前必须核对 schema 和目标租户，只读/写入结果均需记录证据。
- 测试服数据写入前必须先备份待更新行，使用事务内精确行数断言；`ROUTE-XLSX-00001` 无可匹配工作站，不纳入本次绑定。
- 本轮重复绑定前必须重新核对当前测试服真实数据，不复用历史“已绑定”结论；若候选工作站不是 26 个唯一匹配或班时缺失，则停止写入。

## Static Analysis Result

- 测试服问题工艺路线存在无工作站绑定的 `FINITE_HOURLY` / `MANUAL_OVERRIDE` 工序；这类工序不会进入工作站产能指标链路。
- 原重排刷新快照时，如果工序没有绑定工作站，会继续读取旧 `mes_pro_schedule_order_process.shift_hours`；测试服旧快照为 `10.5` 小时，因此用户保存 `7` 或 `14` 小时后，重排仍可能按旧快照计算。
- 修复后，无工作站的手工/小时产能工序在刷新最新发布路线配置时读取 `workstationMapper.selectListForShiftHours()` 的统一工作台班时；若班时缺失、非正数或各工作站不统一，则抛出 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`，不再静默沿用旧快照。
- 静态链路确认：`applyLatestPublishedCapacitySnapshot()` 会把新 `shiftHours` / `shiftCapacityTotal` 写入工序快照；`CapacityWindowAllocator.resolveRouteProcessDailyWindowMinutes()` 和 `SchedulePlanner` 会用刷新后的 `shiftHours` / `shiftCapacityTotal` 计算日窗口和日产能。

## Test Server Data Repair Result

- 目标租户：`tenant_id=1`，租户名 `芋道源码`。
- 目标路线：`route_id=900026`，`ROUTE-XLSX-00002`，产品 `棘突球囊扩张导管`。
- 已备份路线工序到 `zz_codex_backup_route_process_bind_20260802`，共 26 行。
- 已将 26 条 `mes_pro_route_process` 按相同 `process_id` 绑定到唯一匹配的启用工作站；绑定后路线工序 `null_workstation_count=0`，`distinct_workstation_count=26`。
- 当前测试服该路线 26 个工作站 `shift_hours` 均为 `10.50`，因此本次重排只能证明绑定资源链路生效，不能证明 7 小时 / 14 小时差异。

## Test Server Replan Result

- 重排接口：`POST /admin-api/mes/pro/auto-schedule/replan/preview` 后携带 `calendarContextToken` 调用 `POST /admin-api/mes/pro/auto-schedule/replan/apply`。
- 请求范围：排产工单 `127,128,129,130,131,136`，起始时间 `2026-08-03T00:00:00`，产能基准 `PLANNED`，保留手工/锁定任务。
- 19:32 历史应用结果：`applied=true`，生成任务 640 个，删除旧任务 472 个，保留任务 4 个，阻塞问题 0 个，缺料提示 164 个。
- 21:26 本轮应用结果：`applied=true`，生成任务 373 个，删除旧任务 472 个，保留任务 4 个，阻塞问题 0 个，预警 169 个。
- 数据库复核：6 个目标排产工单计划开始/完成时间已更新；`mes_pro_schedule_order_process.shift_hours` 156 行已全部刷新为 `15.00`；`mes_pro_task_schedule_ext` 关联任务 377 个，任务工作站 `null_ws_count=0`。
- 注意：`mes_pro_schedule_order_process` 的历史工作站快照仍存在空/旧工作站字段，当前测试服已部署代码没有把路线工作站绑定回写到该快照表；真实排产任务已使用有效工作站，计划完成时间已变化。
