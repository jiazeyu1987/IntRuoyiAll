# 执行日志：本机排产员工艺流程操作权限修复

## User Intent

用户要求先把本机的 `排产员` 修复成可以操作“工艺流程”。

## Gate Checks

- 已读取 `docs/task-closeout-rules.md`、`docs/local-runtime.md`、`docs/database-rules.md`、`docs/login-access.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md` 相关索引，命中菜单/权限、`system_menu`、`system_role_menu`、工艺流程权限门禁。
- 本机工作区存在其它任务脏改动，本任务只新增 `doc/tasks/20260730-local-scheduler-route-permission/` 并只改本机数据库。

## BDD

- BDD: 本机排产员可以操作工艺流程 -> Given 本机存在启用的 `排产员/mes_scheduler` 角色 / When 用户进入“生产管理 > 工艺流程” / Then 列表页应显示正式操作入口，且后端对应接口权限由 `mes:pro-route:*` 正式权限控制。

## Evidence

- RED: 本机只读 SQL -> FAIL，`tenant_id=1` 的 `排产员/mes_scheduler` 角色存在，但 `5723/mes:pro-route:update` 为 `deleted=1`，`5730/mes:pro-route:version-query` 菜单缺失；操作列对应“产品/编辑/版本”入口无法按正式权限显示。
- Scope evidence: `doc/tasks/20260728-scheduler-route-flow-list-permission/bug-regression-evidence.md` 明确预期为非删除型操作权限 `5723/update` 与 `5730/version-query`，不授予 `5724/delete`。

## Implementation

- 已对本机 Docker MySQL `int-ruoyi-mysql` 的 `ruoyi-vue-pro` 执行 `IntRuoyiBackend\sql\mysql\20260716_mes_route_version_permission_menu.sql`，补齐工艺路线版本菜单 `5730-5734`。
- 已执行 `IntRuoyiBackend\sql\mysql\20260728_mes_scheduler_route_flow_list_permission.sql`，只补齐 `排产员/mes_scheduler` 的 `5723/update` 与 `5730/version-query` 正式绑定。
- 本次未改远端服务器、未重启服务、未改前端或后端代码。

## GREEN

- GREEN: 本机权限 SQL 复验 -> PASS，`tenant_id=1 role_id=910233 排产员/mes_scheduler` 存在；`5723/mes:pro-route:update` 的 `role_menu_deleted=0`，`5730/mes:pro-route:version-query` 的 `role_menu_deleted=0`。
- GREEN: 删除权限边界复验 -> PASS，`5724/mes:pro-route:delete` 的 `role_menu_deleted=1`，未授予排产员删除权限。
- GREEN: 版本菜单复验 -> PASS，`5730-5734` 均存在且 `menu_deleted=0`。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_scheduler_route_flow_list_permission_sql.py IntRuoyiBackend\script\tests\test_mes_route_version_permission_menu_sql.py -q` -> PASS，`9 passed in 0.28s`。

## Remaining Closeout

- 本机修复已完成并进入 `ready_for_closeout`。
- 仓库当前存在其它任务脏改动且分支 `int_main...origin/int_main` 处于 ahead/behind 状态；为避免提交或清理非本任务文件，本任务未执行提交、推送或全局 cleanup。

## Experience Consolidation

- 已按 `project-experience-consolidation` 检查长期经验归宿；本次经验已被 `docs/database-rules.md#租户和菜单权限` 以及既有 `20260728-scheduler-route-flow-list-permission` 任务证据覆盖。
- 本次不新增长期经验文档，避免把一次性本机数据修复状态写入长期记忆。
