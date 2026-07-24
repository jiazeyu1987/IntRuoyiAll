# MySQL 增量恢复前置门禁

## 任务目标

在 `restore-data` 恢复流程中补齐 MySQL 增量备份模式的恢复前置门禁：当备份 manifest 声明 `mysqlBackupMode` 为 binlog 或 xtrabackup 增量模式时，必须在停服务、重建数据库、导入 dump、对象回放之前验证对应增量计划和恢复演练证据；证据不足时 fail fast，不得静默回退为全量 dump 恢复。

## 里程碑

- [x] M1：记录 BDD 场景与 RED 用例。
- [x] M2：实现 `restore-data` 的 MySQL 增量 preflight。
- [x] M3：运行脚本回归，确认全量基线不受影响。
- [x] M4：沉淀备份恢复经验并提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。增量证据缺失时直接阻断。
- `是否从根因和长期维护角度解决`：是。恢复侧按 manifest 契约验证数据库备份模式，避免未来启用增量后被全量 dump 静默掩盖。
- `是否存在临时补丁或绕过`：否。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_mysql_incremental_without_replay_evidence_before_actions -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q`
- `git diff --check`

## 当前状态

已完成。`restore-data` 对 `binlog-*` 和 `xtrabackup-*` 模式执行独立 preflight；证据缺失时在任何高风险动作前阻断。
