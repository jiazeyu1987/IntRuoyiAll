# Backend API Evidence

## Scope

- Controller：`/system/temporary-role-grant`
  - `POST /create`
  - `PUT /approve`
  - `PUT /revoke`
  - `GET /page`
  - `GET /audit-list`
- Service：`TemporaryRoleGrantServiceImpl`
- Job：`temporaryRoleGrantExpireJob`
- Permission hook：`PermissionServiceImpl#hasAnyPermissions`

## API contract and data contract

- 创建请求：`userId`、`roleId`、`expireTime`、`reason`。
- 撤销请求：`id`、`reason`。
- 列表响应：授权 ID、用户、角色、状态、原因、申请/审批/生效/到期/撤销信息。
- 审计响应：事件类型、权限编码、操作者、消息、创建时间。

## Contract

后端合同以 VO、Service 接口和 SQL 迁移共同约束：前端只调用临时授权接口，服务只写临时授权表与审计表，权限判断仅合并 `ACTIVE` 且未过期的临时角色。

## Validation

- 页面/查询：`system:temporary-role-grant:query`
- 创建：`system:temporary-role-grant:create`
- 审批：`system:temporary-role-grant:approve`
- 撤销：`system:temporary-role-grant:revoke`
- 验证：原因不能为空；到期时间必须晚于当前时间；角色必须存在；同一用户同一角色不能存在未关闭的临时授权；只有 `PENDING` 可审批，只有 `PENDING`/`ACTIVE` 可撤销。
- 缺失或非法状态直接抛业务错误，不做 fallback。

## Required config, services, fixtures, and migrations

- 依赖现有 `RoleService`、`MenuService`、`RoleMenuMapper`、`PermissionServiceImpl` 缓存。
- 新增 MySQL 迁移与 H2 测试 schema。
- 新增 `infra_job` 任务配置，调用 `temporaryRoleGrantExpireJob`。

## BDD scenarios

- BDD: 临时角色审批后才生效 -> Given PENDING 授权 When 审批通过 Then 状态转 ACTIVE 并纳入权限判断。
- BDD: 临时权限使用可追溯 -> Given 永久角色无法放行但临时角色可放行 When 权限检查命中临时角色 Then 写入 USE 审计。

## RED: command and expected failure

`mvn.cmd -pl yudao-module-system '-Dtest=TemporaryRoleGrantServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，缺少对应服务与权限集成。

## GREEN: command and passing result

`mvn.cmd -pl yudao-module-system '-Dtest=TemporaryRoleGrantServiceImplTest,PermissionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，35 tests。

## Verification

- `TemporaryRoleGrantServiceImplTest` 覆盖创建校验、审批生效、撤销/过期失效、使用审计。
- `PermissionServiceTest` 覆盖永久角色放行不记录临时使用、临时角色放行才记录 USE。
- `mvn.cmd -pl yudao-module-system -DskipTests compile` -> PASS。

## Observability touchpoints

- `system_temporary_role_grant_audit` 记录 APPLY、APPROVE、REVOKE、EXPIRE、USE。
- 过期任务返回过期处理数量。

## Blockers and downstream skill needs

无。
