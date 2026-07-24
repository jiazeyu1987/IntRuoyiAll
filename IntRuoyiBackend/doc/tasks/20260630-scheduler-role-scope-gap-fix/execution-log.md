# Execution Log：排产员正式角色范围 SQL 漏项修复（后端/SQL）

- `2026-06-30 任务创建`：建立后端修复任务文档，目标是把分析确认的漏项固化到正式 SQL 和 SQL 合同测试。
- `BDD: 排产员角色范围保留生产排产查询权限 -> Given 生产排产页面菜单 5540 已属于排产员正式职责 / When 应用最新 SQL / Then scheduler 白名单同步包含 5541 以满足 get/page/gantt-list 查询鉴权。`
- `BDD: 排产员角色范围保留排产工单调整权限 -> Given 排产工单冻结解冻调整与同步进度入口面向排产员开放 / When 应用最新 SQL / Then scheduler 白名单同步包含 5583 且 workshop_director/team_leader 不新增该权限。`
- GREEN: sql-contract-root-cause -> PASS，已确认 `MesProTaskController` 的 `get/page/gantt-list` 依赖 `mes:pro-task:query`，`MesProScheduleOrderController` 的冻结、解冻、调整、同步进度依赖 `mes:pro-schedule-order:update`，而当前正式 SQL 的 `scheduler` 白名单都未保留。
- RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q -> FAIL, 预期失败原因：新增断言后，scheduler 白名单缺失 5541 和 5583。
- CHANGE: script/tests/test_mes_smart_scheduling_role_scope_sql.py，新增排产员 `5541/5583` 必须存在、且 `workshop_director/team_leader` 不得意外获得这两个权限的合同断言。
- CHANGE: sql/mysql/20260629_mes_smart_scheduling_role_scope.sql，在 `scheduler` 白名单补入 `5541` 与 `5583`。
- GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q -> PASS, 16 passed。
