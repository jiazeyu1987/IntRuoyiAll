# Task: 工单详情 ERP 同步 BOM 前端

## Goal

在工单详情弹窗底部新增 `ERP同步BOM` 按钮，调用工单级同步接口，并在成功后刷新 `工单BOM` 与 `物料需求` 两个页签数据。

## Scope

- 新增前端 API `syncErpBom(workOrderId)`.
- 在 `WorkOrderForm.vue` 页脚将 `ERP同步BOM` 放在 `查看条码` 左侧。
- 为 `WorkOrderBomList` 与 `WorkOrderItemList` 暴露 `reload()`，同步成功后不关闭弹窗，直接刷新两个页签。
- 保持现有工单详情页布局，不做无关重构。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-pro-workorder-product-number-column/task.md`
- Status before this task: completed.
- Impact: no unfinished latest frontend task blocked this delivery.

## Milestones

- [x] M1: Create frontend task directory, task doc, execution log, and evidence file.
- [x] M2: Record BDD scenarios and RED evidence for the missing detail sync entry.
- [x] M3: Implement API wrapper, sync button, loading state, and child component reload hooks.
- [x] M4: Run targeted verification and update evidence.
- [ ] M5: Commit only frontend files produced by this task.

## Expected Verification

- `cmd /c "set NODE_OPTIONS=--max-old-space-size=8192&& pnpm.cmd ts:check"`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-erp-bom-sync run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-workorder-erp-bom-sync\scripts\verify-workorder-erp-bom-sync.mjs`

## Current Status

Completed for code delivery. The detail dialog now exposes `ERP同步BOM`, keeps `查看条码` to its right, calls the backend sync endpoint, and refreshes both BOM-related child lists after success. Real-page verification now covers both fail-fast error surfacing and a true success response for the originally blocked sampled work order.

## Blocker And Impact

- Blocker: repository-wide `ts:check` still reports unrelated baseline TypeScript errors outside the work-order files.
- Impact: the feature code and live UI flow are verified, but project-level TS cleanup remains outside this task’s scope.

## Final Verification Result

- RED: before implementation, `WorkOrderForm.vue` footer exposed `查看条码` only, and `ProWorkOrderApi` did not expose `syncErpBom(workOrderId)`.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-erp-bom-sync run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-workorder-erp-bom-sync\scripts\verify-workorder-erp-bom-sync.mjs` -> PASS for UI-path verification, returning `hasSyncText=true`, `hasBarcodeText=true`, `syncResponseStatus=200`, and `syncResponseSnippet={"code":0,...,"workOrderId":903245,"erpBomVersion":"YXN.037.011.1002_V1.1","syncedBomCount":27}`.
- NOTE: `cmd /c "set NODE_OPTIONS=--max-old-space-size=8192&& pnpm.cmd ts:check"` still fails on unrelated baseline files outside this task’s write scope; no new errors were reported for `WorkOrderForm.vue`, `WorkOrderBomList.vue`, `WorkOrderItemList.vue`, or `src/api/mes/pro/workorder/index.ts`.
