# 执行日志：运行控制台真实备份恢复回滚 E2E

BDD: 测试服备份恢复回滚全链路成功 -> Given 本机运行控制台可访问且 admin 拥有运维权限, When 用户依次点击 `立即备份`、`恢复数据`、`回滚版本` 并填写原因确认执行, Then 每个运行操作都成功完成，日志可在线查看，测试服健康检查通过，恢复使用真实备份数据，回滚后应用服务可访问。

RED: `Test-Path tests\e2e\runtime-control-real-dr-flow.e2e.js` -> FAIL, expected missing complete real backup/restore/rollback E2E before implementation.

GREEN: `node --check tests\e2e\runtime-control-real-dr-flow.e2e.js` -> PASS.

GREEN: `node tests\e2e\runtime-control-real-dr-flow.e2e.js` without `RUNTIME_CONTROL_ALLOW_REAL_DR=1` -> PASS as safety guard, failed before opening a browser or submitting any operation.

RED: `RUNTIME_CONTROL_ALLOW_REAL_DR=1 node tests\e2e\runtime-control-real-dr-flow.e2e.js` -> FAIL before submitting any operation, login page rendered the tenant as an already selected visible value instead of a visible tenant input.

GREEN: `node --check tests\e2e\runtime-control-real-dr-flow.e2e.js` -> PASS after updating login to assert the selected tenant when the tenant input is not visible.

RED: `RUNTIME_CONTROL_ALLOW_REAL_DR=1 RUNTIME_CONTROL_E2E_BASE_URL=http://172.30.30.58:8081 RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://172.30.30.58:48081 RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG=20260525_135729 node tests\e2e\runtime-control-real-dr-flow.e2e.js` -> FAIL, `立即备份` was submitted from the test-server UI but operation `6555bb3f-0cea-4073-b467-60eb1b18df1c` ended with status `failed`.

GREEN: `docker exec intruoyi-backend cat /yudao-server/D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/runtime/runtime-control/6555bb3f-0cea-4073-b467-60eb1b18df1c.json` on `172.30.30.58` -> PASS, failure summary was `运行控制台脚本不存在：D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/script/backup-ops/scripts/backup-ops.ps1`.

GREEN: `docker exec intruoyi-backend wc -c /yudao-server/D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/runtime/runtime-control/logs/6555bb3f-0cea-4073-b467-60eb1b18df1c.log` on `172.30.30.58` -> PASS, log size was `0`, confirming script execution never started.

GREEN: `GET http://172.30.30.58:48081/actuator/health` -> PASS, HTTP 200 after failed backup attempt.

GREEN: `GET http://172.30.30.58:8081/` -> PASS, HTTP 200 after failed backup attempt.

GREEN: `GET http://172.30.30.58:8083/` -> PASS, HTTP 200 after failed backup attempt.

GREEN: `GET http://172.30.30.58:8083/showroom` -> PASS, HTTP 200 after failed backup attempt.

GREEN: `ssh root@172.30.30.58 "find /backup/int-ruoyi /mnt/nas/备份 -maxdepth 2 -type d"` -> PASS, no new backup point was created by the failed UI operation.

GREEN: `ssh root@172.30.30.58 "cd /opt/intruoyi/runtime && sed -n 's/^IMAGE_TAG=//p' .env"` -> PASS, `IMAGE_TAG=20260525_200033` remained unchanged.

GREEN: `publish-int-ruoyi-to-test.ps1 -SkipDatabaseSync -SkipMinioSync` -> PASS, test server published fixed backend/frontend as `IMAGE_TAG=20260525_212649`; backend, admin frontend, Website root, and Website showroom returned HTTP 200.

RED: `RUNTIME_CONTROL_ALLOW_REAL_DR=1 RUNTIME_CONTROL_E2E_BASE_URL=http://172.30.30.58:8081 RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://172.30.30.58:48081 RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG=20260525_200033 node tests\e2e\runtime-control-real-dr-flow.e2e.js` -> FAIL after `立即备份` succeeded with backup point `20260525_214705`; the test script kept the operation log dialog open and the next `waitForResponse` promise failed when the browser was closed.

GREEN: `node --check tests\e2e\runtime-control-real-dr-flow.e2e.js` -> PASS after moving request wait setup to the confirmation click and closing visible operation dialogs between actions.

RED: `RUNTIME_CONTROL_ALLOW_REAL_DR=1 RUNTIME_CONTROL_E2E_BASE_URL=http://172.30.30.58:8081 RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://172.30.30.58:48081 RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG=20260525_200033 node tests\e2e\runtime-control-real-dr-flow.e2e.js` -> FAIL after `restore-data` submitted backup point `20260525_215449`; backend/admin frontend stopped because the restore script was still executing inside the backend container it restarted.

GREEN: `RUNTIME_CONTROL_ALLOW_REAL_DR=1 RUNTIME_CONTROL_E2E_BASE_URL=http://172.30.30.58:8081 RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://172.30.30.58:48081 RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG=20260525_200033 node tests\e2e\runtime-control-real-dr-flow.e2e.js` -> PASS after backend detached runner fix, using real test-server UI buttons and real data.

GREEN: E2E operation evidence -> PASS, `backup-now` operation `1f6fbb91-8753-4907-93a9-0de40b782ef4` created backup point `20260525_222914`; `restore-data` operation `88cc645d-52d5-440c-809e-9a9a6a9cde42` restored backup point `20260525_222914`; `rollback-app` operation `36481351-cf2a-451d-baaf-5dcf84ba0be3` rolled back to `20260525_200033`.

GREEN: E2E final health checks -> PASS, `http://172.30.30.58:48081/actuator/health`, `http://172.30.30.58:8081/`, `http://172.30.30.58:8083/`, and `http://172.30.30.58:8083/showroom` returned HTTP 200.

GREEN: Test-server cleanup verification -> PASS, backend task restored test server to fixed `IMAGE_TAG=20260525_221321` after the rollback verification.

BLOCKED: 真实 DR E2E 不能继续完成 -> missing prerequisite: restore execution must run from a process that is not terminated by the backend container restart it initiates. Impact: cannot verify restore polling, rollback execution, or final health checks from the current frontend E2E path.
