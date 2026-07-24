# 执行日志：运行控制台真实备份恢复回滚验证

BDD: 测试服备份恢复回滚全链路成功 -> Given 本机运行控制台可访问且 admin 拥有运维权限, When 用户依次点击 `立即备份`、`恢复数据`、`回滚版本` 并填写原因确认执行, Then 每个运行操作都成功完成，日志可在线查看，测试服健康检查通过，恢复使用真实备份数据，回滚后应用服务可访问。

GREEN: `Get-Content script\backup-ops\config\backup-ops.config.json` -> PASS, default backup-ops config points `servers.production.host` to `172.30.30.57`; this path must not be used for a test-server real-data E2E.

GREEN: `ssh root@172.30.30.58 "find /opt/intruoyi/ops/backup-ops -maxdepth 5 -type f"` -> PASS, Linux native backup scripts exist under `/opt/intruoyi/ops/backup-ops/linux-native`.

RED: Test-server UI E2E `立即备份` -> FAIL, operation `6555bb3f-0cea-4073-b467-60eb1b18df1c` failed before script execution.

GREEN: Test-server operation record -> PASS, failure summary was `运行控制台脚本不存在：D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/script/backup-ops/scripts/backup-ops.ps1`.

GREEN: `GET http://172.30.30.58:48081/actuator/health` -> PASS, HTTP 200 after failed backup attempt.

GREEN: `GET http://172.30.30.58:8081/` -> PASS, HTTP 200 after failed backup attempt.

GREEN: `GET http://172.30.30.58:8083/` -> PASS, HTTP 200 after failed backup attempt.

GREEN: `GET http://172.30.30.58:8083/showroom` -> PASS, HTTP 200 after failed backup attempt.

GREEN: Backup point inventory after failed E2E -> PASS, no new backup point was created and `IMAGE_TAG=20260525_200033` remained unchanged.

BDD: 测试服 Linux 后端调用本机 DR 脚本 -> Given 运行控制台后端部署在 Linux 测试服容器中, When 用户从测试服页面提交 `立即备份`、`恢复数据` 或 `回滚版本`, Then 后端应调用 `/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh` 并传入 linux-local 配置，而不是查找 Windows `script/backup-ops/scripts/backup-ops.ps1`。

RED: `python -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> FAIL, expected `linux-local` runtime-control backup-ops properties, `.sh` command execution, and test-server compose mounts were missing.

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeBackupNowShouldUseLinuxLocalBackupOpsWhenConfigured test` -> FAIL, `RuntimeControlProperties.getBackupOps()` was missing before implementation.

GREEN: `python -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py -q` -> PASS, 33 tests passed.

GREEN: `publish-int-ruoyi-to-test.ps1 -SkipDatabaseSync -SkipMinioSync` -> PASS, detached runner fix was published to the test server as `IMAGE_TAG=20260525_221321`; backend, admin frontend, Website root, and Website showroom returned HTTP 200.

GREEN: Test-server detached runner prerequisite check -> PASS, backend container had Python, Docker CLI, Docker Compose, `/var/run/docker.sock`, Linux DR scripts, runtime `.env`, and shared runtime-control state directory.

GREEN: `RUNTIME_CONTROL_ALLOW_REAL_DR=1 RUNTIME_CONTROL_E2E_BASE_URL=http://172.30.30.58:8081 RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://172.30.30.58:48081 RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG=20260525_200033 node tests\e2e\runtime-control-real-dr-flow.e2e.js` -> PASS, real UI flow created backup point `20260525_222914`, restored it, rolled back app to `20260525_200033`, and verified backend/admin frontend/Website/showroom HTTP 200.

GREEN: Direct test-server roll-forward cleanup -> PASS, `backup-ops-linux.sh --mode rollback-app --selected-image-tag 20260525_221321` restored the test server to the fixed tag after the rollback verification.

GREEN: Final test-server health check -> PASS, `IMAGE_TAG=20260525_221321`; `http://172.30.30.58:48081/actuator/health`, `http://172.30.30.58:8081/`, `http://172.30.30.58:8083/`, and `http://172.30.30.58:8083/showroom` returned HTTP 200.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 14 tests passed.

GREEN: `publish-int-ruoyi-to-test.ps1 -SkipDatabaseSync -SkipMinioSync` -> PASS, test server published fixed backend/frontend as `IMAGE_TAG=20260525_212649`; backend, admin frontend, Website root, and Website showroom returned HTTP 200.

GREEN: Test-server backend container prerequisite check -> PASS, `/usr/bin/python3`, `/usr/bin/docker`, Docker Compose `2.40.3`, `/var/run/docker.sock`, `/opt/intruoyi/ops`, `/opt/intruoyi/runtime`, and `/backup` were available inside `intruoyi-backend`.

RED: Real UI E2E after publish -> FAIL after `立即备份` succeeded with backup point `20260525_214705`; frontend E2E script did not close the operation log dialog before submitting the next action.

RED: Real UI E2E restore step -> FAIL, `restore-data` operation `1c1ea8df-b507-46ef-9825-da797ac39071` stopped the `intruoyi-backend` container that was executing the restore script, leaving backend/admin frontend unavailable until `docker compose up -d backend frontend website` was run.

RED: `python -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> FAIL, expected detached Linux-local operation runner support was missing.

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeRestoreDataShouldUseDetachedLinuxLocalRunnerWhenConfigured,RuntimeControlServiceImplTest#executeRollbackAppShouldUseDetachedLinuxLocalRunnerWhenConfigured test` -> FAIL, `RuntimeControlCommandExecutor.executeDetachedOperation(...)` was missing before implementation.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#executeRestoreDataShouldUseDetachedLinuxLocalRunnerWhenConfigured,RuntimeControlServiceImplTest#executeRollbackAppShouldUseDetachedLinuxLocalRunnerWhenConfigured test` -> PASS, restore and rollback use detached Linux-local runner.

GREEN: `python -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> PASS, detached runner static contract is present.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 16 tests passed.

GREEN: `python -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py -q` -> PASS, 33 tests passed.
