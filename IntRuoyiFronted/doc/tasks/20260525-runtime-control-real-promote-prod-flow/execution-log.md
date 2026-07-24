# 执行日志：运行控制台真实提升正式服链路验证

BDD: 默认只发代码提升正式服成功 -> Given 运维人员在测试服运行控制台打开“提升正式服”弹窗, When 默认选择“只发代码”、填写原因并输入 `PROD` 后确认执行, Then 前端应提交 `promote-prod` 请求，参数包含 `publishScope=code-only`，并展示动作已提交和可查看日志。

BDD: 正式服提升后前端闭环到正式后端 -> Given 提升正式服完成, When 访问正式环境管理端登录页并执行真实登录, Then 登录请求应闭环到正式后端，不能继续指向测试后端。

BDD: 操作日志可追溯 -> Given 提升正式服动作完成, When 在运行控制台查看最近操作和日志, Then 最近操作应显示“提升正式服”“只发代码”和成功状态，日志可打开并包含发布完成证据。

GREEN: `GET http://localhost:8081/` -> PASS, local runtime-control frontend returned HTTP 200.

GREEN: `GET http://127.0.0.1:48081/actuator/health` -> PASS, local runtime-control backend returned HTTP 200.

GREEN: preflight test server health -> PASS, `172.30.30.58:48081`、`8081`、`8083` returned HTTP 200.

GREEN: preflight production health -> PASS, `172.30.30.57:48081`、`8081`、`8083` returned HTTP 200.

BLOCKED: test server runtime-control backend as execution host -> FAIL, test server lacks the PowerShell promote script and cannot be the execution host for code promotion. The real E2E will use local runtime-control frontend/backend, matching the existing publish-test full-chain task.

RED: `Test-Path tests\e2e\runtime-control-promote-prod-real-flow.e2e.js` -> FAIL, expected missing full real promote-prod E2E before implementation.

GREEN: `node --check tests\e2e\runtime-control-promote-prod-real-flow.e2e.js` -> PASS.

GREEN: `node tests\e2e\runtime-control-promote-prod-real-flow.e2e.js` without `RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD=1` -> PASS as safety guard, failed fast before opening a browser or submitting a production promotion.

GREEN: `NODE_PATH=<npx playwright cache> node tests\e2e\runtime-control-promote-prod.e2e.js` -> PASS, existing promote-prod guard still blocks submission without `PROD`.

GREEN: `RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD=1 node tests\e2e\runtime-control-promote-prod-real-flow.e2e.js` -> PASS, submitted `提升正式服` from the UI, kept default `只发代码`, filled reason `E2E真实提升正式服-只发代码-20260525`, entered `PROD`, waited for online log completion, and printed `PASS: runtime control real code-only promote-prod flow`.

GREEN: runtime-control operation `97f81d47-29b4-457d-ad4b-ed42301dd9e0` -> PASS, status `succeeded`, `action=promote-prod`, `parameters.publishScope=code-only`.

GREEN: promote log `output\runtime\int_main\runtime-control\logs\97f81d47-29b4-457d-ad4b-ed42301dd9e0.log` -> PASS, command included `-SkipDatabaseSync -SkipMinioSync`, included `Skipping database sync for code-only promotion` and `Skipping MinIO sync for code-only promotion`, and ended with `Promotion completed.`

GREEN: production health after promotion -> PASS, `http://172.30.30.57:48081/actuator/health`、`http://172.30.30.57:8081/`、`http://172.30.30.57:8083/`、`http://172.30.30.57:8083/showroom` returned HTTP 200.

GREEN: production frontend login closure -> PASS, real login sent auth requests to `http://172.30.30.57:48081/admin-api/system/auth/login` and did not call `172.30.30.58:48081`.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-real-promote-prod-flow --mode preview` -> PASS, kept only `task.md` and `execution-log.md`; no delete, blocked, or warning entries.
