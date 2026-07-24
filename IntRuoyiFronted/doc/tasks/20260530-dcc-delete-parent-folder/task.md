# 任务：DCC 删除父文件夹前端

## Task Goal

在 DCC 目录管理页为每一行目录增加“删除父文件夹”危险操作。用户点击后必须在二次确认弹窗输入 `PROD` 才能提交，成功后刷新目录树并展示删除统计。

## Previous Task Check

- 上一个前端任务 `20260530-runtime-control-nas-assets` 未完成，已标记 `Blocked`，原因是用户要求立即执行本 DCC 删除任务，不能混入运行控制台未完成实现。
- 当前仓库存在与本任务无关的未提交改动；本任务不回退、不提交这些改动。

## BDD Scenarios

- BDD: 目录行展示删除父文件夹按钮 -> Given 用户有 DCC 目录管理权限 / When 打开目录管理页 / Then 每个目录行展示“删除父文件夹”危险操作。
- BDD: 输入 PROD 后才能删除 -> Given 用户点击删除父文件夹 / When 确认弹窗打开但输入不是 `PROD` / Then 确认按钮不可提交；When 输入 `PROD` 并确认 / Then 调用删除接口。
- BDD: 删除成功刷新目录树 -> Given 删除接口返回删除统计 / When 删除成功 / Then 页面提示成功并刷新目录树。

## Milestones

- [x] M1：创建任务文档并记录 BDD 场景。
- [x] M2：补充前端 RED 静态/契约测试。
- [x] M3：实现前端 API、弹窗与目录行按钮。
- [x] M4：运行前端 targeted 验证并记录 GREEN。
- [x] M5：补充前端证据、执行收尾预览并提交本任务前端改动。

## Expected Verification

- `pnpm exec node scripts/dcc-directory-parent-delete.test.mjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260530-dcc-delete-parent-folder/frontend-feature-evidence.md`

## Cleanup Keep

- `doc/tasks/20260530-dcc-delete-parent-folder/frontend-feature-evidence.md`

## Final Verification

- `pnpm exec node scripts/dcc-directory-parent-delete.test.mjs` -> PASS.
- `pnpm ts:check` -> PASS.
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260530-dcc-delete-parent-folder/frontend-feature-evidence.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260530-dcc-delete-parent-folder --mode preview` -> PASS, no deletion candidates.
- Playwright E2E with current backend/frontend on `http://127.0.0.1:8099` -> PASS for directory parent/child delete flow.

## Current Status

Completed. Full real-file upload/approval/delete E2E remains a recorded environment prerequisite gap because no task-scoped approved controlled-file fixture was available in the test tenant; no mock/API fixture was used.
