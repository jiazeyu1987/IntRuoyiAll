# 测试服用户角色绑定按本机同步

## Task Goal

将本机 `tenant_id=1/芋道源码` 中每个启用用户当前有效绑定的角色，按稳定业务键同步到测试服务器 `172.30.30.58` 的 `tenant_id=1/芋道源码`。用户确认最终范围为“只同步两边都存在的用户角色绑定”。

## Scope

- 源环境：本机 Docker MySQL `int-ruoyi-mysql`，数据库 `ruoyi-vue-pro`，`tenant_id=1`。
- 目标环境：测试服务器 `172.30.30.58`，MySQL `ruoyi-vue-pro`，`tenant_id=1`。
- 同步对象：`system_user_role` 有效绑定。
- 用户匹配键：`system_users.username`。
- 角色匹配键：`system_role.code`。
- 明确排除：不创建用户、不复制本机自增 ID、不同步其它租户、不修改角色定义、不修改角色菜单、不清空 Redis。

## Milestones

- [x] 读取服务器、数据库、发布回滚、PowerShell 编码、权限审查和数据库交付规则。
- [x] 建立任务记录并登记适用经验门禁。
- [x] 只读审计本机与测试服用户、角色和用户角色绑定差异。
- [x] 生成精确备份、回滚路径和正式变更 SQL。
- [x] 执行测试服用户角色绑定同步。
- [x] 精确刷新受影响用户角色缓存并复验。
- [x] 输出验证报告与收尾记录。

## Expected Verification

- 本机源绑定必须按 `username + role.code` 导出，禁止复制 `user_id` / `role_id` 自增值。
- 测试服目标用户和角色必须唯一可解析；缺失或重复时阻塞，不做猜测映射。
- 变更前必须备份目标 `system_user_role` 精确范围，并记录回滚 SQL。
- 写入后目标有效 `username + role.code` 集合必须与本机源集合一致。
- 其它租户用户角色绑定哈希保持不变。
- Redis 只删除受影响用户的 `user_role_ids:<userId>` / `user_role_ids::<userId>` 精确候选键。

## Applicable Experience Gates

- `docs/database-rules.md#跨环境角色权限差异同步门禁`：按稳定键同步，不复制自增 ID，不删除全量角色，不用 API-only 代替权限链验证。
- `docs/database-rules.md#DCC 菜单恢复与无下载角色隔离门禁`：同步用户角色会恢复高权限 DCC 角色时，必须显式记录下载/目录管理风险，不用“看不到下载按钮”代替后端权限链。
- `docs/database-rules.md#系统角色菜单授权-tenant-1-admin-门禁`：用户角色链路必须按正式角色和菜单解析验证。
- `docs/server-access.md` 与 `docs/release-backup-restore.md`：只操作当前授权测试服务器，写前有精确备份和回滚路径。
- `docs/powershell-encoding.md`：中文 SQL、JSON 和任务文档使用 UTF-8 安全通道。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按正式用户角色绑定源头同步。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

测试服 `tenant_id=1` 共同用户范围已同步完成：源绑定 `2282` 条，写入结果为软删 `12` 条、恢复 `2` 条、插入 `140` 条；同步后缺失 `0` 条、额外 `0` 条，其它租户 `system_user_role` 哈希未变化。Redis 精确扫描受影响 `111` 个用户未发现 `user_role_ids` 缓存键，因此删除请求数为 `0`。数据库 evidence validator 通过，cleanup apply 仅删除本任务 `__pycache__`，审计证据已保留。

## Cleanup Keep

- doc/tasks/20260808-test-user-role-bindings-sync-local/affected-user-ids-test.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/affected-users.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/change.sql
- doc/tasks/20260808-test-user-role-bindings-sync-local/database-schema-evidence.md
- doc/tasks/20260808-test-user-role-bindings-sync-local/generate_user_role_sync_sql.py
- doc/tasks/20260808-test-user-role-bindings-sync-local/lock-acquired.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/lock-released.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/pre-change-test-user-role-backup-manifest.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/pre-change-test-user-role-backup.sql
- doc/tasks/20260808-test-user-role-bindings-sync-local/redis-user-role-delete.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/redis-user-role-scan-before-delete.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/remote_user_role_sync_ops.py
- doc/tasks/20260808-test-user-role-bindings-sync-local/rollback.sql
- doc/tasks/20260808-test-user-role-bindings-sync-local/schema-local.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/schema-test.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/source-user-role-audit-local.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/sync-plan.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/system-user-role-index-local.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/system-user-role-index-test.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/target-user-role-audit-test.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/user-role-diff-summary.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/verification-report.md
- doc/tasks/20260808-test-user-role-bindings-sync-local/verify-after-cache.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/verify-after-change-before-cache.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/verify-before-change.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/verify-dcc-dangerous-detail.json
- doc/tasks/20260808-test-user-role-bindings-sync-local/verify-dcc-dangerous-detail.sql
- doc/tasks/20260808-test-user-role-bindings-sync-local/verify.sql
