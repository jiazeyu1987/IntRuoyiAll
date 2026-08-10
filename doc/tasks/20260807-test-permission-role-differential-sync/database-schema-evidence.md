# Database Schema Evidence: 测试服务器权限角色差异同步

## Data Change Goal And Affected Entities

按本机租户 1 的有效角色编码、角色分类编码和菜单权限稳定键，同步测试服租户 1 的角色定义与角色菜单有效权限集合。涉及 `system_role_category`、`system_role`、`system_menu`、`system_role_menu`、`system_tenant_package`、`infra_release_migration`、`infra_release_operation_lock` 与 Redis 权限缓存；不删除测试服专属角色，不修改 `system_user_role`。

## Database Engine And Migration Tool

- Database: MySQL 8，数据库 `ruoyi-vue-pro`。
- Migration tool: IntRuoyi SQL 发布迁移合同与测试服官方快速执行脚本 `IntRuoyiBackend\script\deploy\apply-test-db-sql.ps1`。
- Target SQL: `IntRuoyiBackend\sql\mysql\20260807_test_tenant1_all_role_permission_sync.sql`。

## Schema, Migration, Fixture, Seed, Index Or Constraint Changes

- 不新增 schema、索引或约束。
- 新增测试服专用高风险 data migration：`20260807_test_tenant1_all_role_permission_sync`，`allowedEnvironments=test`，依赖 `20260728_mes_scheduler_route_flow_list_permission`。
- SQL 通过 `tenant_id + role.code` 解析角色，通过 `category.code` 解析角色分类，通过 `menu.permission` 与稳定菜单合同解析目标菜单。
- 本机存在、测试服缺失的 34 个源角色使用测试库生成 ID 创建；共享角色权限集合按本机有效权限对齐；测试服 17 个专属角色保留。
- 缺失的 12 个源权限对应 13 行菜单定义由迁移按稳定父菜单合同创建或补齐。

## Data Safety Analysis

- 禁止全量删除、禁止复制本机角色 ID、菜单 ID 或 `system_role_menu` 自增 ID。
- 写入前导出权限相关 8 张表完整恢复基线，并记录 SHA-256。
- 使用测试环境发布互斥锁，锁内执行，执行后释放为 `APPLIED`。
- 迁移前后核对 `system_user_role` 计数和业务哈希保持不变，证明未改用户角色绑定。
- 迁移前后核对其它租户角色菜单哈希保持不变，证明未同步其它租户。
- Redis 只删除菜单角色与权限菜单映射缓存；因用户角色绑定不变，保留 `user_role_ids:*`。

## Rollback Or Recovery Plan

- 全量恢复基线：`doc/tasks/20260807-test-permission-role-differential-sync/all-role-pre-sync-permission-tables-baseline.sql`。
- 基线 manifest：`all-role-pre-sync-permission-tables-baseline.json`。
- SHA-256：`80b46168d90a56455c3a84fe34c8e14baa1d17c0d4b6cfd88f1b68000205f5cc`。
- 覆盖表：`system_role_category`、`system_role`、`system_menu`、`system_role_menu`、`system_user_role`、`system_tenant_package`、`infra_release_migration`、`infra_release_operation_lock`。
- 若需恢复，只允许按该基线和测试服目标范围恢复，不得扩大到正式服、备份服或非任务表。

## BDD Scenarios

- BDD: 全部共享角色权限对齐 -> Given 本机和测试服存在相同 `tenant_id + role.code` 但自增 ID 不同 / When 按目标菜单稳定键同步有效角色菜单集合 / Then 每个共享角色的有效权限集合与本机一致且用户绑定不变。
- BDD: 本机缺失角色安全创建 -> Given 本机租户 1 存在唯一标准角色编码而测试服缺失 / When 通过唯一性和依赖门禁创建目标角色 / Then 使用测试库生成 ID 并绑定目标菜单，不复制本机 ID。
- BDD: 测试服专属角色保持 -> Given 测试服存在本机没有的环境专属角色 / When 执行本机到测试服的全角色同步 / Then 专属角色、权限和用户绑定不被删除或覆盖。
- BDD: 权限缓存与页面一致 -> Given 数据库权限同步完成 / When 精确权限缓存失效并重新登录 / Then 登录权限响应和工艺路线操作按钮与数据库有效权限一致。

## RED Command And Expected Failure

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_test_tenant1_all_role_permission_sync_sql.py -q` -> FAIL，`5 failed`，预期原因：全角色正式迁移文件尚不存在。
- RED 数据基线：预同步快照 `all-role-pre-sync-snapshot.json` 显示源角色 `60`、测试服已解析源角色 `26`、缺失源权限 `159`、额外源角色权限 `104`。

## GREEN Command And Passing Result

- GREEN: `python -X utf8 -m pytest .\IntRuoyiBackend\script\tests\test_test_tenant1_all_role_permission_sync_sql.py -q` -> PASS，`5 passed in 0.21s`。
- GREEN: `node --check .\doc\tasks\20260807-test-permission-role-differential-sync\route_permission_page_check.mjs` -> PASS。
- GREEN: `node .\doc\tasks\20260807-test-permission-role-differential-sync\route_permission_page_check.mjs` -> PASS，真实登录测试服租户 1 `zhaojie` 并生成 `route-permission-page-check.json`。
- GREEN: release migration policy gate -> PASS，`all-role-migration-policy-gate-latest.json` 状态 `passed`。
- GREEN: 官方测试服 SQL 应用脚本 -> PASS，输出 `APPLY_TEST_DB_SQL_OK`。

## Migration Verification

- 预同步快照：`sourceRoleCount=60`、`targetResolvedSourceRoleCount=26`、`missingSourcePermissionCount=159`、`extraSourceRolePermissionCount=104`、`duplicateTargetSourceRoleCodes=0`。
- 后同步快照：`targetResolvedSourceRoleCount=60`、`missingSourcePermissionCount=0`、`extraSourceRolePermissionCount=0`、`tenant1ActiveRoleCount=77`、`targetOnlyActiveRoleCount=17`。
- 最终快照：`runningTestLockCount=0`，缺失/额外源权限继续为 `0`。
- 用户角色不变量：`tenant1UserRoleHash` 保持 `0696cca53312bb05a836ee2fd4fad7562ef24956202611e3d73798b9f6cf0668`，`tenant1UserRoleCount=2188`。
- 其它租户不变量：`otherTenantRoleMenuHash` 保持 `1d69f0f91e09f34e343ec7413d3dbf7f747f33e82393e52681b67fd6c133e654`。
- Redis 缓存：同步后扫描到 10 个权限相关 key；删除 `menu_role_ids:*` 与 `permission_menu_ids:*` 共 8 个；复扫只剩 2 个 `user_role_ids:*`。
- 页面验证：`zhaojie` 租户 1 权限响应包含 `mes:pro-route:query/update/version-query`，不包含 `create/delete/export`；工艺路线列表可见“产品 / 编辑 / 版本”，不可见“删除”。
- 目标链路：`/admin-api/mes/pro/route/page` HTTP 200、业务 `code=0`、列表总数 `4`。
- 非目标资源异常：Playwright 捕获 5 个 `net::ERR_ABORTED`，路径为 iconify JSON 与 `hm.gif`，未影响目标接口、页面按钮或 pageerror/console error。

## Blockers

- 无。
