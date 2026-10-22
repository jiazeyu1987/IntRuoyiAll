# DCC Detail Native Evidence Static Contract

## Goal

Add frontend static contracts for the native three-workflow training evidence and distribution completion projections in the controlled-file detail page.

## BDD

- BDD: Training evidence summary -> Given a controlled-file detail source with the native training evidence projection, When the training section is inspected, Then the evidence alert is gated by `needTraining && trainingRecordAvailable`, displays `trainingRecordFileName`, and its empty state distinguishes uploaded evidence from ordinary absence without hardcoded or fake evidence.
- BDD: Distribution completion summary -> Given a controlled-file detail source with the native distribution completion projection, When the distribution section is inspected, Then the completion alert is gated by `distributionCompleted` and identifies the completed document-control distribution node and absence of per-department receipts.
- BDD: Ordinary workflow isolation -> Given ordinary workflows receive the native projection flags as false, When the detail source is inspected, Then the frontend native evidence alerts rely only on those projection flags and do not fabricate native evidence.

## Milestones

1. Read repository, frontend, E2E, and task closeout rules; inspect current static specs and source contracts.
2. Append focused static assertions to the two requested specs while preserving existing user changes.
3. Run the two Node static tests and `pnpm ts:check`.
4. Record exact results and mark the task ready for closeout after verification.

## Expected Verification

- From `IntRuoyiFronted`: `node tests/e2e/dcc-detail-training-summary-static.spec.js`
- From `IntRuoyiFronted`: `node tests/e2e/dcc-detail-distribution-summary-static.spec.js`
- From `IntRuoyiFronted`: `pnpm ts:check`
- `git diff --check` for the task-owned files.

## Current Status

blocked

Static contracts and requested checks passed. Repository closeout requires committing and pushing the task changes, but this turn explicitly prohibits Git changes; no Git staging, commit, or push was attempted.

## Design Constraints

- Allowed write set: `IntRuoyiFronted/tests/e2e/dcc-detail-training-summary-static.spec.js`, `IntRuoyiFronted/tests/e2e/dcc-detail-distribution-summary-static.spec.js`, and this task's `doc/tasks/20260928-dcc-detail-native-evidence-static-contract/` directory.
- Do not modify Vue product code, database, Git history/index, or running services.
- Preserve the existing uncommitted distribution spec changes; append assertions without replacing its current behavior.
- Static source contracts must verify the exact projection predicates, fields, and Chinese user-facing copy. Fake or hardcoded evidence values are forbidden.
- This task adds static contracts only; it is not a substitute for real-page E2E.
