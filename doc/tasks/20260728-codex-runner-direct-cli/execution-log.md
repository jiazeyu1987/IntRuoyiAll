# Execution Log

## 2026-07-28

- User intent: 测试管理按需启动仍报错，启动脚本因 `Frontend entry is not reachable: http://127.0.0.1:8081` 退出；用户要求改为“裸调/裸掉 Codex CLI”。
- BDD: On-demand Runner startup should not require frontend entry preflight -> Given Runner token/backend/Codex CLI prerequisites are present and frontend entry is temporarily unreachable, When backend starts the on-demand Runner script, Then the script must not fail before Codex CLI/Runner registration because of frontend entry reachability.
- RED: `node tests/e2e/codex-runner-on-demand-startup-script-static.spec.js` -> FAIL, expected reason: existing `start-codex-test-runner.ps1` still contains `Assert-HttpReachable -Url $FrontendBaseUrl -Name 'Frontend entry'`.
- Fix: removed the frontend entry HTTP reachability assertion from `IntRuoyiFronted/scripts/start-codex-test-runner.ps1`; kept Node, Codex CLI and backend health fail-fast checks.
- GREEN: `node tests/e2e/codex-runner-on-demand-startup-script-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。
- REGRESSION: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerBootstrapServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run 3, failures 0, errors 0。
- Experience: updated `docs/e2e-rules.md` Codex Runner gate so controlled on-demand starter scripts must not hard-block on frontend entry HTTP reachability before Runner registration.
- Cleanup: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-codex-runner-direct-cli --mode preview` -> READY, no delete/blocked/warnings.
- Cleanup: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-codex-runner-direct-cli --mode apply` -> APPLIED, no deleted paths.
- Git note: source/script changes for this task are present in local commit `dfc71011` alongside pre-existing dirty-worktree baseline changes; remaining task evidence is committed separately after cleanup.
- Status: completed。
