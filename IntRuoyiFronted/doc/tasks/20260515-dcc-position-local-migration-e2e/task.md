# Task: DCC 岗位本地化迁移真实 E2E 验证

## Goal

使用真实前端入口 `http://localhost:8081` 和真实后端 `http://127.0.0.1:48081`，验证 `DCC岗位分配` 页面在本地 MySQL 岗位迁移完成后可以显示 31 条本地岗位，并可见代表岗位名称。

## Scope

- 在前端仓库创建本任务文档、执行日志和验证报告。
- 复用现有真实登录路径与 Playwright CLI 脚本风格。
- 不新增页面功能；只做真实 E2E 验证与证据沉淀。
- 页面通过标准固定为：表格非空、可见 31 条岗位、可见代表岗位名称、无 `approval-positions` 运行时错误。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-route-status-toggle-runtime-error/task.md`
- Status before this task: completed.
- Impact: no unfinished frontend task blocks this DCC position E2E verification.

## Milestones

- [x] M1: Confirm previous frontend task status and create this task directory before verification.
- [x] M2: Record BDD scenarios and RED evidence for the current empty local-position runtime state.
- [x] M3: Run real Playwright login and verify the migrated local position list.
- [x] M4: Save screenshot, verification report, and final GREEN evidence.

## Expected Verification

- Real login through `/login?redirect=/index`.
- Navigate to `/dcc/controlled-file/positions`.
- Position table shows `31` visible rows after migration.
- Representative positions `编制人直接主管`、`QA`、`QC`、`文控`、`车间主任`、`排产员` are visible.

## Current Status

Completed. After the live local-position migration, the real DCC 岗位分配 page rendered 31 local rows successfully and the required representative岗位名称 were visible.

## Blocker And Impact

- Blocker: none.
- Impact: the live local-position migration now has real frontend proof on the DCC page.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli -s=dcc-position-local-migration-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\verify-dcc-position-list-e2e.mjs` -> PASS, `visibleRowCount=31` and the screenshot was captured.
