# Change Request: A6 Target Route Correction To Pressure Pump

## Request

- Source: user correction on 2026-08-10.
- Summary: The P7/A6 real fixture and E2E target is `球囊扩张压力泵`, not `球囊扩张导管`; prior catheter-route blockers must not drive the next execution pass.

## Baseline

- Current supervised task directory: `doc/tasks/20260809-active-order-release-dossier-v4-delivery`.
- P1-P6 are completed with execution and independent test evidence.
- P7/A6 is blocked and currently contains stale catheter-route blockers for `routeId=900025`.
- Existing pressure-pump evidence identifies source route `922119 / RT000028 / 球囊扩张压力泵 / 627 / V27 / ACTIVE` with 14 processes and 14 MAIN reports.

## Classification

- Requirement correction / target data correction.
- The correction affects P7/A6 fixture and E2E scope only; it does not invalidate the generic A1-A5 implementation contracts.

## Impact

- Product impact: target route changes from catheter to pressure pump for A6 real fixture and Playwright flow.
- Design impact: P7 blockers must be recalculated against pressure-pump route `922119`, not catheter route `900025`.
- Data impact: catheter product IDs `902231/902252/902262/907242` are no longer relevant target selectors for P7.
- API impact: no production API contract change expected.
- Test impact: preflight and E2E evidence must be rerun or re-evaluated against pressure-pump route data.
- Release impact: task remains blocked until pressure-pump formal prerequisites are proven or configured.
- Operations impact: no write action is authorized by this correction alone.

## Decision

- Accept.
- Do not proceed with catheter route `900025` blockers as current P7 scope.
- Rebase P7/A6 blocker state on pressure-pump route `922119` after read-only verification.

## Downstream

- Update `task.md`, `task-state.json`, `execution-log.md`, and `verification-report.md` to mark catheter blockers stale and pressure-pump route as the active P7 target.
- Rerun only read-only pressure-pump prerequisite checks first.
- Resume A6 execution only if pressure-pump prerequisites satisfy the same no-fallback E2E gates.

## Blockers

- Read-only pressure-pump prerequisite checks must confirm product selection, traditional report bindings, QA/items/equipment, mappings, release approval, credentials, and task-owned fixture feasibility.
- No SQL/API/UI business writes are authorized by this change request alone.
