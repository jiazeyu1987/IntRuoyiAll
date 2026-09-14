# Bug Regression Evidence

## Bug Summary

`没有在线 Codex Runner` recurred because the local process could stay alive while no fresh heartbeat reached the backend. The previous starter also treated any `codex-test-runner.mjs --loop` process as sufficient, which masked stale or token-invalid sessions.

## Expected Behavior

- Runner must heartbeat while idle and while executing.
- API requests must fail fast instead of hanging indefinitely.
- UI/backend must expose a diagnostic Runner status instead of only a generic offline error.
- Startup must fail on missing or invalid prerequisites rather than reporting success from process existence.
- If `/system/codex-test-runner/status` cannot be loaded, the page must fail fast and block execution instead of continuing to create a batch.

## Reproduction

- User-visible symptom: `请求地址不存在:admin-api/system/codex-test-runner/status`.
- Local route probes now show the endpoint exists on `8081/8101/48081/48101/48021/48041/48061`; unauthenticated requests return `401 账号未登录`.
- The remaining frontend defect was fail-open behavior: when `refreshRunnerStatus()` returned `undefined`, execution actions did not block before calling `startCodexTestExecution`.
- Follow-up reproduction after the user still saw the error: `8081` proxied to `48081`, but `48081` was running the runtime worktree jar from `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`, which had not yet loaded `GET /system/codex-test-runner/status`.

## Root Cause

- `postJson()` and artifact upload used fetch without timeout.
- `runOnce()` claimed tasks without first heartbeating while idle.
- The old startup script returned `already_running` based only on process existence.
- Runtime backend token was out of sync with the Runner token, causing registration retry failures while the process remained alive.
- The final `请求地址不存在` recurrence was caused by runtime jar drift: the main workspace source had the endpoint, but the actual `48081` backend process was an older runtime worktree jar without the status route.

## RED / GREEN

- RED: frontend static contract failed on missing Runner status endpoint/API/UI.
- RED: backend targeted test failed on missing `CodexTestRunnerStatusRespVO`.
- GREEN: frontend static contract, Runner syntax, PowerShell parser checks, and backend targeted Maven tests passed.
- RED: `node .\tests\e2e\system-codex-test-management-static.spec.js` -> FAIL, Runner 状态接口不可用时缺少 fail-fast 阻断。
- GREEN: `node .\tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- GREEN: `node .\tests\e2e\system-codex-test-management-real.e2e.js` -> PASS。
- RED: runtime worktree `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestRunnerControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL on missing required test fixture `project`, then H2 progress-column schema drift.
- GREEN: runtime worktree targeted Maven command above -> PASS, 6 tests.
- GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS, rebuilt the runtime jar loaded by `48081`.

## Verification

- Runner status endpoint path is currently present in every local runtime port checked; unauthenticated probes return `401 账号未登录`.
- `blockExecutionWhenRunnerStatusUnavailable(status)` blocks execution when status is unavailable or offline.
- Real test management page E2E opens successfully after the fix.
- Authenticated Playwright verification through `http://127.0.0.1:8081` opened `系统管理 > 测试管理`; `/admin-api/system/codex-test-runner/status` returned HTTP `200` and business `code=0`; `/admin-api/system/codex-test-case/page` returned business `code=0`; page did not show `系统异常`.

## Regression Scope

- Backend Runner status calculation and stale heartbeat diagnostics.
- Frontend execute-time Runner status refresh and visible status strip.
- Runner request timeout and idle heartbeat behavior.
- Stable local startup script prerequisite checks.

## Blockers

- Final commit/push/cleanup remains blocked by unrelated concurrent dirty worktree changes and ahead commits outside this task.
- Existing Runner PID `27644` remains stale/offline after the route reload; no reusable token source was present in the current shell, and the coordinated backend/Runner token restart command was blocked by local policy.
