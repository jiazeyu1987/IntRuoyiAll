# 测试服文控角色菜单解绑

## Task Goal

按用户要求在测试服务器 `172.30.30.58` 的 `芋道源码` 租户中，将 `doc_control / 文控` 角色绑定的 `角色管理(101)`、`权限角色(900183)`、`审批角色(6804)` 三个菜单关系移除，避免 `zhaohaichen` 因继承文控角色看到对应入口。

## Milestones

1. 创建任务记录并核对远端数据变更规则。`completed`
2. 只读备份并确认 `doc_control` 当前绑定目标菜单。`completed`
3. 在测试服精确软删除目标 `system_role_menu` 关系。`completed`
4. 复验 `zhaohaichen` 与 `doc_control` 不再绑定目标菜单。`completed`
5. 记录验证报告和收尾状态。`completed`

## Expected Verification

- 远端只读查询确认目标租户、角色、用户和菜单绑定范围。
- 写入 SQL 使用精确条件：`tenant_id=1`、`role_id=910233`、`menu_id IN (101,900183,6804)`、`deleted=0`。
- 写入后复验目标绑定计数为 `0`，且 `zhaohaichen` 通过 `doc_control` 不再继承这三个菜单。
- 不修改其它角色、菜单、租户套餐、用户账号或业务数据。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本次按用户指定移除测试服 `doc_control` 角色的实际菜单绑定，不通过前端隐藏或权限绕过掩盖。
- `是否存在临时补丁或绕过`：否。

## Experience Gates

- Trigger: Windows PowerShell 通过 SSH 操作测试服 MySQL，尤其涉及容器内 `MYSQL_ROOT_PASSWORD`、中文菜单名、菜单权限关系。
- Preflight check: 使用 `docker exec -i intruoyi-mysql sh -lc 'mysql ... -p"$MYSQL_ROOT_PASSWORD" ...'` 并通过标准输入传 SQL，避免本地 PowerShell 或远端宿主机提前展开密钥；写入前先只读核对真实库、目标租户、角色、用户和菜单关系。
- Blocker: SSH/SQL 输出为空、PowerShell 本地解析 SQL、远端 MySQL 报错、目标绑定数量不等于预期、或查询输出可能包含 secret 且未脱敏时必须停止。
- Verification: 记录 SSH/MySQL 退出码、只读备份摘要、目标关系更新前后数量、`zhaohaichen` 菜单继承复验；不记录数据库密码或完整环境变量。
- Forbidden action: 禁止直接打印 `.env`、密码、token；禁止用前端隐藏、菜单名改文案或租户套餐改动替代移除角色菜单绑定；禁止扩大删除范围到其它角色/租户/菜单。
- Evidence: `docs/experience-index.md` 路由到远端 MySQL / PowerShell SSH 门禁，`docs/database-rules.md#租户和菜单权限`，`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-03-不带数据测试服发布前置门禁`。

## Verification Evidence

- 只读备份：测试服 `doc_control / 文控` 角色 `910233` 写入前绑定目标菜单 `101/6804/900183` 共 `3` 行，主键分别为 `905348/904555/905535`。
- 写入结果：事务内精确软删除 `system_role_menu.id IN (904555,905348,905535)`，`UPDATED_ROWS=3`，`TX_RESULT=COMMITTED`。
- 写入后复验：目标活动绑定计数 `TARGET_ACTIVE_AFTER=0`；三条关系 `deleted=1` 且 `updater=20260730-remove-doc-control-role-menus-test`。
- `zhaohaichen` 复验：按其启用角色链路查询 `101/6804/900183` 不再返回任何菜单行。
- 缓存核对：`/system/auth/get-permission-info` 使用 `getRoleMenuListByRoleId(...)` 直接按角色读取 `system_role_menu`，该方法未走 `MENU_ROLE_ID_LIST` 缓存；无需删除菜单定义或租户套餐。

## Closeout Evidence

- `task-closeout-cleanup preview`：keep `task.md`、`execution-log.md`、`verification-report.md`；delete `<none>`；blocked `<none>`；warnings `<none>`。
- `task-closeout-cleanup apply`：applied；deleted_paths `<none>`；linked worktree `false`。
- 当前工作区存在本任务外既有状态：`int_main...origin/int_main [ahead 9]` 及并行脏改动；本任务未修改或清理这些并行文件。
