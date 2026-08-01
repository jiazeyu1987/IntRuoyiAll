# Frontend Feature Evidence

## Feature

PQC inspector fixed-template panel now uses formal PQC source chains: current active orders, selected order route processes, and all PQC employees plus PQC leaders.

## Acceptance

- PQC order card opens an order picker backed by `/pqc/active-orders`.
- Choosing an active order resets process/employee context and loads route processes from `/pqc/active-order/processes`.
- Choosing a PQC process loads personnel from `/pqc/personnel`.
- Choosing a PQC employee switches through `/pqc/switch-employee` instead of production device-account employee binding.
- PQC submit validation no longer blocks on placeholder text; it fills formal `PQC_RESULT` as `DETECTION_FAILED` if scrap quantity is positive or any inspected piece is marked `不合格`, otherwise `DETECTION_SUCCESS`.
- Production mode keeps existing `processes`, `employee-candidates`, and `switch-employee` paths.

BDD: PQC order selector uses active orders -> Given a PQC inspector opens the fixed template panel / When the order selector loads / Then only active orders are returned and all-order fallback is not allowed.

BDD: PQC process selector uses selected active order route -> Given a PQC inspector selected an active order with product route / When the process selector loads / Then processes come from that product route and missing route fails visibly.

BDD: PQC employee selector uses PQC personnel -> Given a PQC inspector opens the employee selector / When personnel options load / Then the options include all PQC employees and PQC leaders, not unrelated employees.

BDD: PQC leader review is consistent with inspector submissions -> Given PQC inspectors submitted inspection content / When a PQC leader opens the review list / Then list content matches submitted content and correction/submission logs are available.

RED: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js` -> FAIL, expected reason: panel lacked order picker and active-order chain contract.

GREEN: `node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS.

## Verification

- `pnpm ts:check` -> PASS.
- `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS.
- `node tests\e2e\process-pool-review-copy-and-revision-static.spec.js` -> PASS.
- `node tests\e2e\process-pool-event-revision-api-static.spec.js` -> PASS.

## Blockers

No frontend implementation blocker remains. Real browser E2E was not run because this request was verified through focused static contracts and unit tests without starting local services or creating write-type test data.
## 2026-08-01 PQC Submit Feature Optimization

- Feature goal: PQC submit is no longer validate-only; it persists through submitFrontlinePqcInspection before showing 已提交.
- UI entry: FrontlineFixedTemplatePanel PQC mode now requires explicit signatureId input. Missing signatureId blocks submit; no default signature is generated.
- API state: ProFeedbackApi.submitFrontlinePqcInspection posts to /mes/pro/feedback/frontline/device-account/pqc/submit.
- Payload state: rawPayload includes pqcDraft, pqcPieceValues, fieldValues, inspectionResult, selected active order/process/employee and template payload preview.
- RED: node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js -> FAIL before implementation because handleValidate showed 已提交 after validatePayload only.
- GREEN: node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js -> PASS.
- Verification: node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js -> PASS; pnpm ts:check -> PASS; team-leader/review-copy/event-revision static contracts -> PASS.
- Blocker: real write-type Playwright E2E was not run because local services/login/test write data were not prepared in this turn.
