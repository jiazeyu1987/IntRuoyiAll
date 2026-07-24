# 执行日志：运行控制台真实带数据提升正式服验证

BDD: 带数据提升正式服成功 -> Given 运维人员在本机运行控制台打开“提升正式服”弹窗, When 选择“带数据发布”、填写原因并输入 `PROD` 后确认执行, Then 前端应提交 `promote-prod` 请求，参数包含 `publishScope=with-data`，并展示可查看日志。

BDD: 带数据发布后 Website 正常打开 -> Given 带数据提升正式服完成, When 浏览器访问正式服 Website 根路径和 `/showroom`, Then 页面应成功加载且不出现前端运行错误。

BDD: 操作日志可追溯 -> Given 带数据提升正式服动作完成, When 查看运行控制台操作日志, Then 最近操作应显示“提升正式服”“带数据发布”和成功状态。

RED: `Select-String tests\e2e\runtime-control-promote-prod-real-flow.e2e.js -Pattern "RUNTIME_CONTROL_REAL_PROMOTE_SCOPE|with-data|带数据发布"` -> FAIL, existing real promote E2E only supports default `code-only`.

GREEN: `node --check tests\e2e\runtime-control-promote-prod-real-flow.e2e.js` -> PASS after adding configurable promote scope.

GREEN: `RUNTIME_CONTROL_REAL_PROMOTE_SCOPE=with-data RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD=1 node tests\e2e\runtime-control-promote-prod-real-flow.e2e.js` without `RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_WITH_DATA=1` -> PASS as safety guard, failed fast before opening a browser or submitting production data overwrite.

GREEN: preflight health -> PASS, local runtime-control frontend/backend, test backend/frontend/Website, and production backend/frontend/Website all returned HTTP 200.

RED: real with-data promote operation `4af0097f-399b-4538-8e8c-129730c98a1c` -> FAIL, production root filesystem was full while uploading the release image bundle.

RED: real with-data promote operation `e0f4242a-1f83-46a3-9c93-815d627a006e` -> FAIL, data sync completed but Website `/showroom` returned 500 because the Website container needed to be recreated after the dist bind mount directory was replaced.

GREEN: `RUNTIME_CONTROL_REAL_PROMOTE_SCOPE=with-data RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD=1 RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_WITH_DATA=1 node tests\e2e\runtime-control-promote-prod-real-flow.e2e.js` -> PASS, operation `5806c1d8-ebd9-405e-85cf-f37b322397c2` succeeded.

GREEN: operation audit `5806c1d8-ebd9-405e-85cf-f37b322397c2` -> PASS, `action=promote-prod`, `parameters.publishScope=with-data`, status `succeeded`.

GREEN: production health after with-data promotion -> PASS, `http://172.30.30.57:48081/actuator/health`, `http://172.30.30.57:8081/`, `http://172.30.30.57:8083/`, and `http://172.30.30.57:8083/showroom` returned HTTP 200.

GREEN: production Website browser verification -> PASS, Playwright opened `http://172.30.30.57:8083/` and `http://172.30.30.57:8083/showroom`; both rendered non-empty content with no page errors or critical request failures.

GREEN: production login closure -> PASS, login requests were sent to `http://172.30.30.57:48081/admin-api/system/auth/login` and did not call the test backend.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-real-promote-prod-with-data --mode preview` -> PASS, kept only `task.md` and `execution-log.md`; no delete, blocked, or warning entries.
