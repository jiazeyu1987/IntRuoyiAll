# 执行日志

BDD: binlog 增量请求先采集前置证据且不得伪装成功 -> Given 备份配置声明 `mysqlBackupMode=binlog-incremental`，When 用户执行 `backup-now`，Then 脚本必须查询 MySQL binlog 前置条件并写入 `mysql/binlog-preflight.json`，随后因 binlog 文件导出和恢复回放尚未实现而阻断成功，且不得回退为全量 dump 成功。

RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_records_binlog_preflight_before_blocking_incremental_success -q` -> FAIL，当前实现没有写入 binlog preflight 证据，流程继续进入后续备份步骤。

GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_records_binlog_preflight_before_blocking_incremental_success -q` -> PASS。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_mysql_incremental_without_replay_evidence_before_actions -q` -> PASS。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q` -> PASS，42 passed。

经验沉淀: MySQL binlog 增量要分为前置证据、binlog 文件采集、恢复回放、演练放行四层；前置证据通过只说明环境具备继续实施条件，不能代表备份可恢复。
