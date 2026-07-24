# 任务：恢复数据禁止覆盖正式服

## 任务目标

按用户新增约束收紧 `restore-data`：正式服务器备份数据可以覆盖备份服/测试服正常 runtime，但不能覆盖正式服。所有脚本入口、菜单入口、Linux-local 入口和测试契约必须 fail-fast 阻断正式服目标。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-prod-backup-restore-to-test/task.md`
- 状态：`completed`
- 影响：上一任务补齐了 `restore-data` 的测试目标环境投影；本任务在此基础上移除/阻断正式服覆盖路径。

## BDD 场景

- BDD: restore-data 不得覆盖正式服 -> Given 操作员执行 `restore-data` / When 未显式指定测试目标环境或目标环境为 `prod` / Then 启动器必须在停止服务、恢复 MySQL、覆盖 MinIO 前失败，并提示只能恢复到备份服/测试服。
- BDD: 菜单恢复只能指向备份服 -> Given 操作员通过 Windows 控制台或 `03-恢复数据.bat` 执行恢复 / When 包装器调用主脚本 / Then 必须显式传入 `TargetEnvironment=test`，不得使用默认正式服目标。
- BDD: 其他模式目标边界不扩散 -> Given 操作员指定 `TargetEnvironment=test` / When 模式是 `rollback-app` 或 `rehearsal` / Then 仍必须 fail-fast，不得隐式投影到测试服。

## Milestones

- [x] M1：建立任务文档并确认上一任务完成。
- [x] M2：先写 RED 测试，证明当前默认 `restore-data` 仍可能指向正式服。
- [x] M3：收紧 PowerShell、Windows 包装器、Linux-local 入口和 runbook。
- [x] M4：执行 GREEN 与回归验证。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k "target_environment or action_wrappers or console_menu"`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k target_environment`
- `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -k restore_data_runbook`
- 备份/恢复相关回归测试。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。正式服目标直接 fail-fast，不做自动改写或静默转向。
- `是否从根因和长期维护角度解决`：是。统一在入口目标环境投影层和所有包装入口阻断正式服恢复。
- `是否存在临时补丁或绕过`：否。不通过文档提示替代代码门禁，不保留默认正式服恢复入口。

## 当前状态

completed

## 验证结果

- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k "target_environment or action_wrappers or console_launcher or forbids_production"` -> PASS, 4 passed.
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment` -> PASS, 1 passed.
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k "target_environment or forbids_non_test"` -> PASS, 4 passed.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -k restore_data_runbook` -> PASS, 1 passed.
- REGRESSION: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_backup_ops_linux_runtime_rollback_tooling.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_manifest_tooling.py script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_release_go_no_go_contract_docs.py` -> PASS, 75 passed.
- REGRESSION: `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1` -> PASS.
- REGRESSION: `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_manifest_ports.ps1` -> PASS.
- CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-data-test-only-safety --mode preview` -> ready, no delete candidates.

## Blockers

- none.
