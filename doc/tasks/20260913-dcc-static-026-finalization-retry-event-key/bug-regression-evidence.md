# Bug Regression Evidence

## Bug Summary

DCC-STATIC-026 covered finalization retry after a failed retry. `retryStamp` reused `dcc-finalization-retry:<id>` for every retry, so the unified controlled-content lifecycle core treated the second retry as a replay of the first `RETRY_FINALIZATION` event and did not move the candidate back to `FINALIZING`.

## Expected Behavior

Each failed finalization retry attempt must keep a deterministic idempotency key for the same attempt, but the next legal retry after that attempt fails must use a new attempt number. The sequence must allow `FINALIZATION_FAILED -> FINALIZING -> FINALIZATION_FAILED -> FINALIZING -> ACTIVE`.

## Reproduction Command Or Path

- Static RED: `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-026-finalization-retry-event-key-static.spec.cjs`
- Targeted Maven: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileFinalizationServiceImplTest#retryStamp_failedRetryThenSecondRetryUsesNewAttemptEventKey" test`

## Root Cause

The retry event key was file-scoped instead of attempt-scoped. After the first retry inserted a `RETRY_FINALIZATION` audit and then failed, the next retry reused the same key; `transitionVersionRefByDomainEvent` saw the existing audit and returned without changing unified status.

## Regression Test Added Or Updated

- Added static contract `IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-026-finalization-retry-event-key-static.spec.cjs`.
- Added unit regression method `retryStamp_failedRetryThenSecondRetryUsesNewAttemptEventKey` in `DccControlledFileFinalizationServiceImplTest`.
- Added adapter unit coverage `nextFinalizationRetryEventKey_shouldUseNextRetryTransitionCount` in `DccControlledContentAdapterTest`.

## RED

- RED: `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-026-finalization-retry-event-key-static.spec.cjs` -> FAIL before the fix, expected reason: `retryStamp must not reuse one file-scoped key for every retry attempt`.

## GREEN

- GREEN: `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-026-finalization-retry-event-key-static.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileFinalizationServiceImplTest#retryStamp_failedRetryThenSecondRetryUsesNewAttemptEventKey" test` -> PASS, 1 test run.
- GREEN: `mvn -pl yudao-module-dcc -am "-DskipTests" compile` -> PASS.
- GREEN: `git diff --check -- <task-owned files>` -> PASS with line-ending warnings only.

## Verification

Scoped verification passed through the DCC-STATIC-026 static contract, targeted unit regression, and main-code compile. The static contract covers removal of the fixed file-scoped retry key, adapter-based attempt-key generation, committed audit-count source, and regression assertions for attempt-1 and attempt-2 keys.

## Fix Summary

`retryStamp` now asks `DccControlledContentAdapter` for an attempt-scoped event key. The adapter counts already committed `RETRY_FINALIZATION` audits for the controlled-content version ref and returns `dcc-finalization-retry:<fileId>:attempt-<count+1>`. The failure path still receives the same event key for that attempt, so failure recording remains idempotent; once that attempt fails and its retry audit is committed, the next legal retry gets the next attempt number.

## Risk And Regression Scope

The change is limited to DCC controlled-file finalization retry event-key generation and a read-only controlled-content audit count. No E2E, service restart, database write, or remote server operation was performed.

## Blockers

- None for scoped implementation and targeted verification.
