# Task: DCC 文件类别自动派生四层审批矩阵

## Goal

把当前 DCC “按类别手工维护 4 个审批节点”的模式改成“文件类别维护审批矩阵，审批路线按类别自动派生固定四层”，并让受控文件提交、路线预览、快照冻结和审批推进统一按该矩阵执行。

## Scope

- 检查并显式阻塞上一条未闭环后端任务后再开始当前任务。
- 先创建当前后端任务文档、执行日志、数据库/后端证据文件，再开始生产代码变更。
- 严格按 BDD + TDD 先补失败测试，再做最小实现。
- 改造 DCC 后端数据模型、接口、派生逻辑、快照逻辑、流程审批人装配和种子/迁移脚本。
- 不在运行时直接 OCR/解析 `审核会签.pdf`；仓库内只保留受控种子数据。
- 如果固定角色 `文控`、第二层角色集合、第三层两批准角色、角色到用户解析或 BPM 装配任一前置缺失，必须失败并暴露精确 blocker。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-task-closeout-cleanup-skill/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused cleanup-skill work does not block this DCC backend delivery.

## Milestones

- [x] M1: Block the previous unfinished backend task and create this task directory.
- [x] M2: Record BDD scenarios plus RED tests for schema, route derivation, snapshot freezing, and workflow semantics.
- [x] M3: Implement schema and persistence changes for category matrix and derived route storage.
- [x] M4: Implement backend APIs and route derivation logic for fixed four stages.
- [x] M5: Implement controlled-file submission, snapshot, and BPM assignee wiring changes.
- [x] M6: Add migration/seed support for imported file categories and validate fail-fast prerequisites.
- [x] M7: Run GREEN backend verification and update evidence.

## Expected Verification

- Backend tests prove:
  - stage 2 requires one or more signoff positions and resolves to `ALL_REQUIRED`
  - stage 3 requires exactly two approval positions and resolves to `ANY_ONE`
  - fixed `文控` position is used for stages 1 and 4
  - submit freezes stage position collections and resolved users
  - BPM stage transitions keep stage 2 unanimous and stage 3 any-one
- Migration/seed validation fails fast if `文控` is missing or any category cannot derive a valid four-stage route.

## Current Status

Completed. The backend matrix save/read contract, fixed-four-stage derivation, workflow snapshot persistence, BPM assignee-variable wiring, and bundled IntAuth matrix seed import are implemented and verified.

## Blocker And Impact

- Blocker: none remaining for the backend implementation itself.
- Impact: the frontend and runtime can now consume a category-driven fixed-four-stage approval matrix instead of the old single-source route model.

## Final Verification Result

- `mvn --% -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -pl yudao-server -am -DskipTests package` -> PASS
- `POST /admin-api/dcc/file-categories/import-intauth-matrix` -> PASS, `totalCount=48`, `seededCount=48`, `skippedCount=0`
