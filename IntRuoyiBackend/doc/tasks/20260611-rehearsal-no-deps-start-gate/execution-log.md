# 执行日志

## 2026-06-11

- BDD: 演练只启动目标服务 -> Given 恢复演练已经完成 MySQL 与对象恢复, When 启动 backend/frontend 做验证, Then Docker Compose 命令必须显式禁止依赖启动，不能拉起 OnlyOffice 等非本次目标服务并占用测试服已有端口。
- 真实流程证据: 第四轮真实流程已完成 MySQL dump 导入与对象恢复，但启动 backend/frontend 时 compose 自动创建 `intruoyi-rehearsal-intruoyi-onlyoffice`，因宿主 `8080` 已被占用而失败。
- RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> FAIL, `Start-BackupOpsFrontendBackend` 未设置 `NoDeps = $true`。
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> PASS。
- REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> PASS, 52 passed。
