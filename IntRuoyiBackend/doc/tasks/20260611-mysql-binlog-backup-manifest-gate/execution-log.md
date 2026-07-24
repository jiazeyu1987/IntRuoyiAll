# 执行日志

BDD: binlog 备份点可生成但恢复必须等待演练 -> Given 备份配置声明 `mysqlBackupMode=binlog-incremental`，When 用户执行 `backup-now`，Then 备份点应包含全量 dump、binlog preflight、binlog segment manifest、DCC manifest 和对象 inventory，manifest 记录 MySQL 增量状态为 `requires-rehearsal`；When 用户未完成 rehearsal 就执行 `restore-data`，Then 恢复必须阻断。

RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_writes_binlog_manifest_and_requires_rehearsal -q` -> FAIL，旧实现导出 binlog segment 后直接阻断，无法形成待演练备份点。

GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_writes_binlog_manifest_and_requires_rehearsal -q` -> PASS。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_mysql_incremental_without_replay_evidence_before_actions script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_unverified_backup_before_actions -q` -> PASS，2 passed。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q` -> PASS，45 passed。

经验沉淀: MySQL binlog 增量备份应允许生成“待演练备份点”，但 manifest 必须明确 `requires-rehearsal`，restore-data 必须继续以 rehearsal 证据作为放行条件。
