# Task: Material edit dialog ERP BOM sync button

## Goal

Add a `从ERP同步` action to the MES material/product edit dialog so editing users can replace the current product `BOM组成` list with the latest approved ERP BOM for the current item.

## Scope

- Explicitly block the current same-repository frontend task `doc/tasks/20260518-workorder-row-freeze-toggle-action/task.md` before starting this work.
- Create this frontend task package before production code changes.
- Record BDD scenarios and strict TDD evidence for the product edit dialog button, confirmation flow, API call, and BOM table refresh.
- Add a frontend API method for the new product-level ERP BOM sync endpoint.
- Show the new button only in edit mode with an existing item ID, positioned to the left of the dialog `确定` button.
- Refresh only the `BOM组成` child table after a successful sync; do not redesign the dialog or change other tabs.
- Use the real frontend path `http://localhost:8081` for E2E verification and do not introduce mock data or fallback behavior.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-workorder-row-freeze-toggle-action/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused work-order row-freeze task remains isolated and does not block this material ERP BOM sync frontend slice.

## Milestones

- [x] M1: Block the previous same-repository frontend task and create this task package first.
- [x] M2: Record BDD scenarios and add RED verification for the edit-dialog ERP sync button.
- [x] M3: Implement the minimal frontend button, API integration, confirmation flow, and BOM refresh.
- [x] M4: Run targeted frontend verification and update evidence.
- [x] M5: Preview closeout artifacts and prepare a task-scoped frontend commit.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-md-item-erp-bom-sync-button\scripts\verify-md-item-erp-bom-sync-source.mjs`
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session md-item-erp-bom-sync-button run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-md-item-erp-bom-sync-button\scripts\verify-md-item-erp-bom-sync.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260518-md-item-erp-bom-sync-button/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-md-item-erp-bom-sync-button --mode preview`

## Current Status

Completed. Frontend implementation, source verification, real browser success-path verification, and cleanup preview are complete.

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-md-item-erp-bom-sync-button\scripts\verify-md-item-erp-bom-sync-source.mjs` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session md-item-erp-bom-sync-button run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-md-item-erp-bom-sync-button\scripts\verify-md-item-erp-bom-sync.mjs` -> PASS, using real item code `YXN.037.011.1002`, the frontend reached the backend endpoint and returned `itemId=902262`, `erpBomVersion=YXN.037.011.1002_V1.1`, `syncedBomCount=27`, then reloaded the BOM child table successfully
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-md-item-erp-bom-sync-button --mode preview` -> PASS

## Blocker And Impact

- None currently.
