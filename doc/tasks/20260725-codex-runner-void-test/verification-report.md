# Verification Report

## Result

- Status: `BLOCKED`
- Completed: 本机 Codex Runner 已按当前电脑环境配置完成，前后端 Runner 协议、租户头、审计字段、后端 jar 和本机重启均已验证。
- Blocked: 当前系统没有“作废测试”测试项，无法按真实页面行级“执行”运行该项。

## Evidence

- `codex.cmd --version` -> `codex-cli 0.145.0`
- Frontend local entry: `http://127.0.0.1:8081/login?redirect=/index` -> HTTP 200
- Backend health: `http://127.0.0.1:48081/actuator/health` -> `UP`
- Backend RED: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> FAIL, Runner 无登录写入 `creator/updater=null`
- Backend GREEN: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerControllerTest,CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS
- Frontend static contract: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS
- Backend package: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS
- Runner register probe: POST `/admin-api/system/codex-test-runner/register` with task token and `tenant-id=1` -> `code=0`, `runnerSessionId=1`
- Runner loop: `start-codex-runner-loop.ps1` -> background process `PID=51372`
- Runner online DB check: `system_codex_test_runner_session.runner_name=local-codex-runner-20260725`, `status=ONLINE`, `heartbeat_age_seconds=9`, `tenant_id=1`
- UI target search: `run-void-test-from-ui.mjs` -> “作废测试” search total `0`; “作废” search total `0`
- Read-only current case list: `test-case-list-summary.json` -> total `1`, only `排产工单手动重排 881MO093613/881MO093615`
- Read-only DB check: `system_codex_test_case` contains no record whose name/method/test data includes “作废”

## Open Blocker

- Resolved for visible page error: the local Codex Runner is now online and should no longer trigger `没有在线 Codex Runner`.
- Remaining missing precondition: create or restore a real enabled `作废测试` test item in `系统管理 > 测试管理`, with formal test method rows and target/checkpoint rows.
- Impact: Runner can now accept executions, but it still cannot execute the originally requested `作废测试` item until that item exists.

## Next Required Input

- Provide or authorize the formal “作废测试” test item definition: 测试项名称、测试方法项、测试目标项/检查点、测试租户和是否允许并行。
