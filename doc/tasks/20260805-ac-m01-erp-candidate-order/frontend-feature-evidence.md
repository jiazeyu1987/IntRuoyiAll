# Frontend Feature Evidence

## Feature

- Feature goal: ensure the schedule-order admission UI exposes backend blocked reasons for AC-M01 and prevents blocked production orders from being selected for batch admission.
- Non-goal: no redesign and no new frontend data source; the backend remains authoritative for candidate eligibility.

## Acceptance

- AC-M01: only ERP-confirmed production orders with formal ERP identity may be selectable.
- UI entry point: `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`, tab `待同步生产工单` / admission diff table.
- API wrapper: `IntRuoyiFronted/src/api/mes/pro/scheduleorder/index.ts`, admission diff row exposes `reasonCode`, `message`, and `selectable`.

## BDD

- BDD: Missing formal ERP identity excluded -> Given backend returns `BLOCKED_ERP_SYNC_RECORD_MISSING`, `selectable=false`, and a message; When the user views the admission diff table; Then the row shows a clear ERP formal-order reason and cannot be selected for batch admission.

## RED

- RED: `node tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> FAIL, missing `BLOCKED_ERP_SYNC_RECORD_MISSING: '缺 ERP 正式订单'` local reason label.

## GREEN

- GREEN: `node tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> PASS.
- GREEN: `node --check tests\e2e\mes-pro-schedule-order-erp-sync-admission-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Verified admission table selection delegates to `isAdmissionRowSelectable`.
- Verified `isAdmissionRowSelectable` allows only rows with `row.selectable && row.admissionStatus === 'READY_TO_ADMIT'`.
- Verified batch submit re-filters selected rows with the same guard before calling `createFromWorkOrders`.
- Verified reason cell displays backend `message` first and has a local `BLOCKED_ERP_SYNC_RECORD_MISSING` label for user-visible clarity.

## Blockers

- Adjacent static contract `node tests\e2e\smart-scheduling-smoke-real-flow-static.spec.js` still fails on unrelated historical marker `autoSchedulePublishResult`; this failure is outside the AC-M01 ERP admission change and was not modified.
- Real browser E2E remains pending or blocked until a task-owned ERP synced sample order and confirmed login/runtime path are available.
