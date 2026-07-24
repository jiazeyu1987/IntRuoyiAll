# Task: Add Status Modes To Ops Bat

## Goal

Extend the unified IntRuoyi operations launchers with test and production status modes so operators can inspect remote runtime state without triggering publish or restart behavior.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this status-mode task.
- Record BDD scenarios for status-only behavior before changing deployment tooling.
- Add failing script tests for the new status script, wrappers, and unified launcher routes.
- Implement the minimal remote-status PowerShell script and test/prod status bat wrappers.
- Verify real status queries against both the test and production servers.

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-ops-bat-restart-modes/task.md`
- Status before this task: completed.
- Impact: the restart-mode task is already closed, so the status-mode task can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this status-mode task package.
- [x] M2: Record BDD and RED evidence for status-only tooling.
- [x] M3: Implement status scripts and launcher routes.
- [x] M4: Verify real test and production status queries.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.58 -RemoteAppDir /opt/intruoyi/runtime -FrontendPort 8081 -BackendPort 48081`
- `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.57 -RemoteAppDir /opt/intruoyi/runtime -FrontendPort 8081 -BackendPort 48081`

## Current Status

Completed on 2026-05-19. Read-only status scripts and launcher routes are in place, and both the test and production runtime queries have been verified against the live servers.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.58 -RemoteAppDir /opt/intruoyi/runtime -FrontendPort 8081 -BackendPort 48081`
- PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.57 -RemoteAppDir /opt/intruoyi/runtime -FrontendPort 8081 -BackendPort 48081`
- PASS: test status reported runtime present plus HTTP `200 / 200`
- PASS: production status reported runtime present plus HTTP `200 / 200`

## Blocker And Impact

- Blocker: none.
- Impact: none.
