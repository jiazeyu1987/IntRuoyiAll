# Execution Log

## User Intent

- 用户要求：在芋道源码的用户管理里面，给随机的 30 个没有任何权限的用户赋予 PQC 的权限角色；如果没有 PQC 的权限角色则创建一个。
- 用户追加变更：改成权限角色小于 2 个的用户。

## BDD Scenarios

- BDD: 已有 PQC 权限角色时分配无权限用户 -> Given `芋道源码` 租户存在 PQC 权限角色且至少 30 个用户没有任何角色 / When 随机选择 30 个无角色用户并绑定该角色 / Then 这 30 个用户拥有 PQC 角色且原本已有角色的用户不被修改。
- BDD: 缺少 PQC 权限角色时先创建再分配 -> Given `芋道源码` 租户不存在可用 PQC 权限角色 / When 执行本任务 / Then 创建带 PQC 菜单权限的角色，并将随机 30 个无角色用户绑定到该角色。
- BDD: 按权限角色数小于 2 分配 PQC 角色 -> Given `芋道源码` 租户至少 30 个启用用户的有效角色数小于 2 / When 随机选择 30 个尚未拥有 PQC 角色的用户并绑定 PQC 角色 / Then 这 30 个用户都拥有 PQC 角色且绑定后有效角色数为 2。

## Milestone Updates

- in_progress: 已读取 `database-schema-delivery` 技能、数据库规则、登录规则、本机运行态规则、服务器访问规则、备份恢复规则、任务收尾规则、PowerShell 编码规则和并发 Git 门禁。
- completed: 本机 MySQL 容器 `int-ruoyi-mysql` 已确认运行，应用本机 JDBC 指向 `127.0.0.1:23306/ruoyi-vue-pro`，未访问远端环境。
- completed: 只读核对表结构：`system_tenant`、`system_users`、`system_role`、`system_user_role`、`system_menu`、`system_role_menu`。
- completed: 只读定位 `芋道源码` 租户 ID 为 1，启用未删除用户数为 2125。
- completed: 严格“无角色绑定”候选数为 0；放宽为“无有效菜单权限”候选数仍为 0。
- completed: 未查到名称或编码含 PQC 的业务角色；实际绑定 PQC 菜单 `900438 一线PQC`、`900435 PQC组长` 的角色只有 `1 超级管理员`。
- blocked: 缺少 30 个符合“没有任何权限”的目标用户，按 fail-fast 和无部分写入原则停止；未创建角色、未绑定用户。
- completed: 用户将目标口径改为“权限角色小于 2 个”；只读核对该口径下候选数为 2045。
- RED: 首次事务在写入前因 `utf8mb4_unicode_ci` 与 `utf8mb4_0900_ai_ci` 字符串比较排序规则不一致失败；复核 `PQC权限角色` 创建数为 0、本任务用户绑定数为 0。
- completed: 使用显式 `utf8mb4_unicode_ci` 变量重跑事务，创建 `PQC权限角色` ID `910438`，绑定菜单 `5100,900220,900435,900438`，插入 30 条用户角色绑定。
- completed: 独立复核角色存在、菜单绑定数为 4、本任务用户角色绑定数为 30、无无效用户、无重复绑定，且 30 名目标用户绑定后有效角色数均为 2。

## Data Safety

- 目标环境：本机数据库；不访问测试服、正式服或备用服。
- 目标租户：`芋道源码`。
- 写入边界：仅允许新增缺失 PQC 角色、角色菜单绑定、以及 30 个目标用户到 PQC 角色的用户角色绑定。
- 回滚方式：删除本次新增的 30 条用户角色绑定；若本次创建了 PQC 角色，则删除该角色及其角色菜单绑定。

## TDD Evidence

- RED: original read-only SQL precondition check -> FAIL, because `芋道源码` has 0 users without roles and 0 users without effective menu permissions.
- RED: first mutation transaction -> FAIL before data mutation due collation mismatch; verification showed role count 0 and inserted user-role count 0.
- GREEN: retry transaction with explicit `utf8mb4_unicode_ci` -> PASS; created role `910438`, selected 30 users, inserted 30 user-role rows.
- REGRESSION: post-write verification -> PASS; role menu count 4, assigned count 30, invalid selected users 0, duplicate bindings 0, selected users with after-role-count not equal to 2 equals 0.

## Blockers

- None for the updated scope. Closeout/commit remains separate because the shared workspace has concurrent unrelated changes.
