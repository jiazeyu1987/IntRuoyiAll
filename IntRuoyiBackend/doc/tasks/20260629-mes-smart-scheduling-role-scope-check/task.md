# 任务：MES 智能排产角色及关联页签权限调整

## 任务目标

- 按目标范围调整本机运行库中“排产员 / 车间主任 / 班组长”三个角色的智能排产页签权限及其关联页签权限。
- 通过正式幂等 SQL 完成角色创建/收敛，不做一次性手工改库。
- 回查真实库，确认角色、菜单与绑定结果符合目标，并覆盖关联页签链。

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-work-order-material-demand-warning-clear\task.md`
- 状态：`COMPLETED`
- 处理说明：上一后端任务已完成，本次仅做权限现状核查。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - PowerShell、MySQL 查询输出与日志写入必须显式 UTF-8。
  - 动态菜单结论必须落到真实 `system_menu / system_role / system_role_menu`，不得只依据 SQL 模板或前端路由。
- 真实库写入前必须先记录 `GREEN: experience-preflight -> PASS`。
- 必须通过正式幂等 SQL 调整角色和菜单绑定，不做裸 UPDATE/INSERT 脚本拼凑交付。
- 关联页签链必须以真实源码和历史权限诊断证据为准。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。目标是新增正式权限迁移，持续收敛三类角色“主页签 + 关联页签”授权范围。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产员页签权限按目标收敛 -> Given 本机运行库已有智能排产菜单体系与排产员角色 / When 执行权限迁移 / Then 排产员应拥有除璞慧排产外的智能排产子页签。`
- `BDD: 车间主任页签权限按目标收敛 -> Given 本机运行库已有车间主任角色 / When 执行权限迁移 / Then 车间主任应仅拥有排产工单、报工、工艺排产路线及其必要关联菜单。`
- `BDD: 班组长角色自动补齐 -> Given 本机运行库没有独立班组长角色 / When 执行权限迁移 / Then 应创建独立班组长角色并仅授予报工页面及必要按钮权限。`
- `BDD: 工艺排产路线关联页签不再断链 -> Given 角色拥有工艺排产路线页签 / When 打开详情、资源或关联对象 / Then 同步具备工艺流程、车间、工作站、设备类型、设备台账等必要只读权限。`

## 里程碑

1. M1：建立后端核查任务台账。`COMPLETED`
2. M2：确认智能排产子页签与关联按钮正式菜单定义。`COMPLETED`
3. M3：补 RED 测试与正式权限迁移 SQL。`COMPLETED`
4. M4：应用本机运行库并完成真实回查。`COMPLETED`
5. M5：按关联页签链补第二轮权限迁移。`IN_PROGRESS`

## 预期验证

- `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\<role-scope-test>.py`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro < sql/mysql/<role-scope>.sql`
- 只读回查 `system_role`、`system_role_menu`。

## 最终验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_related_tabs_sql.py -q` -> PASS
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_scheduler_role_smart_scheduling_tab_sql.py -q` -> PASS
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_assignment_sql.py -q` -> PASS
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_assignment.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS
- 真实库回查 `system_role/system_role_menu` -> PASS
- 真实库回查 `system_user_role` -> PASS

## 最终结论

- 已交付正式幂等 SQL `sql/mysql/20260629_mes_smart_scheduling_role_scope.sql`。
- 已交付正式幂等 SQL `sql/mysql/20260629_mes_smart_scheduling_role_assignment.sql`。
- `排产员` 当前已移除 `璞慧排产`，保留其余智能排产子页签。
- `排产员` 当前已补齐与 `工艺排产路线 / 生产工单` 相关的关联页签链：`工艺流程 / 车间设置 / 工作站设置 / 设备类型 / 设备台账 / 生产工单` 及对应 query 权限。
- `车间主任` 当前已移除 `排产员工作台`，保留 `排产工单 / 报工 / 工艺排产路线`，并同步拥有同一条关联页签链。
- 已创建独立 `班组长` 角色 `910239 / mes_team_leader / 班组长`，当前拥有 `报工` 页面和 `查询/创建/更新` 按钮权限，并补齐 `生产工单 / 工单查询 / 工艺流程 / 工艺路线查询` 最小只读关联链。
- `zhaojie` 当前已绑定 `排产员`，`guliya / wuxiaolei / zhangjiayi` 当前已绑定 `车间主任`。

## 当前状态

COMPLETED

## 当前阻塞

- 无。
