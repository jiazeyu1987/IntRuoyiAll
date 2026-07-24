# 任务：补全回滚版本目标环境后端支持

## 任务目标

补全运行控制台 `rollback-app` 后端与 backup-ops 脚本目标环境契约：前端提交 `targetEnvironment=test|backup` 后，后端必须正式接收、校验、记录并传给 PowerShell/Linux 回滚脚本；`prod` 或缺失目标环境必须 fail fast，不能影响正式服务器程序和数据。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-runtime-control-restore-target-backend/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制台后端、备份恢复脚本、相关测试和任务证据。

## BDD 场景

- BDD: 前端回滚版本可回滚到测试服 -> Given 前端提交 `rollback-app`、`targetEnvironment=test`、回滚候选和原因 / When 后端执行运维动作 / Then 后端记录环境为 `test`，命令参数包含 `-TargetEnvironment test` 或 `--target-environment test`。
- BDD: 前端回滚版本可回滚到备份服务器 -> Given 前端提交 `rollback-app`、`targetEnvironment=backup`、回滚候选和原因 / When 后端执行运维动作 / Then 后端记录环境为 `backup`，命令参数包含 `-TargetEnvironment backup` 或 `--target-environment backup`。
- BDD: 回滚版本禁止正式服目标 -> Given 前端或恶意调用提交 `rollback-app targetEnvironment=prod` / When 后端校验动作 / Then 请求被阻断并记录 blocked 操作，不执行任何脚本。
- BDD: 回滚版本脚本投影目标环境 -> Given 脚本入口收到 `rollback-app` 且目标为 `test|backup` / When 解析目标环境 / Then 脚本把 production runtime 配置投影到对应目标服务器，仍只切换应用版本。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：新增 RED 测试覆盖后端 `rollback-app` 目标环境和脚本支持。
- [x] M3：实现后端目标环境校验、命令参数、责任人目标和脚本目标投影。
- [x] M4：运行后端验证并记录证据。
- [x] M5：收尾预览并提交后端改动。

## Expected Verification

- RED/GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest" test`
- RED/GREEN：`python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_runtime_control_ops_scripts.py -q`
- GREEN：backend API evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少目标环境、目标为正式服或脚本配置缺失时直接阻塞。
- `是否从根因和长期维护角度解决`：是。将回滚目标环境纳入后端契约、操作记录、脚本参数和脚本投影。
- `是否存在临时补丁或绕过`：否。不绕过后端责任人门禁、不降级到 production、不手写成功报告。

## 当前状态

completed

## 验证结果

- VERIFY：上一后端任务 `doc/tasks/20260604-runtime-control-restore-target-backend/task.md` 状态为 `completed`。
- RED：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> FAIL，原因：后端仍把 `rollback-app targetEnvironment=test|backup` 判为非法参数，且缺少 test/backup 默认回滚发布责任人。
- RED：`python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_runtime_control_ops_scripts.py -q` -> FAIL，原因：PowerShell/Linux backup-ops 目标环境投影仍只允许 `backup-now` 与 `restore-data`，不支持 `rollback-app`。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#getOverviewShouldReadProductionStatusWhenWriteAccessIsDisabled" test` -> PASS，正式服写禁用时仍可只读状态查询，动作按钮保持禁用。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest" test` -> PASS，53 tests。
- GREEN：`python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS，57 passed。
- GREEN：backend API evidence validator -> PASS。
- GREEN：`git diff --check` -> PASS，仅有 Windows 行尾规范化提示。
- CLOSEOUT PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-rollback-target-backend --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-rollback-target-backend/backend-api-evidence.md`
- `doc/tasks/20260604-runtime-control-rollback-target-backend/execution-log.md`
