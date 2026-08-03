# Verification Report

## Summary

Pressure-pump all-process switching is implemented as an explicit role permission path. Authorized users can list enabled pressure-pump route processes without workstation/post binding; ordinary users retain the existing binding path; missing route/process configuration fails fast. The permission migration is release-gate checked.

## Commands

- `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineWorkstationPostRouteBindingSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests.
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260803_mes_frontline_pressure_pump_all_process_permission.sql --output doc\tasks\20260803-pressure-pump-role-process-switch\migration-policy-gate.json` -> PASS.

## Result

- Backend behavior: PASS.
- Permission migration policy: PASS.
- No fallback, mock success, silent downgrade, or default-empty success path introduced.
