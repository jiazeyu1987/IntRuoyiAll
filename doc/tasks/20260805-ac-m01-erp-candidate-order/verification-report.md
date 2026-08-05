# AC-M01 Verification Report

## Scope

- Requirement: `AC-M01 | 确认生产订单` must exclude unconfirmed, missing-formal-ID, and non-selectable ERP orders from candidate admission.
- Implemented scope: backend admission diff and batch admission gate; frontend static contract for blocked reason visibility and selection guard; RRM real-flow script action evidence hook for AC-M01 before production leader active-order join.

## Matrix

- Confirmed ERP synced order with formal `sourceFid/sourceBillNo` -> backend remains selectable when route requirements pass.
- Confirmed local order without formal ERP sync identity -> backend admission diff returns blocked and batch admission throws fail-fast error.
- Non-confirmed local order -> batch admission throws fail-fast error before insert.
- Frontend blocked row -> cannot be selected because row guard requires backend `selectable=true` and `READY_TO_ADMIT`.
- RRM production leader real-flow action -> opens `/mes/pro/schedule-order`, clicks real `同步工单` tab, queries `admission-diff` by `RRM_PRODUCTION_ORDER_CODE`, and requires `BLOCKED_ERP_SYNC_RECORD_MISSING` rows to remain `selectable=false`.

## Test

- Backend JUnit: `MesProScheduleOrderAdmissionDiffServiceTest`, `MesProScheduleOrderServiceImplTest`.
- Frontend static: `mes-pro-schedule-order-erp-sync-admission-static.spec.js`.
- RRM static: `mes-pro-schedule-order-erp-sync-rrm-action-static.spec.js`, plus adjacent `role-requirement-matrix-preflight-static.spec.cjs` contract update for production leader action order.
- TypeScript: `pnpm ts:check`.

## RED

- RED: backend target Maven -> FAIL with 3 expected behavior failures before implementation.
- RED: frontend static contract -> FAIL because the new ERP formal identity blocker had no local label.
- RED: RRM AC-M01 action static contract -> FAIL because `role-requirement-matrix-real-flow.e2e.js` did not include `verifyScheduleOrderErpCandidateAdmission` before `joinActiveOrder`.

## GREEN

- GREEN: backend target Maven -> PASS, 70 tests, 0 failures/errors/skips.
- GREEN: frontend static contract and `node --check` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: RRM AC-M01 action static contract -> PASS.
- GREEN: `node --check tests\e2e\role-requirement-matrix-real-flow.e2e.js`、`node --check tests\e2e\role-requirement-matrix-preflight-static.spec.cjs`、`node --check tests\e2e\mes-pro-schedule-order-erp-sync-rrm-action-static.spec.js` -> PASS.

## Verification

- Backend now requires `MesProWorkOrderStatusEnum.CONFIRMED` for schedulable work orders.
- Backend now requires matching Kingdee sync record with nonblank `sourceFid` and `sourceBillNo`.
- Admission diff now returns `BLOCKED_ERP_SYNC_RECORD_MISSING` with `selectable=false` for missing formal ERP identity.
- Frontend now labels the new blocker as `缺 ERP 正式订单` and already blocks selection through existing row guard.
- RRM real-flow script now records AC-M01 as `scheduleOrderErpCandidateAdmission` action evidence before AC-M04 `joinActiveOrder`; if the formal candidate or missing-ERP blocker sample is absent, the action returns structured `BLOCKED` instead of mock PASS.

## Blockers

- Real E2E: not completed in this pass. `pnpm e2e:role-requirement-matrix:real:check` is BLOCKED by missing RRM runtime/env prerequisites: frontend/backend URL, tenant, six role credentials, signature JSON, production order ID/code, route/version/process IDs, transfer IDs, batch-record report ID, and QA regulation version ID.
- RRM adjacent preflight: `pnpm e2e:role-requirement-matrix:preflight:static` now passes the AC-M01 action-order assertion but still fails on unrelated AC-M19 batch-record backfill idempotency-key assertion.
- Adjacent static suite: `smart-scheduling-smoke-real-flow-static.spec.js` fails on unrelated marker `autoSchedulePublishResult`.
- Git closeout: repository is already `int_main...origin/int_main [ahead 13]` with many unrelated dirty files, so task-owned commit/push cannot be safely completed without user authorization for baseline handling.
