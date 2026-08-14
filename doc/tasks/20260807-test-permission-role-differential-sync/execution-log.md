# 执行日志：测试服务器权限角色差异同步

## User Intent

- 用户确认不采用“删除测试服全部角色后复制本机”的高风险方案，授权按最合理方式进行权限角色平移。
- 本任务将“最合理方式”固定为：稳定业务键差异审计、正式迁移白名单、精确备份、最小写入、精确缓存刷新和真实账号验证。
- 后续范围变更：用户明确要求不仅同步 `zhaojie` 的排产员角色，而是同步本机租户 1 的全部角色权限配置。

## BDD

BDD: 排产员正式权限差异同步 -> Given 本机和测试服的排产员角色 ID 不同但角色编码均为 `mes_scheduler` / When 按 `tenant_id + role.code + menu.permission` 执行正式迁移 / Then 测试服排产员获得 `update/version-query`，不获得 `delete`，原有用户角色绑定和测试服专属角色不变。

BDD: 缺失迁移前置失败 -> Given 测试服缺少版本菜单或正式排产员角色 / When 执行权限同步 / Then 迁移 fail fast，事务不产生部分权限写入。

BDD: 权限缓存与页面一致 -> Given 数据库权限同步完成 / When `zhaojie` 的精确权限缓存失效并重新登录 / Then 登录权限响应和工艺路线操作按钮与数据库有效权限一致。

BDD: 全部共享角色权限对齐 -> Given 本机和测试服存在相同 `tenant_id + role.code` 但自增 ID 不同 / When 按目标菜单稳定键同步有效角色菜单集合 / Then 每个共享角色的有效权限集合与本机一致且用户绑定不变。

BDD: 本机缺失角色安全创建 -> Given 本机租户 1 存在唯一标准角色编码而测试服缺失 / When 通过唯一性和依赖门禁创建目标角色 / Then 使用测试库生成 ID 并绑定目标菜单，不复制本机 ID。

BDD: 测试服专属角色保持 -> Given 测试服存在本机没有的环境专属角色 / When 执行本机到测试服的全角色同步 / Then 专属角色、权限和用户绑定不被删除或覆盖。

## Command Intent

- 只读审计：比较本机和测试服角色编码、菜单权限、有效角色菜单与用户绑定，不输出凭据。
- 迁移前验证：运行仓库迁移合同测试和 release migration policy gate，确认正式 SQL 白名单和删除权限边界。
- 远端写入：仅在备份成功、差异唯一且合同测试通过后执行正式迁移。
- 后置验证：核对影响行、迁移状态、非目标哈希、缓存和真实页面。

## Milestone Updates

### M1 差异审计

- 状态：完成。
- 本机租户 1 排产员角色 ID 为 `910233`，测试服为 `910216`，稳定键均为 `1 + mes_scheduler`。
- 测试服 `5723` 菜单本身有效，但角色菜单绑定为 `deleted=1`；版本菜单 `5730..5734` 缺失。
- 测试服租户 1 排产员有效权限基线为 `5721/5726/5727`；`5724` 删除权限为无效绑定，必须保持无效。
- 测试服存在租户 122 排产员，其套餐不含 `5720`，不属于本次有效授权目标。
- 迁移白名单：`20260716_mes_route_version_permission_menu`、`20260728_mes_scheduler_route_flow_list_permission`。

### M2 白名单、备份与 RED

- 状态：完成。
- 静态迁移合同：`python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_mes_route_version_permission_menu_sql.py IntRuoyiBackend/script/tests/test_mes_scheduler_route_flow_list_permission_sql.py -q` -> PASS，`5 passed`。
- 精确快照：`pre-migration-snapshot.json`，SHA-256 `32A0A37F92D65D6FD35DE3361628A74E6F680599B55D3831D9DEE95E5C93344E`。
- 快照断言：目标菜单行 `0`、目标角色菜单行 `1`、源授权行 `9`、受影响套餐 `1`、目标迁移状态行 `0`。
- RED: 远端有效权限断言命令 -> FAIL，预期原因：租户 1 `mes_scheduler` 仅拥有 `0/2` 个目标权限。

### M3 正式迁移与不变量验证

- 状态：完成。
- 取得测试环境正式发布互斥锁：`test-permission-sync-20260807T1700`。
- 官方 preflight planner 结果：`targeted-preflight-plan.json` 状态 `passed`，仅两项白名单迁移为 `APPLY`。
- `20260716_mes_route_version_permission_menu` -> `RUNNING` -> SQL PASS -> `APPLIED`。
- `20260728_mes_scheduler_route_flow_list_permission` -> `RUNNING` -> SQL PASS -> `APPLIED`。
- 后置不变量：版本菜单合同 `5/5`；租户 1 排产员目标权限 `2/2`；创建/删除/导出权限 `0`；租户 122 排产员目标权限 `0`。
- 角色与用户绑定保持：角色行 `71`、有效角色 `70`、用户角色行 `2215`、有效绑定 `2188`、用户角色业务哈希保持 `938d4e40a79ef9c0fd8e567285e8f2a3645194d9a205ad74eadb1e3893361411`。
- 角色菜单变化与精确差异一致：总行 `6892 -> 6905`，有效行 `4698 -> 4712`。
- 套餐 111 菜单数 `589 -> 595`，新增白名单 `5723/5730..5734`。
- 两项迁移状态和 SHA-256 均为 `APPLIED`；互斥锁已以 `APPLIED` 释放。

### M4 缓存与单角色验证

- 状态：完成于最终页面验收阶段。
- 单角色阶段 Redis DB 1 中不存在 `menu_role_ids` 或 `permission_menu_ids` 匹配键；执行精确 `DEL` 返回 `0`，未清空全库缓存。
- 数据库按 `zhaojie(user_id=1074)` 全部有效角色计算出的工艺路线权限为：`query/schedule-config:query/schedule-config:update/update/version-query`；不含 `create/delete/export`。
- 最终真实页面验收并入 M9 完成。

### M6 全角色差异审计

- 状态：完成。
- 本机租户 1 有效角色 `60`，测试服 `43`；共享角色 `26`、本机独有角色 `34`、测试服独有角色 `17`，两侧有效角色编码均无重复或空值。
- 本机有效菜单 `1697`、唯一非空权限 `1285`；测试服有效菜单 `1740`、唯一非空权限 `1325`。
- 共享角色中 `23` 个有效权限集合不同；排除停用菜单后，本机共有 `1676` 个去重后的有效 `role.code + menu.permission` 目标关系。
- 测试服菜单全集缺少本机有效菜单权限 `16` 个，其中实际被本机有效角色使用的权限为 `12` 个、对应菜单定义 `13` 行。
- 本机已分配菜单中另有 `26` 个菜单 ID 在测试服指向不同业务，证明不能复制菜单 ID。
- RED: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_test_tenant1_all_role_permission_sync_sql.py -q` -> FAIL，`5 failed`，预期原因：全角色正式迁移文件尚不存在。

### M7 全角色清单、备份与门禁

- 状态：完成。
- 新增正式 SQL：`IntRuoyiBackend\sql\mysql\20260807_test_tenant1_all_role_permission_sync.sql`。
- 新增合同测试：`IntRuoyiBackend\script\tests\test_test_tenant1_all_role_permission_sync_sql.py`。
- 全角色 SQL 合同：`python -X utf8 -m pytest .\IntRuoyiBackend\script\tests\test_test_tenant1_all_role_permission_sync_sql.py -q` -> PASS，`5 passed`。
- Release migration policy gate：`all-role-migration-policy-gate-latest.json` -> PASS；新迁移 `20260807_test_tenant1_all_role_permission_sync`，`allowedEnvironments=["test"]`，`riskLevel=high`，依赖 `20260728_mes_scheduler_route_flow_list_permission`。
- 测试服 pre-sync snapshot：`sourceRoleCount=60`、`targetResolvedSourceRoleCount=26`、`tenant1ActiveRoleCount=43`、`targetOnlyActiveRoleCount=17`、`missingSourcePermissionCount=159`、`extraSourceRolePermissionCount=104`、`duplicateTargetSourceRoleCodes=0`、`runningTestLockCount=0`。
- 恢复基线：`all-role-pre-sync-permission-tables-baseline.sql`，SHA-256 `80b46168d90a56455c3a84fe34c8e14baa1d17c0d4b6cfd88f1b68000205f5cc`。
- 基线覆盖表：`system_role_category`、`system_role`、`system_menu`、`system_role_menu`、`system_user_role`、`system_tenant_package`、`infra_release_migration`、`infra_release_operation_lock`。

### M8 全角色同步执行与不变量验证

- 状态：完成。
- 测试发布锁：`all-role-sync-lock-acquired.json`，operation id `test-tenant1-all-role-permission-sync-20260807T2008`，release tag `manual-test-tenant1-all-role-permission-sync-20260807`。
- 官方 SQL 执行命令：`apply-test-db-sql.ps1 -SqlPath .\IntRuoyiBackend\sql\mysql\20260807_test_tenant1_all_role_permission_sync.sql -ServerHost 172.30.30.58 -ExpectedServerHost 172.30.30.58` -> `APPLY_TEST_DB_SQL_OK`。
- Post-sync snapshot：`targetResolvedSourceRoleCount=60`、`missingSourcePermissionCount=0`、`extraSourceRolePermissionCount=0`、`tenant1ActiveRoleCount=77`、`targetOnlyActiveRoleCount=17`。
- 用户角色不变量：`tenant1UserRoleHash` 保持 `0696cca53312bb05a836ee2fd4fad7562ef24956202611e3d73798b9f6cf0668`。
- 其它租户不变量：`otherTenantRoleMenuHash` 保持 `1d69f0f91e09f34e343ec7413d3dbf7f747f33e82393e52681b67fd6c133e654`。
- 发布锁释放：`all-role-sync-lock-released.json`，状态 `APPLIED`。
- Final snapshot：`runningTestLockCount=0`，缺失/额外源权限继续为 `0`，源角色解析继续为 `60`。

### M9 缓存与真实页面验证

- 状态：完成。
- Redis pre-delete scan：`redis-permission-cache-before-delete.json`，DB 1 共 10 个权限相关 key，包括 2 个 `user_role_ids:*`、4 个 `menu_role_ids:*`、4 个 `permission_menu_ids:*`。
- Redis delete：`redis-permission-cache-delete.json`，仅按前缀 `menu_role_ids` 与 `permission_menu_ids` 删除 8 个 key，Redis 返回 `8`。
- Redis after-delete scan：`redis-permission-cache-after-delete.json`，仅剩 2 个 `user_role_ids:*`；因 `system_user_role` 未变更，未删除用户角色缓存。
- 账号核对：测试服只读查询确认 `zhaojie` 为租户 1 `芋道源码` 启用账号，`user_id=1074`。
- 登录前置：`node .\scripts\preflight\login-preflight.mjs --base-url http://172.30.30.58:8081/ --tenant 芋道源码 --username zhaojie --target-path /mes/pro/route --target-text 工艺路线` -> PASS；密码从既有本地测试凭据来源读取，未写入日志。
- 页面脚本语法：`node --check .\doc\tasks\20260807-test-permission-role-differential-sync\route_permission_page_check.mjs` -> PASS。
- 页面验收：`node .\doc\tasks\20260807-test-permission-role-differential-sync\route_permission_page_check.mjs` -> PASS，证据 `route-permission-page-check.json`。
- 权限响应包含 `mes:pro-route:query/update/version-query`，不包含 `mes:pro-route:create/delete/export`。
- 工艺路线真实页面 `/admin-api/mes/pro/route/page` HTTP 200、业务 `code=0`、列表总数 `4`；可见按钮包含“产品 / 编辑 / 版本”，不含“删除”。
- 浏览器 `pageErrors=[]`、`consoleErrorCount=0`；5 个 requestfailed 为 iconify JSON 与 `hm.gif` 的非目标 `net::ERR_ABORTED`，不影响目标链路。

## Verification Evidence

- 数据库同步 PASS：`all-role-final-snapshot.json`。
- 缓存处理 PASS：`redis-permission-cache-after-delete.json`。
- 页面验收 PASS：`route-permission-page-check.json` 与 `route-permission-page-check.png`。
- Database evidence validator：`python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence .\doc\tasks\20260807-test-permission-role-differential-sync\database-schema-evidence.md` -> PASS。
- Cleanup preview：`task_closeout.py --task-id 20260807-test-permission-role-differential-sync --mode preview` -> READY，仅计划删除任务自有 `__pycache__`。
- Cleanup apply：`task_closeout.py --task-id 20260807-test-permission-role-differential-sync --mode apply` -> APPLIED，仅删除 `__pycache__\remote_role_permission_sync.cpython-312.pyc`。
- 可复用经验：既有 `docs/database-rules.md#跨环境角色权限差异同步门禁` 已覆盖本任务新增经验，本轮未新建长期经验文档。

## Blockers

- 无。

## Current Status

completed
