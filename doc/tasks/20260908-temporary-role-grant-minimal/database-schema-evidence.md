# Database Schema Evidence

## Data change goal and affected entities

为“通用临时角色授权最小闭环”新增独立数据模型，避免把临时授权写入永久 `system_user_role`。受影响实体：

- `system_temporary_role_grant`：临时角色授权主记录，保存申请、审批、生效、失效、撤销状态。
- `system_temporary_role_grant_audit`：授权生命周期与使用审计事件。
- `infra_job`：注册过期扫描任务。
- `system_menu` / `system_role_menu`：注册临时角色授权管理页面与按钮权限。

## Database engine and migration tool

- 数据库：MySQL，测试侧补充 H2 schema。
- 迁移文件：`IntRuoyiBackend/sql/mysql/20260908_system_temporary_role_grant.sql`。
- 测试 schema：`IntRuoyiBackend/yudao-module-system/src/test/resources/sql/create_tables.sql`、`clean.sql`。

## Migration

迁移新增两张临时授权表、一个过期扫描任务和一组菜单/按钮权限；迁移元数据声明 allowedEnvironments、dependsOn、type 和 riskLevel。

## Schema, migration, fixture, index, constraint changes

- 主表包含 `user_id`、`role_id`、`status`、`reason`、`apply_time`、`effective_time`、`expire_time`、审批人与撤销人字段。
- 审计表包含 `grant_id`、`event_type`、`permission_code`、操作人与消息。
- 索引覆盖用户状态有效期查询、角色查询、授权审计查询。
- 迁移不写入 `system_user_role`，仅新增临时授权表与菜单/任务种子数据。

## Safety

- 临时授权与永久角色分离；撤销/过期只改临时授权状态，不破坏永久角色配置。
- `expire_time` 必填且由服务层验证必须晚于当前时间。
- 过期任务只处理 `ACTIVE` 且到期的临时授权。

## Rollback and recovery plan

若迁移需要回滚，先停用 `temporaryRoleGrantExpireJob`，删除新增菜单与角色菜单关系，再备份并删除两张临时授权表。由于未写入 `system_user_role`，不会污染永久授权。

## BDD scenarios

- BDD: 临时角色申请必须有有效期 -> Given 管理员申请临时角色 When 缺少有效期或有效期不晚于当前时间 Then 后端拒绝创建。
- BDD: 临时角色到期或撤销后失效 -> Given 存在 ACTIVE 授权 When 到期扫描或撤销 Then 状态变为 EXPIRED/REVOKED 并不再参与权限判断。

## RED: command and expected failure

`mvn.cmd -pl yudao-module-system '-Dtest=TemporaryRoleGrantServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，缺少临时角色授权服务、表结构与实现。

## GREEN: command and passing result

- `mvn.cmd -pl yudao-module-system '-Dtest=TemporaryRoleGrantServiceImplTest,PermissionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，35 tests。
- `python -X utf8 -m pytest script\tests\test_system_temporary_role_grant_sql.py -q` -> PASS，3 passed。

## Verification

静态 SQL 测试验证迁移元数据、必要表/索引/菜单/任务片段、H2 测试表同步，并确认未将临时授权写入 `system_user_role`。

## Blockers

无。
