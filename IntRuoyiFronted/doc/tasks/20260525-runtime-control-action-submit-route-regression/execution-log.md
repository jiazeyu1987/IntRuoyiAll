# 执行日志：运行控制台发布请求路由回归

BDD: 发布测试服确认提交必须命中后端动作接口 -> Given `芋道源码/admin` 打开运行控制台, When 点击 `发布测试服`、填写原因并确认执行, Then 浏览器发出的 POST 目标必须是测试服后端 `/admin-api/infra/runtime-control/actions`，请求体包含 `action=publish-test` 和 `publishScope=code-only`，测试中止该请求且不执行真实发布。

RED: `node tests\e2e\runtime-control-publish-test-submit-route.e2e.js` -> FAIL, expected missing regression file for the submit request route.

DIAGNOSIS: Direct backend probe `POST http://172.30.30.58:48081/admin-api/infra/runtime-control/actions` without login -> PASS for route existence, returned `{"code":401,"msg":"账号未登录","data":null}` instead of `No static resource`.

DIAGNOSIS: Frontend bundle probe `http://172.30.30.58:8081/assets/index-CU3pwOmw.js` -> PASS, current deployed frontend contains backend origin `172.30.30.58:48081`.

GREEN: `node --check tests\e2e\runtime-control-publish-test-submit-route.e2e.js` -> PASS.

GREEN: `node tests\e2e\runtime-control-publish-test-submit-route.e2e.js` with `NODE_PATH=C:\Users\BJB110\AppData\Local\npm-cache\_npx\e41f203b7505f1fb\node_modules` -> PASS, captured and aborted `POST http://172.30.30.58:48081/admin-api/infra/runtime-control/actions` with `action=publish-test` and `publishScope=code-only`.

REGRESSION: original five runtime-control button E2E files and `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260525-runtime-control-action-submit-route-regression\bug-regression-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-action-submit-route-regression --mode preview` -> PASS, cleanup preview kept `task.md` and `execution-log.md`, and marked temporary bug evidence for deletion.
