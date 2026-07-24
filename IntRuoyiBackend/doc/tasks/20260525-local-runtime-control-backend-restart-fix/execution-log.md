# 执行日志：修复本地运行控制台后端重启脚本

BDD: 本地后端重启后动作接口存在 -> Given 运维人员重启本地后端, When 探测 `POST /admin-api/infra/runtime-control/actions`, Then 请求进入当前后端 Controller，未登录返回 `401`，不得返回 `No static resource`。

BDD: 本地前端发布请求路由正确 -> Given 本地前端 `localhost:8081` 打开运行控制台, When 填写 `发布测试服` 原因并确认, Then 浏览器发出到 `127.0.0.1:48081/admin-api/infra/runtime-control/actions` 的 POST，测试中止请求且不执行真实发布。

DIAGNOSIS: `POST http://127.0.0.1:48081/admin-api/infra/runtime-control/actions` -> FAIL, local backend was not listening.

DIAGNOSIS: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PARTIAL, Maven build succeeded and restart was dispatched, but backend health never became ready.

DIAGNOSIS: `output\runtime\int_main\backend-runtime-control-20260525-192806.err.log` -> FAIL, generated backend command parsed `--server.port=48081` and JDBC URL `&` separators as PowerShell syntax instead of Java arguments.

RED: `python -m pytest script\tests\test_runtime_control_scripts.py -q` -> FAIL, expected missing `$backendArgs = @(` and `& java @backendArgs`; existing restart script still used a PowerShell line-continuation `java -jar` command.

GREEN: `python -m pytest script\tests\test_runtime_control_scripts.py -q` -> PASS, 4 tests passed after switching backend startup to a Java argument array.

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS, Maven package succeeded and backend process started from `output\runtime\int_main\backend-runtime-control-20260525-193544.jar`.

GREEN: `GET http://127.0.0.1:48081/actuator/health` -> PASS, returned HTTP 200.

GREEN: unauthenticated `POST http://127.0.0.1:48081/admin-api/infra/runtime-control/actions` -> PASS, returned `{"code":401,"msg":"账号未登录","data":null}` instead of `No static resource`.

GREEN: frontend local E2E `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081 RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://127.0.0.1:48081 node tests\e2e\runtime-control-publish-test-submit-route.e2e.js` -> PASS, request was captured and aborted before execution.

GREEN: `python -m pytest script\tests\test_runtime_control_scripts.py script\tests\test_runtime_control_ops_scripts.py -q` -> PASS, 7 tests passed.

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260525-local-runtime-control-backend-restart-fix\bug-regression-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-local-runtime-control-backend-restart-fix --mode preview` -> PASS, cleanup preview kept `task.md` and `execution-log.md`, and marked temporary bug evidence for deletion.
