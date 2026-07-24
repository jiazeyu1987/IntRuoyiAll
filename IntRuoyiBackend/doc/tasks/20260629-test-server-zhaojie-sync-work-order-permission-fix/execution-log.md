# Execution Log：排产员同步工单权限正式修复并落测试服

BDD: 排产员保留同步工单诊断与创建权限 -> Given 排产员拥有排产工单页签 / When 角色范围 SQL 收敛智能排产菜单 / Then 排产员仍保留 5581=query、5582=create、5584=admission-diff、5585=preflight。
BDD: 车间主任保留排产工单最小查询权限 -> Given 车间主任保留排产工单页签 / When 角色范围 SQL 收敛智能排产菜单 / Then 车间主任至少保留 5581=query，避免页面本身不可读。
BDD: 本地与测试服应用同一正式 SQL 后行为一致 -> Given 本地库与测试服库都执行同一份幂等迁移 / When 回查角色菜单绑定 / Then 排产员角色在两端都具备同步工单所需关键菜单绑定。
GREEN: experience-preflight -> PASS，已按门禁读取 PowerShell / 服务器 / 发布恢复文档，允许当前测试服最小真实库修复。
RED: head-role-scope-scheduler-whitelist -> FAIL，旧版 `20260629_mes_smart_scheduling_role_scope.sql` 的 `scheduler` 白名单缺失 `5581`，同组也未保留 `5582/5584/5585`。
GREEN: pytest D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro\\script\\tests\\test_mes_smart_scheduling_role_scope_sql.py -q -> PASS，13 passed。
GREEN: apply-local-role-scope-sql -> PASS，已将更新后的正式 SQL 应用到本地运行库。
GREEN: local-role-menu-verify -> PASS，`排产员(role_id=910233)` 当前拥有 `5581/5582/5584/5585/5590/900170`，未拥有 `900171`。
GREEN: apply-test-server-role-scope-sql -> PASS，已将同一份正式 SQL 应用到测试服务器 `172.30.30.58`。
GREEN: test-server-role-menu-verify -> PASS，`排产员(role_id=910216)` 当前拥有 `5581/5582/5584/5585/5590/900170`，未拥有 `900171`。
