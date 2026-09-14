# DF11 Frontend Feature Evidence

## Feature Goal

Expose a typed frontend contract for the dedicated frontline PQC process response: activeOrderId-only request helper, full QA inspection item projection, resultType union, inspectionRuleKey union, task status/options, production submit candidates, and stable task ordering identity.

## Non-Goals

- No page feature changes; the only page edit removes the obsolete local task-synthesis fallback so formal pqcTaskOptions remain authoritative. INT12 owns all final page behavior.
- No backend, schema, route-DCC, QA assembly, or supervisor document changes.
- No fallback, mock data, compatibility shim, or default-success behavior.

## Requirements and Acceptance IDs

- DF11 dev-plan objective and TC-DF11-FRONTEND-PROJECTION.
- Acceptance IDs: AC-03, AC-04, AC-05, AC-06, AC-08, AC-11, AC-12, AC-13.

## UI Entry Points and Owned Files

- UI entry point: existing frontline PQC page remains unchanged for INT12.
- Owned files:
  - `IntRuoyiFronted/src/api/mes/pro/feedback/index.ts`
  - `IntRuoyiFronted/src/api/mes/qc/template/index.ts`
  - `IntRuoyiFronted/src/api/mes/pro/feedback/pqcProjection.ts`
  - `IntRuoyiFronted/src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts` (activeOrderId request identity only)
  - `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue` (remove process-field task synthesis only)
  - `IntRuoyiFronted/tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs`

## API Contracts and Data States

- `getPqcProcesses(activeOrderId)` sends only `activeOrderId`.
- `FrontlinePqcInspectionRuleKey` keeps `FIRST | PATROL_AM | PATROL_PM | FINAL`.
- `FrontlinePqcResultType` keeps `BOOLEAN | NUMERIC | TEXT`.
- `FrontlinePqcTaskStatus` exposes `PENDING | SUBMITTED | CONFIRMED | CANCELLED`; summary state separately includes `NOT_CREATED | MIXED`.
- Process/task/item/equipment/candidate structures retain full backend fields needed by INT12.
- Pure projection sorts by businessDate/ruleSort/roundNo/pqcTaskId and rejects stale responses by request serial.
- The active-order consumer owns a request token and rejects a response superseded by a newer activeOrderId selection before mutating processOptions.

## BDD Scenarios

- BDD: Active order request identity -> Given activeOrderId selection, When requesting PQC processes, Then frontend does not require workOrderId + routeId.
- BDD: Full item projection -> Given QA item details, When frontend consumes items, Then all item/equipment/result fields remain typed.
- BDD: Rule task identity -> Given PATROL_AM and PATROL_PM tasks, When frontend consumes task options, Then AM/PM remain distinct rule keys.
- BDD: Production candidates -> Given production submit candidates, When frontend consumes process projection, Then candidate event identity remains available.

## RED Command and Expected Failure

- RED: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> FAIL because the formal pqcProjection.ts did not exist.

## GREEN Command and Passing Result

- GREEN: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS; output confirms complete DTOs, formal request identity, stable AM/PM order, and stale isolation.
- RED: node focused contract -> FAIL while the page still synthesized a task option from process-level fields.
- GREEN: node focused contract -> PASS after removing the synthesis fallback.
- REGRESSION: pnpm ts:check -> PASS, exit 0.
- RED: node focused contract -> FAIL while the active-order consumer lacked a request token.
- GREEN: node focused contract -> PASS after stale active-order responses are rejected before state mutation.
- RED: node focused contract -> FAIL while the API/page retained acceptanceStandard/processInspectionMethod aliases.
- GREEN: node focused contract -> PASS after the API/page consume only canonical standardText/inspectionMethod.
- RED: node focused contract -> FAIL while the numeric branch accepted legacy result-type aliases.
- GREEN: node focused contract -> PASS after the numeric branch accepts only canonical NUMERIC; pnpm ts:check also PASS.
- RED: node focused contract -> FAIL while active-order refresh compared workOrderId + routeId and could retain a different duplicate order.
- GREEN: node focused contract -> PASS after refresh selection identity uses activeOrderId only; pnpm ts:check also PASS.
- GREEN: 2026-08-14 post-restart node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS, confirming full DTOs, activeOrderId-only identity, stable AM/PM order, duplicate-order identity and real-consumer stale isolation.
- GREEN: 2026-08-14 post-restart pnpm ts:check -> PASS, exit 0.
- GREEN: 2026-08-14 production source scans -> PASS, confirming no old PQC process helper, synthetic task snapshot helper, unused projection loader/rule-order export, process-level flattened task reads, or workOrderId+routeId active-order identity remain in production source.

## Responsive, Accessibility, Loading, Empty, Error, and Permission Checks

- No visible UI, responsive layout, accessibility, loading, empty, error, or permission behavior changed; INT12 owns final runtime UI behavior.
- API helper does not add fallback or hidden success states.

## E2E or Component Verification Path

- Static contract: `node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs`.
- Type verification: `pnpm ts:check`.

## Blockers and Follow-Up Skills

- None. The formal DTO remains strict; no optional compatibility fields, synthesized fallback task, unused production projection loader, or workOrderId+routeId picker identity were retained.
