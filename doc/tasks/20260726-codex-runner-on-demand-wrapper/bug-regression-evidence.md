# Bug Regression Evidence

## Bug Summary

The execution button repeatedly showed `没有在线 Codex Runner` because the frontend blocked execution when the status endpoint reported offline/stale, and the backend start path required an existing online heartbeat before creating the execution.

## Expected Behavior

Clicking execute should create a test execution through the backend. If no Runner is currently online, the backend should start the controlled local Runner wrapper, wait for real registration, and then proceed. Missing prerequisites must fail fast with a specific reason.

## Reproduction

- Old baseline contains `blockExecutionWhenRunnerStatusUnavailable` and `runnerLastHeartbeatText`, proving execution was coupled to constant Runner online status and exposed heartbeat diagnostics.

## Root Cause

- Frontend coupled execute permission to a preflight status check.
- Backend `CodexTestExecutionServiceImpl#startExecution` called `validateRunnerOnline()` before creating execution records.
- There was no backend-owned, testable lifecycle boundary for starting the local Runner wrapper.

## Fix

- Added `CodexTestRunnerBootstrapService` and `CodexTestRunnerBootstrapServiceImpl`.
- Backend start execution now calls `ensureRunnerAvailable()` before execution snapshot creation.
- Wrapper startup is restricted to configured `.ps1` script and waits for real heartbeat/capability registration.
- Frontend execution now calls `/system/codex-test-execution/start` directly and switches to monitor on success.

## Regression Tests

- Backend: Runner bootstrap missing-script, existing-online-runner, and delayed-registration cases.
- Backend: execution start uses bootstrap service and surfaces bootstrap failure.
- Frontend: static contract rejects heartbeat/online-blocking UI and requires backend on-demand path.

## RED:

- Backend RED: compile failure for missing bootstrap service and error codes.
- Frontend RED: old baseline contains `blockExecutionWhenRunnerStatusUnavailable` and `runnerLastHeartbeatText`.

## GREEN:

- Backend GREEN: Maven targeted suite PASS, 11 tests.
- Frontend GREEN: static contract PASS.

## Verification

- Backend Maven targeted suite PASS.
- Frontend static contract PASS.

## Blockers

- Full frontend type-check is blocked by unrelated `RouteEditPage.vue` symbol error.
- Live runtime has not been restarted.

## Risk

- The local backend process must run with `CODEX_TEST_RUNNER_TOKEN` configured; otherwise startup fails fast with token error.
- If frontend or backend entries are down, `start-codex-test-runner.ps1` fails fast instead of pretending Runner is available.
