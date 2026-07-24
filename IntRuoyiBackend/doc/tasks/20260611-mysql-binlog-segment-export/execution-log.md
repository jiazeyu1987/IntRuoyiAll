# 执行日志

BDD: binlog 增量段导出后仍等待恢复回放 -> Given 备份配置声明 `mysqlBackupMode=binlog-incremental` 且 binlog preflight 通过，When 用户执行 `backup-now`，Then 脚本必须导出目标 binlog 段、生成 `mysql/binlog-segment-manifest.json`，并因恢复回放演练尚未完成而阻断成功，不能回退为全量 dump 成功。

RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_exports_binlog_segment_manifest_before_replay_gate -q` -> FAIL，当前实现只写入 binlog preflight，没有生成 `mysql/binlog-segment-manifest.json`。

GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_exports_binlog_segment_manifest_before_replay_gate -q` -> PASS。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_records_binlog_preflight_before_blocking_incremental_success -q` -> PASS。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q` -> PASS，43 passed。

经验沉淀: binlog 段文件和 segment manifest 只是增量链的中间产物；只有完成恢复回放并写入演练证据后，才能把 MySQL 增量备份声明为可恢复。
