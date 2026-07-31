# 测试服文控角色菜单解绑执行日志

## User Intent

- 用户要求：“把文控绑定的这三个角色去掉”。
- 结合上一轮只读回读，本次范围解释为：从测试服 `doc_control / 文控` 角色解绑 `101 角色管理`、`900183 权限角色`、`6804 审批角色` 三个菜单关系。

## Rule Reads

- 已读取 `docs/server-access.md`：测试服主机 `172.30.30.58`，远端操作需明确授权。
- 已读取 `docs/login-access.md`：测试服登录/远端环境需记录授权范围，不记录凭据。
- 已读取 `docs/database-rules.md`：菜单权限数据操作需核对真实库结构、目标租户、角色菜单绑定与验证结果。
- 已读取 `docs/powershell-encoding.md`：中文与 SSH/MySQL stdin 使用 UTF-8 安全路径。
- 已读取 `docs/task-closeout-rules.md`：任务需记录验证与收尾状态。
- 已读取 `docs/release-backup-restore.md`：远端操作不得切换环境或隐式回滚。
- 已读取 `docs/powershell-memory.md`：PowerShell/SSH/MySQL 多行命令需明确退出码和敏感信息脱敏。

## BDD / TDD Evidence

- BDD: 文控角色不再暴露角色管理入口 -> Given 测试服 `zhaohaichen` 仅通过 `doc_control` 继承角色管理相关菜单 When 移除 `doc_control` 对 `101/900183/6804` 的菜单绑定 Then `zhaohaichen` 不再通过该角色看到 `角色管理/权限角色/审批角色` 入口。
- RED: 只读远端 SQL -> FAIL, 写入前 `doc_control` 仍绑定 `101/900183/6804`，`zhaohaichen` 仍继承这些菜单。

## Experience Gate Summary

- 使用 SSH 标准输入向测试服 MySQL 传 SQL，避免本地 PowerShell 展开 `$MYSQL_ROOT_PASSWORD`。
- 写入前后均以真实库 `ruoyi-vue-pro`、租户 `1`、角色 `910233/doc_control`、用户 `zhaohaichen` 和菜单 `101/900183/6804` 为精确核对范围。
- 敏感值只记录字段名或脱敏摘要，不记录数据库密码、token 或完整环境变量。

## Execution Evidence

- PRECHECK: 真实库 `ruoyi-vue-pro` 表结构确认 `system_role_menu` 具备 `id/role_id/menu_id/tenant_id/deleted/updater/update_time` 字段，支持按主键软删除。
- PRECHECK: `TARGET_ROLE` 返回 `910233 / doc_control / 文控 / tenant_id=1 / status=0 / deleted=0`。
- PRECHECK: `TARGET_USER` 返回 `376 / zhaohaichen / 赵海辰 / tenant_id=1 / status=0 / deleted=0`。
- PRECHECK: `TARGET_BINDING_BEFORE` 返回 3 行：`905348 -> 101 角色管理`、`904555 -> 6804 审批角色`、`905535 -> 900183 权限角色`。
- PRECHECK: `ZHAOHAICHEN_MENU_BEFORE` 返回 `doc_control` 继承的 `101/6804/900183` 三个菜单。
- WRITE: 精确事务更新 `system_role_menu`，条件为 `tenant_id=1 AND role_id=910233 AND id IN (904555,905348,905535) AND menu_id IN (101,6804,900183) AND deleted=0`；结果 `UPDATED_ROWS=3`、`TX_RESULT=COMMITTED`。
- GREEN: 写入后远端 SQL -> PASS，`TARGET_ACTIVE_AFTER=0`；三条关系均为 `deleted=1`，`updater=20260730-remove-doc-control-role-menus-test`。
- GREEN: `zhaohaichen` 菜单继承复验 -> PASS，按启用角色链路查询 `101/6804/900183` 无返回行。
- CACHE CHECK: 代码核对 `/system/auth/get-permission-info` 从 `permissionService.getRoleMenuListByRoleId(...)` 获取授权菜单；`getRoleMenuListByRoleId(Collection<Long>)` 未加 `@Cacheable`，无需额外清 Redis。现有已登录前端会话可能需要刷新或重新登录以重新拉取菜单树。
- EXPERIENCE CONSOLIDATION: 已按 `project-experience-consolidation` 技能搜索并复用现有 `docs/experience-index.md`、`docs/powershell-preflight-lessons.md`、`release-build-preflight-lessons.md` 远端 MySQL / SSH 标准输入门禁；本次没有新增可复用长期经验，不新建长期文档。

## Blockers / Residual Risks

- 当前主分支已有非本任务状态：`int_main...origin/int_main [ahead 9]`，且存在并行脏改动与未跟踪任务目录。本次不混入这些并行改动。

## Closeout

- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260730-remove-doc-control-role-menus-test --mode preview` -> PASS，keep 核心三文件，delete `<none>`，blocked `<none>`，warnings `<none>`。
- CLEANUP APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260730-remove-doc-control-role-menus-test --mode apply` -> PASS，deleted_paths `<none>`。
- FINAL STATUS: 数据修复、验证和 cleanup 已完成；任务状态更新为 `completed`。

## Reopen: Approval Role Still Visible

- 用户反馈：`还是可以看到审批角色`。
- RECHECK: 远端只读 SQL -> PASS，`zhaohaichen` 当前启用角色为 `doc_control` 与 `approval_center_entry`；静态角色菜单中 `101/900183/6804` 无活动绑定。
- RECHECK: 动态授权账本 -> PASS，`system_entitlement_grant` 对 `zhaohaichen(id=376)` 无 `101/900183/6804` 活动授权。
- RECHECK: 同名菜单定义 -> PASS，测试服只有一条 `审批角色` 菜单定义 `id=6804/path=approval-role`，但用户没有授权关系。
- PERMISSION API: 使用 `zhaohaichen` 现有有效 token 只读调用 `/admin-api/system/auth/get-permission-info`，仅输出匹配计数，不打印 token；结果 `approval_role_count=0`、`approval_role_name_count=0`、`permission_manage_count=0`。
- 结论：服务端返回菜单已不含 `审批角色`；用户仍可见来自当前浏览器会话/前端路由缓存。下一步吊销 `zhaohaichen` 测试服现有 access/refresh token 和 Redis token 缓存，强制重新登录拉取新菜单。
- TOKEN REVOKE: 先盘点 `zhaohaichen(id=376)` 测试服现有 token，`access_tokens=6`、`refresh_tokens=6`；Redis token 键删除计数为 `0`，说明当前 Redis 未命中这些键或已无对应缓存。
- TOKEN REVOKE DB: 使用已验证 MySQL stdin 通道更新 `system_oauth2_access_token` 与 `system_oauth2_refresh_token`，结果 `access_updated=6`、`refresh_updated=6`。
- TOKEN REVOKE VERIFY: 写入后 `active_access=0`、`active_refresh=0`。
- OLD TOKEN CHECK: 使用一枚已吊销 token 调 `/admin-api/system/auth/get-permission-info`，只输出状态摘要不打印 token；结果 `auth_missing_count=1`、`approval_markers=0`。旧会话已经不能继续通过后端获取菜单。
- CLEANUP PREVIEW 2: `task-closeout-cleanup --mode preview` -> PASS，delete `<none>`，blocked `<none>`。
- CLEANUP APPLY 2: `task-closeout-cleanup --mode apply` -> PASS，deleted_paths `<none>`。
- FINAL STATUS 2: 追加旧会话吊销完成，任务状态更新为 `completed`。
