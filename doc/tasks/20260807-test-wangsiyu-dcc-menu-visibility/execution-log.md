# 执行日志

## 2026-08-07

- User intent: 解释测试服务器 `wangsiyu` 账号为什么看不到文控里的页签。
- Scope: 测试服务器 `172.30.30.58`，优先只读诊断，不直接修改权限、缓存或运行态。
- BDD: wangsiyu DCC menu visibility -> Given 测试服芋道源码租户存在 `wangsiyu` 账号, When 该账号登录测试服前端, Then 可见菜单应由其有效角色、角色菜单权限、租户菜单包和登录权限缓存共同决定。
- Rules read: `docs/server-access.md`, `docs/login-access.md`, `docs/database-rules.md`, `docs/e2e-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`.
- Experience gate: `docs/experience-index.md` 命中 DCC 菜单恢复、跨环境角色同步、用户角色缓存 `user_role_ids`，本任务采用只读核对优先。
- RED: 测试服只读 SQL 初版 -> FAIL，expected reason：`system_users.username` 与变量排序规则不一致触发 `ERROR 1267 Illegal mix of collations`，未读取业务结果、未改数据。
- RED: 测试服只读 SQL 临时表版 -> FAIL，expected reason：同一语句多次引用临时表触发 MySQL `ERROR 1137 Can't reopen table`，未读取业务结果、未改数据。
- GREEN: 测试服字段 collation 核对 -> PASS，`system_users.username`、`system_role.code/name`、`system_menu.permission/name` 均为 `utf8mb4_unicode_ci`。
- GREEN: 测试服变量驱动只读 SQL -> PASS，`wangsiyu(id=910250, tenant_id=1, tenantName=芋道源码)` 唯一命中；有效角色只有 `approval_center_entry(910295)`，DCC 权限数 `0`，根菜单 `6800/900218/990200` 均未由有效角色解析。
- TEST DB EVIDENCE: 历史绑定 `doc_control(910233)` 与 `wenkong_download(910234)` 在测试服均为 `bindingDeletedHex=1`，更新时间 `2026-08-02T23:54:20`；候选 `wenkong_no_download(910417)` 存在，覆盖 `6800/900218/990200` 且危险权限计数 `0`，但未绑定给 `wangsiyu`。
- GREEN: 本机 Docker MySQL 只读对比 -> PASS，本机租户 1 同名用户 `wangsiyu(id=910250)` 有效角色包含 `wenkong(910231)`、`wenkong_download(910232)`、`approval_center_entry(910295)`、`doc_control(910218)`、`dcc_distribute_e2e(910431)`、`dcc_action_view_independent(910432)`、`dcc_action_distribute_independent(910435)`。
- GREEN: 测试服 Redis 精确缓存只读核对 -> PASS，DB 1 存在 `user_role_ids:910250=["java.util.HashSet",[[ "java.lang.Long",910295]]]`，DB 0 无该键；未删除任何缓存。
- CONCLUSION: 上次跨环境角色权限同步只同步角色定义和 `system_role_menu`，并保持 `system_user_role` 哈希不变；因此测试服角色定义已存在，但 `wangsiyu` 的用户角色绑定没有跟随本机平移。
- NO WRITE: 本次未修改测试服 MySQL、Redis、代码或运行态。
