# 测试服务器权限角色差异同步

## Task Goal

在测试服务器 `172.30.30.58` 上，以本机租户 1 的全部有效角色权限配置为源，按稳定业务键差异同步角色定义与角色菜单权限；保留测试服专属角色和全部用户角色绑定，不复制本机自增 ID，不执行全量删除。

## Scope

- 目标环境：测试服务器 `172.30.30.58`，MySQL `ruoyi-vue-pro`。
- 数据范围：本机/测试服租户 1 的 `system_role_category`、`system_role`、`system_menu`、`system_role_menu`、必要的 `system_tenant_package.menu_ids` 与权限缓存。
- 目标角色：本机租户 1 全部未删除角色，按 `tenant_id + role.code` 匹配；本机存在而测试服缺失的标准角色在唯一性与依赖门禁通过后创建。
- 明确排除：不删除测试服角色，不改 `system_user_role`，不复制本机角色或关系自增 ID，不同步其它租户，不修改业务数据。
- 缓存范围：只删除 Redis DB 1 中 `menu_role_ids:*` 与 `permission_menu_ids:*` 权限映射缓存；`system_user_role` 未变更，因此不删除 `user_role_ids:*`。

## Milestones

- [x] M1：建立本机与测试服角色、菜单和有效权限的稳定业务键差异清单。
- [x] M2：冻结白名单、备份测试服精确目标行并完成 RED 前置验证。
- [x] M3：运行正式迁移并核对影响范围、迁移状态和非目标权限保持不变。
- [x] M4：刷新精确权限缓存并验证 `zhaojie` 登录权限与工艺路线页面。
- [x] M5：完成回归、清理和收尾证据。
- [x] M6：审计本机/测试服租户 1 全部角色、菜单和有效权限稳定键矩阵。
- [x] M7：冻结全角色同步清单，建立覆盖全部目标行的精确备份、恢复脚本和 RED。
- [x] M8：执行全角色权限差异同步，并验证角色、用户绑定、危险权限和跨租户不变量。
- [x] M9：精确处理受影响权限缓存，完成代表性账号复验和任务收尾。

## Expected Verification

- 本机/测试服差异按 `tenant_id + role.code + menu.permission` 生成，不按角色或角色菜单自增 ID 比较。
- 测试服同步前后备份并比较全角色、角色菜单、租户套餐、发布锁与用户绑定。
- 本机租户 1 每个唯一角色编码在测试服均可解析；其有效菜单权限集合与本机一致。
- 测试服专属角色和全部 `system_user_role` 业务键保持不变；其它租户角色菜单保持不变。
- 新建角色使用目标库生成 ID，所有角色菜单关系均通过目标角色和目标菜单稳定键解析。
- `zhaojie` 重新登录后权限响应包含 `query/update/version-query`，不包含 `create/delete/export`。
- 工艺路线列表真实页面显示“产品 / 编辑 / 版本”，不显示“删除”。
- Redis 只精确删除角色菜单与权限菜单映射缓存，不清空全库缓存，不删除未受影响的用户角色缓存。

## Applicable Experience Gates

- `docs/database-rules.md#跨环境角色权限差异同步门禁`：按稳定键同步，不复制 ID，不删除目标环境专属角色，不用 API-only 代替页面验收。
- `docs/database-rules.md#系统角色菜单授权-tenant-1-admin-门禁`：按正式角色、菜单和用户链路验证，不使用前端隐藏或 admin bypass。
- `docs/database-rules.md#租户和菜单权限`：同时核对菜单、角色绑定、租户套餐和登录权限响应。
- `docs/server-access.md`、`docs/release-backup-restore.md`：仅操作明确授权的测试服务器，写入前建立精确备份和恢复路径。
- `docs/e2e-rules.md`、`docs/login-access.md`：真实登录、真实页面、目标链路与非目标资源异常分开归因。
- `docs/powershell-encoding.md`：SQL、任务文档与中文命令使用 UTF-8 安全通道，不在日志中记录密码或 token。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以正式迁移和稳定业务键同步，不复制环境自增 ID。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

- 数据库同步：完成；全角色源权限缺口 `159 -> 0`，额外源角色权限 `104 -> 0`。
- 缓存处理：完成；删除 Redis DB 1 中 `menu_role_ids:*` 与 `permission_menu_ids:*` 共 8 个键，保留 `user_role_ids:*`。
- 真实页面验证：完成；`zhaojie` 以租户 1 `芋道源码` 重新登录测试服，工艺路线列表按钮权限符合预期。
- 收尾清理：`task_closeout.py --mode preview` 与 `--mode apply` 均通过，仅删除任务自有 `__pycache__`，所有恢复与验收证据保留。

## Key Results

- 全角色 SQL 合同测试：`python -X utf8 -m pytest .\IntRuoyiBackend\script\tests\test_test_tenant1_all_role_permission_sync_sql.py -q` -> PASS，`5 passed`。
- 发布迁移策略门禁：`all-role-migration-policy-gate-latest.json` -> `passed`，迁移 `20260807_test_tenant1_all_role_permission_sync` 仅允许 `test`，`riskLevel=high`。
- 测试服预同步快照：本机源角色 `60`，测试服已解析源角色 `26`，缺失源权限 `159`，额外源角色权限 `104`，重复源角色编码 `0`。
- 恢复基线：`all-role-pre-sync-permission-tables-baseline.sql`，SHA-256 `80b46168d90a56455c3a84fe34c8e14baa1d17c0d4b6cfd88f1b68000205f5cc`。
- 测试服同步执行：官方 `apply-test-db-sql.ps1` 返回 `APPLY_TEST_DB_SQL_OK`，发布锁 `test-tenant1-all-role-permission-sync-20260807T2008` 以 `APPLIED` 释放。
- 最终快照：测试服已解析源角色 `60`，缺失源权限 `0`，额外源角色权限 `0`，发布运行锁 `0`。
- 不变量：`tenant1UserRoleHash` 保持 `0696cca53312bb05a836ee2fd4fad7562ef24956202611e3d73798b9f6cf0668`；`otherTenantRoleMenuHash` 保持 `1d69f0f91e09f34e343ec7413d3dbf7f747f33e82393e52681b67fd6c133e654`。
- 页面验收：`route-permission-page-check.json` -> PASS；`/admin-api/mes/pro/route/page` HTTP 200、业务 `code=0`、列表总数 `4`，可见按钮包含“产品 / 编辑 / 版本”，不含“删除”。
- Evidence validator：`validate_database_schema.py --evidence .\doc\tasks\20260807-test-permission-role-differential-sync\database-schema-evidence.md` -> PASS。

## Cleanup Keep

- doc/tasks/20260807-test-permission-role-differential-sync/all-role-audit-local.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-audit-test.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-final-snapshot.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-migration-policy-gate-latest.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-migration-policy-gate.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-post-sync-snapshot.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-pre-sync-permission-tables-baseline.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-pre-sync-permission-tables-baseline.sql
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-pre-sync-snapshot.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-source-definitions.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-sync-lock-acquired.json
- doc/tasks/20260807-test-permission-role-differential-sync/all-role-sync-lock-released.json
- doc/tasks/20260807-test-permission-role-differential-sync/database-schema-evidence.md
- doc/tasks/20260807-test-permission-role-differential-sync/generate-all-role-sync.ps1
- doc/tasks/20260807-test-permission-role-differential-sync/post-migration-snapshot.json
- doc/tasks/20260807-test-permission-role-differential-sync/pre-migration-snapshot.json
- doc/tasks/20260807-test-permission-role-differential-sync/redis-permission-cache-after-delete.json
- doc/tasks/20260807-test-permission-role-differential-sync/redis-permission-cache-before-delete.json
- doc/tasks/20260807-test-permission-role-differential-sync/redis-permission-cache-delete.json
- doc/tasks/20260807-test-permission-role-differential-sync/remote_role_permission_sync.py
- doc/tasks/20260807-test-permission-role-differential-sync/rollback-permission-sync.sql
- doc/tasks/20260807-test-permission-role-differential-sync/route-permission-page-check.json
- doc/tasks/20260807-test-permission-role-differential-sync/route-permission-page-check.png
- doc/tasks/20260807-test-permission-role-differential-sync/route_permission_page_check.mjs
- doc/tasks/20260807-test-permission-role-differential-sync/target-state-before-sync.json
- doc/tasks/20260807-test-permission-role-differential-sync/targeted-migration-manifest.json
- doc/tasks/20260807-test-permission-role-differential-sync/targeted-preflight-plan.json
- doc/tasks/20260807-test-permission-role-differential-sync/test-permission-tables-baseline.sql
