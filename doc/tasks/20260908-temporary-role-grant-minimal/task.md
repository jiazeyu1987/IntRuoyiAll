# 通用临时角色授权最小闭环

## Task Goal

实现检查清单 1.10“临时权限管理失控”的最小系统闭环：临时角色授权必须有申请记录、有效期、审批/启用、撤销/到期回收和使用记录；不得复用长期 `system_user_role` 直接绕过有效期。

## Milestones

1. 数据模型：新增临时角色授权表和审计事件表，覆盖有效期、状态、审批、撤销、过期和使用事件。
2. 后端闭环：提供申请、审批、撤销、过期扫描、列表接口；权限判断能识别有效临时角色并记录使用。
3. 前端闭环：新增系统管理页面，支持申请、审批、撤销、查看状态/有效期/审计。
4. 验证：BDD/TDD RED/GREEN，后端定向测试、SQL 静态测试、前端静态/类型检查通过。

## Expected Verification

- Maven：`mvn.cmd -pl yudao-module-system '-Dtest=TemporaryRoleGrantServiceImplTest,PermissionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- Python SQL 静态测试：新增临时角色授权 SQL 合同测试并通过。
- Frontend：新增静态合同测试、`pnpm ts:check`。
- 技能证据：database/backend/frontend evidence validator PASS。

## Current Status

ready_for_closeout：通用临时角色授权最小闭环已完成实现与定向验证，进入收尾清理预览。

## 设计约束检查

- 不修改或禁用 admin，admin 仍用于测试。
- 临时角色授权不得写成永久 `system_user_role` 分配。
- 没有有效期不得申请；有效期过期不得放行。
- 审批人、撤销人、过期扫描和使用记录必须可追溯。
- 不引入 fallback、默认成功或 mock 授权。

## Cleanup Keep

- doc/tasks/20260908-temporary-role-grant-minimal/database-schema-evidence.md
- doc/tasks/20260908-temporary-role-grant-minimal/backend-api-evidence.md
- doc/tasks/20260908-temporary-role-grant-minimal/frontend-feature-evidence.md
