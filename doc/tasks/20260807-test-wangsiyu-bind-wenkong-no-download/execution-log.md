# 执行日志

## 2026-08-07

- User intent: “帮我绑定文控”。
- Scope: 测试服务器 `172.30.30.58`、租户 `芋道源码(tenant_id=1)`、用户 `wangsiyu(id=910250)`。
- Safety decision: 按上一轮诊断建议采用 `wenkong_no_download(910417)`，只恢复文控入口，不绑定含下载能力的 `wenkong` / `wenkong_download`。
- Rules read: `docs/server-access.md`, `docs/login-access.md`, `docs/database-rules.md`, `docs/release-backup-restore.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`。
- Skill used: `database-schema-delivery`，已读取 `references/database-contract.md`。
- BDD: wangsiyu safe DCC menu binding -> Given 测试服芋道源码 `wangsiyu` 当前只有审批中心入口角色且看不到文控, When 绑定已有 `wenkong_no_download` 并清理该用户精确角色缓存, Then 该用户重新登录后应获得文控中心、电子签名、基础数据入口，同时没有 DCC 下载放行来源。
- RED: 上一轮只读诊断 -> FAIL，expected reason：测试服 `wangsiyu` 有效角色只有 `approval_center_entry(910295)`，DCC 权限数为 `0`，`user_role_ids:910250` 也只缓存 `910295`。
- GREEN: 写前门禁核对 -> PASS，`wangsiyu(id=910250, tenant_id=1, status=0, deleted=0)` 唯一启用；`wenkong_no_download(id=910417)` 唯一启用；目标根菜单计数 `3`；候选角色菜单数 `10`；危险菜单权限、角色/用户/岗位/部门链类别和目录下载规则、活动动态授权均为 `0`。
- GREEN: `change.sql` 测试服 MySQL 单事务执行 -> PASS，返回 `COMMITTED 910250 910417 4236`，新增 `system_user_role.id=4236`。
- GREEN: 精确 Redis 缓存清理 -> PASS，仅请求删除 `user_role_ids:910250` / `user_role_ids::910250`；DB 0 `deleted=0`，DB 1 `before=1 deleted=1 after=0`；未清全库。
- RED: 首次 `verify.sql` -> FAIL，expected reason：后半段统计 `system_entitlement_grant.status='ACTIVE'` 遇到 `ERROR 1267 Illegal mix of collations`；前半段已返回用户、有效角色和三个根菜单；已修正脚本字面量排序规则后重跑。
- GREEN: 修正后 `verify.sql` -> PASS，有效角色为 `approval_center_entry(910295)` 与 `wenkong_no_download(910417)`；根菜单 `6800 文控中心`、`900218 电子签名`、`990200 基础数据` 均由 `wenkong_no_download` 解析；`NO_DOWNLOAD_ROLE_SUMMARY 910417 wenkong_no_download 10 0`；`NO_DOWNLOAD_RULE_COUNTS 0 0 0 0 0 0 0 0`；活动动态授权计数 `0`。
- GREEN: `rollback.sql` 语法验证 -> PASS，替换业务 `CALL` 为 `SELECT 'ROLLBACK_SYNTAX_ONLY'` 后远端 MySQL 返回 `ROLLBACK_SYNTAX_ONLY`；未执行回滚。
- UI/API: 未持有 `wangsiyu` 账号密码或活动 token，未冒充已登录页面验收；用户重新登录后将重新拉取权限。
- NO FALLBACK: 未绑定含下载能力的 `wenkong` / `wenkong_download`，未清理全库 Redis，未修改角色定义、角色菜单、其它用户或代码。
- GREEN: Database schema validator self-test -> PASS，`Database schema validator self-test passed.`。
- GREEN: Database schema evidence validator -> PASS，`Database schema evidence is valid.`。
- GREEN: `git diff --check -- doc/tasks/20260807-test-wangsiyu-bind-wenkong-no-download` -> PASS。
- GREEN: UTF-8 读取校验 -> PASS，任务目录内 `change.sql`、`verify.sql`、`rollback.sql`、`task.md`、`execution-log.md`、`verification-report.md`、`database-schema-evidence.md` 均可按 UTF-8 读取。
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260807-test-wangsiyu-bind-wenkong-no-download --mode preview` -> PASS；keep 为七个任务证据文件，delete/blocked/warnings 均为 `<none>`。
- CLEANUP APPLY: 同一脚本 `--mode apply` -> PASS；未删除任何文件，当前为主 worktree，未执行 worktree merge/remove。
- EXPERIENCE CONSOLIDATION: 现有 `docs/database-rules.md#DCC 菜单恢复与无下载角色隔离门禁` 已覆盖本次经验，不新增长期经验文档。
- FINAL STATUS: completed；本任务未执行 Git stage/commit/push。
