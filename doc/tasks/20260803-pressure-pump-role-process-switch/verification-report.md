# Verification Report

## Summary

Pressure-pump all-process switching is implemented as an explicit role permission path. Authorized users can list enabled pressure-pump route processes without workstation/post binding; ordinary users retain the existing binding path; missing route/process configuration fails fast. The permission migration is release-gate checked.

Runtime follow-up fixed the account 1 / post 14 error by switching the pressure-pump all-process check from explicit role-id permission lookup to the system-standard login-user permission lookup.

## Commands

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests.
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260803_mes_frontline_pressure_pump_all_process_permission.sql --output doc\tasks\20260803-pressure-pump-role-process-switch\migration-policy-gate.json` -> PASS.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260803-pressure-pump-role-process-switch/backend-api-evidence.md` -> PASS.
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260803-pressure-pump-role-process-switch/database-schema-evidence.md` -> PASS.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest#shouldListAllPressurePumpProcessesWhenRoleHasPressurePumpAllProcessPermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL under old explicit-role-only check, expected fallback to post/workstation binding.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests.
- Current rerun: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests.
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260803-pressure-pump-role-process-switch/bug-regression-evidence.md` -> PASS.

## Result

- Backend behavior: PASS.
- Permission migration policy: PASS.
- Backend API evidence: PASS.
- Database schema evidence: PASS.
- Runtime account 1 / post 14 regression: PASS.
- Bug regression evidence validator: PASS.
- No fallback, mock success, silent downgrade, or default-empty success path introduced.
