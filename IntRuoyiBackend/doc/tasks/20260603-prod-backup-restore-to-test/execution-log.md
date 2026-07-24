# 执行记录：正式备份恢复到测试服务器

## BDD

BDD: 正式备份可恢复到测试服务器 -> Given 测试服务器备份仓库存在已通过门禁的正式备份点 `<backupId>` / When 操作员执行 `restore-data` 并显式指定测试目标环境 / Then 恢复流程必须把目标 runtime、MySQL、MinIO、健康检查投影到测试服务器正常 runtime，而不是正式服务器或演练槽位。

BDD: 高危恢复仍需显式恢复点 -> Given 操作员准备恢复到测试服务器 / When 未显式提供 `SelectedBackupId` 或 `--selected-backup-id` / Then 命令必须在停止服务、导入数据库或覆盖对象文件前失败，不得自动选择最近备份点。

BDD: 非授权目标环境不扩散 -> Given 操作员指定 `TargetEnvironment=test` / When 模式不是已支持的 `backup-now` 或 `restore-data` / Then 启动器必须 fail fast，不能把应用回滚或其他动作隐式投影到测试服。

## Evidence

- INSPECT: `script/backup-ops/scripts/backup-ops.ps1` 当前 `Resolve-BackupOpsTargetEnvironmentConfig` 对 `TargetEnvironment=test` 仅允许 `backup-now`。
- INSPECT: `script/backup-ops/linux/backup_ops_linux.py` 当前 `project_target_environment` 对 `target_environment=test` 仅允许 `backup-now`。
- INSPECT: `rehearsal` 当前恢复到测试服务器独立演练槽位，不覆盖测试服务器正常 runtime，不能等同为本任务目标。

## RED

- RED: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k target_environment` -> FAIL, expected reason: PowerShell `Resolve-BackupOpsTargetEnvironmentConfig` still only supports `backup-now` for `TargetEnvironment=test`.
- RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment` -> FAIL, expected reason: Linux-local source still lacks the `{"backup-now", "restore-data"}` explicit allow-list.
- RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k target_environment` -> FAIL, expected reason: `project_target_environment(config, "restore-data", "test")` raises `target-environment test is only supported for backup-now`.

## GREEN

- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k target_environment` -> PASS, PowerShell `TargetEnvironment=test` now explicitly supports `backup-now` and `restore-data`.
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment` -> PASS, Linux-local source now has the explicit `{"backup-now", "restore-data"}` allow-list.
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k target_environment` -> PASS, `project_target_environment(config, "restore-data", "test")` projects production runtime fields to the configured test runtime while `rollback-app` remains blocked.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -k restore_data_runbook` -> PASS, runbook documents default prod restore and explicit test normal runtime restore.

## REGRESSION

- REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_backup_ops_linux_runtime_rollback_tooling.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_manifest_tooling.py script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_release_go_no_go_contract_docs.py` -> FAIL, expected reason after widening scope: shared Linux runtime ports fixture missed required `tools.minioClientImage`, while production code correctly fails fast on missing prerequisite.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1` -> PASS.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_manifest_ports.ps1` -> PASS.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backup-disaster-recovery-readiness\scripts\validate_backup_disaster_recovery.py --evidence docs\recovery\backup-disaster-recovery.md` -> PASS.
- REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_backup_ops_linux_runtime_rollback_tooling.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_manifest_tooling.py script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_release_go_no_go_contract_docs.py` -> PASS, 72 passed.

## Closeout

- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-prod-backup-restore-to-test --mode preview` -> READY, keep `task.md` / `execution-log.md`, delete `<none>`, blocked `<none>`, warnings `<none>`.
