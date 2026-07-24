# Task: DCC 岗位分配隐藏来源与备注列

## Goal

在 `DCC岗位分配` 页面中移除 `来源` 和 `备注` 两列，仅保留用户关心的岗位主信息、分配信息与操作列。

## Scope

- 前端仓库内完成本次页面列裁剪。
- 不修改后端接口，不改变岗位数据来源，不调整其他页面。
- 复用真实 Playwright 登录路径验证列展示结果。
- 记录 BDD / RED / GREEN 证据，并补齐前端证据文件。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-dcc-position-local-migration-e2e/task.md`
- Status before this task: completed.
- Impact: the prior DCC position local-migration verification is complete and does not block this UI simplification.

## Milestones

- [x] M1: Confirm previous frontend task status and create this task directory before code changes.
- [x] M2: Record BDD scenarios and RED evidence for the current extra columns.
- [x] M3: Remove the `来源` and `备注` columns from the DCC 岗位分配 page.
- [x] M4: Run targeted verification and update evidence.

## Expected Verification

- `DCC岗位分配` 页面不再显示 `来源` 列标题。
- `DCC岗位分配` 页面不再显示 `备注` 列标题。
- 现有岗位列表仍正常显示，且代表岗位名称仍可见。
- 前端证据校验通过。

## Current Status

Completed. The DCC 岗位分配 page now hides `来源` and `备注`, while the real migrated local position list still renders correctly.

## Blocker And Impact

- Blocker: none.
- Impact: the page now exposes only the user-facing岗位信息 and no longer shows the internal source/remark metadata.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-position-hide-source-remark\scripts\verify-dcc-position-columns.mjs` -> PASS
- `pnpm exec eslint src/views/dcc/controlled-file/positions/index.vue` -> PASS
