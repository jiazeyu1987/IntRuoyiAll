# MySQL Binlog 增量段采集

## 任务目标

在第六阶段数据库增量实施中，基于已完成的 binlog 前置证据采集，继续实现 binlog 文件导出与增量段 manifest。`mysqlBackupMode=binlog-incremental` 时，脚本应在 staging 目录生成可审计的 `mysql/binlog/<binlog-file>.sql` 和 `mysql/binlog-segment-manifest.json`，记录起止位置、文件校验值和恢复回放状态。恢复回放尚未实现前，备份流程仍必须阻断成功，禁止将未演练的 binlog 段声明为可恢复备份。

## 里程碑

- [x] M1：补充 BDD 场景和 RED 用例。
- [x] M2：实现 binlog 文件导出。
- [x] M3：生成 binlog segment manifest。
- [x] M4：保持成功备份阻断直到恢复回放演练完成。
- [x] M5：运行回归、沉淀经验并提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。binlog 导出失败或缺回放演练时必须阻断。
- `是否从根因和长期维护角度解决`：是。将 MySQL 增量拆为可审计的 preflight、segment export、restore replay 三层。
- `是否存在临时补丁或绕过`：否。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_exports_binlog_segment_manifest_before_replay_gate -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q`
- `git diff --check`

## 当前状态

已完成本切片。`binlog-incremental` 模式会生成 `mysql/binlog/<binlog-file>.sql` 与 `mysql/binlog-segment-manifest.json`，并在恢复回放演练完成前阻断备份成功。
