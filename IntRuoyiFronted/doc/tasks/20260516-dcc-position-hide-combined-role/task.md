# Task: DCC 岗位分配隐藏合并岗位

## Goal

在 `DCC岗位分配` 主列表中隐藏 `编制部门负责人或授权代表` 这条合并岗位，只保留 `部门负责人` 与 `部门授权代表` 两条拆分后的固定岗位。

## Scope

- 前端仓库内完成本次列表过滤。
- 仅影响 `DCC岗位分配` 主列表，不改其他 DCC 预览页。
- 不改后端接口契约，不删除数据库记录。
- 使用真实页面路径验证隐藏结果。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-dcc-special-position-list-visibility/task.md`
- Status before this task: completed.
- Impact: the fixed local positions are already visible, so this task only removes the obsolete combined row from the main list.

## Milestones

- [x] M1: Create task directory and task doc before code changes.
- [x] M2: Record BDD scenarios and RED evidence for the obsolete combined row still being visible.
- [x] M3: Filter the combined row from the positions page.
- [x] M4: Run real-page verification and update evidence.

## Expected Verification

- `编制部门负责人或授权代表` no longer appears in `DCC岗位分配` 主列表。
- `部门负责人` and `部门授权代表` remain visible.
- Real-page verification passes.

## Current Status

Completed. The live `DCC岗位分配` page now hides `编制部门负责人或授权代表` and keeps `部门负责人 / 部门授权代表` visible.

## Blocker And Impact

- Blocker: none.
- Impact: the main list now exposes only the two split roles and no longer shows the obsolete combined role.

## Final Verification Result

- real positions-page verification -> PASS, `rowCount=32`, `hasDeptOwner=true`, `hasAuthRep=true`, `hasCombined=false`.
