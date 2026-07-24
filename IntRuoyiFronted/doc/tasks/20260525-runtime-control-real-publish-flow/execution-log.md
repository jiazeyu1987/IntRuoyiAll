# 执行日志：运行控制台真实发布测试服 E2E

BDD: 发布测试服全链路成功 -> Given 本机运行控制台可访问且 admin 拥有运维权限, When 用户点击 `发布测试服`、保持 `只发代码`、填写原因并确认执行, Then 后端创建运行操作、执行发布脚本、日志可在线查看，最终状态为成功，测试服后端、管理端前端和 Website 健康检查通过。

RED: `node tests\e2e\runtime-control-publish-test-real-flow.e2e.js` -> FAIL, expected missing full publish E2E file.

GREEN: `node --check tests\e2e\runtime-control-publish-test-real-flow.e2e.js` -> PASS.

GREEN: `node tests\e2e\runtime-control-publish-test-real-flow.e2e.js` without `RUNTIME_CONTROL_ALLOW_REAL_PUBLISH=1` -> PASS as safety guard, failed fast before opening a browser or submitting a publish.

GREEN: `RUNTIME_CONTROL_ALLOW_REAL_PUBLISH=1 node tests\e2e\runtime-control-publish-test-real-flow.e2e.js` -> PASS, submitted `发布测试服` from the UI, filled reason `E2E真实发布测试服-只发代码-20260525`, waited for online log completion, and printed `PASS: runtime control real code-only publish-test flow`.

GREEN: runtime-control operation `9e6b9fe5-56e9-4651-84b9-9f9b4528f271` -> PASS, status `succeeded`, `parameters.publishScope=code-only`.

GREEN: publish log `output\runtime\int_main\runtime-control\logs\9e6b9fe5-56e9-4651-84b9-9f9b4528f271.log` -> PASS, command included `-SkipDatabaseSync -SkipMinioSync`, logs ended with `Publish completed.`

GREEN: `GET http://172.30.30.58:48081/actuator/health` -> PASS, HTTP 200.

GREEN: `GET http://172.30.30.58:8081/` -> PASS, HTTP 200.

GREEN: `GET http://172.30.30.58:8083/` -> PASS, HTTP 200.

GREEN: `GET http://172.30.30.58:8083/showroom` -> PASS, HTTP 200.

GREEN: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence doc\tasks\20260525-runtime-control-real-publish-flow\qa-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --self-test` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-real-publish-flow --mode preview` -> PASS, only temporary `qa-evidence.md` was marked for deletion.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-real-publish-flow --mode apply` -> PASS, deleted temporary `qa-evidence.md`.

GREEN: `node --check tests\e2e\runtime-control-publish-test-real-flow.e2e.js` -> PASS after removing swallowed refresh/JSON parse errors from the E2E helper flow.
