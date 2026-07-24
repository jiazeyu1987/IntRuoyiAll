# 执行日志：回滚版本目标环境后端支持

## BDD

- BDD: 前端回滚版本可回滚到测试服 -> Given 前端提交 `rollback-app`、`targetEnvironment=test`、回滚候选和原因 / When 后端执行运维动作 / Then 后端记录环境为 `test`，命令参数包含 `-TargetEnvironment test` 或 `--target-environment test`。
- BDD: 前端回滚版本可回滚到备份服务器 -> Given 前端提交 `rollback-app`、`targetEnvironment=backup`、回滚候选和原因 / When 后端执行运维动作 / Then 后端记录环境为 `backup`，命令参数包含 `-TargetEnvironment backup` 或 `--target-environment backup`。
- BDD: 回滚版本禁止正式服目标 -> Given 前端或恶意调用提交 `rollback-app targetEnvironment=prod` / When 后端校验动作 / Then 请求被阻断并记录 blocked 操作，不执行任何脚本。
- BDD: 回滚版本脚本投影目标环境 -> Given 脚本入口收到 `rollback-app` 且目标为 `test|backup` / When 解析目标环境 / Then 脚本把 production runtime 配置投影到对应目标服务器，仍只切换应用版本。

## RED

- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> FAIL，原因：后端仍把 `rollback-app targetEnvironment=test|backup` 判为非法参数，且缺少 test/backup 默认回滚发布责任人。
- RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_runtime_control_ops_scripts.py -q` -> FAIL，原因：PowerShell/Linux backup-ops 目标环境投影仍只允许 `backup-now` 与 `restore-data`，不支持 `rollback-app`。

## GREEN

- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#getOverviewShouldReadProductionStatusWhenWriteAccessIsDisabled" test` -> PASS，正式服写禁用时仍可只读状态查询，动作按钮保持禁用。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> PASS，53 tests。
- GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS，57 passed。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260604-runtime-control-rollback-target-backend/backend-api-evidence.md` -> PASS，Backend API evidence is valid。

## REGRESSION

- GREEN: `git diff --check` -> PASS，仅有 Windows 行尾规范化提示。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-rollback-target-backend --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`。
