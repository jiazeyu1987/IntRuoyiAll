# DF11 Execution Log

## Intent

DF11 implements the frontend-side contract for dedicated frontline PQC process projection. Owned files are limited to `IntRuoyiFronted/src/api/mes/pro/feedback/**`, `IntRuoyiFronted/src/api/mes/qc/template/index.ts`, `IntRuoyiFronted/tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs`, and this task's own documents.

## Rule Reads

- Read `AGENTS.md`.
- Read `docs/task-closeout-rules.md`.
- Read `docs/frontend-development.md`.
- Read `docs/powershell-encoding.md`.
- Read `docs/powershell-memory.md` before git diff verification.
- Read `frontend-feature-delivery` skill and `references/frontend-contract.md`.
- Read DF11 section in `dev-plan.md` and TC-DF11-FRONTEND-PROJECTION in `test-plan.md`.
- Design package note: `system-design.md` was not present under the delivery-supervision task; DF11 uses the existing PRD/dev-plan/test-plan and current backend VO/read-only contract as the formal source for this scoped frontend API task.

## BDD Scenarios

- BDD: Active order request identity -> Given the user selects a frontline PQC active order with activeOrderId, When frontend requests dedicated PQC processes, Then the API helper sends only activeOrderId to `/mes/pro/feedback/frontline/device-account/pqc/active-order/processes` and does not require workOrderId + routeId inference.
- BDD: Full item projection -> Given the backend returns QA item fields with result types and equipment options, When frontend consumes `FrontlinePqcProcessVO`, Then it keeps item code/name, standard/method text, numeric bounds, unit, precision, equipment requirement/options, and `BOOLEAN | NUMERIC | TEXT` resultType without reducing the item model.
- BDD: Rule task identity -> Given a QA process has FIRST, PATROL_AM, PATROL_PM, and FINAL rule tasks, When frontend consumes task options, Then it preserves `inspectionRuleKey`, task status, business date, round, task id, and planned quantity so AM and PM patrols remain separate and sorted by rule order.
- BDD: Production candidates -> Given task overlay includes production submit candidates, When frontend consumes process projection, Then it preserves event id and submit time for later INT12 page integration without filtering the outer QA process.
- BDD: Duplicate work-order and route rows -> Given two valid active orders share the same workOrderId and routeId, When the active-order pool refreshes, Then the selected row is retained only when its activeOrderId still exists and the two rows are never collapsed.

## Evidence

- RED: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs on isolated baseline a386dc0da plus only the DF11 static contract -> FAIL, expected reason: Frontline PQC API did not expose the formal FIRST/PATROL_AM/PATROL_PM/FINAL rule-key union.
- GREEN: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS, static contract preserves activeOrderId, item, task, rule, status, and candidates.
- REGRESSION: pnpm ts:check -> PASS after installing frontend dependencies with pnpm install --frozen-lockfile because the DF11 worktree initially had no node_modules.
- NOTE: The process-level and task-option projections keep inspectionRuleKey/taskStatus required. Existing page-local task snapshots now pass these two formal fields through without changing selection, submission, or other INT12-owned page behavior.

## Blockers

- None. The supervisor clarified the narrow typecheck scope: the page may remove the local task-synthesis fallback but may not change rendering, interaction, selection, loading, or submission behavior.

## 2026-08-13 Verification

- GREEN: git status ownership check -> PASS, changed files are limited to src/api/mes/pro/feedback/index.ts, src/api/mes/qc/template/index.ts, tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs, and DF11 task evidence files.
- GREEN: node static contract -> PASS.
- GREEN: pnpm ts:check -> PASS.
- GREEN: frontend-feature evidence validator -> PASS.
- GREEN: git diff --check -> PASS.
- GREEN: introduced forbidden-source scan -> PASS, no new fallback/compat/mock/default-success/formBindings/legacy workOrderId+routeId/product-QA/material-QA path.

## 2026-08-13 Independent Test Round 2 Fix

- RED: node focused contract -> FAIL with ENOENT for missing `src/api/mes/pro/feedback/pqcProjection.ts`.
- GREEN: node focused contract -> PASS; verifies complete inspectionTypeRules/taskSummary/task option rule/status/item/candidate contracts, exact frozen endpoint, absence of old helper, reverse-order stable sorting, AM/PM preservation, and stale-response rejection.
- RED: node focused contract -> FAIL because FrontlineFixedTemplatePanel.vue still contained getProcessPqcTaskSnapshot and synthesized a task option from process-level fields.
- GREEN: node focused contract -> PASS after deleting that fallback and consuming only formal pqcTaskOptions.
- REGRESSION: pnpm ts:check -> PASS, exit 0.
- GREEN: frontend evidence validator -> PASS.
- GREEN: git diff --check -> PASS.
- GREEN: exact introduced production-line forbidden scan -> PASS.
- GREEN: legacy helper/wrong-path source scan -> PASS, rg exit 1 with no matches for getFrontlinePqcActiveOrderProcesses or /pqc/processes.
- SCOPE: FrontlineFixedTemplatePanel.vue changes are limited to deleting getProcessPqcTaskSnapshot and using getDefaultPqcTaskOption directly; no rendering, interaction, selection, loading, or submission contract was added.
- RED: node focused contract -> FAIL because the active-order consumer lacked a request token and could let an older order response overwrite the newly selected order.
- GREEN: node focused contract -> PASS after adding pqcActiveOrderSelectionRequestToken and a dedicated stale-selection rejection path; the page ignores only that explicit superseded-request signal.
- REGRESSION: pnpm ts:check -> PASS after the active-order token correction.
- RED: node focused contract -> FAIL because the frontend item DTO/page still retained acceptanceStandard/processInspectionMethod compatibility aliases outside the frozen published-version contract.
- GREEN: node focused contract -> PASS after replacing those aliases with canonical standardText/inspectionMethod usage only.
- REGRESSION: pnpm ts:check -> PASS after canonical item-field migration.
- RED: node focused contract -> FAIL because the PQC numeric branch accepted NUMBER/DECIMAL/MEASURE/MEASURED_VALUE aliases.
- GREEN: node focused contract -> PASS after isPqcNumericResultType accepts only the typed NUMERIC value.
- REGRESSION: pnpm ts:check -> PASS after the canonical resultType branch change.
- RED: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> FAIL because active-order refresh retained selection by workOrderId + routeId and could collapse duplicate active orders.
- GREEN: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS after active-order refresh retains selection only by activeOrderId.
- REGRESSION: pnpm ts:check -> PASS after the activeOrderId refresh-identity correction.

## 2026-08-14 Post-Restart Round 3 Finishing

- GREEN: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS, output: "PASS: frontline PQC process contract preserves full DTOs, formal identity, stable AM/PM order, and stale isolation".
- GREEN: pnpm ts:check -> PASS, exit 0.
- GREEN: frontend-feature evidence validator self-test -> PASS.
- GREEN: bug-regression evidence validator self-test -> PASS.
- GREEN: frontend-feature evidence validator -> PASS before final evidence refresh.
- RED: bug-regression evidence validator -> FAIL before final evidence refresh, expected reason: evidence still had "Pending worker verification" and lacked required GREEN:/Verification markers.
- GREEN: production source old-helper scan -> PASS, no matches in IntRuoyiFronted/src for getFrontlinePqcActiveOrderProcesses, old /pqc/processes, getProcessPqcTaskSnapshot, createFrontlinePqcProjectionLoader, or FRONTLINE_PQC_RULE_KEY_ORDER.
- GREEN: production flattened-process read scan -> PASS, no process.pqcTaskId / process.inspectionType / process.businessDate / process.shiftCode / process.roundNo / process.plannedInspectionQuantity reads and no withPqcTaskOption helper.
- GREEN: active-order legacy identity scan -> PASS, no workOrderId+routeId picker key/equality pattern; picker key/equality uses buildFrontlineActiveOrderPickerKey/isSameFrontlineActiveOrder backed by activeOrderId.
- GREEN: git diff --check -> PASS, exit 0; Git emitted only LF-to-CRLF working-copy warnings.
- GREEN: frontend-feature evidence validator -> PASS after final evidence refresh.
- GREEN: bug-regression evidence validator -> PASS after final evidence refresh.
- GREEN: final git diff --check -> PASS, exit 0; Git emitted only LF-to-CRLF working-copy warnings.
- STATUS: DF11 remains ready_for_closeout with no external blocker and no running commands.
