# 执行日志：restore-data 恢复前版本一致性门禁

## BDD

- BDD: 恢复集程序版本不一致必须阻塞 -> Given 目标运行时 `IMAGE_TAG` 与所选备份 `recoverySet.program.imageTag` 不一致 / When 执行 `restore-data` / Then 在停服、预恢复快照、数据库导入、对象覆盖前失败，并提示当前版本与恢复集版本。
- BDD: 恢复集缺少 Redis 或配置范围必须阻塞 -> Given 所选备份 `recoverySet` 缺少 `redis.policy` 或 `configuration.manifestPath`、`configuration.composePath`、`checksums.sha256` / When 执行 `restore-data` / Then 在任何高风险操作前失败，并提示缺失字段。
- BDD: 恢复集版本一致且范围完整才可继续 -> Given 目标运行时 `IMAGE_TAG` 与恢复集 `program.imageTag` 完全一致，且 Redis、配置、校验字段完整 / When 执行 `restore-data` / Then 允许进入既有预恢复快照、停服、导入、恢复对象、启动和验证流程。

## TDD Evidence

- RED: `python -m pytest script/tests/test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_program_image_mismatch_before_actions script/tests/test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_requires_recovery_scope_before_actions` -> FAIL, 版本不一致、缺 `recoverySet.redis.policy`、缺 `recoverySet.configuration.*` 时当前实现继续进入 `docker compose stop backend frontend`，说明恢复前门禁缺失；缺 `recoverySet.checksums.sha256` 已由既有门禁阻塞。
- GREEN: `python -m pytest script/tests/test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_program_image_mismatch_before_actions script/tests/test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_requires_recovery_scope_before_actions` -> PASS，Linux `restore_data` 在预恢复快照和 Docker 命令前阻塞版本不一致、缺 Redis 策略、缺配置范围、缺 checksum。
- RED: `python -m pytest script/tests/test_backup_ops_tooling.py::test_restore_data_blocks_program_image_mismatch_before_snapshot script/tests/test_backup_ops_tooling.py::test_restore_data_blocks_missing_recovery_scope_before_start_notification` -> FAIL，PowerShell `Invoke-RestoreDataUseCase` 当前会先发送 started 通知并进入 pre-restore 快照，随后由快照 stub 抛出普通 fail，而不是恢复前 blocked。
- GREEN: `python -m pytest script/tests/test_backup_ops_tooling.py::test_restore_data_blocks_program_image_mismatch_before_snapshot script/tests/test_backup_ops_tooling.py::test_restore_data_blocks_missing_recovery_scope_before_start_notification script/tests/test_backup_ops_tooling.py::test_restore_data_sends_start_notification_before_mutating_steps` -> PASS，PowerShell `restore-data` 在开始通知和 pre-restore 快照前校验当前 `IMAGE_TAG`、恢复集 `program.imageTag`、Redis 策略、配置范围与 checksum 字段。
- RED: `mvn -pl yudao-module-infra -Dtest=RuntimeOpsGuideServiceImplTest#recommendShouldBlockDataExceptionWhenRecoveryConfigurationComposePathIsMissing test` -> FAIL，缺少 `recoverySet.configuration.composePath` 时 Java Runtime Control 候选仍为 `AVAILABLE`。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeOpsGuideServiceImplTest#recommendShouldBlockDataExceptionWhenRecoveryConfigurationComposePathIsMissing test` -> PASS，Java Runtime Control 候选服务要求 `recoverySet.configuration.composePath` 指向存在的编排配置文件。
- REGRESSION: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsGuideServiceImplTest,RuntimeControlServiceImplTest" test` -> PASS，48 个 Runtime Control Java 测试通过。
- REGRESSION: `python -m pytest script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_backup_ops_tooling.py` -> PASS，67 个 backup-ops 脚本契约与端口/恢复回归测试通过。
- REGRESSION: `git diff --check -- script/backup-ops/linux/backup_ops_linux.py script/backup-ops/scripts/modules/UseCases/RestoreData.psm1 script/backup-ops/scripts/modules/Infra/DockerOps.psm1 script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_backup_ops_tooling.py yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/controller/admin/runtimecontrol/vo/RuntimeControlRestoreCandidateRespVO.java yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeOpsCandidateServiceImpl.java yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeOpsGuideServiceImplTest.java doc/tasks/20260605-restore-data-version-compatibility-gate` -> PASS，仅 Git 提示 Windows 换行警告，无 whitespace 错误。
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-restore-data-version-compatibility-gate --mode preview` -> ready，delete/blocked/warnings 均为空。

## E2E Handoff

- 本切片完成的是恢复前 fail-fast 门禁与 Runtime Control 候选可用性判定，不执行真实恢复。Playwright 真实路径恢复/回滚演练继续归入父任务 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260604-dr-recovery-rollback-gap-audit` 的 test/backup 恢复演练切片；不得用接口调用、mock 路由或假备份点替代。
