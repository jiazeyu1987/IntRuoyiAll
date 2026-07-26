# Backend API Evidence

## Scope

- `GET /system/codex-test-runner/status`
- `CodexTestRunnerService#getRunnerStatus()`

## Contract

- Returns `online`, `status`, `onlineCount`, `staleRunnerCount`, `currentRunningCount`, `requiredCapabilitiesPresent`, latest runner metadata, heartbeat age, timeout threshold, and a user-readable diagnostic message.
- Does not auto-start Runner or fake online status.
- Start execution remains protected by the existing backend online Runner validation.

## BDD

- BDD: Runner 状态接口可访问 -> Given 用户已通过真实前端登录 / When 打开 `系统管理 > 测试管理` / Then `/admin-api/system/codex-test-runner/status` 返回 HTTP `200` 和业务 `code=0`，不再返回 `请求地址不存在`。
- BDD: Runner 心跳过期可诊断 -> Given Runner 进程存在但 heartbeat 超过后端阈值 / When 页面加载 Runner 状态 / Then 后端返回过期诊断信息，不把离线伪装成在线。

## Verification

- RED: missing status VO caused targeted Maven compile failure.
- GREEN: targeted Maven test suite passed with 8 tests covering runner status and existing execution behavior.
- RED: latest runtime worktree verification failed before schema alignment because status-related tests exercised code paths whose H2 schema lacked the existing `project` and progress columns.
- GREEN: runtime worktree `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestRunnerControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` passed, 6 tests.
- GREEN: runtime worktree `mvn.cmd -pl yudao-server -am "-DskipTests" package` rebuilt the jar used by local backend `48081`.
- GREEN: authenticated Playwright verification through `http://127.0.0.1:8081` received HTTP `200` and business `code=0` from `/admin-api/system/codex-test-runner/status`.

## Validation

- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence E:\IntRuoyi\doc\tasks\20260726-codex-runner-availability-hardening\backend-api-evidence.md`

## Operational Notes

- Endpoint requires backend jar containing the current source changes.
- Existing old runtime jar can still execute once Runner token and heartbeat are fixed, but it cannot serve the new status endpoint until rebuilt/reloaded.
- Current `48081` backend PID `18212` is running the rebuilt runtime jar and health is `UP`.
- Current Runner process PID `27644` still exists but the status endpoint reports stale heartbeat; this is a Runner availability follow-up, not a missing route.

## Blockers

- Runner online heartbeat was not restored in this pass because no reusable token source was available in the current shell and the coordinated backend/Runner token restart command was blocked by local policy.
- Final commit/push/cleanup remains blocked by unrelated concurrent dirty worktree changes and ahead commits outside this task.
