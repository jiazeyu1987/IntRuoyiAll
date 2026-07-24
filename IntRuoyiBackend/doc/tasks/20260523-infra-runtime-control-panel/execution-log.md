# Execution Log

BDD: Runtime overview shows fixed targets -> Given the operator opens the runtime control panel, When the overview API is loaded, Then Local/Test/Production and IntRuoyi frontend/backend/full plus Website frontend statuses are returned with URL, port, runtime state, HTTP state, and last operation fields.

BDD: Non-production restart is whitelisted and audited -> Given an operator restarts a test environment component, When the restart API receives a known environment and component, Then only the configured whitelisted script command is dispatched and a JSON operation record is persisted.

BDD: Production restart is guarded -> Given an operator restarts a production component, When the reason is blank or the confirmation text is not exactly `PROD`, Then the API rejects the request and no command is dispatched.

BDD: Runtime scripts expose JSON component contracts -> Given the backend calls tracked runtime-control scripts, When local or remote status/restart scripts are invoked with component arguments, Then the scripts fail fast on missing prerequisites and output structured JSON for backend parsing.

BDD: Frontend panel survives reconnect polling -> Given the local frontend or backend restarts during operation, When the page loses API connectivity, Then it keeps the current state visible and retries polling until the runtime returns.

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> FAIL, expected missing runtime-control VO/config/service classes.

RED: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> FAIL, expected missing local runtime-control scripts and missing remote JSON/website component contract.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS

GREEN: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\\deploy\\show-int-ruoyi-local-status.ps1 -Component backend -Json` -> PASS

REGRESSION: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\\deploy\\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.58 -RemoteAppDir /opt/intruoyi/runtime -Component website -Json` -> PASS

REGRESSION: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\\deploy\\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.57 -RemoteAppDir /opt/intruoyi/runtime -Component website -Json` -> PASS, returned degraded production Website status; no restart attempted.

BLOCKED: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ruoyi-vue-pro\\script\\deploy\\restart-int-ruoyi-local.ps1 -Component full` -> FAIL, frontend `8081` recovered but the freshly packaged backend died during Spring startup because table `ruoyi-vue-pro.dcc_controlled_file_nas_transfer_task` does not exist for unrelated DCC NAS transfer changes in the dirty source tree.

GREEN: manual restore `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-runtime-control-20260523-184951.jar` on `48081` -> PASS, `/actuator/health` returned `{\"status\":\"UP\"}` and the runtime-control API became reachable again.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session runtime-control run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-infra-runtime-control-panel\scripts\verify-runtime-control-live.mjs` -> PASS, `http://127.0.0.1:8081/infra/monitors/runtime-control` rendered the Local/Test/Production matrix, the page fetched `http://localhost:48081/admin-api/infra/runtime-control/overview`, production Website showed degraded status for `172.30.30.57:8083`, and the production dialog blocked empty reason / missing `PROD` without sending any restart request.

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component full` -> PASS after the DCC missing-table prerequisite was resolved; frontend `8081` and backend `48081` both recovered from the current source tree.

GREEN: `GET http://127.0.0.1:48081/actuator/health` after current-source restart -> PASS, returned `{"status":"UP"}`.

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\show-int-ruoyi-local-status.ps1 -Component backend -Json` after current-source restart -> PASS, returned `status=running`, `httpStatus=HTTP 200`, `runtimeState=listening`.

GREEN: local schema auto-apply path -> PASS, `restart-int-ruoyi-local.ps1` now probes `information_schema.tables` for `dcc_controlled_file_nas_transfer_task` and applies `sql/mysql/20260523_dcc_nas_transfer_task.sql` through `docker exec int-ruoyi-mysql mysql ...` before backend packaging.

GREEN: local stale watcher cleanup -> PASS, the same script now stops existing `yudao-ui-admin-vue3` watcher processes before relaunching the frontend, and `show-int-ruoyi-local-status.ps1 -Component full -Json` eventually returns `status=running`, `frontend=HTTP 200`, `backend=HTTP 200`.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS on the final closeout rerun.

GREEN: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS on the final closeout rerun.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session runtime-control run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-infra-runtime-control-panel\scripts\verify-runtime-control-live.mjs` -> PASS on the current-source local runtime; the page fetched `http://127.0.0.1:48081/admin-api/infra/runtime-control/overview`, `local intruoyi-backend` showed `运行中 / listening / HTTP 200`, and production restart remained blocked until `PROD` is entered.

INFO: current-source restart residual -> backend startup succeeded, but the DCC NAS transfer scheduler logged a non-fatal runtime error because `last_failure_message` exceeded the current column length.

GREEN: scoped backend commit -> PASS, committed only runtime-control task files in `ruoyi-vue-pro` as `12bff41802 (任务: 新增运行控制台后端)`.
