# Execution Log

- 2026-05-18: Created frontend task package `20260518-md-item-erp-bom-sync-button`.
- 2026-05-18: Blocked previous frontend task `20260518-workorder-row-freeze-toggle-action` due user priority switch.
- BDD: Show sync button only in edit mode -> Given the material dialog is opened in update mode with an existing item ID, When the footer renders, Then a `从ERP同步` button appears immediately to the left of `确定`.
- BDD: Hide sync button outside edit mode -> Given the material dialog is opened in create or detail mode, When the footer renders, Then no `从ERP同步` button is shown.
- BDD: Confirm before overwriting BOM rows -> Given the user clicks `从ERP同步`, When the action starts, Then the dialog asks for confirmation that current product `BOM组成` will be overwritten by ERP BOM.
- BDD: Call product BOM sync API and refresh BOM tab -> Given the user confirms the overwrite, When the sync request succeeds, Then the frontend calls the product BOM sync API, shows the ERP version and synced count, and refreshes only the `BOM组成` child table.
- BDD: Surface backend fail-fast errors -> Given the backend rejects the sync because ERP BOM is missing or child items are unmapped, When the request fails, Then the frontend surfaces the real error and does not hide or downgrade it.
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-md-item-erp-bom-sync-button\scripts\verify-md-item-erp-bom-sync-source.mjs` -> FAIL, `MdItemForm.vue` does not yet expose the `从ERP同步` footer button or API wiring.
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-md-item-erp-bom-sync-button\scripts\verify-md-item-erp-bom-sync-source.mjs` -> PASS, the edit dialog source now exposes the ERP sync button, API contract, confirmation copy, and BOM reload hook.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS, the frontend changes type-check successfully when the Node heap is increased to 8GB.
- GREEN: real browser page load -> PASS, `/mes/md/item` now renders the material list and no longer reproduces the earlier `TypeError: isFunction is not a function` route-loading blocker.
- GREEN: updated the Playwright verification script to target the visible `请输入物料编码` search input, wait for the async edit-mode footer button, and normalize the spaced `确 定` footer label.
- RED: candidate scan -> FAIL for the first five visible items, all of which failed fast with real backend messages about missing local MES child-item mappings.
- GREEN: switched the real E2E verification sample to `YXN.037.011.1002`, which is visible in the live item list and already has the necessary local MES child-item mappings.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session md-item-erp-bom-sync-button run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-md-item-erp-bom-sync-button\scripts\verify-md-item-erp-bom-sync.mjs` -> PASS, returned `itemId=902262`, `erpBomVersion=YXN.037.011.1002_V1.1`, `syncedBomCount=27`, and success toast `ERP BOM同步成功：版本 YXN.037.011.1002_V1.1，共 27 条`.
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-md-item-erp-bom-sync-button --mode preview` -> PASS, keep only `task.md` and `execution-log.md`; evidence and helper scripts remain cleanup candidates.
