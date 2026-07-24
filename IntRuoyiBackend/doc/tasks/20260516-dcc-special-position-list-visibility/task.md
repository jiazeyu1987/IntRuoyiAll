# Task: DCC 岗位分配显示固定本地岗位

## Goal

让 `DCC岗位分配` 页面可见本地固定岗位 `900333 / 900334`，并确保它们分别显示为 `部门负责人`、`部门授权代表`，而不是只剩 `编制部门负责人或授权代表` 一条 IntAuth 岗位。

## Scope

- 后端仓库内完成岗位列表口径修复。
- 仅调整 `GET /dcc/approval-positions` 的显示口径，不新增接口。
- 保持现有 IntAuth 导入岗位逻辑不变。
- 允许同步更新 live 本地 MySQL 里这两条固定岗位记录名称，使前后数据口径一致。
- 记录 BDD / RED / GREEN 证据并完成定向验证。

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-workorder-erp-bom-sync/task.md`
- Status before this task: completed.
- Impact: no unfinished latest backend task blocks this DCC list-visibility fix.

## Milestones

- [x] M1: Create task directory and task document before code changes.
- [x] M2: Record BDD scenarios and RED evidence for the missing fixed岗位.
- [x] M3: Update the backend list behavior to include the fixed local岗位 and add targeted regression coverage.
- [x] M4: Align the live local MySQL names for `900333 / 900334`.
- [x] M5: Run targeted verification, update evidence, and prepare a scoped backend commit.

## Expected Verification

- `GET /dcc/approval-positions` returns the two fixed local岗位 in addition to imported IntAuth岗位.
- The returned names for these rows are `部门负责人` and `部门授权代表`.
- Other local seed rows such as `E2E` remain excluded.
- Backend evidence validation passes.

## Current Status

Completed. `GET /dcc/approval-positions` now includes the two fixed local岗位 in addition to imported IntAuth岗位, and the live local names for `900333 / 900334` are aligned to `部门负责人 / 部门授权代表`.

## Blocker And Impact

- Blocker: none.
- Impact: the岗位分配 page now displays the two fixed approval roles the user expected to see.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- Live API verification -> PASS, `GET /dcc/approval-positions` returned `totalCount=33` and included:
  - `900333 -> 部门负责人`
  - `900334 -> 部门授权代表`
