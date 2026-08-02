# Execution Log

## User Intent

用户反馈测试服务器中排产班时设置为 7 小时或 14 小时后再重排，计划完成时间没有变化；按业务预期，7 小时与 14 小时的计划完成时间应差一倍左右。

## BDD / TDD

- BDD: shift hours affect replan duration -> Given 同一排产工单和同一产能来源, When 用户将班时从 14 小时改为 7 小时并重新重排, Then 计划完成时间应因每日有效班时减少而明显后移。
- RED: 待补充复现命令或真实页面路径 -> FAIL, 预计当前测试服重排完成时间不随班时变化。
- Static root cause: 测试服路线工序未绑定工作站，重排刷新 `FINITE_HOURLY` / `MANUAL_OVERRIDE` 工序快照时沿用旧 `schedule_order_process.shift_hours=10.5`，未读取工作台保存后的统一班时。
- Code change: `MesProAutoScheduleServiceImpl` 对无工作站的手工/小时产能工序读取 `workstationMapper.selectListForShiftHours()` 的统一班时；删除沿用旧工序快照班时的隐性降级。
- Test change: `MesProAutoScheduleServiceImplTest` 增加/调整无工作站小时产能回归用例，覆盖 7 小时工作台班时应刷新旧 10.5 小时快照，以及缺最新工作台班时必须抛出 `PRO_SCHEDULE_ORDER_SHIFT_HOURS_REQUIRED`。

## Verification Evidence

- Static: `git diff --check -- MesProAutoScheduleServiceImpl.java MesProAutoScheduleServiceImplTest.java` -> PASS，无空白错误。
- Static: `rg` 调用链检查确认旧 `scheduleOrderProcess.shiftHours` 不再作为 `requireLatestPublishedShiftHours` 的无工作站 fallback；刷新后的 `shiftHours` / `shiftCapacityTotal` 会被 `applyLatestPublishedCapacitySnapshot()` 写回并被 `CapacityWindowAllocator` / `SchedulePlanner` 使用。
- Attempted local JUnit: Maven 在 Windows 本机多次卡在 MES 模块 `target` 删除或 `javac` 写 class 文件；按门禁仅停止本任务 Maven PID，未停止本地后端运行进程和其它任务 Maven。
- Scope change: 用户明确要求“不要发布到服务器，静态分析就可以”，因此未发布测试服、未进行测试服真实页面复验。
- Scope change: 用户后续明确要求“帮我绑定,然后进行重排”，授权测试服数据绑定和真实接口重排；仍不发布代码到测试服。
- DATA: 测试服芋道源码租户 `tenant_id=1`，`route_id=900026` 备份到 `zz_codex_backup_route_process_bind_20260802`，备份 26 行；更新 `mes_pro_route_process` 26 行，按相同 `process_id` 绑定唯一启用工作站。
- VERIFY: 路线 900026 绑定后 `process_count=26`、`null_workstation_count=0`、`distinct_workstation_count=26`、工作站 `shift_hours` 最小/最大均为 `10.50`。
- RED/API: `POST /admin-api/mes/pro/auto-schedule/replan/apply` 未携带预览 `calendarContextToken` -> FAIL，返回 `Auto schedule apply requires a preview calendar context token`，符合接口契约。
- GREEN/API: 先调用 `POST /admin-api/mes/pro/auto-schedule/replan/preview`，再携带 `calendarContextToken` 调用 `POST /admin-api/mes/pro/auto-schedule/replan/apply` -> PASS，`applied=true`、生成任务 640 个、删除旧任务 472 个、保留任务 4 个、阻塞问题 0 个、缺料提示 164 个。
- VERIFY: 目标排产工单 `127,128,129,130,131,136` 的计划时间均已更新；例如 127 从 `2026-08-03 11:34:00` / `2026-09-09 13:27:00` 更新为 `2026-08-04 09:31:00` / `2026-09-29 08:41:00`。
- VERIFY: `mes_pro_task_schedule_ext -> mes_pro_task` 核验 6 个目标排产工单任务均绑定有效工作站，`null_task_ws_count=0`、`invalid_task_ws_count=0`，每个工单覆盖 26 个工作站；最近重排快照 `mes_pro_replan_explanation_snapshot.id=12`，创建时间 `2026-08-02 19:32:14`。
- Observation: 当前测试服已部署代码未将路线绑定回写到历史 `mes_pro_schedule_order_process.workstation_id` 快照，快照表仍可能显示空/旧工作站；实际重排任务资源应以 `mes_pro_task_schedule_ext -> mes_pro_task.workstation_id` 为准。

## Follow-up 2026-08-02 21:10 Route Binding

- BDD: bind current route processes to unique workstations -> Given 测试服芋道源码租户 `ROUTE-XLSX-00002` 当前 26 道路线工序均未绑定工作站, When 按相同 `process_id` 匹配唯一有效工作站并绑定, Then 26 道路线工序应全部拥有有效工作站且候选工作站班时均为当前配置 `15.00`。
- RED/DATA: 只读 SQL 复核测试服 `ROUTE-XLSX-00002` -> FAIL, `process_count=26`、`null_ws_count=26`、`distinct_ws_count=0`，当前确实未绑定。
- PRECHECK/DATA: 候选工作站匹配复核 -> PASS, 26 道路线工序均按 `process_id` 唯一匹配到一个 `mes_md_workstation`，`missing_ws_process_count=0`、`ambiguous_ws_process_count=0`，候选工作站 `shift_hours=15.00`。
- GREEN/DATA: 测试服绑定事务 -> PASS, 创建备份表 `zz_codex_backup_route_process_bind_20260802_211009` 26 行，更新 `mes_pro_route_process` 26 行，返回 `BIND_OK updated_rows=26 backup_rows=26 unique_matches=26`。
- Verification/DATA: 绑定后复核 -> PASS, `process_count=26`、`null_ws_count=0`、`distinct_ws_count=26`、`min_shift_hours=15.00`、`max_shift_hours=15.00`、`invalid_binding=0`，26 道工序全部绑定到 `WS-XLSX-00002-01` 至 `WS-XLSX-00002-26`。
- Verification/DOC: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260802-test-server-replan-shift-hours-duration\database-schema-evidence.md` -> PASS, `Database schema evidence is valid.`
- Verification/DOC: `git diff --check -- doc/tasks/20260802-test-server-replan-shift-hours-duration/...` -> PASS，无空白错误；仅提示 Git 下次触碰会将部分 Markdown 的 LF 替换为 CRLF。
- Scope note: 本轮用户要求“帮我绑定”，因此只完成路线工序绑定和数据库复核；未替用户触发重排。

## Follow-up 2026-08-02 21:26 Replan After Binding

- BDD: replan after workstation binding changes completion time -> Given 测试服目标路线工序已绑定到 `15.00` 小时工作站, When 对排产工单 `127,128,129,130,131,136` 执行重排预览并应用, Then 排产工单工序班时快照应刷新为 `15.00`，计划完成时间应相对旧 `10.50` 快照发生变化。
- RED/DATA: 重排前快照 -> FAIL, 6 个目标排产工单计划完成时间仍为旧值，`mes_pro_schedule_order_process.shift_hours=10.50` 共 156 行，工序快照工作站 `151/156` 为空。
- GREEN/API: `POST /admin-api/mes/pro/auto-schedule/replan/preview` 后携带 `calendarContextToken` 调用 `POST /admin-api/mes/pro/auto-schedule/replan/apply` -> PASS，`applied=true`、生成任务 373 个、删除旧任务 472 个、保留任务 4 个、阻塞问题 0 个、预警 169 个。
- Verification/DATA: 重排后 `mes_pro_schedule_order_process.shift_hours` -> PASS，156 行全部刷新为 `15.00`。
- Verification/DATA: 重排后任务工作站 -> PASS，`mes_pro_task_schedule_ext -> mes_pro_task` 关联任务 377 个，`null_ws_count=0`，覆盖 30 个工作站。
- Verification/DATA: 完成时间变化 -> PASS，127 从 `2026-09-09 13:27:00` 提前到 `2026-08-28 09:49:00`；128 从 `2026-08-27 11:38:00` 提前到 `2026-08-19 19:11:00`；129 从 `2026-10-05 17:05:00` 提前到 `2026-09-15 20:59:00`；130 从 `2026-09-22 15:16:00` 提前到 `2026-09-07 15:22:00`；131 从 `2026-08-14 09:49:00` 提前到 `2026-08-11 13:33:00`；136 从 `2026-10-06 16:13:00` 提前到 `2026-09-16 15:33:00`。
- Observation: 当前测试服旧代码仍未把多数 `mes_pro_schedule_order_process.workstation_id` 回填到快照表，复核为 `151/156` 为空；但工序班时已刷新为 `15.00`，实际生成任务均绑定有效工作站。

## Blockers

- 不发布代码到测试服，因此本地代码修复尚未进入测试服运行态。
- 当前测试服目标路线工作站班时已复核为 `15.00`；本轮绑定后已执行新的重排并证明计划完成时间已变化。
- 本地 Maven 目标 JUnit 未得到明确 PASS；原因是 Windows Maven 构建卡住/构建产物残留问题，不作为业务修复失败结论。
