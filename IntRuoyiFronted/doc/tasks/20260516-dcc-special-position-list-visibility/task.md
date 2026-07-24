# Task: DCC 岗位分配显示固定本地岗位前端验证

## Goal

验证 `DCC岗位分配` 页面在真实运行环境中可见 `部门负责人` 与 `部门授权代表` 两条固定本地岗位。

## Scope

- 前端仓库内创建任务文档、执行日志、验证报告与验证脚本。
- 复用真实登录与真实页面路径，不新增页面功能。
- 验证重点只放在 `DCC岗位分配` 主列表。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-workorder-erp-bom-sync/task.md`
- Status before this task: completed.
- Impact: no unfinished frontend task blocks this DCC page verification.

## Milestones

- [x] M1: Create frontend task directory before verification.
- [x] M2: Record BDD scenarios and RED evidence for the missing fixed岗位.
- [x] M3: Run real page verification for `DCC岗位分配`.
- [x] M4: Update evidence and verification report.

## Expected Verification

- Real login reaches `http://127.0.0.1:8081/dcc/controlled-file/positions`.
- `部门负责人` and `部门授权代表` are visible in the live岗位列表.
- The page no longer relies on only the single `编制部门负责人或授权代表` row to represent these two roles.

## Current Status

Completed. The real `DCC岗位分配` page now exposes both fixed local岗位 `部门负责人 / 部门授权代表`.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-special-position-list-visibility\scripts\verify-dcc-position-list-special-roles.mjs` -> PASS, `rowCount=33`, and both fixed local role names were visible.
