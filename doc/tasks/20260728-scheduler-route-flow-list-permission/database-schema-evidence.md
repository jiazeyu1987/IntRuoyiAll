# Database Schema Evidence

## Data Change Goal

补齐排产员角色对 MES 工艺流程列表的正式菜单权限绑定，使“产品/编辑/版本”列表操作可见并可走原有后端权限校验。

## Affected Entities

- `system_menu`: 读取并校验既有菜单 `5723` / `mes:pro-route:update` 和 `5730` / `mes:pro-route:version-query`。
- `system_role`: 定位启用状态的排产员角色，匹配 `code = 'mes_scheduler'` 或 `name = '排产员'`。
- `system_role_menu`: 幂等恢复或插入排产员角色菜单绑定。
- `system_tenant_package`: 对已包含工艺流程父菜单 `5720` 的套餐补齐 `5723/5730`。

## Database Engine And Migration Tool

MySQL migration SQL under `IntRuoyiBackend/sql/mysql` with release metadata header consumed by `script/release/run-release-migration-policy-gate.py`.

## Migration Changes

- Updated canonical role scope migration `20260629_mes_smart_scheduling_role_scope.sql` to keep `5723/5730` in scheduler allowed menu scope.
- Added `20260728_mes_scheduler_route_flow_list_permission.sql` with release metadata, fail-fast menu checks, tenant package JSON validation, idempotent role-menu restore, and insert-if-missing behavior.

## Data Safety Analysis

The migration is additive and non-destructive. It restores deleted rows only for the two required menu IDs and inserts missing active bindings. It does not delete role menus, does not grant route delete/export/create permissions, and fails fast if required menu rows are missing or target scheduler tenant package JSON is invalid.

## Rollback Or Recovery Plan

Rollback can remove or soft-delete the two task-owned role-menu bindings created by updater/creator `mes-scheduler-route-flow-list-permission` and restore affected tenant package `menu_ids` from backup. No schema shape is changed.

## BDD Scenarios

- BDD: Scheduler can operate route-flow list -> Given a scheduler role, When route-flow list permissions are materialized, Then `5723` and `5730` are active role menu bindings and delete permission is not granted.
- BDD: Tenant package visibility remains scoped -> Given a non-admin tenant package that includes route parent `5720`, When the migration runs, Then `5723/5730` are included in that package before role binding is made effective.

## RED

RED: `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_scheduler_route_flow_list_permission_sql.py` -> FAIL, expected missing `5723/5730` and missing migration.

## GREEN

GREEN: `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_scheduler_route_flow_list_permission_sql.py` -> PASS, `29 passed in 2.33s`.

## Migration Verification

- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file ... --output E:\IntRuoyi\doc\tasks\20260728-scheduler-route-flow-list-permission\migration-policy-gate-targeted.json` -> PASS, `migrationCount=10`.
- Full gate remains blocked by unrelated pre-existing metadata `20260725_mes_edhr_recordbook_global_setting.sql: config-seed`.

## Blockers

No blocker for the task-owned migration. Full global migration policy gate has an unrelated blocker outside this task.
