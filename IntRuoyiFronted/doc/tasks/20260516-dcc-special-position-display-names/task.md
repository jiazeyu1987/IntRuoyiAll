# Task: DCC 固定岗位显示名修复

## Goal

让 DCC 固定本地岗位 `900333` 与 `900334` 在审批路线、审批矩阵预览和上传路线预览中显示明确岗位名，而不再回退成 `岗位#900333 / 岗位#900334`。两者显示名固定为 `部门负责人`、`部门授权代表`。

## Scope

- 前端仓库内完成本次显示名修复。
- 统一修复 DCC 路线页、审批矩阵预览、上传路线预览中的岗位名解析。
- 不改动通用 IntAuth 岗位迁移逻辑，不扩展为成员分配修复。
- 允许同步修正 live 本地库这两条岗位记录名称，但不扩展为新的后端接口改造。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-workorder-erp-bom-sync/task.md`
- Status before this task: completed.
- Impact: no unfinished frontend task blocks this DCC display-name fix.

## Milestones

- [x] M1: Confirm previous frontend task status and create this task directory before code changes.
- [x] M2: Record BDD scenarios and RED evidence for the current fallback display.
- [x] M3: Implement shared fixed-position name resolution and update affected DCC views/helpers.
- [x] M4: Run targeted verification, record GREEN evidence, and update task status.

## Expected Verification

- DCC routes preview no longer shows `岗位#900333 / 岗位#900334`.
- The display names become `部门负责人 / 部门授权代表`.
- Representative DCC matrix-derived preview verification remains green with the new expected names.
- Frontend evidence validation passes.

## Current Status

Completed. DCC route-related previews now render `部门负责人 / 部门授权代表` for fixed local positions `900333 / 900334`, and the fallback numeric position labels are gone.

## Blocker And Impact

- Blocker: none.
- Impact: the DCC preview UI now shows clear岗位名称 instead of leaking internal numeric ids.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-special-position-display-names\scripts\verify-dcc-special-position-display-names.mjs` -> PASS
- `pnpm exec eslint src/views/dcc/controlled-file/shared/utils.ts src/views/dcc/controlled-file/routes/index.vue src/views/dcc/controlled-file/categories/components/CategoryMatrixDialog.vue src/views/dcc/controlled-file/upload/submitter.ts` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-category-matrix-derived-route\scripts\verify-dcc-category-matrix-derived-route.mjs` -> PASS after updating the historical script expectation to the new display names.
