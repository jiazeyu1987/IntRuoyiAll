# MySQL Binlog 恢复回放证据

## 任务目标

在第六阶段数据库增量实施中，补齐 binlog 增量段的恢复回放证据写回能力。恢复演练导入全量基线后，如果备份点包含 `mysql/binlog-segment-manifest.json`，必须按 manifest 中的 segment 顺序回放 binlog SQL 文件，并将 `replayStatus`、回放时间和 segment 级结果写回 manifest；任何 segment 缺失或回放失败都不得标记演练通过。

## 里程碑

- [x] M1：补充 BDD 场景和 RED 用例。
- [x] M2：实现 binlog segment 回放 helper。
- [x] M3：接入 rehearsal 流程。
- [x] M4：运行回归、沉淀经验并提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。segment 缺失或回放失败直接阻断演练。
- `是否从根因和长期维护角度解决`：是。将 binlog 增量是否可恢复绑定到演练写回证据。
- `是否存在临时补丁或绕过`：否。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_rehearsal_replays_binlog_segment_and_records_evidence -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q`
- `git diff --check`

## 当前状态

已完成本切片。恢复演练会在全量 dump 导入后回放 `mysql/binlog-segment-manifest.json` 中的 segment，并写回 `replayStatus=passed` 与 segment 级回放证据。
