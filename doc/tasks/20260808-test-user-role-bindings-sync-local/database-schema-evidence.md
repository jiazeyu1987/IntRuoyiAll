# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: 将本机 `tenant_id=1` 中两边都存在的启用用户有效角色绑定同步到测试服 `172.30.30.58` 的 `tenant_id=1`。
- Affected entities: `system_user_role` 写入；`system_users` 和 `system_role` 只读解析；`infra_release_operation_lock` 记录测试服数据操作锁。
- Stable keys: 用户使用 `system_users.username`，角色使用 `system_role.code`；禁止复制 `user_id`、`role_id`、`system_user_role.id`。

## Database Engine And Migration Tool

- Engine: MySQL in Docker container `intruoyi-mysql`，database `ruoyi-vue-pro`。
- Migration tool: 任务级 SQL `change.sql` 通过官方 `IntRuoyiBackend/script/deploy/apply-test-db-sql.ps1` 执行，非版本化发布迁移。

## Schema, Migration, Fixture, Seed, Index, Or Constraint Changes

- Schema changes: none.
- Migration/data changes: `system_user_role` tenant 1 共同用户范围内有效绑定对齐本机源集合。
- Index evidence: 写前审计确认测试服 `system_user_role` 无重复总 pair、无重复有效 pair；仅使用当前表结构，不新增索引。

## Data Safety Analysis

- Scope boundary: 仅 `tenant_id=1`，仅共同用户范围，未创建测试服缺失用户，未修改角色定义、角色菜单或租户套餐。
- Pre-change source count: local active users `2125`，test active users `2148`，common scope users `2120`。
- Source pair count: resolvable source user-role pairs `2282`；unresolvable source pairs `8`，原因是 5 个本机用户不存在于测试服。
- Planned change: insert/reactivate `142` pairs，soft-delete `12` target-only pairs in common-user scope。
- Security impact: 源角色包含 `doc_control`、`super_admin`、`wenkong`、`wenkong_download`；`wangsiyu` 同步后获得 `doc_control`、`wenkong`、`wenkong_download` 等文控高权限角色。
- Cache safety: 只扫描并删除受影响 `111` 个测试服用户的 `user_role_ids` 精确候选键；实际未发现候选键，删除请求数 `0`。

## Rollback Or Recovery Plan

- Pre-change backup: `pre-change-test-user-role-backup.sql`，SHA-256 `8c409b3de44a3e9846cbc3b6a754f400189bba74834adc9538b3094049230501`。
- Backup tables: `system_users`、`system_role`、`system_user_role`、`infra_release_operation_lock`。
- Rollback SQL: `rollback.sql` 将共同用户范围恢复到写前测试服有效绑定集合。
- Operation lock: `test-tenant1-user-role-binding-sync-20260808T001` acquired then released as `APPLIED`。

## BDD Scenarios

- BDD: user-role binding parity -> Given 本机租户 1 用户按 `username`、角色按 `role.code` 可稳定匹配测试服目标 / When 同步 `system_user_role` 有效绑定 / Then 测试服每个可解析用户的有效角色编码集合与本机一致，且不复制自增 ID、不修改其它租户。
- BDD: missing mapping blocks -> Given 本机存在启用用户或角色在测试服无法唯一解析 / When 生成同步 SQL / Then 当前同步必须 fail fast 并列出缺失或重复业务键，不猜测映射。
- BDD: cache precision -> Given 用户角色绑定发生变化 / When 数据库同步提交 / Then 只删除受影响用户的精确 `user_role_ids` Redis key，不清空全库缓存。

## RED Command And Expected Failure

- RED: `python -X utf8 doc/tasks/20260808-test-user-role-bindings-sync-local/remote_user_role_sync_ops.py run-sql-file --sql doc/tasks/20260808-test-user-role-bindings-sync-local/verify.sql --output doc/tasks/20260808-test-user-role-bindings-sync-local/verify-before-change.json` -> FAIL, acceptance mismatch with `VERIFY_MISSING=142` and `VERIFY_EXTRA=12` before data change.

## GREEN Command And Passing Result

- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend/script/deploy/apply-test-db-sql.ps1 -SqlPath doc/tasks/20260808-test-user-role-bindings-sync-local/change.sql -ServerHost 172.30.30.58 -ExpectedServerHost 172.30.30.58 -Reason tenant1-common-user-role-binding-sync` -> PASS, `COMMITTED` with `soft_deleted_count=12`, `reactivated_count=2`, `inserted_count=140`, `APPLY_TEST_DB_SQL_OK`。
- GREEN: `python -X utf8 doc/tasks/20260808-test-user-role-bindings-sync-local/remote_user_role_sync_ops.py run-sql-file --sql doc/tasks/20260808-test-user-role-bindings-sync-local/verify.sql --output doc/tasks/20260808-test-user-role-bindings-sync-local/verify-after-cache.json` -> PASS, `VERIFY_MISSING=0` and `VERIFY_EXTRA=0`。

## Migration Verification

- Verify counts: `VERIFY_COUNTS 2120 2282 2282`。
- Verify parity: `VERIFY_MISSING 0` and `VERIFY_EXTRA 0`。
- Verify `wangsiyu` roles: `approval_center_entry,dcc_action_distribute_independent,dcc_action_view_independent,dcc_distribute_e2e,doc_control,wenkong,wenkong_download`。
- Verify high-risk DCC grants: `7` dangerous grants across `wangsiyu` and `zhaohaichen` for directory/category/download permissions, recorded in `verify-dcc-dangerous-detail.json`。
- Verify tenant boundary: other tenant `system_user_role` hash remains `b460456a55b8f40b578bfb1512d4006e11d45de29beb56cc76f77cb82e76e117`。

## Blockers

- No active blocker for the approved scope.
- Out of scope by user instruction: 5 local users absent on test (`edhrmatrixapprover`、`pqce2efullscreen`、`smokeappr1`、`smokeerp1`、`smokeread1`) and their 8 source role bindings were not created or guessed.

## Evidence Validation

- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260808-test-user-role-bindings-sync-local/database-schema-evidence.md` -> PASS, `Database schema evidence is valid.`。
