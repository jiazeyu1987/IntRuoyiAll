# 执行日志

## 2026-06-11

- BDD: 演练 MySQL 必须等待真实健康状态 -> Given 演练栈启动 MySQL 容器, When MySQL entrypoint 临时初始化服务短暂响应 ping 但 Docker health 仍未 healthy, Then 恢复脚本不得导入 dump，必须继续等待或超时失败。
- 真实流程证据: 第二轮 DCC 增量真实流程已通过演练目录门禁，但 B3 rehearsal 在 `intruoyi-rehearsal-mysql` 初始化窗口失败；远端随后显示容器为 healthy，说明脚本存在就绪误判。
- RED: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> FAIL, `Wait-BackupOpsMySqlReady` 未检查 Docker health 状态。
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> PASS。
- REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q` -> PASS, 51 passed。
- REGRESSION-E2E: 第三轮真实流程 B3 rehearsal 暴露 `Merge-BackupOpsRequest` 在 `DockerOps.psm1` 作用域不可用，修正为 `Merge-BackupOpsDockerRequest`，并补充静态测试约束。
