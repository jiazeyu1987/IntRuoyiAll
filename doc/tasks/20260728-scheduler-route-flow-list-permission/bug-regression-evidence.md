# Bug Regression Evidence

## Bug Summary

排产员打开 MES 工艺流程列表时，列表“操作”列为空。当前前端列表按钮分别依赖 `mes:pro-route:update` 和 `mes:pro-route:version-query` 等正式菜单权限；排产员角色范围只包含 `5720/5721` 路由父菜单和查询权限，未包含可显示“产品/编辑/版本”的操作权限。

## Expected Behavior

排产员角色应具备工艺流程列表的非删除型操作权限：`5723` / `mes:pro-route:update` 用于“产品/编辑”，`5730` / `mes:pro-route:version-query` 用于“版本”。不得默认授予删除权限。

## Reproduction

`python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_scheduler_route_flow_list_permission_sql.py`

## Root Cause

`20260629_mes_smart_scheduling_role_scope.sql` 的 scheduler allowed menu block 仅包含 `5720`、`5721`，未包含当前工艺流程列表操作列所需的 `5723` 和 `5730`。既有迁移已经建立路线版本菜单，但没有面向排产员补齐这些角色菜单绑定。

## Regression Test

- `script/tests/test_mes_smart_scheduling_role_scope_sql.py::test_role_scope_sql_keeps_scheduler_route_flow_list_operation_permissions`
- `script/tests/test_mes_scheduler_route_flow_list_permission_sql.py`

## RED

RED: `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_scheduler_route_flow_list_permission_sql.py` -> FAIL，失败点为 scheduler block 缺少 `5723/5730` 且新增迁移文件缺失。

## GREEN

GREEN: `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_scheduler_route_flow_list_permission_sql.py` -> PASS，`29 passed in 2.33s`。

## Verification

REGRESSION: `python -X utf8 -m pytest script/tests/test_mes_route_version_permission_menu_sql.py script/tests/test_mes_route_flow_config_migration_sql.py` -> PASS，`11 passed in 0.35s`。

## Risk And Regression Scope

变更只增加排产员非删除型工艺流程列表操作权限，未添加 `5724` / `mes:pro-route:delete`。相邻路线版本权限和工艺流程迁移合同已回归通过。

## Blockers And Follow-up

全量迁移策略门禁被既有 unrelated SQL 元数据 `20260725_mes_edhr_recordbook_global_setting.sql: config-seed` 阻塞；本任务新增迁移及依赖链选择性门禁已通过。
