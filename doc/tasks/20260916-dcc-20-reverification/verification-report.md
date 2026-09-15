# Verification Report

## Scope
Current `int_qms` HEAD re-verification for DCC-MAIN-01 through DCC-MAIN-20 after the latest int_main merge and the prior DCC remediation commits. This report uses current source and current test output; older task evidence is background only.

## Conclusion
All 20 DCC-MAIN items are fixed in the current static/local regression scope. No remaining code/static defect was identified for DCC-MAIN-01 through DCC-MAIN-20. This pass does not claim real-page E2E, production database execution, deployment, or server-runtime verification because the current user instruction did not explicitly request E2E and the repository rules require real Playwright E2E only when requested in the current turn.

## Per-item Result
| Item | Result | Current evidence |
|---|---|---|
| DCC-MAIN-01 | PASS | Final approval path no longer waits for distribution; finalization/approval defaults/static contracts and backend finalization regression passed. |
| DCC-MAIN-02 | PASS | Final approval path no longer waits for training; training side-effect static contract, workflow/finalization regressions and backend training regression passed. |
| DCC-MAIN-03 | PASS | Existing-file changes go through checkout/checkin with explicit MINOR/MAJOR and same-session CAD/PDF identity; frontend and backend version/checkin contracts passed. |
| DCC-MAIN-04 | PASS | Version-specific attachment projection and content comparison on metadata-only CAD replay are covered by query/finalization regression. |
| DCC-MAIN-05 | PASS | Checkout, cancel-checkout and working submission use lock/current-state/CAS paths; lifecycle and working-submission SQL regressions passed. |
| DCC-MAIN-06 | PASS | DCC required candidate validation rejects missing/disabled required approvers instead of silently dropping them; BPM/DCC candidate regression included in 555-pass run. |
| DCC-MAIN-07 | PASS | Ordinary return/transfer write entrypoints reject; frontend ordinary action visibility and backend removed-action tests passed. |
| DCC-MAIN-08 | PASS | Ordinary sign/add-sign write entrypoints reject; final approval still binds stamped PDF once with signature verification. |
| DCC-MAIN-09 | PASS | Upload pending/failed state cannot fall back to metadata-only checkin; frontend upload-state and browser checkin contracts passed. |
| DCC-MAIN-10 | PASS | Invalid template items remain visible for admin repair but are excluded/rejected for upload; project-template backend and frontend template contracts passed. |
| DCC-MAIN-11 | PASS | Upload creates new files with explicit valid initial version; revision upload/standalone major entrypoints absent from ordinary paths; new-upload version contracts passed. |
| DCC-MAIN-12 | PASS | Final approval uses configured default directory and preserves logical identity across same-name/different-project cases; finalization and defaults contracts passed. |
| DCC-MAIN-13 | PASS | Self/descendant directory parent cycles are blocked and corrupted cycles fail bounded; directory admin/cycle tests passed. |
| DCC-MAIN-14 | PASS | Related-file search is paginated/server-filtered, preserves selected labels, and relation binding checks project and per-target name authorization; frontend and backend relation tests passed. |
| DCC-MAIN-15 | PASS | Product number is resolved through the same server source used for persistence; unbound product remains null and product-bound categories fail explicitly when required. |
| DCC-MAIN-16 | PASS | Name/content permission projection is separate per version; content fields/actions are redacted or exposed by each version's grant; query/project and frontend name-content contracts passed. |
| DCC-MAIN-17 | PASS | Concurrent old transfer/sign mutation paths are closed for ordinary DCC; backend public ordinary methods reject before BPM/signature mutation. |
| DCC-MAIN-18 | PASS | Directory/list/permission/pagination async results carry current-context ownership; stale success/failure/loading/cache updates cannot overwrite current view. |
| DCC-MAIN-19 | PASS | Approval PDF upload uses task/file/stage/purpose scoped authorization and frozen signature evidence is reverified before activation. |
| DCC-MAIN-20 | PASS | Checkin/rejected replacement requires fresh available tickets and cleanup lost-claim/bound-ticket races fail explicitly instead of reporting false success. |

## Verification Evidence
- Backend static contracts: `Push-Location IntRuoyiBackend; node --test yudao-module-dcc\src\test\js\dcc-*.cjs` -> 21 PASS, 0 failed.
- Frontend direct 20-item contracts: `Push-Location IntRuoyiFronted; node --test <14 focused files>` -> 42 PASS, 0 failed.
- Backend targeted regression: `Push-Location IntRuoyiBackend; mvn.cmd -pl yudao-module-dcc -am "-Dtest=<27 selected classes>" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 555 tests, 0 failures, 0 errors, 0 skipped.
- Frontend type/build: `pnpm ts:check` -> PASS; `pnpm build:local` -> PASS.
- Hygiene: `git diff --check` -> PASS; only Git CRLF conversion warnings were emitted for touched test files.

## Code Adjustments Made During Recheck
- `IntRuoyiFronted/src/views/dcc/controlled-file/browser/checkin-main-flow.spec.cjs`: refreshed the local test sandbox to match current checkin state (`versionChangeType`, cleanup loading and source-upload readiness). No production frontend code change.
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupTransactionIntegrationTest.java`: isolated the intentional failure-injection mapper to the rollback tests and updated the idempotent approval-event assertion from stale `READY_TO_PUBLISH` to current direct `ACTIVE` behavior. No production backend code change.
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationNotificationTransactionIntegrationTest.java`: current test fixture includes the historical linked file row and mapper injection needed to assert linked revision version display from current source. No production backend code change.

## Boundaries
- No Playwright real-page E2E was executed in this turn. The result is code/static/local-regression closure, not browser-path acceptance against a live runtime.
- No database write, service restart, deployment, or remote-server operation was performed.
- The broad DCC script inventory still contains stale/unrelated historical tests; those failures are documented in the execution log and were not used as DCC-MAIN-01..20 gate evidence.
## Closeout
- Implementation/reverification commit: e2ed5fcab.
- Cleanup: direct review only; no temporary evidence files were present, and no cleanup script executable exists in this checkout/user profile.
- Final status: completed pending closeout commit push verification.
