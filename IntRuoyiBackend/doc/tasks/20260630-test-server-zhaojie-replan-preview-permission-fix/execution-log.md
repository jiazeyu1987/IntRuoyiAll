# Execution Log：测试服 zhaojie 预览重排无权限修复（后端/SQL）

- `2026-06-30 任务创建`：建立后端任务文档，目标是补齐智能排产角色范围 SQL 中排产员缺失的自动排产重排权限。
- `BDD: 排产员角色范围保留自动排产权限 -> Given scheduler 正式角色范围由 20260629 角色收敛 SQL 统一维护 / When 应用最新 SQL / Then scheduler 白名单保留 900180/900181/900182。`
- `BDD: 车间主任与班组长范围不被误扩大 -> Given 本次仅修复排产员缺失的 replan 权限 / When 应用最新 SQL / Then workshop_director/team_leader 白名单保持原边界，不被顺带放大。`
- `GREEN: sql-contract-root-cause -> PASS`，已确认 `MesProAutoScheduleController` 的 `replan/preview` 与 `replan/apply` 均要求 `mes:pro-auto-schedule:replan`，而当前 `20260629_mes_smart_scheduling_role_scope.sql` 的 `scheduler` 白名单缺少 `900180/900181/900182`。
- `RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q -> FAIL`，新增断言后确认 `scheduler` 白名单缺失 `900180`。
- `CHANGE: sql/mysql/20260629_mes_smart_scheduling_role_scope.sql`，已在 `scheduler` 白名单补入 `900180/900181/900182`。
- `GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q -> PASS`，`14 passed`。
