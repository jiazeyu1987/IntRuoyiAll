# Test Report

## Status

ready_for_closeout

## Passed Verification

- TP-F1/TP-F2/TP-F3/TP-F4/TP-F7/TP-F8 combined JUnit: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPool*Test,MesProFrontlineFeedback*Test,FrontlineTemplate*Test,ProductionTemplateContractTest,PqcSimpleTemplateContractTest,MesFrontline*Test,ProcessPoolTimeline*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 57 tests.
- Backend compile: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS.
- SQL contract: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS, 3 tests.
- Frontend typecheck: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- Frontline template static contracts: `node IntRuoyiFronted\src\views\mes\pro\feedback\frontline-template-render.spec.cjs` and `node IntRuoyiFronted\src\views\mes\pro\feedback\frontline-template-switch.spec.cjs` -> PASS.
- Process-pool timeline static contracts: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-frontend-static.spec.cjs` and `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
- Merge guards: `git diff --check` and `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS.

## Blocked Or Not Claimed

- Real Playwright E2E was not run. Missing confirmed prerequisites: test tenant/account with menu permissions, device-account route/process/employee bindings, usable electronic signature test identity, production work order/process-pool seed data, and explicit runtime startup scope for this integrated flow.
- Impact: the code-level, SQL, typecheck, and static contracts are verified; real page-path evidence for combined submit, employee switching, PQC submit, FIFO consumption, and timeline inspection is not claimed.
