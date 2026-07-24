# Task: MES 生产工单编码改用 ERP 单据编号

## Goal

Make MES production work orders synced from ERP use the ERP production order bill number as the work-order code, and hide the `来源单据编号` column on the production work-order list page.

## Scope

- Check the latest frontend task status before starting this work.
- Create the task document and execution log before editing production code.
- Add a real-page Playwright verification script for `http://localhost:8081/mes/pro/workorder`.
- Hide only the production work-order list column `来源单据编号`; do not redesign the page or change unrelated filters/forms unless required by the requested behavior.
- Keep existing API contracts and routing unchanged.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-dcc-approval-route-add-button-disabled/task.md`
- Status before this task: completed.
- Impact: no blocker to this MES work-order display change.

## Milestones

- [x] M1: Confirm the previous frontend task is completed and create this task document.
- [x] M2: Record BDD and RED evidence for the real-page column-visibility check.
- [x] M3: Implement the minimal frontend change to hide the `来源单据编号` list column.
- [x] M4: Complete GREEN verification with Playwright and frontend type checks.
- [x] M5: Update evidence and create a scoped frontend commit.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-erp-billno run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-workorder-erp-billno-code\scripts\verify-workorder-list-columns.mjs`
- `pnpm ts:check`

## Current Status

Completed. The production work-order list now shows ERP bill numbers as the visible work-order code when available and no longer renders the `来源单据编号` table column.

## Blocker And Impact

- Blocker: repo-wide `pnpm ts:check` still reports unrelated pre-existing type errors outside this feature scope.
- Impact: this task's changed page passed real-page verification and targeted ESLint validation, but the shared repository still has broader type-health work outstanding.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-erp-billno run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-workorder-erp-billno-code\scripts\verify-workorder-list-columns.mjs` -> PASS
- `pnpm exec eslint src/views/mes/pro/workorder/index.vue` -> PASS
- `pnpm ts:check` -> FAIL, due pre-existing unrelated repository type errors in files such as `src/components/bpmnProcessDesigner/package/designer/ProcessViewer.vue`, `src/config/axios/service.ts`, `src/layout/components/TagsView/src/TagsView.vue`, and other non-MES pages.
- Verified behavior:
  - real page `http://127.0.0.1:8081/mes/pro/work-order` returned `hasCodeHeader=true`
  - `来源单据编号` column is no longer visible
  - visible legacy `KDMO-...` codes are no longer present on the list page
