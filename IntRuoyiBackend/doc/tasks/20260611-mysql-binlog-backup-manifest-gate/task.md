# MySQL Binlog 备份 Manifest 放行门禁

## 任务目标

完善第六阶段数据库增量闭环：`mysqlBackupMode=binlog-incremental` 时，`backup-now` 可以生成包含全量基线 dump、binlog preflight、binlog segment 和对象/DCC manifest 的备份点，但该备份点默认 `rehearsalStatus=not-run`，恢复仍必须等待 rehearsal 回放 binlog 并写入通过证据后才允许。禁止把未演练的 binlog 增量备份点当作可恢复。

## 里程碑

- [x] M1：补充 BDD 场景和 RED 用例。
- [x] M2：允许 binlog 模式生成备份点，而不是导出 segment 后直接阻断。
- [x] M3：manifest 记录 binlog segment 与 `requires-rehearsal` 状态。
- [x] M4：验证 restore-data 对未演练 binlog 备份仍阻断。
- [x] M5：运行回归、沉淀经验并提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。binlog 模式不会回退为全量 dump；未演练只允许备份点生成，不允许恢复放行。
- `是否从根因和长期维护角度解决`：是。区分“备份点已生成”和“恢复可放行”两个阶段，解决 binlog 需要先生成备份再演练的流程矛盾。
- `是否存在临时补丁或绕过`：否。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_writes_binlog_manifest_and_requires_rehearsal -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_mysql_incremental_without_replay_evidence_before_actions -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q`
- `git diff --check`

## 当前状态

已完成本切片。binlog 增量模式可以生成未演练备份点，manifest 明确标记 `requires-rehearsal`，恢复仍要求 rehearsal 通过。
