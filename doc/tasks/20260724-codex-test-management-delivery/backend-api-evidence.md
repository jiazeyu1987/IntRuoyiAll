# Backend API Evidence

## Scope

Implemented system-module backend for Codex test management: test case CRUD, execution orchestration, Runner registration/claim/heartbeat/checkpoint completion, and temporary artifact access.

## API Contract

- User APIs: `/system/codex-test-case/*` and `/system/codex-test-execution/*`.
- Runner APIs: `/system/codex-test-runner/register`, `/claim`, `/heartbeat`, `/checkpoint-result`, `/artifact`, `/complete-case`.
- User APIs use `system:codex-test:*` permissions.
- Runner APIs require `X-Codex-Runner-Token`; missing or mismatched token fails fast.

## Validation And Error Behavior

- Rejects empty natural-language methods and empty checkpoints.
- Rejects disabled cases, offline Runner, invalid target tenant, unsafe parallel execution, and malformed Runner results.
- `FAIL` checkpoint result requires mismatch description.
- Batch status rolls up from case/checkpoint results; Runner cannot report PASS if any checkpoint failed.

## BDD

- BDD: 测试项维护 -> Given 测试管理员填写自然语言方法和检查点 / When 保存测试项 / Then 后端持久化方法、用户手写数据和任意检查点。
- BDD: 并行安全拒绝 -> Given 测试项 `parallelSafe=false` / When 请求并行执行 / Then 后端拒绝且不降级为顺序执行。
- BDD: Runner 失败回写 -> Given Runner 回写失败检查点 / When 提供差异描述 / Then 执行项和批次汇总为失败。

## Verification

- RED: Maven test compile failed because `CodexTestCaseService`, `CodexTestExecutionService`, `CodexTestRunnerService` and impl classes were missing.
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest,CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed with 6 tests.
- Evidence files: `IntRuoyiBackend/yudao-module-system/target/surefire-reports/*CodexTest*.txt`.

## Observability

- Execution, execution case, checkpoint result, runner session, and artifact records store explicit statuses and timestamps.
- Failure evidence stores mismatch description and artifact ID, not server absolute paths.

## Blockers

- Real Runner integration requires configured Runner token, Codex CLI, Playwright browser, and target tenant credential mapping.
