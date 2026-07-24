# 执行记录：补全恢复数据目标环境后端支持

BDD: 前端恢复数据可恢复到测试服 -> Given 前端提交 `restore-data`、`targetEnvironment=test`、恢复候选和原因 / When 后端执行运维动作 / Then 后端记录环境为 `test`，命令参数包含 `-TargetEnvironment test` 或 `--target-environment test`，不要求正式服访问启用。

BDD: 前端恢复数据可恢复到备份服务器 -> Given 前端提交 `restore-data`、`targetEnvironment=backup`、恢复候选和原因 / When 后端执行运维动作 / Then 后端记录环境为 `backup`，命令参数包含 `-TargetEnvironment backup` 或 `--target-environment backup`。

BDD: 恢复数据禁止正式服目标 -> Given 前端或恶意调用提交 `restore-data targetEnvironment=prod` / When 后端校验动作 / Then 请求被阻断并记录 blocked 操作，不执行任何脚本。

BDD: 恢复数据脚本禁止正式服目标 -> Given 脚本入口收到 `restore-data` 且目标不是 `test|backup` / When 解析目标环境 / Then 脚本直接阻塞，不能回落到 production。

VERIFY: 上一后端任务 `doc/tasks/20260604-commit-runtime-control-state/task.md` 状态为 `completed`。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> FAIL，expected reason：`restore-data` 仍把 `targetEnvironment` 判为非法参数，`test/backup` 恢复数据默认数据责任人不存在，旧高危测试仍暴露正式服默认禁用基线。

RED: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_backup_ops_linux_runtime_tooling.py -q` -> FAIL，expected reason：PowerShell/Linux backup-ops 目标环境投影仍只支持 `test`，入口没有 `backup` 目标，恢复数据脚本仍用“only supports test”阻断 `backup`。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> PASS，51 tests，0 failures，0 errors。

GREEN: `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_backup_ops_linux_runtime_tooling.py -q` -> PASS，57 passed。

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260604-runtime-control-restore-target-backend/backend-api-evidence.md` -> PASS，Backend API evidence is valid。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-restore-target-backend --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。
