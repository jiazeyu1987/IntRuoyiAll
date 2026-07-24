# 执行日志：运行控制台真实发布测试服全链路

BDD: 发布测试服全链路成功 -> Given 本机运行控制台可访问且 admin 拥有运维权限, When 用户点击 `发布测试服`、保持 `只发代码`、填写原因并确认执行, Then 后端创建运行操作、执行发布脚本、日志可在线查看，最终状态为成功，测试服后端、管理端前端和 Website 健康检查通过。

GREEN: `GET http://localhost:8081/` -> PASS, local frontend returned HTTP 200.

GREEN: `GET http://127.0.0.1:48081/actuator/health` -> PASS, local backend returned HTTP 200.

RED: `pnpm exec vite --version` in `yudao-ui-admin-vue3` -> FAIL, `ERR_PNPM_RECURSIVE_EXEC_FIRST_FAIL Command "vite" not found`; current publish scripts that call `pnpm exec vite build --mode test` would fail before a real publish can complete.

RED: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected `publish-int-ruoyi-to-test.ps1`、`promote-int-ruoyi-test-to-prod.ps1`、`publish-int-ruoyi-frontend-only-to-prod.ps1` to expose `Invoke-FrontendViteBuild -FrontendDir $frontendDir` and no longer call `pnpm exec vite`.

GREEN: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_ops_scripts.py -q` -> PASS, 28 tests passed.

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\vite\bin\vite.js --version` -> PASS, `vite/5.1.4`.

GREEN: `rg "pnpm exec vite|Invoke-CheckedCommand -FilePath 'pnpm' -ArgumentList @\('exec', 'vite'\)|Require-Command 'pnpm'" script\deploy\publish-int-ruoyi-to-test.ps1 script\deploy\promote-int-ruoyi-test-to-prod.ps1 script\deploy\publish-int-ruoyi-frontend-only-to-prod.ps1` -> PASS, no matches.

GREEN: `RUNTIME_CONTROL_ALLOW_REAL_PUBLISH=1 node tests\e2e\runtime-control-publish-test-real-flow.e2e.js` from `yudao-ui-admin-vue3` -> PASS, operation `9e6b9fe5-56e9-4651-84b9-9f9b4528f271` completed with status `succeeded`.

GREEN: runtime-control audit file `output\runtime\int_main\runtime-control\9e6b9fe5-56e9-4651-84b9-9f9b4528f271.json` -> PASS, `parameters.publishScope=code-only` and reason `E2E真实发布测试服-只发代码-20260525`.

GREEN: runtime-control operation log `output\runtime\int_main\runtime-control\logs\9e6b9fe5-56e9-4651-84b9-9f9b4528f271.log` -> PASS, command included `-SkipDatabaseSync -SkipMinioSync` and ended with `Publish completed.`

GREEN: `GET http://172.30.30.58:48081/actuator/health` -> PASS, HTTP 200.

GREEN: `GET http://172.30.30.58:8081/` -> PASS, HTTP 200.

GREEN: `GET http://172.30.30.58:8083/` -> PASS, HTTP 200.

GREEN: `GET http://172.30.30.58:8083/showroom` -> PASS, HTTP 200.

GREEN: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc\tasks\20260525-runtime-control-real-publish-flow\ci-cd-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --self-test` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-real-publish-flow --mode preview` -> PASS, only temporary `ci-cd-evidence.md` was marked for deletion.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-real-publish-flow --mode apply` -> PASS, deleted temporary `ci-cd-evidence.md`.
