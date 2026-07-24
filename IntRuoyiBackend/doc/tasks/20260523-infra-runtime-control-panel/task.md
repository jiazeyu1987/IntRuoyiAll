# 20260523 IntRuoyi Runtime Control Panel

## Goal

Build an infra runtime control panel for the local IntRuoyi admin UI to inspect and restart IntRuoyi frontend/backend/full and Website frontend across local, test, and production environments.

## Milestones

- [x] Identify previous task state and current repository status.
- [x] Add BDD scenarios and RED tests for backend runtime-control behavior, script contracts, and frontend wiring.
- [x] Implement backend runtime-control API, fixed configuration, command whitelist, operation records, menu SQL, and tests.
- [x] Implement tracked local/remote PowerShell scripts with JSON status and component-scoped restart.
- [x] Implement frontend runtime-control page and API client.
- [x] Run targeted backend tests, script tests, frontend checks, and API verification.
- [x] Complete real-path Playwright verification for the runtime-control page.
- [x] Mark task completed and record final verification evidence.
- [x] Commit only current-task changes.

## Expected Verification

- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`
- `python -m pytest script/tests/test_runtime_control_scripts.py -q`
- frontend static contract check with `node tests/e2e/runtime-control-static.spec.js`
- frontend type check with `pnpm ts:check`
- Playwright real-user verification through `http://localhost:8081`

## Current Status

Completed. The current-source local runtime now recovers through `restart-int-ruoyi-local.ps1 -Component full`, and the live runtime-control page verifies successfully against the freshly started local backend on `48081`.

## Verification Evidence

- Backend targeted test passed: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`.
- Script contract test passed: `python -m pytest script/tests/test_runtime_control_scripts.py -q`.
- Local restart script now auto-applies `sql/mysql/20260523_dcc_nas_transfer_task.sql` when probe table `dcc_controlled_file_nas_transfer_task` is missing.
- Current-source local full restart passed: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component full`.
- Current-source local backend health passed after restart: `GET http://127.0.0.1:48081/actuator/health -> {"status":"UP"}`.
- Current-source local full status passed after startup settled: `show-int-ruoyi-local-status.ps1 -Component full -Json -> status=running`.
- Local backend JSON status passed: `show-int-ruoyi-local-status.ps1 -Component backend -Json`.
- Test server Website JSON status passed for `172.30.30.58`.
- Production Website JSON status returned degraded for `172.30.30.57`; no restart was attempted.
- Real Playwright verification passed through `http://127.0.0.1:8081/infra/monitors/runtime-control`; the matrix populated from `http://127.0.0.1:48081/admin-api/infra/runtime-control/overview`, and the production restart dialog blocked empty reason / missing `PROD` without sending a restart request.
- Verification screenshot saved to `D:\ProjectPackage\Int\IntRuoyi\output\playwright\runtime-control-panel-live.png`.
- Current backend owner after restart: `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-runtime-control-20260523-220859.jar`.

## Remaining Blockers

- No functional blocker remains for the runtime-control task itself.
- Scoped commit still depends on staging only runtime-control files and excluding unrelated dirty work in both repositories.

## Notes

- Previous completed backend task observed before this implementation: `20260523-clean-outdated-unfinished-task-dirs`.
- Production restart must require restart permission, a non-blank reason, exact `PROD` confirmation, and an operation record.
- During the final rerun, the DCC NAS transfer scheduler still logged a non-fatal runtime error because `last_failure_message` exceeded the current column length; the backend stayed up and the runtime-control verification was unaffected.
- The local restart script now also stops stale workspace watcher processes before relaunching the frontend, avoiding the transient `EMFILE: too many open files` failure that appeared when old `yudao-ui-admin-vue3` node watchers accumulated.
- Task-only backend commit: `12bff41802` (`任务: 新增运行控制台后端`).
