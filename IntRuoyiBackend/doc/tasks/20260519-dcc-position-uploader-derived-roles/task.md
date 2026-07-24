# Task: DCC 上传人派生岗位运行时解析

## Goal

调整 DCC 审批岗位中的 `编制人直接主管`、`部门负责人`、`部门授权代表` 规则，使其不再依赖固定人物分配，而是在上传文件时根据上传人上下文动态解析实际审批人，并对缺少真实来源的情况失败提示。

## Non-Scope

- 不改普通固定岗位的解析方式。
- 不新增数据库表或兼容兜底分支。
- 不修改 DCC 审批矩阵的文件类别选择规则。

## Previous Task Check

- Previous same-repo task: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-notify-message-delivery\task.md`
- Status before this task: completed.
- Impact: DCC notify delivery write scope is independent from the position and workflow resolution changes needed here.

## Scope

- `yudao-module-dcc` approval-position save validation
- `yudao-module-dcc` upload route preview / route snapshot approver resolution
- Focused DCC position and workflow tests
- This task directory evidence

## Milestones

- [x] M1: Create the task package and inspect current special-position behavior.
- [x] M2: Add RED tests proving the current backend still allows fixed-user assignments or static resolution for uploader-derived positions.
- [x] M3: Implement minimal runtime resolution and save validation for uploader-derived positions.
- [x] M4: Run focused backend verification and evidence validation.
- [x] M5: Execute closeout preview and prepare a task-only commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccApprovalPositionAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-position-uploader-derived-roles\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-dcc-position-uploader-derived-roles --mode preview`

## Current Status

Completed. The DCC uploader-derived position rule now resolves `编制人直接主管` and `部门负责人` from the live IntAuth org source, rejects manual assignments for those roles, preserves route preview semantics when no submitter context is available, and fail-fast blocks `部门授权代表` because the current data model still lacks a confirmed runtime source.

## Write Boundary

- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/position/**`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/**`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/**`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/position/**`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/**`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/route/**`
- `doc/tasks/20260519-dcc-position-uploader-derived-roles/**`

## Risks And Constraints

- Runtime resolution must fail fast when the uploader context cannot resolve the required approver; no silent fallback to old fixed-user assignments.
- Current repo organization APIs only directly expose department leader data; any `部门授权代表` behavior must use a confirmed real source or stay blocked.
- Frontend restrictions must match backend validation exactly.

## Final Verification Result

- PASS: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccApprovalPositionRuntimeResolverTest,DccApprovalPositionAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-position-uploader-derived-roles\backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-dcc-position-uploader-derived-roles --mode preview`
