# Task: EDHR Archive Approval Evidence Frontend and E2E

## Goal

Extend the real approval-tracking E2E so Worker A can create fresh eDHR DRAFT records from the production feedback UI with real work order/task context, require `open-or-create` to return `created=true`, save a real `FIELD_CHANGE` audit before the approve flow submit, and keep the archive approval, download, tracking, and signature evidence checks.

## Milestones

- [completed] M0 Create frontend task record before code changes.
- [completed] M1 Add RED frontend contract checks for archive approval snapshot evidence.
- [completed] M2 Update TypeScript archive contract and E2E script evidence capture.
- [completed] M3 Run frontend static contract and E2E syntax checks.
- [completed] M4 Run real E2E only if fresh approve/reject draft records and verification prerequisites are available; otherwise record BLOCKED.
- [completed] M5 Replace approve/reject preset draft ID/code inputs with fresh UI draft creation from real work order/task context.
- [completed] M6 Add approve-flow `FIELD_CHANGE` UI save, `verify-chain`, and detail evidence before submit.
- [completed] M7 Refresh BDD/evidence output and run `pnpm e2e:edhr:approval-tracking:check`.
- [completed] M8 Add fail-closed CommonResult assertions for submit/approve/reject action responses.
- [completed] M9 Run full real UI E2E with seeded tenant 122 fresh contexts.

## BDD

BDD: Archive API contract exposes approval snapshot evidence -> Given a sealed archive response is returned by the backend, When frontend code handles archive generation/latest/page data, Then TypeScript types preserve `approvalSnapshotId` and `approvalSnapshotHash` for evidence and display/assertion code.

BDD: Real E2E downloads sealed archive -> Given the real UI generated a sealed archive, When the user clicks archive download, Then Playwright waits for the controlled backend download response, saves the downloaded artifact, recomputes SHA-256, and requires it to equal the archive response `sha256`.

BDD: Real E2E hash verification fails fast -> Given DB/hash verifier prerequisites are missing, When the real E2E reaches final verification, Then it records BLOCKED instead of treating UI-only success as full release evidence.

BDD: Fresh eDHR draft creation -> Given a real test-tenant work order/task context, When the executor opens eDHR from `/mes/pro/feedback`, Then `open-or-create-by-context` must return `created=true` with executionId/executionCode and the script must fail fast instead of reusing historical records.

BDD: FIELD_CHANGE audit before approval submit -> Given the approve-flow fresh draft is editable, When the executor changes a real field, enters a change reason, and signs `FIELD_CHANGE`, Then `/field-audit/save-changes` and `/field-audit/verify-chain` return `hashVerification.status=VALID` and the detail page shows `FIELD_CHANGE`.

BDD: Action API responses fail closed -> Given submit/approve/reject return HTTP 200 with a CommonResult body, When the E2E continues the workflow, Then it must assert `code=0` and the expected data shape before waiting for the next UI state.

## Expected Verification

- `node --test scripts\edhr-archive-export.test.mjs`
- `pnpm e2e:edhr:approval-tracking:check`
- `pnpm e2e:edhr:approval-tracking` only with complete real test tenant prerequisites and unused work order/task contexts.

## Current Status

Completed for Worker A script, reviewer SHA-256 evidence hardening, and CommonResult fail-closed action assertions. `node --test scripts\edhr-archive-export.test.mjs`, `pnpm e2e:edhr:approval-tracking:check`, and `pnpm e2e:edhr:approval-tracking` pass against real seeded tenant 122 contexts.

## Cleanup Keep

- `doc/tasks/20260528-edhr-archive-approval-evidence/real-e2e-evidence.md`
