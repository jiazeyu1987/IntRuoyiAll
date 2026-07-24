# 20260528 EDHR Domain Trace Frontend and E2E Implementation

## Goal

Implement the frontend API, page entry, and contract-test slice for eDHR domain master-data traceability in this service repository, following the root task `doc/tasks/20260528-edhr-domain-trace-implementation` and the production implementation docs.

This frontend worker scope excludes `tests/e2e` and `package.json`; the E2E worker owns the real Playwright path in parallel.

## Milestones

- [completed] M0 Create frontend task record before code changes.
- [completed] M1 Add RED frontend API/page contract tests for the domain trace path.
- [completed] M2 Implement minimal frontend API client, visible route/page entry, and page states to pass contract tests.
- [completed] M3 Implement Playwright real user path; E2E worker added the fail-fast real path script and package scripts. Real execution is GREEN with test tenant execution `id=9 / BRE202605242206492170009`.
- [completed] M4 Run focused frontend verification and record GREEN evidence. Frontend Node contracts, regressions, `pnpm ts:check`, and real Playwright E2E are GREEN.
- [completed] M5 Reviewer gate repair: align frontend domain trace fields with backend final item/blocker contract. Final narrow repair removed item-level `id/domainTraceSnapshotId/verifiedAt` remnants from frontend type and detail table.

## Expected Verification

- Node contract tests for domain trace API client and UI route/page semantics.
- Playwright E2E test file for real menu navigation, domain trace detail, and verification behavior is implemented by the E2E worker; real execution requires configured `EDHR_E2E_*` values and real test tenant data.
- No test-only controls, no mock success, no live tenant data modification.

## Current Status

Final reviewer gate complete in frontend and E2E scope. `EdhrDomainTraceItemVO` now declares only backend item fields `itemType/itemKey/itemName?/sourceId?/sourceCode?/sourceVersion?/snapshotJson?/snapshotHash?/status/blockerReason?`; the detail item table no longer renders item-level `domainTraceSnapshotId` or `verifiedAt`. Top-level detail `domainTraceSnapshotId` remains visible. Focused domain trace contracts, related eDHR regressions, `pnpm ts:check`, and real E2E are GREEN against current worktree frontend `http://localhost:8081`, current backend `48080`, and test tenant execution `9 / BRE202605242206492170009`.

After the backend final reviewer repair, real E2E was re-run against the rebuilt backend. The prior duplicate snapshot 500 on repeated verify is fixed; the frontend contract extractor was also made CRLF-safe for Windows route/table parsing.
