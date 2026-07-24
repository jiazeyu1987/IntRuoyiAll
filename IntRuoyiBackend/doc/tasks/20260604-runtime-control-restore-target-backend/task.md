# 任务：补全恢复数据目标环境后端支持

## 任务目标

补全运行控制台 `restore-data` 后端与备份恢复脚本目标环境契约：前端提交 `targetEnvironment=test|backup` 后，后端必须正式接收、校验、记录并传给 PowerShell/Linux 恢复脚本；`prod` 或缺失目标环境必须 fail fast，不能影响正式服务器程序和数据。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260604-commit-runtime-control-state/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制台后端、备份恢复脚本、相关测试和任务证据。

## BDD 场景

- BDD: 前端恢复数据可恢复到测试服 -> Given 前端提交 `restore-data`、`targetEnvironment=test`、恢复候选和原因 / When 后端执行运维动作 / Then 后端记录环境为 `test`，命令参数包含 `-TargetEnvironment test` 或 `--target-environment test`，不要求正式服访问启用。
- BDD: 前端恢复数据可恢复到备份服务器 -> Given 前端提交 `restore-data`、`targetEnvironment=backup`、恢复候选和原因 / When 后端执行运维动作 / Then 后端记录环境为 `backup`，命令参数包含 `-TargetEnvironment backup` 或 `--target-environment backup`。
- BDD: 恢复数据禁止正式服目标 -> Given 前端或恶意调用提交 `restore-data targetEnvironment=prod` / When 后端校验动作 / Then 请求被阻断并记录 blocked 操作，不执行任何脚本。
- BDD: 恢复数据脚本禁止正式服目标 -> Given 脚本入口收到 `restore-data` 且目标不是 `test|backup` / When 解析目标环境 / Then 脚本直接阻塞，不能回落到 production。

## Milestones

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：新增 RED 测试覆盖后端 `restore-data` 目标环境和脚本 `backup` 支持。
- [x] M3：实现后端目标环境校验、命令参数、责任人默认值和脚本目标环境投影。
- [x] M4：运行目标单测、脚本测试和 backend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交后端改动。

## Expected Verification

- RED：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest" test` 先失败，指出 `restore-data` 不接收目标环境或责任人目标不匹配。
- RED：`python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_backup_ops_linux_runtime_tooling.py -q` 先失败，指出 `backup` 目标未支持。
- GREEN：上述命令通过。
- GREEN：backend API evidence validator 通过。
- GREEN：task-closeout-cleanup 预览通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少目标环境、目标为正式服或脚本配置缺失时直接阻塞。
- `是否从根因和长期维护角度解决`：是。将恢复目标环境纳入后端契约、操作记录、脚本参数和脚本投影，不靠前端隐藏或人工直调接口。
- `是否存在临时补丁或绕过`：否。不绕过后端责任人门禁、不降级到 production、不手写成功报告。

## 当前状态

completed

## 验证结果

- VERIFY：上一后端任务 `doc/tasks/20260604-commit-runtime-control-state/task.md` 状态为 `completed`。
- RED：后端目标测试失败，原因是 `restore-data` 仍把 `targetEnvironment` 判为非法参数，`test/backup` 默认数据责任人不存在。
- RED：脚本目标测试失败，原因是 PowerShell/Linux backup-ops 目标投影仍只支持 `test`，不支持 `backup`。
- GREEN：后端目标测试通过，51 tests，0 failures，0 errors。
- GREEN：脚本目标测试通过，57 passed。
- GREEN：backend API evidence validator 通过。
- CLOSEOUT PREVIEW：task-closeout-cleanup 预览通过，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-restore-target-backend/backend-api-evidence.md`
