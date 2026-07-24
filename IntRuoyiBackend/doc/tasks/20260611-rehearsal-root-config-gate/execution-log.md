# 执行日志

## 2026-06-11

- BDD: 演练目录配置必须匹配安全门禁 -> Given 备份恢复演练配置用于测试服务器独立演练, When 读取 `servers.test.rehearsalRoot`, Then 默认配置和示例配置都必须位于 `/backup/int-ruoyi/rehearsal/*` 范围内，否则真实演练会在高风险动作前以 `INTBK-7001` 阻断。
- 真实流程证据: DCC B3/B4/B5 备份已成功生成，但 B3 rehearsal 被阻断；失败原因是 `测试演练目录不在预期范围内：/opt/intruoyi/runtime/data/backup-ops/rehearsal/runtime`。
- RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> FAIL, 默认配置 `backup-ops.config.json` 的 `servers.test.rehearsalRoot` 不在 `/backup/int-ruoyi/rehearsal/*` 范围内。
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> PASS。
- REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> PASS, 50 passed。
