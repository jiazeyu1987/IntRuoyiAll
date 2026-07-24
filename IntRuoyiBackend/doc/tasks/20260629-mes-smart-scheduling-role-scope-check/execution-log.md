BDD: 排产员页签权限按目标收敛 -> Given 本机运行库已有智能排产菜单体系与排产员角色 / When 执行权限迁移 / Then 排产员应拥有除璞慧排产外的智能排产子页签。
BDD: 车间主任页签权限按目标收敛 -> Given 本机运行库已有车间主任角色 / When 执行权限迁移 / Then 车间主任应仅拥有排产工单、报工、工艺排产路线及其必要关联菜单。
BDD: 班组长角色自动补齐 -> Given 本机运行库没有独立班组长角色 / When 执行权限迁移 / Then 应创建独立班组长角色并仅授予报工页面及必要按钮权限。
GREEN: previous-task-check -> PASS，最近同仓后端任务 `20260629-mes-work-order-material-demand-warning-clear` 已 COMPLETED。
GREEN: experience-preflight -> PASS，本轮仅写本机运行库角色和菜单绑定，已先完成只读菜单/角色审计并确认最小改动范围。
GREEN: readonly-sql-baseline -> PASS，`20260615_mes_scheduler_workbench_only_summary.sql` 定义了智能排产当前可见页签：`5590/5580/5550/5262/900121/5540/900104`，隐藏页签 `5985`。
RED: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> FAIL，新增三角色权限合同后，SQL 尚未按测试要求声明允许菜单集合。
GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，6 passed。
GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_mes_scheduler_role_smart_scheduling_tab_sql.py -q` -> PASS，5 passed。
GREEN: `Get-Content -Encoding utf8 ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS，本机库已应用角色范围迁移。
GREEN: db-role-readback -> PASS，真实库现存在 `910233/mes_scheduler/排产员`、`910238/mes_workshop_director/车间主任`、`910239/mes_team_leader/班组长`。
GREEN: db-role-menu-readback -> PASS，真实库现满足：
- `排产员`：拥有 `900120/5590/5580/5550/5262/900121/5540/5985`，不再拥有 `900104/璞慧排产`。
- `车间主任`：拥有 `900120/5580/5550/900121`，不再拥有 `5590/排产员工作台`。
- `班组长`：拥有 `900120/5550/5551/5552/5553`，即仅报工页签及查询/创建/更新按钮。
BDD: 指定账号角色分配 -> Given 目标账号和目标角色已存在 / When 执行账号角色分配迁移 / Then `zhaojie` 绑定排产员，`guliya/wuxiaolei/zhangjiayi` 绑定车间主任且保持有效。
RED: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q` -> FAIL，测试先错误要求四个账号共享同一用户名过滤表达。
GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS，3 passed。
GREEN: `Get-Content -Encoding utf8 ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_assignment.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS，本机库已应用账号角色分配迁移。
GREEN: db-user-role-readback -> PASS，真实库当前账号绑定为：
- `zhaojie` -> `910233 / mes_scheduler / 排产员`，绑定记录 `system_user_role.id=140`
- `guliya` -> `910238 / mes_workshop_director / 车间主任`
- `wuxiaolei` -> `910238 / mes_workshop_director / 车间主任`
- `zhangjiayi` -> `910238 / mes_workshop_director / 车间主任`
BDD: 主页签依赖的关联页签链同时放行 -> Given 角色拥有工艺排产路线或生产工单主页签 / When 打开详情与关联对象 / Then 角色同时拥有工艺流程、车间、工作站、设备类型、设备台账和生产工单查询等必要只读菜单权限。
GREEN: related-chain-baseline -> PASS，结合源码与历史真实诊断证据确认关联链为 `工艺排产路线 -> 工艺流程 -> 车间设置 / 工作站设置 / 设备类型 / 设备台账`，并补充 `生产工单 / 工单查询`。
RED: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> FAIL，关联页签链合同新增后，现有角色范围 SQL 尚未覆盖这些菜单。
GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> PASS，3 passed。
RED: `Get-Content -Encoding utf8 ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> FAIL，首次因菜单集合处漏写 `UNION ALL` 导致 MySQL `ERROR 1064`。
GREEN: sql-syntax-fix -> PASS，修正 SQL 语法后重新跑 `test_mes_smart_scheduling_role_scope_related_tabs_sql.py`、`test_mes_smart_scheduling_role_scope_sql.py`、`test_mes_smart_scheduling_role_assignment_sql.py`、`test_mes_scheduler_role_smart_scheduling_tab_sql.py`，合计 17 项断言均通过。
GREEN: `Get-Content -Encoding utf8 ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS，修正后的角色范围 SQL 已成功应用本机库。
GREEN: related-chain-readback -> PASS，真实库当前：
- `排产员` 已拥有 `基础数据 / 车间设置 / 车间查询 / 工作站设置 / 工位查询 / 设备管理 / 设备类型 / 类型查询 / 设备台账 / 设备查询 / 生产管理 / 生产工单 / 工单查询 / 工艺流程 / 工艺路线查询` 及智能排产主页签。
- `车间主任` 已拥有同一条关联链，并保留 `排产工单 / 报工 / 工艺排产路线`。
- `班组长` 仍仅保留 `报工 + 报工查询/创建/更新`。
BDD: 班组长报工归属最小关联链补齐 -> Given 班组长负责报工归属选择排产工单和工序 / When 执行第二轮角色范围迁移 / Then 班组长获得 `生产工单 / 工单查询 / 工艺流程 / 工艺路线查询` 只读链，但不扩到设备和工作站维护链。
GREEN: team-leader-minimal-chain-contract -> PASS，历史需求与任务证据一致指向：班组长最小关联链是 `生产工单 + 工艺流程` 只读，不要求设备/工作站维护。
GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> PASS，3 passed。
GREEN: `Get-Content -Encoding utf8 ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS，再次应用角色范围 SQL。
GREEN: team-leader-readback -> PASS，原始 `system_role_menu` 回查确认 `班组长(role_id=910239)` 当前已有效拥有 `5530/5531/5720/5721`，并保留 `5550/5551/5552/5553/900120`。
