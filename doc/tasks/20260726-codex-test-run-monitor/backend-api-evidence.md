# Backend API Evidence

## Scope

- Runner progress reporting endpoint.
- Admin monitor query endpoint.
- Execution detail VO progress fields.

## API Contract

- `POST /system/codex-test-runner/progress`
  - Header: `X-Codex-Runner-Token` and `tenant-id`.
  - Body: `executionCaseId`, `phase`, `currentMethodSort`, `currentCheckpointSort`, `progressMessage`.
  - Valid phases: `METHOD`, `CHECKPOINT`, `DONE`.
- `GET /system/codex-test-execution/monitor`
  - Permission: `system:codex-test:query`.
  - Returns unfinished executions with cases and checkpoint results.

## Validation / Error Behavior

- Invalid Runner token fails through existing token validation.
- Invalid phase fails with `CODEX_TEST_RESULT_SCHEMA_INVALID`.
- METHOD without `currentMethodSort` fails.
- CHECKPOINT without `currentCheckpointSort` fails.
- Non-running execution case cannot accept progress updates.

## BDD Scenarios

- BDD: Runner progress -> Given a claimed execution case / When Runner reports METHOD or CHECKPOINT progress / Then backend persists phase and current item index.
- BDD: Monitor query -> Given unfinished executions exist / When admin queries monitor / Then backend returns execution details with cases and checkpoint results.

## RED / GREEN

- RED: `mvn -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest#reportProgress_updatesRunningCaseAndMonitorDetailFields" "-Dsurefire.failIfNoSpecifiedTests=false" test` failed before progress VO/service contract existed.
- GREEN: `mvn -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed, 7 tests.

## Verification

- Verification: Runner progress service test passed.
- Verification: execution monitor query service test passed.
- Verification: combined focused backend suite passed.

## Observability Touchpoints

- Runner writes `progress_message` and status transitions to `system_codex_test_execution_case`.
- Existing checkpoint results and failure screenshots remain unchanged.

## Blockers

- Blockers: none for backend contract implementation.