# Execution Log: 20260528-edhr-archive-approval-evidence

BDD: Archive API contract exposes approval snapshot evidence -> Given a sealed archive response is returned by the backend, When frontend code handles archive generation/latest/page data, Then TypeScript types preserve `approvalSnapshotId` and `approvalSnapshotHash` for evidence and display/assertion code.

BDD: Real E2E downloads sealed archive -> Given the real UI generated a sealed archive, When the user clicks archive download, Then Playwright waits for the controlled backend download response and a non-empty download artifact.

BDD: Real E2E hash verification fails fast -> Given DB/hash verifier prerequisites are missing, When the real E2E reaches final verification, Then it records BLOCKED instead of treating UI-only success as full release evidence.

BDD: Fresh eDHR draft creation -> Given a real test-tenant work order/task context, When the executor opens eDHR from `/mes/pro/feedback`, Then `open-or-create-by-context` must return `created=true` with executionId/executionCode and the script must fail fast instead of reusing historical records.

BDD: FIELD_CHANGE audit before approval submit -> Given the approve-flow fresh draft is editable, When the executor changes a real field, enters a change reason, and signs `FIELD_CHANGE`, Then `/field-audit/save-changes` and `/field-audit/verify-chain` return `hashVerification.status=VALID` and the detail page shows `FIELD_CHANGE`.

- RED: `node --test scripts\edhr-archive-export.test.mjs` -> FAIL, expected archive response type/detail page/E2E script do not yet expose or assert `approvalSnapshotId` / `approvalSnapshotHash` and controlled archive download evidence.
- GREEN: `node --test scripts\edhr-archive-export.test.mjs` -> PASS, 6 tests / 0 failures.
- GREEN: `pnpm e2e:edhr:approval-tracking:check` -> PASS, E2E script syntax is valid.
- BLOCKED: `pnpm e2e:edhr:approval-tracking` with known test tenant values -> FAIL-fast before login/write, missing `EDHR_E2E_APPROVE_FLOW_DRAFT_EXECUTION_ID`, `EDHR_E2E_APPROVE_FLOW_EXECUTION_CODE`, `EDHR_E2E_REJECT_FLOW_DRAFT_EXECUTION_ID`, and `EDHR_E2E_REJECT_FLOW_EXECUTION_CODE`. Evidence: `doc/tasks/20260528-edhr-archive-approval-evidence/real-e2e-evidence.md`.
- GREEN: `pnpm ts:check` -> PASS.
- BLOCKED: overall reviewer gate -> no commit because real mutating E2E is still missing fresh approve/reject draft records.
- RED: Worker A review of `tests/e2e/edhr-approval-tracking-real-flow.e2e.js` -> FAIL, script still requires `EDHR_E2E_APPROVE_FLOW_DRAFT_EXECUTION_ID`, `EDHR_E2E_APPROVE_FLOW_EXECUTION_CODE`, `EDHR_E2E_REJECT_FLOW_DRAFT_EXECUTION_ID`, and `EDHR_E2E_REJECT_FLOW_EXECUTION_CODE`; it does not create fresh drafts from the real feedback UI and does not save `FIELD_CHANGE` before approve-flow submit.
- GREEN: Worker A implementation -> PASS, approve/reject flow no longer requires preset draft ID/code; script creates fresh DRAFT through `/mes/pro/feedback` work order/task selectors and requires `open-or-create-by-context` `created=true`.
- GREEN: Worker A FIELD_CHANGE implementation -> PASS, approve flow modifies one editable field, records reason, signs with `EDHR_E2E_FIELD_CHANGE_SIGNATURE_PASSWORD`, asserts `save-changes` VALID, runs `verify-chain`, opens detail, and requires visible `FIELD_CHANGE`.
- GREEN: `pnpm e2e:edhr:approval-tracking:check` -> PASS, `node --check tests/e2e/edhr-approval-tracking-real-flow.e2e.js`.
- BLOCKED: `pnpm e2e:edhr:approval-tracking` without real env -> FAIL-fast before login/write, missing test-tenant accounts, signature passwords, expected names, unused fresh DRAFT work order/task contexts, and one SUBMITTED negative input mode. Evidence: `doc/tasks/20260528-edhr-archive-approval-evidence/real-e2e-evidence.md`.
- RED: reviewer download SHA contract -> `node --test scripts\edhr-archive-export.test.mjs` -> FAIL, expected real E2E saved no controlled download artifact and did not compare downloaded file SHA-256 with archive response `sha256`.
- GREEN: reviewer download SHA implementation -> PASS, E2E now saves the Playwright download under `test-results/edhr-approval-tracking/`, computes `downloadedSha256`, asserts it equals `archiveEvidence.sha256`, and records `downloadedFilePath` / `downloadedSha256` in evidence.
- GREEN: `node --test scripts\edhr-archive-export.test.mjs` -> PASS, 6 tests / 0 failures after SHA hardening.
- GREEN: `pnpm e2e:edhr:approval-tracking:check` -> PASS after SHA hardening.
- BLOCKED: `pnpm e2e:edhr:approval-tracking` without complete env -> FAIL-fast before login/write, missing base URL, test tenant accounts, signature passwords, expected approver/reject reason, unused DRAFT/approve/reject work order-task contexts, and one SUBMITTED negative input mode. Evidence: `doc/tasks/20260528-edhr-archive-approval-evidence/real-e2e-evidence.md`.
- RED: action CommonResult contract -> `node --test scripts\edhr-archive-export.test.mjs` -> FAIL, expected E2E did not have `readApiBoolean` and submit/approve/reject trusted HTTP 200 without checking `code=0`.
- GREEN: action CommonResult implementation -> PASS, E2E now asserts submit `data=true` and approve/reject response data via CommonResult before waiting for UI state transitions.
- RED: first seeded real E2E -> `pnpm e2e:edhr:approval-tracking` -> FAIL, submit response exposed local DB missing DCC signature authorization column `authorization_state`; execution remained DRAFT, no false UI success was recorded.
- GREEN: full seeded real E2E -> `pnpm e2e:edhr:approval-tracking` -> PASS after local test DB applied existing DCC signature authorization migration and fresh suffix `REVIEWLIVE05280405` contexts were seeded; evidence written to `real-e2e-evidence.md`.
- REGRESSION: final frontend contract -> `node --test scripts\edhr-archive-export.test.mjs` PASS, 7 tests / 0 failures.
- REGRESSION: final frontend E2E syntax -> `pnpm e2e:edhr:approval-tracking:check` PASS.
- REGRESSION: final frontend typecheck -> `pnpm ts:check` PASS.
