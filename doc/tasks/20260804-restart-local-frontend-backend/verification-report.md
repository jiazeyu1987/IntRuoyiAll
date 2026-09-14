# Verification Report

## Result

runtime_start_passed

## Checks

- Regression guard: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest#routeFlowProcessQueryMethods_shouldNotBeResourceInjectionMethods" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 1, Failures: 0, Errors: 0`.
- Standard startup: `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full` -> dispatched; Maven package reached `BUILD SUCCESS`.
- Backend health: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health -TimeoutSec 10` -> PASS, `status=UP`.
- Frontend HTTP: `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/ -TimeoutSec 10` -> PASS, HTTP `200`.
- Runtime ports: `48081` listener PID `31572`; `8081` listener PID `17816`.
- Local status: `show-int-ruoyi-local-status.ps1` -> `Status: running`, `HTTP: frontend=HTTP 200; backend=HTTP 200`, `Runtime: frontend=listening; backend=listening`.
- Runtime Jar: `output\runtime\int_main\backend-runtime-control-20260804-160504.jar`.
- Bug evidence validator: `validate_bug_regression.py --evidence doc\tasks\20260804-restart-local-frontend-backend\bug-regression-evidence.md` -> PASS.
- Documentation diff check: `git diff --check -- doc\tasks\20260804-restart-local-frontend-backend\...` -> PASS.

## Runtime Evidence

- Backend log: `output\runtime\int_main\logs\yudao-server.log` records `Tomcat started on port 48081` and `Started YudaoServerApplication in 125.699 seconds`.
- Frontend log: `output\runtime\int_main\frontend-runtime-control-20260804-160512.out.log` records `VITE v5.1.4 ready` and local entry `http://localhost:8081/`.

## Closeout Boundary

The local runtime request is satisfied. Repository closeout commit/push was not attempted because the workspace already has substantial parallel dirty changes and `int_main...origin/int_main [ahead 9]`; staging this task now would risk mixing unrelated work.
