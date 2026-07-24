# 执行日志

BDD: MySQL 增量恢复证据缺失时阻断 -> Given 备份 manifest 声明 `mysqlBackupMode=binlog-incremental` 且 `mysqlIncrementalPlan.binlog.status=requires-prerequisite`，When 用户执行 `restore-data`，Then 恢复必须在停服务、重建数据库、导入 dump、对象回放之前失败，并提示缺少 `restoreReplayRehearsal` 等增量恢复证据。

RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_mysql_incremental_without_replay_evidence_before_actions -q` -> FAIL，当前实现未在 MySQL 增量 preflight 阶段阻断，而是进入后续对象恢复校验后报 `INTBK-4002`。

GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_mysql_incremental_without_replay_evidence_before_actions -q` -> PASS。

REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q` -> PASS，41 passed。

经验沉淀: MySQL 增量恢复必须由 manifest 明确声明增量类型、计划状态、证据字段和恢复演练结果；证据不足时不得自动按全量 dump 恢复。
