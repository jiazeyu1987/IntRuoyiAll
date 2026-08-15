# DF11 Verification Report

## Scope

- Frontend API/types: src/api/mes/pro/feedback/index.ts and src/api/mes/qc/template/index.ts.
- Pure projection: src/api/mes/pro/feedback/pqcProjection.ts.
- Active-order consumer identity: src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts.
- Static contract test: tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs.
- The page diff only removes the old process-field-to-task synthesis fallback; no visible page behavior, backend, schema, supervisor state, or shared business data was modified.

## Results

- RED: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> FAIL, missing pqcProjection.ts.
- GREEN: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS.
- RED: node focused contract -> FAIL while FrontlineFixedTemplatePanel.vue contained getProcessPqcTaskSnapshot.
- GREEN: node focused contract -> PASS after the page consumes only formal pqcTaskOptions.
- REGRESSION: pnpm ts:check -> PASS, exit 0.
- RED: node focused contract -> FAIL while selectFrontlinePqcActiveOrder had no request token.
- GREEN: node focused contract -> PASS after stale active-order responses are rejected before mutating current state.
- RED: node focused contract -> FAIL while the frontend retained non-contract item aliases.
- GREEN: node focused contract -> PASS after canonical standardText/inspectionMethod migration; pnpm ts:check also PASS.
- RED: node focused contract -> FAIL while the page recognized non-canonical numeric aliases.
- GREEN: node focused contract -> PASS after NUMERIC-only branching; pnpm ts:check PASS.
- RED: node focused contract -> FAIL while active-order refresh retained selection by workOrderId + routeId.
- GREEN: node focused contract -> PASS after selection retention and cache identity both use activeOrderId; pnpm ts:check PASS.
- GREEN: frontend-feature evidence validator -> PASS.
- GREEN: frontend-feature validator self-test -> PASS.
- GREEN: git diff --check -> PASS.
- GREEN: introduced forbidden-source scan -> PASS, no newly added fallback/compat/mock/default-success/formBindings/legacy workOrderId+routeId/product-QA/material-QA path.
- GREEN: legacy helper/wrong-path source scan -> PASS, no source matches.
- GREEN: 2026-08-14 post-restart node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS.
- GREEN: 2026-08-14 post-restart pnpm ts:check -> PASS, exit 0.
- GREEN: bug-regression evidence validator self-test -> PASS.
- GREEN: frontend-feature evidence validator self-test -> PASS.
- GREEN: frontend-feature evidence validator -> PASS after final evidence refresh.
- GREEN: bug-regression evidence validator -> PASS after final evidence refresh.
- GREEN: production source old-helper scan -> PASS, no matches for getFrontlinePqcActiveOrderProcesses, old /pqc/processes, getProcessPqcTaskSnapshot, createFrontlinePqcProjectionLoader, or FRONTLINE_PQC_RULE_KEY_ORDER in IntRuoyiFronted/src.
- GREEN: production flattened-process read scan -> PASS, no process-level flattened task reads and no withPqcTaskOption helper in production source.
- GREEN: active-order legacy identity scan -> PASS, no workOrderId+routeId picker key/equality pattern remains.
- GREEN: git diff --check -> PASS, exit 0; Git emitted only LF-to-CRLF working-copy warnings.

## Notes

- Task option inspectionRuleKey/taskStatus/ruleSort/inspectionTypeRule remain required; NOT_CREATED/MIXED exist only in task summary state.
- Page component change is limited to deleting the obsolete task-synthesis helper and its call; formal task options are now the only task source.
- No fallback, mock success, default-success path, product/material inference, or route/process QA validation was added.
- Duplicate work-order/route rows remain independent because active-order selection, cache, refresh retention, and process reads all use activeOrderId.
- The round-3 independent findings are addressed: picker identity is activeOrderId-only, FrontlinePqcProcessVO has no top-level flattened task snapshot, stale-response coverage exercises the real selectFrontlinePqcActiveOrder consumer, and unused projection/rule-order production exports are absent.
