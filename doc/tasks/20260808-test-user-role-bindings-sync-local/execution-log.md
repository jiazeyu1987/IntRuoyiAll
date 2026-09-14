# 执行日志

## 2026-08-08

- User intent: “把本机每个用户绑定了哪些角色也复制过去”。
- Scope: 本机 `tenant_id=1/芋道源码` 到测试服 `tenant_id=1/芋道源码` 的 `system_user_role` 有效绑定同步。
- Skills used: `security-privacy-compliance-review`、`database-schema-delivery`；已读取对应合同文件。
- Rules read: `docs/server-access.md`、`docs/database-rules.md`、`docs/release-backup-restore.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`。
- Experience gates: 命中 `跨环境角色权限差异同步门禁`、`DCC 菜单恢复与无下载角色隔离门禁`、`系统角色菜单授权 tenant 1 admin 门禁`。
- BDD: user-role binding parity -> Given 本机租户 1 用户按 `username`、角色按 `role.code` 可稳定匹配测试服目标 / When 同步 `system_user_role` 有效绑定 / Then 测试服每个可解析用户的有效角色编码集合与本机一致，且不复制自增 ID、不修改其它租户。
- BDD: missing mapping blocks -> Given 本机存在启用用户或角色在测试服无法唯一解析 / When 生成同步 SQL / Then 当前同步必须 fail fast 并列出缺失或重复业务键，不猜测映射。
- BDD: cache precision -> Given 用户角色绑定发生变化 / When 数据库同步提交 / Then 只删除受影响用户的精确 `user_role_ids` Redis key，不清空全库缓存。
- User scope update: “只同步两边都存在的用户角色绑定”；本任务不创建测试服缺失用户，不猜测绑定。
- Generated plan: common users `2120`，resolvable source pairs `2282`，unresolvable source pairs `8`，planned insert/reactivate `142`，planned soft-delete `12`。
- Missing test users intentionally excluded: `edhrmatrixapprover`、`pqce2efullscreen`、`smokeappr1`、`smokeerp1`、`smokeread1`。
- RED: `python -X utf8 doc/tasks/20260808-test-user-role-bindings-sync-local/remote_user_role_sync_ops.py run-sql-file --sql doc/tasks/20260808-test-user-role-bindings-sync-local/verify.sql --output doc/tasks/20260808-test-user-role-bindings-sync-local/verify-before-change.json` -> FAIL, acceptance mismatch before sync with `VERIFY_MISSING=142` and `VERIFY_EXTRA=12`。
- Backup: `pre-change-test-user-role-backup.sql` captured `system_users`、`system_role`、`system_user_role`、`infra_release_operation_lock`; SHA-256 `8c409b3de44a3e9846cbc3b6a754f400189bba74834adc9538b3094049230501`。
- Lock: acquired `test-tenant1-user-role-binding-sync-20260808T001` with releaseTag `manual-test-tenant1-user-role-binding-sync-20260808`。
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend/script/deploy/apply-test-db-sql.ps1 -SqlPath doc/tasks/20260808-test-user-role-bindings-sync-local/change.sql -ServerHost 172.30.30.58 -ExpectedServerHost 172.30.30.58 -Reason tenant1-common-user-role-binding-sync` -> PASS, `COMMITTED` with soft-deleted `12`, reactivated `2`, inserted `140`, and `APPLY_TEST_DB_SQL_OK`。
- GREEN: `python -X utf8 doc/tasks/20260808-test-user-role-bindings-sync-local/remote_user_role_sync_ops.py run-sql-file --sql doc/tasks/20260808-test-user-role-bindings-sync-local/verify.sql --output doc/tasks/20260808-test-user-role-bindings-sync-local/verify-after-cache.json` -> PASS, `VERIFY_MISSING=0`, `VERIFY_EXTRA=0`, other-tenant hash unchanged。
- Cache precision: affected test users `111`; Redis scan found `0` matching `user_role_ids` keys; delete request count `0`，未清空 Redis。
- DCC security impact: `wangsiyu` now has `approval_center_entry,dcc_action_distribute_independent,dcc_action_view_independent,dcc_distribute_e2e,doc_control,wenkong,wenkong_download`; test-only `wenkong_no_download` was soft-deleted for `wangsiyu` because local source did not have it。
- DCC dangerous permission detail: `verify-dcc-dangerous-detail.json` records 7 grants across `wangsiyu` and `zhaohaichen` for directory/category/download permissions。
- Lock release: released operation lock as `APPLIED`。
- Evidence validator: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260808-test-user-role-bindings-sync-local/database-schema-evidence.md` -> PASS, `Database schema evidence is valid.`。
- UTF-8 check: task core Markdown files read successfully with `python -X utf8`。
- Project experience consolidation: existing `docs/database-rules.md` and `docs/experience-index.md` already contain the applicable cross-environment role sync and DCC no-download isolation gates; no new long-term experience document created。
- Cleanup preview/apply: `task_closeout.py --task-id 20260808-test-user-role-bindings-sync-local --mode preview/apply` -> PASS, only deleted `doc/tasks/20260808-test-user-role-bindings-sync-local/__pycache__/remote_user_role_sync_ops.cpython-312.pyc`; all backup, rollback, SQL and verification artifacts kept。
- Final status: `task.md` marked `completed`。
