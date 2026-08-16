# INT12 Verification Report

## Result

completed

## Passed Checks

- Backend focused Maven: PASS, 33 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS, 2026-08-14T12:07:31+08:00.
- Runtime static contract: PASS, node tests/e2e/mes-frontline-pqc-qa-process-runtime-static.spec.cjs.
- Formal submit static contract: PASS, node tests/e2e/frontline-pqc-formal-submit-static.spec.js.
- TypeScript: PASS, pnpm ts:check, exit 0.
- Root handoff recheck: PASS, backend focused Maven rerun 2026-08-14T12:52:53+08:00, 33 tests, 0 failures, 0 errors, 0 skipped.
- Root handoff recheck: PASS, runtime static contract, formal submit static contract, pnpm ts:check, backend/frontend/bug evidence validators, git diff --check, and scoped formal-contract forbidden scans.
- Post-merge contract repair: PASS, removed the unused `workOrderId + routeId` runtime process query and its product/project-code inference instead of adapting tests to the forbidden path.
- Post-merge focused service test: PASS, 4 tests, 0 failures/errors/skips, BUILD SUCCESS, 2026-08-14T16:32:16+08:00.
- Post-merge frozen regression: PASS, 33 tests, 0 failures/errors/skips, BUILD SUCCESS, 2026-08-14T16:43:14+08:00.
- Post-restart frontend verification: PASS, both static contracts and `pnpm ts:check`.
- 2026-08-15 dependency gate: PASS, `pnpm install --frozen-lockfile` exit 0; lockfile current and dependencies already up to date.
- 2026-08-15 frontend static closeout: PASS, `pnpm ts:check`, runtime static contract, and formal submit static contract each exited 0 in the required order.
- 2026-08-15 evidence validation: PASS, frontend-feature/backend-api/bug-regression validators and all three validator self-tests exited 0.
- 2026-08-15 diff check: PASS, `git diff --check` exited 0 with existing LF-to-CRLF advisories only.
- 2026-08-15 formal response contract repair: PASS, the exact RED exposed all eight process-level task fields and the focused GREEN passed 5/5 after their removal.
- 2026-08-15 expanded backend regression: PASS, 44 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS at 2026-08-15T15:39:40+08:00.
- 2026-08-15 frontend dependency and static recheck: PASS, dependency state ready; `pnpm ts:check` and both required static-contract tests exited 0 in order.

## Resolved Check

- The earlier formal-contract forbidden scan found 16 backend violations from eight duplicated process-level task fields and their mapping writes. Strict TDD removed the fields and all writers; focused GREEN passed 5 tests and expanded regression passed 44 tests.
- Final evidence validators: PASS, frontend-feature/backend-api/bug-regression validators and all three self-tests exited 0.
- Final `git diff --check`: PASS, exit 0; only existing LF-to-CRLF working-copy advisories were emitted.
- Final precise formal-contract forbidden scan: PASS, exit 0, 0 violations across backend outer/nested response shape, process-level writers, controller compatibility mapper, frontend outer/nested DTO shape, activeOrderId-only API request, and frontend process-level reads.

## Contract Notes

- DF10/DF11 formal contracts are authoritative.
- Conflicting old patches are not applied automatically; any prior old changes must stay as patch backup only until explicitly reviewed against the formal contract.
- No fallback, current-QA inference, product/material inference, route-process existence validation, or compatibility submit path was intentionally introduced.

## Verification Exception

- The user explicitly instructed `不用测试，继续推进` on 2026-08-15. Real Playwright write-path E2E was not run and is not marked PASS.
- Residual risk: signed submission, tenant permissions, active-order/PQC task data, and separate AM/PM behavior have not been exercised through a confirmed real browser write path in this closeout.
- No API-only, mock, fallback, or default-success result was used as a substitute.

## Final Closeout

- Cleanup preview/apply: PASS, zero paths deleted; task records, formal evidence, formal tests, production code, and prerequisite overlap backup retained.
- Task-owned temporary worktree cleanup: PASS, exact scope/hash and patch-equivalence checks completed before removing `D:/IntRuoyiWorktree/int12-formal-response-contract-fix` and its temporary branch; unrelated worktrees were not modified.
- Local integration: PASS, prerequisite `254bb6181` and response-contract implementation `389c7bf9e` are on `int_main`.
- Final result: completed with the explicitly recorded user waiver for real Playwright write-path verification; no new test was run after that instruction.
