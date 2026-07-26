# Backend API Evidence

## Scope

- Endpoint path remains `POST /system/codex-test-execution/start`.
- Service boundary changed from direct stale heartbeat validation in `CodexTestExecutionServiceImpl` to `CodexTestRunnerBootstrapService.ensureRunnerAvailable()`.
- New service starts only the controlled PowerShell wrapper script and waits for a real online Runner session with `playwright` and `codex` capabilities before creating execution records.

## API Contract

- Success: existing request body `targetTenantId + executionMode + caseIds` is unchanged.
- Failure: missing token, missing startup script, non-ps1 starter, starter process failure, timeout, missing capability, or disabled on-demand startup all fail fast through backend error codes.
- No fallback: no mock success, no silent sequential downgrade, no process-existence-only online proof.

## Data Contract

- Existing Runner session table remains the source of truth for availability.
- Existing execution, execution case, and checkpoint result snapshots are unchanged.
- Startup confirmation requires fresh `system_codex_test_runner_session.last_heartbeat_time` within timeout and required capabilities.

## Auth And Validation

- Existing `system:codex-test:execute` permission remains on the start endpoint.
- Existing target tenant validation remains before Runner startup.
- Runner token remains backend config and is passed only as child process environment variable `CODEX_TEST_RUNNER_TOKEN`.

## BDD

- BDD: 按需执行可拉起 Runner -> Given 测试管理存在可执行测试项且本机配置了 Runner 启动器 When 用户点击该测试项执行 Then 后端创建执行任务并触发本机 Runner 注册领取任务 And 前端不再因为没有常驻 Runner 直接失败。
- BDD: 缺少 Runner 启动前置时 fail-fast -> Given 本机未配置 Runner 启动脚本或 Codex CLI When 用户点击执行 Then 后端拒绝启动并返回明确缺失前置 And 前端展示该原因。

## RED:

- `mvn -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest,CodexTestRunnerBootstrapServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, missing `CodexTestRunnerBootstrapService`, `CodexTestRunnerBootstrapServiceImpl`, `CODEX_TEST_RUNNER_STARTER_MISSING`, and `CODEX_TEST_RUNNER_START_FAILED`.

## GREEN:

- `mvn -pl yudao-module-system -am "-Dtest=CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest,CodexTestRunnerBootstrapServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests, 0 failures.

## Verification

- Maven targeted suite verifies backend behavior.
- Runner startup is confirmed by real heartbeat/capability records, not process existence.

## Observability

- Runner registration and heartbeat remain observable through the existing Runner status endpoint and monitor list.
- User-facing UI no longer exposes raw heartbeat wording on the test management page; heartbeat remains a backend diagnostic signal.

## Blockers

- Live runtime was not restarted in this step; running `48081` must be rebuilt/restarted before the source change appears in the browser.
