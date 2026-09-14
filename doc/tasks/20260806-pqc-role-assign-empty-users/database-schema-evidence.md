# Database Schema Evidence

## Feature / Data Change Goal

- 在本机 `芋道源码` 租户中，为随机 30 个有效权限角色数小于 2 的用户授予 PQC 权限角色。
- 如果缺少 PQC 权限角色，则创建角色并绑定 PQC 相关菜单权限。

## Acceptance

- 30 个本次选中的用户在执行前有效角色数小于 2，且尚未拥有 PQC 角色。
- 执行后这 30 个用户均绑定同一个 PQC 权限角色。
- PQC 权限角色存在且具备 PQC 菜单权限。
- 不修改非目标租户、非目标用户和无关角色。

## Database Engine And Migration Tool

- Engine: MySQL in local Docker container `int-ruoyi-mysql`, database `ruoyi-vue-pro`.
- Migration tool: 不新增发布迁移；本任务是本机数据操作，需记录 SQL、影响行数和回滚 SQL。

## Schema / Data Scope

- verified: `system_tenant`
- verified: `system_users`
- verified: `system_role`
- verified: `system_user_role`
- verified: `system_menu`
- verified: `system_role_menu`
- target tenant: `芋道源码`, tenant ID `1`
- target PQC menu candidates: `900438 一线PQC`, `900435 PQC组长`; parent chain includes `900220 eDHR批记录` and `5100 MES 系统`.

## Data Safety Analysis

- 不访问远端环境。
- 不输出密码、token、私钥或连接串密钥。
- 写入前必须先只读导出目标用户、角色和菜单 ID 清单。
- Candidate pool check failed before any write: `芋道源码` active undeleted users = 2125, no-role users = 0, no-effective-menu-permission users = 0.
- Existing PQC role check: no business role name/code contains PQC; only `super_admin` currently has both PQC menus, and it is not a valid business PQC role to assign.
- Updated scope candidate pool: users with effective role count less than 2 = 2045.
- Data mutation completed in local MySQL only: created role `910438` / `pqc_permission`, inserted 4 role-menu bindings and 30 user-role bindings.

## Rollback Or Recovery Plan

- 删除本次新增的 30 条 `system_user_role` 记录。
- 如果本次创建新 PQC 角色，则删除该角色的 `system_role_menu` 绑定，再删除该 `system_role`。
- 回滚前后均按同一租户和记录清单复核。

## BDD Scenarios

- BDD: 已有 PQC 权限角色时分配无权限用户 -> Given `芋道源码` 租户存在 PQC 权限角色且至少 30 个用户没有任何角色 / When 随机选择 30 个无角色用户并绑定该角色 / Then 这 30 个用户拥有 PQC 角色且原本已有角色的用户不被修改。
- BDD: 缺少 PQC 权限角色时先创建再分配 -> Given `芋道源码` 租户不存在可用 PQC 权限角色 / When 执行本任务 / Then 创建带 PQC 菜单权限的角色，并将随机 30 个无角色用户绑定到该角色。
- BDD: 按权限角色数小于 2 分配 PQC 角色 -> Given `芋道源码` 租户至少 30 个启用用户的有效角色数小于 2 / When 随机选择 30 个尚未拥有 PQC 角色的用户并绑定 PQC 角色 / Then 这 30 个用户都拥有 PQC 角色且绑定后有效角色数为 2。

## RED

- RED: read-only SQL precondition check -> FAIL, expected target pool is unavailable because `芋道源码` has 0 users without roles and 0 users without effective menu permissions.
- RED: first mutation transaction -> FAIL before data mutation due explicit collation mismatch; follow-up verification showed no role or user-role rows were inserted.

## GREEN

- GREEN: retry transaction with explicit `utf8mb4_unicode_ci` variables -> PASS; role `910438` created and 30 user-role rows inserted.

## Migration Verification

- Verification: role `910438` exists with name `PQC权限角色`, code `pqc_permission`, tenant `1`.
- Verification: role menu IDs are `5100,900220,900435,900438`.
- Verification: assigned count is 30; invalid selected users 0; duplicate bindings 0; selected users whose effective role count after binding is not 2 equals 0.

## Blockers

- None for updated scope. Closeout/commit remains separate because shared workspace has concurrent unrelated changes.
