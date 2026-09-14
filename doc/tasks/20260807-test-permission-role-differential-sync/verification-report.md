# 验证报告：测试服务器权限角色差异同步

## Result

PASS。测试服租户 1 全部本机源角色权限已按稳定键差异同步完成；测试服专属角色、用户角色绑定和其它租户角色菜单均保持不变；代表账号 `zhaojie` 真实页面验收通过。

## Database Evidence

- SQL 合同测试：`python -X utf8 -m pytest .\IntRuoyiBackend\script\tests\test_test_tenant1_all_role_permission_sync_sql.py -q` -> PASS，`5 passed`。
- 发布迁移门禁：`all-role-migration-policy-gate-latest.json` -> `passed`；迁移 `20260807_test_tenant1_all_role_permission_sync` 仅允许 `test`，`riskLevel=high`。
- 备份基线：`all-role-pre-sync-permission-tables-baseline.sql`，SHA-256 `80b46168d90a56455c3a84fe34c8e14baa1d17c0d4b6cfd88f1b68000205f5cc`。
- 测试服 SQL 执行：官方 `apply-test-db-sql.ps1` 返回 `APPLY_TEST_DB_SQL_OK`。
- 最终差异：源角色解析 `26 -> 60`；缺失源权限 `159 -> 0`；额外源角色权限 `104 -> 0`；重复源角色编码 `0`。
- 不变量：`system_user_role` 业务哈希、其它租户角色菜单哈希、测试服专属角色权限哈希均保持不变。
- 发布锁：`test-tenant1-all-role-permission-sync-20260807T2008` 以 `APPLIED` 释放，最终 `runningTestLockCount=0`。

## Cache Evidence

- Redis 预清理：DB 1 共 10 个权限相关 key，包括 2 个 `user_role_ids:*`、4 个 `menu_role_ids:*`、4 个 `permission_menu_ids:*`。
- Redis 精确删除：只删除 `menu_role_ids` 与 `permission_menu_ids` 前缀，共请求删除 8 个 key，Redis 返回 `8`。
- Redis 复扫：仅剩 2 个 `user_role_ids:*`；因 `system_user_role` 未变更，未删除用户角色缓存，未清空全库。

## Page Evidence

- 登录前置：`node .\scripts\preflight\login-preflight.mjs --base-url http://172.30.30.58:8081/ --tenant 芋道源码 --username zhaojie ... --target-path /mes/pro/route --target-text 工艺路线` -> PASS。
- 页面脚本：`node .\doc\tasks\20260807-test-permission-role-differential-sync\route_permission_page_check.mjs` -> PASS。
- 权限响应：包含 `mes:pro-route:query`、`mes:pro-route:update`、`mes:pro-route:version-query`；不包含 `mes:pro-route:create`、`mes:pro-route:delete`、`mes:pro-route:export`。
- 工艺路线页面：`/admin-api/mes/pro/route/page` HTTP 200、业务 `code=0`、总数 `4`；可见按钮包含“产品 / 编辑 / 版本”，不含“删除”。
- 浏览器健康：`pageErrors=[]`、`consoleErrorCount=0`；5 个 `requestfailed` 均为 iconify JSON 或 `hm.gif` 的非目标 `net::ERR_ABORTED`，不影响目标链路。

## Final Status

completed。验证、文档更新、database evidence validator 和 cleanup preview/apply 均已通过；cleanup 仅删除任务自有 `__pycache__`，所有恢复与验收证据保留。
