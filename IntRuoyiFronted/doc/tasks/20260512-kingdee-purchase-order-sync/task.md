# Task: Kingdee purchase order synchronization frontend

## Goal

Add a purchase order list button that manually triggers Kingdee K3Cloud purchase order synchronization.

## Scope

- Add a frontend API wrapper for the backend sync endpoint.
- Add a button on the ERP purchase order list with permission control and loading state.
- Refresh the list after a successful sync and rely on the existing axios error handler to surface backend failures.
- Add task-local verification before production code changes because this frontend repository has no configured component test runner.

## Milestones

- [x] M1: Previous frontend task checked and completed.
- [x] M2: Frontend task documentation created in the frontend repository before production code changes.
- [x] M3: RED verification script written and observed failing before UI/API changes.
- [x] M4: API wrapper and purchase order list button implemented.
- [x] M5: Frontend task-local verification and focused lint pass.
- [x] M6: Evidence updated and frontend task marked completed.
- [x] M7: Frontend changes committed on `feature/kingdee-purchase-order-sync`.

## Expected Verification

- The API wrapper exposes `syncKingdeePurchaseOrders()` and posts to `/erp/purchase-order/sync-kingdee`.
- The purchase order page has a Kingdee sync button guarded by `erp:purchase-order:sync-kingdee`.
- The button sets a loading state, calls the API, shows a success summary, and refreshes the list.

## Current Status

Completed on `feature/kingdee-purchase-order-sync`. Task-local contract verification, focused ESLint, and evidence validation passed. Repository-wide `pnpm ts:check` is blocked by pre-existing unrelated TypeScript errors outside this task scope; see `frontend-feature-evidence.md`.
