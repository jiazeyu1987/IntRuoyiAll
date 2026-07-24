# Task: Add Quick Restart Modes To Ops Bat

## Goal

Extend the current IntRuoyi operations launchers with quick restart modes so operators can restart the already-deployed backend and frontend containers on test or production without rebuilding or reimporting data.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this restart-mode task.
- Record BDD scenarios for restart-only behavior before changing deployment tooling.
- Add failing script tests for the restart wrapper and unified menu routing.
- Implement the minimal restart PowerShell script, test/prod restart bat wrappers, and unified-menu options.
- Verify the real restart flow on the test server and safe wrapper behavior for production.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-ops-bat-unified/task.md`
- Status before this task: completed.
- Impact: the unified launcher task is already closed, so the restart-mode task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this restart-mode task package.
- [x] M2: Record BDD and RED evidence for restart-only tooling.
- [x] M3: Implement restart-only scripts and launcher routes.
- [x] M4: Verify test restart flow and safe production wrapper behavior.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-remote.ps1 -ServerHost 172.30.30.58 -RemoteAppDir /opt/intruoyi/runtime -FrontendPort 8081 -BackendPort 48081`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-to-prod.bat cancel`

## Current Status

Completed on 2026-05-18. Restart-only scripts and launcher routes are in place, the test restart flow has been proven on the real test server, and the production restart wrapper safe cancel path has been verified.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-remote.ps1 -ServerHost 172.30.30.58 -RemoteAppDir /opt/intruoyi/runtime -FrontendPort 8081 -BackendPort 48081`
- PASS: `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-to-prod.bat cancel`
- PASS: test restart output ended with:
  - `Restart completed.`
  - `Frontend: http://172.30.30.58:8081`
  - `Backend health: http://172.30.30.58:48081/actuator/health`

## Blocker And Impact

- Blocker: none.
- Impact: none.
