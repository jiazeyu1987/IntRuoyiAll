# Execution Log：排产员按钮权限正式角色范围修复

- `2026-06-30 任务创建`：建立后端角色范围 SQL 修复任务文档，目标是把用户明确要求的 MES 按钮权限正式收口到 `排产员`。
- `BDD: 排产员保留生产工单按钮权限 -> Given 排产员已拥有生产工单页面入口 / When 应用正式角色范围 SQL / Then scheduler 白名单包含 5532=create、5535=export、900200=create-erp。`
- `BDD: 排产员保留生产报工按钮权限 -> Given 排产员已拥有生产报工页面入口 / When 应用正式角色范围 SQL / Then scheduler 白名单包含 5552=create、5555=export、5969=approve。`
- `BDD: 角色范围修复不顺带放大其他角色 -> Given 本次仅修复排产员按钮权限 / When 应用正式角色范围 SQL / Then workshop_director 与 team_leader 不额外获得 5532、5535、5555、5969、900200。`
- `BDD: 缺失菜单基线时 SQL 必须 fail-fast -> Given 角色范围 SQL 依赖上述按钮菜单存在 / When 某个目标菜单缺失 / Then SQL 应通过基线检查直接失败，而不是静默跳过授权。`
- GREEN: experience-preflight -> PASS, 已按门禁读取 docs\experience-index.md 与 docs\powershell-memory.md，允许当前本机最小权限 SQL 写入与只读回查。
- GREEN: role-scan -> PASS, 本机只读回查确认这些权限已有现成角色承载：super_admin 全量拥有；tenant_id=122 的 mes_scheduler 已拥有 5532/5552/900200；mes_team_leader 已拥有 5552。
- GREEN: root-cause-check -> PASS, 正式 SQL 当前已包含 5552，但仍缺 5532/5535/5555/5969/900200；同时基线菜单检查未覆盖这些新依赖菜单，存在静默漏授权风险。
- RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q -> FAIL, 新增断言后确认旧版 scheduler 白名单缺失 5532/5535/5552/5555/5969/900200，且菜单基线检查未覆盖新依赖菜单。
- CHANGE: script/tests/test_mes_smart_scheduling_role_scope_sql.py，新增排产员工单/报工按钮权限合同断言，以及菜单基线检查断言。
- CHANGE: sql/mysql/20260629_mes_smart_scheduling_role_scope.sql，在 scheduler 白名单补入 5532/5535/5552/5555/5969/900200，并把菜单基线检查扩到这些依赖菜单。
- GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q -> PASS, 19 passed。
- GREEN: apply-local-role-scope-sql -> PASS, 已将更新后的 20260629_mes_smart_scheduling_role_scope.sql 应用到本机运行库 int-ruoyi-mysql/ruoyi-vue-pro。
- GREEN: local-role-menu-verify -> PASS, tenant_id=1 的 mes_scheduler/排产员 当前已拥有 5532/5535/5552/5555/5969/900200。
