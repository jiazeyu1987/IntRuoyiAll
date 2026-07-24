# 执行日志

BDD: 演练回放 binlog segment 并写回证据 -> Given 备份点包含 `mysql/binlog-segment-manifest.json` 和对应 binlog SQL 文件，When 恢复演练导入全量基线后执行 binlog 回放，Then 每个 segment 必须被按 manifest 顺序导入演练 MySQL，manifest 写回 `replayStatus=passed` 和 segment 级回放证据；任一 segment 缺失或回放失败时演练失败。

RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_rehearsal_replays_binlog_segment_and_records_evidence -q` -> FAIL，当前实现没有 `replay_mysql_binlog_segments`。

GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_rehearsal_replays_binlog_segment_and_records_evidence -q` -> PASS。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> PASS，4 passed。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q` -> PASS，44 passed。

经验沉淀: MySQL binlog 增量段只有在隔离演练库中回放并写回 `replayStatus=passed` 后，才具备进入恢复候选放行判断的证据基础。
