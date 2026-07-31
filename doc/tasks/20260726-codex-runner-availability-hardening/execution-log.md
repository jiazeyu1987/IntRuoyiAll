# Execution Log

## User Intent

- 用户指出执行测试时反复出现 `没有在线 Codex Runner`，希望采用长期方案而不是补丁式修复。
- 用户追加反馈：`请求地址不存在:admin-api/system/codex-test-runner/status`。

## BDD / TDD

- BDD: Runner 在线执行 -> Given 本机存在已注册且心跳未过期的 Codex Runner / When 用户点击测试项执行 / Then 后端创建执行批次且不会提示没有在线 Runner。
- BDD: Runner 离线可诊断 -> Given 没有在线 Runner / When 用户点击测试项执行 / Then 页面展示 Runner 离线原因、最近心跳、启动指引或自动启动状态，不只给出笼统错误。
- BDD: Runner 前置条件缺失 -> Given Codex CLI、Runner token、Node、前端入口或后端入口缺失 / When 触发 Runner 启动或探测 / Then 系统 fail fast 并展示具体缺失项，不创建伪成功执行。
- BDD: Runner 心跳过期 -> Given Runner 记录存在但 heartbeat 超时 / When 用户执行测试 / Then 后端将其视为离线并返回可诊断状态。

## Command Log

- CREATED: task docs for `20260726-codex-runner-availability-hardening`.
- GREEN: experience-preflight -> PASS, applicable gates copied from `docs/e2e-rules.md#Codex Runner 自动测试门禁` and `docs/local-runtime.md`.

## Blockers / Limits

- Pending: inspect current Runner lifecycle and reproduce the recurring offline error.

## 2026-07-26 Runner Availability Hardening Evidence

- RED: `node tests\e2e\system-codex-test-management-static.spec.js` -> FAIL, missing `/system/codex-test-runner/status` frontend/API contract.
- RED: `mvn -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, missing `CodexTestRunnerStatusRespVO`.
- IMPLEMENTED: backend Runner status contract with online count, stale count, latest heartbeat age, required capability check, and diagnostic message.
- IMPLEMENTED: frontend Runner status strip and execute-time status refresh; if status endpoint is unavailable, execution fail-fast blocks before calling the start endpoint.
- IMPLEMENTED: `codex-test-runner.mjs` API request timeout via `AbortController` and idle heartbeat before task claim.
- IMPLEMENTED: stable local runner startup script `IntRuoyiFronted/scripts/start-codex-test-runner.ps1`; it fails fast on missing Node/Codex/token/backend/frontend and no longer treats process existence as online.
- GREEN: PowerShell parser checks for runner startup and backend-token restart scripts -> PASS.
- GREEN: `node --check scripts\codex-test-runner.mjs` -> PASS.
- GREEN: `node tests\e2e\system-codex-test-management-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests.
- RUNTIME: old Runner process `39240` was replaced by PID `27644` using the stable startup script.
- RUNTIME: backend `48081` was restarted from confirmed jar `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` with the current Runner token; new backend PID `53560`, health `UP`.
- RUNTIME: DB session `id=8`, runner `local-codex-runner-availability-hardening`, status `ONLINE`, heartbeat age `2` seconds immediately after restart and `3` seconds after waiting 25 seconds.
- BLOCKER: current DB has no test case named or containing `作废测试`; decoded current enabled case names are batch-record and schedule cases only, so no `作废测试` execution was started.
- BLOCKER: final task commit/push/cleanup cannot be completed safely in this turn because the main worktree is ahead of origin and contains unrelated concurrent dirty changes outside this task.
- EXPERIENCE: updated `docs/e2e-rules.md#Codex Runner 自动测试门禁` to require idle heartbeat verification and forbid process-existence-only Runner online checks.

## 2026-07-26 status endpoint missing follow-up

- RED: `node .\tests\e2e\system-codex-test-management-static.spec.js` -> FAIL, Runner 状态接口失败返回空时执行动作仍可能继续调用 `startCodexTestExecution`。
- RUNTIME: unauthenticated probes to `8081/8101/48081/48101/48021/48041/48061` for `/admin-api/system/codex-test-runner/status` all returned `401 账号未登录`, not `请求地址不存在`; current local route handler is loaded.
- IMPLEMENTED: `blockExecutionWhenRunnerStatusUnavailable(status)` blocks both status API failure and offline Runner status before execution starts.
- GREEN: `node .\tests\e2e\system-codex-test-management-static.spec.js` -> PASS.
- GREEN: `node .\tests\e2e\system-codex-test-management-real.e2e.js` -> PASS.
- GREEN: BOM check for `src/views/system/codex-test-management/index.vue` -> PASS.
- SIDE EFFECT HANDLED: real E2E updated the old admin E2E summary timestamp; timestamp diff was restored and not retained as task output.

## 2026-07-26 runtime jar status route reload

- RED: authenticated user report still showed `请求地址不存在:admin-api/system/codex-test-runner/status` because local frontend `8081` proxied to backend `48081`, whose Java process was running `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`; that runtime worktree did not yet expose `GET /system/codex-test-runner/status`.
- IMPLEMENTED: patched the runtime worktree status contract in `CodexTestRunnerController`, `CodexTestRunnerService`, `CodexTestRunnerServiceImpl`, `CodexTestRunnerSessionMapper`, `CodexTestRunnerStatusRespVO`, controller test, service test, and aligned the H2 test schema with existing `project` and progress columns.
- RED: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestRunnerControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL first on missing test case `project`, then on H2 schema drift for progress columns.
- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestRunnerControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests.
- GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS; rebuilt `D:\IntRuoyiWorktree\codex-test-run-monitor-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` at `2026-07-26 16:11:39`.
- RUNTIME: stopped confirmed old backend PID `53560` on `48081` and started PID `18212` from the rebuilt runtime jar; `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`.
- GREEN: Playwright real frontend login to `http://127.0.0.1:8081` opened `系统管理 > 测试管理`; `/admin-api/system/codex-test-runner/status` returned HTTP `200`, business `code=0`, and `/admin-api/system/codex-test-case/page` returned business `code=0`; page did not show `系统异常`.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260726-codex-runner-availability-hardening\bug-regression-evidence.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence E:\IntRuoyi\doc\tasks\20260726-codex-runner-availability-hardening\backend-api-evidence.md` -> PASS.
- EXPERIENCE: merged runtime jar drift lesson into `docs/local-runtime.md#2026-07-24 隔离构建 Jar 加载门禁`; future `请求地址不存在:<接口>` checks must confirm the actual `48081` PID/Jar source and rebuild the runtime worktree jar when that is what the port is loading.
- BLOCKER: existing Runner process PID `27644` is still stale/offline; no reusable token source was available in the current shell, and the attempted coordinated backend/Runner token restart was blocked by local command policy, so Runner online heartbeat was not restored in this pass.
