# DF11 Independent Test Report - Round 2

## Objective

Independently re-verify DF11 against the supervisor `dev-plan.md` DF11 contract, `TC-DF11-FRONTEND-PROJECTION`, and section 2 of the approved frontend interface contract. This review does not modify production code, tests, supervisor state, or any other task record.

## Evidence Reviewed

- `doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/dev-plan.md`, DF11 lines 259-276.
- `doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/test-plan.md`, `TC-DF11-FRONTEND-PROJECTION` lines 151-162.
- `E:/IntRuoyi/doc/tasks/20260811-frontline-pqc-dcc-qa-agent-design/interface-contracts.md`, section 2.
- `E:/IntRuoyi/doc/tasks/20260811-frontline-pqc-dcc-qa-agent-design/agent-tasks/DF11-items-page-projection.md`.
- DF11 task records, full production diff, focused static contract, Git status, and relevant frontend/backend source.
- Current DF10 worktree was inspected read-only to judge DTO parity; it is still uncommitted and remains separate from the DF11 worktree.

## Requirement Coverage

| Requirement | Result | Evidence |
|---|---|---|
| `getPqcProcesses(activeOrderId)` sends only `activeOrderId` | PASS | The focused helper sends `params: { activeOrderId }`; static contract passes. |
| Formal request path matches section 2 and old request is absent | FAIL | Section 2 freezes `/pqc/active-order/processes?activeOrderId=...`, but the new helper calls `/pqc/processes`. The old `getFrontlinePqcActiveOrderProcesses({ workOrderId, routeId })` and `/pqc/active-order/processes` request remain in the API module. |
| Formal API types require `inspectionRuleKey` and task status | PARTIAL | The optional compatibility fields were removed and `pnpm ts:check` passes after local snapshot pass-through. However, the frontend field is `taskStatus`, while the current DF10 `PqcTaskOption` DTO uses `status`, so cross-layer DTO parity is not established. |
| Complete published item fields | PASS | The frontend item type includes item sort, names/method/tool/sampling, structured numeric fields, equipment, applicable types, quantities, critical/failure rule, and all source fields. |
| Canonical `BOOLEAN/NUMERIC/TEXT` and four rule keys | PASS | Both unions are strict and the focused test checks them. |
| `ruleSort` and complete `inspectionTypeRule` | FAIL | Neither field exists in `FrontlinePqcTaskOptionVO`; the static test does not assert either field. A standalone `FRONTLINE_PQC_RULE_KEY_ORDER` constant is not the response field required by the contract. |
| `inspectionTypeRules` | FAIL | `FrontlinePqcProcessVO` does not expose the locked version's `inspectionTypeRules`; no static assertion covers it. |
| `taskSummary` | FAIL | `FrontlinePqcProcessVO` has no `taskSummary`, and there is no summary type for `state/totalCount/pendingCount/submittedCount/confirmedCount/cancelledCount`. |
| Full task status contract | FAIL | `FrontlinePqcTaskStatus` allows only `NOT_CREATED | PENDING`; section 2 requires task options to support `PENDING/SUBMITTED/CONFIRMED/CANCELLED`, with `NOT_CREATED/MIXED` represented in the summary state. |
| Production submit candidates | PASS | Candidate type retains `eventId/serverSubmitTime/activeOrderId/routeProcessId/processId`; process candidate list is required. |
| Stable task projection and stale-response isolation | FAIL | DF11 owns a pure projection/composable and requires sorting by `businessDate/ruleSort/roundNo/taskId` plus stale-response isolation. No projection/composable exists, `getPqcProcesses` has no consumer, and the static test only checks a constant. |
| No fallback/compat/default-success/formBindings/NUMBER/CHOICE in added production lines | PASS | Exact added-line scan across the three changed production files returned no hits. |
| Scope conforms to supervisor ownership | FAIL | Supervisor `dev-plan.md` says page components must not be edited, but `FrontlineFixedTemplatePanel.vue` is modified. The change is type-only, yet it is still outside the frozen write scope. |

## Verification Commands

- `node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs` -> PASS: `PASS: frontline PQC process API contract preserves activeOrderId, item, task, rule, status, and candidates`.
- `pnpm ts:check` -> PASS, exit 0.
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260812-frontline-pqc-dcc-qa-df11\frontend-feature-evidence.md` -> PASS.
- Validator `--self-test` -> PASS.
- `git diff --check` -> PASS, exit 0; only Git line-ending warnings were emitted.
- Exact scan of 57 added production lines for fallback/compat/default-success/mock/formBindings/`'NUMBER'`/`'CHOICE'`/legacy helper/legacy path/workOrderId/routeId -> PASS, zero hits.
- Read-only `rg` source checks -> found no `taskSummary`, `ruleSort`, or `inspectionTypeRule` in the frontend API type or focused test; found no use of `getPqcProcesses`; found the old request helper still present.

## Findings

1. High: The frontend DTO is incomplete. It omits `inspectionTypeRules`, `taskSummary`, task-option `ruleSort`, and task-option `inspectionTypeRule`, all explicitly required by the approved section 2 contract and DF11 task definition.
2. High: The status contract is modeled incorrectly. Task options must cover `PENDING/SUBMITTED/CONFIRMED/CANCELLED`; `NOT_CREATED` and `MIXED` belong to task summary state. The current union is only `NOT_CREATED | PENDING`.
3. High: The request migration is not complete. The new helper uses a path not authorized by section 2, while the old workOrderId/routeId helper and request path remain in source.
4. High: Stable projection behavior is absent. No pure projection/composable sorts task options from reverse network order by `businessDate/ruleSort/roundNo/taskId`, and no stale-response isolation exists. `getPqcProcesses` is not used anywhere.
5. Medium: Compile-time/backend DTO parity is still not proven. The current separate DF10 implementation uses `PqcTaskOption.status`, while DF11 requires `taskStatus`; it also does not yet expose all required rule/summary fields. Passing local TypeScript checks cannot prove JSON field parity.
6. Medium: The focused static test is too weak for the stated contract. It passes without asserting the missing fields, complete status states, projection ordering, stale response isolation, exact approved path, or global removal of the old request.
7. Medium: The page component change violates the supervisor's explicit DF11 write scope, even though the two pass-through edits are behavior-neutral.

## Residual Risks

- The frontend can compile while receiving undefined task status and rule metadata from the eventual backend payload because there is no shared/generated DTO or fixture-based parity test.
- INT12 would have to infer labels/order from rule keys or existing page logic, contrary to the contract, because complete rule objects and server ruleSort are unavailable.
- Both old and new request helpers can coexist and permit callers to continue the prohibited workOrderId/routeId request path.

## Decision

FAIL

The optional-field compatibility defect from round 1 is fixed, and all requested commands pass. DF11 still lacks mandatory contract fields and projection behavior, retains the prohibited legacy request, uses an unapproved endpoint path, and lacks sufficient tests to detect those gaps.

## Follow-Up Actions

- Align the helper with the frozen section 2 endpoint and remove the old workOrderId/routeId request helper.
- Add required process `inspectionTypeRules` and `taskSummary`, and task-option `ruleSort`, complete `inspectionTypeRule`, and full task statuses.
- Add a pure projection/composable that performs the specified stable ordering and stale-response isolation.
- Add compile-time or fixture-driven frontend/backend DTO parity tests, including the exact task status field name.
- Extend the static contract to fail on every missing field, old request presence, wrong path, status narrowing, reverse-order tasks, and stale response overwrite.
- Restore the supervisor-owned page component to DF11's frozen scope or obtain an explicit scope amendment before reconsideration.
