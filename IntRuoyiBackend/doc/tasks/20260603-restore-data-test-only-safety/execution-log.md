# 执行记录：恢复数据禁止覆盖正式服

## BDD

BDD: restore-data 不得覆盖正式服 -> Given 操作员执行 `restore-data` / When 未显式指定测试目标环境或目标环境为 `prod` / Then 启动器必须在停止服务、恢复 MySQL、覆盖 MinIO 前失败，并提示只能恢复到备份服/测试服。

BDD: 菜单恢复只能指向备份服 -> Given 操作员通过 Windows 控制台或 `03-恢复数据.bat` 执行恢复 / When 包装器调用主脚本 / Then 必须显式传入 `TargetEnvironment=test`，不得使用默认正式服目标。

BDD: 其他模式目标边界不扩散 -> Given 操作员指定 `TargetEnvironment=test` / When 模式是 `rollback-app` 或 `rehearsal` / Then 仍必须 fail-fast，不得隐式投影到测试服。

## RED

RED: Windows restore-data 入口不得默认正式服 -> `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k "target_environment or action_wrappers or console_launcher or forbids_production"` -> FAIL, expected reason: console/action wrappers did not pass `TargetEnvironment test`, launcher did not contain production forbid guard.

RED: Linux restore-data 入口不得默认正式服 -> `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment` -> FAIL, expected reason: Linux launcher did not contain `restore-data only supports --target-environment test`.

RED: Linux restore-data 用例层不得绕过目标环境 -> `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k "target_environment or forbids_non_test"` -> FAIL, expected reason: `project_target_environment(..., "restore-data", "prod")` did not block and direct `restore_data` call reached mutating prerequisites on non-test config.

RED: runbook 不得保留正式服恢复语义 -> `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -k restore_data_runbook` -> FAIL, expected reason: runbook lacked `不能覆盖正式服` / `正式服目标必须 fail-fast` and still documented default production target.

## GREEN

GREEN: Windows restore-data 正式服阻断与包装入口测试目标 -> `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k "target_environment or action_wrappers or console_launcher or forbids_production"` -> PASS, 4 passed.

GREEN: Linux launcher 正式服阻断契约 -> `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment` -> PASS, 1 passed.

GREEN: Linux target projection and direct restore-data use case guard -> `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k "target_environment or forbids_non_test"` -> PASS, 4 passed.

GREEN: restore-data runbook contract -> `python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -k restore_data_runbook` -> PASS, 1 passed.

## REGRESSION

GREEN: backup/restore regression -> `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_backup_ops_linux_runtime_rollback_tooling.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_manifest_tooling.py script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_release_go_no_go_contract_docs.py` -> PASS, 75 passed.

GREEN: rehearsal evidence PowerShell contract -> `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1` -> PASS.

GREEN: manifest ports PowerShell contract -> `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_manifest_ports.ps1` -> PASS.

## Closeout

GREEN: closeout preview -> `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-data-test-only-safety --mode preview` -> ready, no delete candidates.
