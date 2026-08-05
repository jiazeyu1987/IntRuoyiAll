# AC-M01 Verification Report

## Scope

- Requirement: `AC-M01 | 确认生产订单` must exclude unconfirmed, missing-formal-ID, and non-selectable ERP orders from candidate admission.
- Implemented scope: backend admission diff and batch admission gate; frontend static contract for blocked reason visibility and selection guard.

## Matrix

- Confirmed ERP synced order with formal `sourceFid/sourceBillNo` -> backend remains selectable when route requirements pass.
- Confirmed local order without formal ERP sync identity -> backend admission diff returns blocked and batch admission throws fail-fast error.
- Non-confirmed local order -> batch admission throws fail-fast error before insert.
- Frontend blocked row -> cannot be selected because row guard requires backend `selectable=true` and `READY_TO_ADMIT`.

## Test

- Backend JUnit: `MesProScheduleOrderAdmissionDiffServiceTest`, `MesProScheduleOrderServiceImplTest`.
- Frontend static: `mes-pro-schedule-order-erp-sync-admission-static.spec.js`.
- TypeScript: `pnpm ts:check`.

## RED

- RED: backend target Maven -> FAIL with 3 expected behavior failures before implementation.
- RED: frontend static contract -> FAIL because the new ERP formal identity blocker had no local label.

## GREEN

- GREEN: backend target Maven -> PASS, 70 tests, 0 failures/errors/skips.
- GREEN: frontend static contract and `node --check` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- Backend now requires `MesProWorkOrderStatusEnum.CONFIRMED` for schedulable work orders.
- Backend now requires matching Kingdee sync record with nonblank `sourceFid` and `sourceBillNo`.
- Admission diff now returns `BLOCKED_ERP_SYNC_RECORD_MISSING` with `selectable=false` for missing formal ERP identity.
- Frontend now labels the new blocker as `缺 ERP 正式订单` and already blocks selection through existing row guard.

## Blockers

- Real E2E: not completed in this pass; needs confirmed local runtime/login and task-owned ERP-synced sample data through the real UI path.
- Adjacent static suite: `smart-scheduling-smoke-real-flow-static.spec.js` fails on unrelated marker `autoSchedulePublishResult`.
- Git closeout: repository is already `int_main...origin/int_main [ahead 1]` with many unrelated dirty files, so task-owned commit/push cannot be safely completed without user authorization for baseline handling.
