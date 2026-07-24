# 任务：SRM管理员角色与SRM页签可见性收口

## Goal

在后端权限与数据库菜单层正式实现：

- 新增 `SRM管理员` 角色；
- 只有 `SRM管理员` 可见后台 `SRM` 页签；
- 为 `admin` 账号补充 `SRM管理员` 角色；
- 审批中心 `SRM` 模块也同步按该角色过滤。

## 经验门禁

- `docs/experience-index.md`：本任务属于权限/菜单/角色变更，需先建任务记录再进入实现。
- `docs/powershell-memory.md`：涉及 PowerShell、中文和 SQL 文件读写，必须使用显式 UTF-8 路径。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；同时修正超管默认放开 SRM 菜单与 SRM provider 描述的问题。
- 是否存在临时补丁或绕过：否。

## Milestones

1. 建立任务文档并确认冲突链路。状态：completed。
2. 先写 RED 测试覆盖菜单与审批中心 SRM 模块过滤。状态：completed。
3. 实现后端权限过滤与审批中心 provider 过滤。状态：completed。
4. 新增 SQL migration 创建 `SRM管理员` 并给 `admin` 绑定。状态：completed。
5. 跑 GREEN 验证并更新证据。状态：completed。

## Expected Verification

- `AuthControllerTest`、`PermissionServiceTest`、`ApprovalCenterServiceImplTest`、`ApprovalCenterControllerTest`、`ApprovalCenterTimelineContractTest` 通过。
- `SrmSupplierPortalApprovalTaskAdapterTest` 通过。
- SQL 契约测试通过。
- 目标改动不影响非 SRM 权限链路。

## Current Status

COMPLETED

## Final Verification

- PASS: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-bpm -Dtest=PermissionServiceTest,AuthControllerTest,ApprovalCenterServiceImplTest,ApprovalCenterControllerTest,ApprovalCenterTimelineContractTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am -Dtest=SrmSupplierPortalApprovalTaskAdapterTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_visibility_sql.py`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-srm-admin-role-visibility\backend-api-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-srm-admin-role-visibility\database-schema-evidence.md`
