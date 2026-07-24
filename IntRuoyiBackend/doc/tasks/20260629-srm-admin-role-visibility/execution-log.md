BDD: only users with SRM admin role can see SRM menus -> Given an authenticated backend user without `srm_admin` role / When permission info is built for login / Then SRM menus and `srm:*` permissions must be excluded even if the user has `super_admin`
BDD: approval center must hide SRM provider from non SRM admins -> Given a backend user can query approval center / When the module descriptor list is built / Then SRM provider descriptor must be omitted unless the user has `srm_admin`
BDD: admin keeps SRM access through explicit SRM admin role -> Given admin is assigned the new `srm_admin` role / When login permission info and approval center modules are queried / Then SRM menus, `srm:*` permissions, and SRM approval module remain visible

RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl "yudao-module-system,yudao-module-bpm,yudao-module-srm" "-Dtest=PermissionServiceTest,ApprovalCenterServiceImplTest,ApprovalCenterControllerTest,ApprovalCenterTimelineContractTest,SrmSupplierPortalApprovalTaskAdapterTest" -DfailIfNoTests=false test` -> FAIL，BPM 测试仍使用旧的 `ApprovalCenterServiceImpl` 构造签名与旧的 `listProviders()` 调用。

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system,yudao-module-bpm -Dtest=PermissionServiceTest,AuthControllerTest,ApprovalCenterServiceImplTest,ApprovalCenterControllerTest,ApprovalCenterTimelineContractTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，系统/BPM 侧 36 个目标测试全部通过。

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am -Dtest=SrmSupplierPortalApprovalTaskAdapterTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，SRM 适配器可见性与审批行为测试 6 项全部通过。

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_visibility_sql.py` -> PASS，migration 契约已覆盖 `srm_admin` 角色、SRM 菜单树范围、admin 绑定和 fail-fast 保护。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-srm-admin-role-visibility\backend-api-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-srm-admin-role-visibility\database-schema-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260629-srm-admin-role-visibility --mode preview` -> PASS，默认保留 `task.md` 与 `execution-log.md`，临时 evidence 文件可清理，未执行 apply。
