# Database Schema Evidence

## Data Change Goal

为 tenant 1 创建或修复权限角色 `用户列表访问`，并绑定已有菜单权限 `system:user:query`。

## Affected Entities

- `system_role`
- `system_role_menu`
- `system_menu`
- `system_role_category`

## Database Engine And Migration Tool

- MySQL required SQL under `IntRuoyiBackend/sql/mysql`.
- 使用 release migration metadata 标记可发布 SQL。

## Data Safety

- 非破坏性：不删除、不清空、不停用任何角色或权限。
- 幂等：按 `system_role.code='user_list_access'` 和 `system_menu.permission='system:user:query'` 解析并补齐。
- Fail fast：缺少 tenant 1 `menu` 角色分类、缺少启用 `system:user:query` 菜单或存在多个启用 `user_list_access` 角色时 SQL 抛错。
- 字符集：权限临时表声明 `VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`，中文角色名和角色 code 使用 `_utf8mb4 ... COLLATE utf8mb4_unicode_ci`。

## Rollback Plan

- 若需回滚，只删除本 SQL 创建的 `system_role.code='user_list_access'` 角色和其 `system_role_menu` 关系；不得删除 `system:user:query` 菜单。

## BDD Scenarios

- BDD: 用户列表访问角色拥有用户查询权限 -> Given tenant 1 存在权限角色分类 `menu` 且系统菜单存在启用的 `system:user:query` When 权限 SQL 执行 Then tenant 1 启用角色 `用户列表访问` 存在且其角色菜单关系绑定到真实 `system:user:query` 菜单。

## RED

- RED: `python -m pytest IntRuoyiBackend\script\tests\test_user_list_access_role_sql.py` -> FAIL, expected reason: missing `IntRuoyiBackend/sql/mysql/20260728_user_list_access_role.sql`.

## GREEN

- GREEN: `python -m pytest IntRuoyiBackend\script\tests\test_user_list_access_role_sql.py` -> PASS, 4 passed.

## Migration Verification

- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260707_system_role_category_management.sql --sql-file IntRuoyiBackend\sql\mysql\20260728_user_list_access_role.sql` -> PASS, migration metadata and dependency contract passed for the new SQL and its dependency.
- Full SQL policy gate over all MySQL SQL files -> BLOCKED by pre-existing `20260725_mes_edhr_recordbook_global_setting.sql` invalid type `config-seed`; not part of this task.

## Verification

- `python -m pytest IntRuoyiBackend\script\tests\test_user_list_access_role_sql.py` -> PASS, 4 passed.
- `git diff --check -- IntRuoyiBackend\script\tests\test_user_list_access_role_sql.py IntRuoyiBackend\sql\mysql\20260728_user_list_access_role.sql` -> PASS.

## Blockers

- 无本任务阻塞。全量历史 SQL policy gate 存在既有阻塞，已记录为非本任务问题。
