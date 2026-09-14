# DF11 Independent Verification Report - Round 3

## Objective

Independently verify the post-restart DF11 implementation against the approved DF11 task, the frozen frontline PQC interface contract, `TC-DF11-FRONTEND-PROJECTION`, and the no-fallback/no-compatibility constraints. This verification changed no production code, test code, supervisor state, or supervisor report.

## Evidence Reviewed

- Supervisor `prd.md`, `dev-plan.md`, and `test-plan.md`, including the amended DF11 affected paths and page-local integration boundary.
- Approved `common-background.md`, `interface-contracts.md`, and `agent-tasks/DF11-items-page-projection.md`.
- Applicable root `AGENTS.md`, frontend development rules, PowerShell/encoding rules, and the independent-verification-gate skill.
- Complete current DF11 Git status and diff for the API types/helper, pure projection, active-order consumer, page adapter, QA API types, focused static contract, and task evidence.
- Direct source scans for the formal request identity, rule keys/order, item fields, legacy identities, compatibility fields, fallback/default-success patterns, and actual projection/stale-response consumers.

## Requirement Coverage

| Requirement | Result | Evidence |
|---|---|---|
| Frozen process endpoint uses only `activeOrderId` | PASS | `getPqcProcesses(activeOrderId)` calls `/pqc/active-order/processes` with only `params: { activeOrderId }`; the old request helper is absent. |
| Four formal rule keys and stable task order | PASS | Strict rule-key union is present; runtime projection sorts by `businessDate`, `ruleSort`, `roundNo`, and `pqcTaskId`; reverse-order executable fixture preserves FIRST, AM patrol, PM patrol, FINAL independently. |
| Complete published item, equipment, rule, summary, task, and candidate fields | PASS | Required process/task/item interfaces contain the frozen fields, canonical names, strict statuses, and complete source metadata. |
| Canonical `BOOLEAN/NUMERIC/TEXT` only | PASS | API and QA types use the strict union; page numeric handling accepts only `NUMERIC`; legacy numeric aliases and item-field aliases are absent from the changed path. |
| Network projection is consumed | PASS | `getPqcProcesses` calls `projectFrontlinePqcProcesses`, and the active-order consumer calls `getPqcProcesses`. |
| Older network result cannot overwrite a newer selection | PASS with test gap | The real consumer increments and checks `pqcActiveOrderSelectionRequestToken` before state assignment. The executable stale-order test exercises a separate loader rather than the real consumer, so this behavior is only source-asserted. |
| `activeOrderId` is the unique page selection identity, including duplicate work-order/route rows | FAIL | The cache and refresh-retention path use `activeOrderId`, but the actual picker key is still `${order.workOrderId}-${order.routeId}` and `isSameActiveOrder` still compares `workOrderId + routeId`. Duplicate active-order rows therefore share a Vue key and active state. |
| No process-level compatibility task snapshot/fallback remains | FAIL | `FrontlinePqcProcessVO` still declares optional top-level `pqcTaskId/inspectionType/businessDate/shiftCode/roundNo/plannedInspectionQuantity`; the page copies task-option data back into these fields and reads `process.pqcTaskId`. This is the old flattened compatibility shape outside the frozen response contract. |
| Scope and regression verification | PASS | Changes are confined to the supervisor-approved DF11 paths/task records; focused static test, typecheck, evidence validator, and diff check pass. |

## Verification Commands

- `node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs` -> PASS: full DTOs, formal identity, AM/PM ordering, and synthetic stale isolation.
- `pnpm ts:check` -> PASS, exit 0.
- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\frontend-feature-delivery\\scripts\\validate_frontend_feature.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df11/frontend-feature-evidence.md` -> PASS.
- `git diff --check` -> PASS, exit 0; only line-ending warnings were emitted.
- Added-line forbidden scan for fallback/compat/mock/default-success/formBindings/product-QA aliases/legacy item fields/result-type aliases -> PASS, zero introduced matches.
- Exact source scan -> FAIL for page identity and strict response shape: picker key and equality still use `workOrderId + routeId`; API/page still retain and consume flattened top-level task fields.

## Findings

1. High: The page does not use `activeOrderId` as its unique picker identity. `FrontlineFixedTemplatePanel.vue:2083` keys active-order rows by work order and route, while `FrontlineFixedTemplatePanel.vue:4246-4252` marks equality with the same pair. Two effective rows with the same work order and route can therefore collapse in Vue reconciliation or display as simultaneously active, violating the explicit duplicate-order acceptance behavior.
2. High: The dedicated process response is not yet a strict frozen DTO. `src/api/mes/pro/feedback/index.ts:248-254` retains the old optional process-level task snapshot, and `FrontlineFixedTemplatePanel.vue:2508-2524` reconstructs that compatibility shape from a task option. Downstream reads such as `FrontlineFixedTemplatePanel.vue:1693` still depend on `process.pqcTaskId`. The approved response contract places these fields only in `pqcTaskOptions`; keeping both shapes leaves two authorities and permits fallback behavior to return.
3. Medium: The stale-response test does not execute the production consumer. `createFrontlinePqcProjectionLoader` is used only by the test, while `selectFrontlinePqcActiveOrder` implements a separate request-token mechanism. The current implementation appears correct by inspection, but a focused executable consumer test is needed to prove an older response cannot mutate `processOptions`, `selectedActiveOrder`, `lastError`, or loading state after a newer selection.
4. Medium: `FRONTLINE_PQC_RULE_KEY_ORDER` and `createFrontlinePqcProjectionLoader` are production exports with no production consumer. They duplicate server `ruleSort`/the real consumer's request token and add avoidable parallel abstractions. Remove them unless the real data flow is changed to consume them directly.

## Residual Risks

- The three pressure-pump orders may render correctly when their work-order/route pairs differ, while the explicit duplicate-row invariant remains broken and untested in the real picker.
- Two task identities remain available on the same process object. Future INT12 code could accidentally read stale flattened fields instead of the selected formal task option.
- A refactor of the real active-order consumer could break stale isolation without failing the current executable synthetic-loader test.

## Decision

FAIL

All required commands pass, but DF11 is not releasable because the actual picker still identifies active orders by `workOrderId + routeId` and the frontend response/page retain the prohibited flattened compatibility task snapshot. Passing static/type checks do not cover these contract violations.

## Follow-Up Actions

- Use `String(order.activeOrderId)` for the picker row key and compare active orders only by `activeOrderId`; add a focused fixture with two rows sharing the same work order and route.
- Remove the process-level optional task fields from `FrontlinePqcProcessVO`; keep the selected task as a page-local task-option identity rather than writing it back into the server process DTO.
- Exercise the real `selectFrontlinePqcActiveOrder` consumer with out-of-order promises and assert all relevant state remains owned by the latest activeOrderId.
- Remove the two unused production abstractions or make the real production path consume one coherent projection/stale-loader implementation.
