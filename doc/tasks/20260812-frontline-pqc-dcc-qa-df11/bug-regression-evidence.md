# DF11 Round-3 Bug Regression Evidence

## Bug Summary And Expected Behavior

The frontline picker must use `activeOrderId` as its only row identity, process responses must expose task identity only through `pqcTaskOptions`, and out-of-order requests in the real `selectFrontlinePqcActiveOrder` consumer must never overwrite the latest selection.

## Reproduction

RED: Round-3 independent source review finds the picker key/equality still use `workOrderId + routeId`, the process DTO/page retain flattened task fields, and the executable stale-response scenario calls an unused loader instead of the real consumer.

## Root Cause

The strict API projection was added without fully removing page-local legacy identity and flattened task state. Stale-response testing was attached to a parallel helper rather than the production consumer.

## Regression Tests

- Two rows sharing workOrderId/routeId but having different activeOrderId must have distinct picker keys and active state.
- The process DTO/page must not declare, write or read flattened task identity fields.
- Reversed promises through the real consumer must leave selected order, process options, loading state and error state owned by the latest activeOrderId.
- Unused projection/order helpers must be absent.

## GREEN

GREEN: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS, output: "PASS: frontline PQC process contract preserves full DTOs, formal identity, stable AM/PM order, and stale isolation".

GREEN: pnpm ts:check -> PASS, exit 0.

GREEN: production source old-helper scan -> PASS, no matches in IntRuoyiFronted/src for getFrontlinePqcActiveOrderProcesses, old /pqc/processes, getProcessPqcTaskSnapshot, createFrontlinePqcProjectionLoader, or FRONTLINE_PQC_RULE_KEY_ORDER.

GREEN: production flattened-process read scan -> PASS, no process.pqcTaskId / process.inspectionType / process.businessDate / process.shiftCode / process.roundNo / process.plannedInspectionQuantity reads and no withPqcTaskOption helper.

GREEN: active-order legacy identity scan -> PASS, no workOrderId+routeId picker key/equality pattern remains.

## Verification

- The picker row key and active-state equality are backed by activeOrderId through buildFrontlineActiveOrderPickerKey and isSameFrontlineActiveOrder.
- The process DTO exposes task identity only through pqcTaskOptions; task option selection stays page-local through activePqcTaskOptionId and the selected option object.
- The real selectFrontlinePqcActiveOrder consumer uses pqcActiveOrderSelectionRequestToken and the regression contract resolves two activeOrderId requests out of order to prove the stale response does not mutate selectedActiveOrder, processOptions, loading state, or error state.
- The unused createFrontlinePqcProjectionLoader and FRONTLINE_PQC_RULE_KEY_ORDER production abstractions are absent from production source.

## Risk And Scope

Scope is limited to formal active-order/task identity, real-consumer stale isolation, focused tests and evidence. Personnel, equipment, submission, backend and route management are unchanged.

## Blockers And Follow-up

No external blocker. Round-3 independent re-verification findings have been addressed and validated by focused static contract, typecheck, source scans, diff check, and evidence validators.
