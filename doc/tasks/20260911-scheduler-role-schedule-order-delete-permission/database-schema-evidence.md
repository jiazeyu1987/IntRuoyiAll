# Database Schema Evidence

## Data Change Goal

为每个租户唯一活动的 `mes_scheduler/排产员` 角色授予 `mes:pro-schedule-order:delete`，使角色成员在排产工单页面看到删除入口，同时保持其它角色和用户角色绑定不变。

## Affected Entities

- `system_role`：只读解析 `code=mes_scheduler` 的活动角色。
- `system_menu`：只读解析唯一活动 `mes:pro-schedule-order:delete` 按钮菜单。
- `system_role_menu`：恢复一条历史软删除绑定或在不存在时新增一条活动绑定。

## Database Engine And Migration Tool

- Database: MySQL 8.0.39。
- Migration: 版本化 SQL `20260911_mes_scheduler_schedule_order_delete_permission.sql`，由 IntRuoyi 发布迁移策略识别为 `type=permission`。
- Target preflight: 同 migrationId 的只读 `target-preflight/*.preflight.sql`。

## Changes

- 新增前向、幂等权限迁移。
- 新增只读目标环境 preflight。
- 迁移清单与策略明确排除 `target-preflight` 目录，防止把只读探针当成可执行迁移。
- 新增静态合同测试和 MySQL 隔离集成测试。

## Data Safety Analysis

- 角色按 `tenant_id + code=mes_scheduler` 唯一解析；同租户重复角色立即失败。
- 菜单按 permission、type、status、deleted 唯一解析；缺失或重复立即失败。
- 已有单一活动绑定不更新；只有不存在活动绑定时恢复最小历史 ID 或新增。
- 执行前若已有多个活动绑定立即失败；执行后强制每个目标角色恰好一个活动绑定。
- 不更新 `system_role`、`system_menu`、`system_user_role` 或 `system_tenant_package`，不删除物理数据。

## Rollback Plan

- 回滚脚本：`doc/tasks/20260911-scheduler-role-schedule-order-delete-permission/rollback.sql`。
- 仅软删除 creator/updater 带本迁移标记的活动绑定，因此不会撤销迁移前已经有效的授权。
- 测试服执行前保存精确 pre-state；若页面验收或后置断言失败，立即执行该回滚并复核目标角色活动绑定恢复为 0。

## BDD Scenarios

- BDD: Given 目标角色和菜单唯一，When 迁移执行，Then 每个排产员角色得到且只得到一个删除权限绑定。
- BDD: Given 历史存在多条软删除绑定，When 迁移执行，Then 只恢复一条，重复执行不新增。
- BDD: Given 前置缺失、歧义或已有重复活动绑定，When preflight/迁移执行，Then 明确阻断且不产生部分授权。
- BDD: Given 迁移需要回滚，When 精确回滚执行，Then 只撤销本迁移新增或恢复的绑定。

## RED Evidence

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_scheduler_schedule_order_delete_permission_sql.py -q` -> `5 failed`，迁移与 target preflight 文件尚不存在。
- RED: 测试服当前 `zhaojie` 权限响应不含目标 permission，页面 20 行、删除按钮 0。

## GREEN Evidence

- GREEN: 静态合同 + MySQL 隔离验证：`6 passed`。
- GREEN: 迁移策略及 target-preflight 排除回归：`10 passed`。
- GREEN: MySQL 隔离验证覆盖首次执行、两条历史软删除只恢复一条、重复执行、非目标角色不变、异常前置阻断和精确回滚；测试数据库最终删除。

## Migration Verification

- 测试服 schema 只读核对：`system_role_menu` 包含 `id/role_id/menu_id/creator/updater/deleted/tenant_id` 所需字段。
- 测试服活动排产员角色：租户 1 与 122 各唯一一个。
- 删除菜单：全库唯一活动菜单 `5586 / mes:pro-schedule-order:delete / type=3`。
- 测试服写入与 Playwright fresh 登录验证待实现提交和推送后执行。

## Blockers

- 无当前实现 blocker；测试服写入尚未执行。
