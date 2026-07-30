# 验证报告：本机排产员工艺流程操作权限修复

## Summary

- 本机 `tenant_id=1` 的 `排产员/mes_scheduler` 已恢复“生产管理 > 工艺流程”所需的非删除型正式操作权限。
- 修复范围仅限本机 Docker MySQL `int-ruoyi-mysql` / `ruoyi-vue-pro` 数据，不涉及测试服、远端发布、服务重启或前后端代码变更。
- 权限边界保持不扩大：未授予 `mes:pro-route:delete`。

## RED Evidence

- 修复前只读 SQL 证明 `排产员/mes_scheduler` 存在，但 `5723/mes:pro-route:update` 在 `system_role_menu` 中为 `deleted=1`。
- 修复前 `5730/mes:pro-route:version-query` 菜单缺失，导致版本查询类操作入口无法按正式权限显示。
- 既有范围证据要求只补 `5723/update` 与 `5730/version-query`，不得授予 `5724/delete`。

## Applied Fix

- 执行 `IntRuoyiBackend\sql\mysql\20260716_mes_route_version_permission_menu.sql`，补齐工艺路线版本菜单 `5730-5734`。
- 执行 `IntRuoyiBackend\sql\mysql\20260728_mes_scheduler_route_flow_list_permission.sql`，补齐本机排产员角色的非删除型工艺流程列表操作权限。

## GREEN Evidence

- 本机 DB 复验：`tenant_id=1 role_id=910233 排产员/mes_scheduler` 存在。
- `5723 工艺路线更新 / mes:pro-route:update`：`role_menu_deleted=0`。
- `5730 工艺路线版本查询 / mes:pro-route:version-query`：`role_menu_deleted=0`。
- `5724 工艺路线删除 / mes:pro-route:delete`：`role_menu_deleted=1`，删除权限未授予。
- `5730-5734` 工艺路线版本菜单存在且未删除。

## Regression

- Command: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_scheduler_route_flow_list_permission_sql.py IntRuoyiBackend\script\tests\test_mes_route_version_permission_menu_sql.py -q`
- Result: `9 passed in 0.28s`

## Closeout Status

- 本机数据修复与验证完成。
- 仓库存在其它任务脏改动且分支 ahead/behind；未提交、未推送、未清理其它任务文件。
