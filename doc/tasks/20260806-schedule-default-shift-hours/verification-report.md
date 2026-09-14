# Verification Report

## Summary

- Backend default shift-hours behavior is verified for auto schedule refresh and schedule order creation.
- Frontend scheduler workbench settings default and validation are verified by static contracts.
- Single-module Maven without `-am` failed due stale local reactor dependencies; final backend verification used project-approved `-am`.

## Commands

- RED: `node IntRuoyiFronted/tests/e2e/scheduler-workbench-shift-hours-default-static.spec.cjs` -> FAIL, missing default constant.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing,MesProScheduleOrderAdmissionTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderServiceImplTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderNoDefaultConfigContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, old backend behavior still raised missing shift-hours errors.
- GREEN: `node IntRuoyiFronted/tests/e2e/scheduler-workbench-shift-hours-default-static.spec.cjs` -> PASS.
- GREEN: `node IntRuoyiFronted/tests/e2e/mes-scheduler-workbench-shift-hours-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#refreshScheduleOrderProcessesFromRouteConfig_shouldUseDefaultShiftHoursWhenWorkbenchShiftHoursMissing,MesProScheduleOrderAdmissionTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderServiceImplTest#createFromWorkOrder_shouldUseDefaultShiftHoursWhenWorkstationShiftHoursMissing,MesProScheduleOrderNoDefaultConfigContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests, 0 failures, 0 errors.

## Evidence Validators

- `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java IntRuoyiBackend/yudao-module-mes/src/test/java IntRuoyiFronted/src IntRuoyiFronted/tests/e2e doc/tasks/20260806-schedule-default-shift-hours` -> PASS; only LF/CRLF warnings.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260806-schedule-default-shift-hours/backend-api-evidence.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-schedule-default-shift-hours/frontend-feature-evidence.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-schedule-default-shift-hours/bug-regression-evidence.md` -> PASS.
- Experience consolidation check: `rg -n "Maven 单模块陈旧依赖|stale local reactor dependencies|不带 -am 测试前失败" docs\experience-index.md docs\powershell-memory.md` -> PASS.
- Cleanup preview/apply: task-closeout-cleanup kept `task.md`, `execution-log.md`, `verification-report.md`; deleted only 3 temporary evidence files; no blockers/warnings.

## Remaining Checks

- None for this task. Implementation commit `84565e2ae` was pushed to `origin/int_main`; this completed-status record is part of the final closeout commit.
