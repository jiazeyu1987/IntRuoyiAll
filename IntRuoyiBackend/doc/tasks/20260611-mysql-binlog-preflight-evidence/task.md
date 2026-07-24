# MySQL Binlog 增量前置证据采集

## 任务目标

在第六阶段数据库增量实施中，先补齐 `binlog-incremental` 的真实前置探测能力：当备份配置声明 MySQL binlog 增量模式时，脚本必须查询目标 MySQL 的 binlog 开关、binlog 格式、保留窗口和当前位置，并写入 `mysql/binlog-preflight.json`。在 binlog 文件导出和恢复回放尚未实现前，备份流程必须继续阻断成功，禁止静默回退到全量 dump。

## 里程碑

- [x] M1：补充 BDD 场景和 RED 用例。
- [x] M2：实现 binlog preflight 查询与证据落盘。
- [x] M3：保持 binlog 增量成功路径阻断，避免半成品被声明为可恢复。
- [x] M4：运行 Linux backup-ops 回归。
- [x] M5：沉淀备份恢复经验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。binlog 增量请求不会回退为全量 dump 成功。
- `是否从根因和长期维护角度解决`：是。先把 binlog 增量所需的环境证据结构化落盘，为后续 binlog 文件采集和恢复回放提供契约。
- `是否存在临时补丁或绕过`：否。当前阻断是正式安全门，不是绕过。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py::test_linux_backup_now_records_binlog_preflight_before_blocking_incremental_success -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -q`
- `git diff --check`

## 当前状态

已完成本切片。`binlog-incremental` 模式会生成 `mysql/binlog-preflight.json`，但仍阻断成功备份；完整 MySQL 增量备份仍需后续实现 binlog 文件导出、增量段 manifest、恢复回放和演练证据。
