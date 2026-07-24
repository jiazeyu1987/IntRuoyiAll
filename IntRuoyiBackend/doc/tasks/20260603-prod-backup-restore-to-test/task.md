# 任务：正式备份恢复到测试服务器

## 任务目标

确认正式服务器备份数据是否支持恢复/回滚到测试服务器正常 runtime；若当前不支持，则补齐 `restore-data` 的测试服务器目标环境能力，并保留现有恢复点、演练证据、现场快照与显式目标选择门禁。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-other-category-directory-binding/task.md`
- 状态：`completed`
- 影响：上一个任务已完成并提交；本任务只修改备份恢复脚本、相关测试与本任务文档，不接管 runtime 运行态文件。

## BDD 场景

- BDD: 正式备份可恢复到测试服务器 -> Given 测试服务器备份仓库存在已通过门禁的正式备份点 `<backupId>` / When 操作员执行 `restore-data` 并显式指定测试目标环境 / Then 恢复流程必须把目标 runtime、MySQL、MinIO、健康检查投影到测试服务器正常 runtime，而不是正式服务器或演练槽位。
- BDD: 高危恢复仍需显式恢复点 -> Given 操作员准备恢复到测试服务器 / When 未显式提供 `SelectedBackupId` 或 `--selected-backup-id` / Then 命令必须在停止服务、导入数据库或覆盖对象文件前失败，不得自动选择最近备份点。
- BDD: 非授权目标环境不扩散 -> Given 操作员指定 `TargetEnvironment=test` / When 模式不是已支持的 `backup-now` 或 `restore-data` / Then 启动器必须 fail fast，不能把应用回滚或其他动作隐式投影到测试服。

## Milestones

- [x] M1：确认已有能力与缺口。
- [x] M2：建立任务文档并记录 BDD 场景。
- [x] M3：先写 RED 测试，证明 `restore-data` 目前不能指定测试目标环境。
- [x] M4：最小实现 `restore-data` 的测试目标环境投影。
- [x] M5：运行 GREEN 与回归验证，更新恢复文档/证据。
- [x] M6：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- `python -m pytest script\tests\test_backup_ops_tooling.py -k target_environment`
- `python -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment`
- `python -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k target_environment`
- 任务日志必须包含 BDD、RED、GREEN/REGRESSION 证据。
- 本轮不执行真实服务器恢复；若需要真实恢复，必须另行获得明确授权并按 `docs/server-access.md` 与登录/恢复文档执行。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅开放显式 `restore-data` + 测试目标环境，不自动选择恢复点、不绕过恢复门禁。
- `是否从根因和长期维护角度解决`：是。沿用现有目标环境投影机制，让恢复流程的目标 runtime 从配置根上切换，而不是在各步骤硬编码测试服务器分支。
- `是否存在临时补丁或绕过`：否。不手写恢复报告、不跳过演练证据、不使用演练槽位冒充正常测试服务器恢复。

## 当前状态

completed

## 已确认结论

- 当前已有 `rehearsal` 能把备份恢复到测试服务器独立演练槽位 `/backup/int-ruoyi/rehearsal/runtime`，但该能力明确不覆盖测试服务器正常 runtime。
- 当前 `backup-ops.ps1` 与 `backup_ops_linux.py` 都只允许 `backup-now` 使用测试目标环境；`restore-data` 指定测试目标环境会被启动器阻断。
- 因此，用户询问的“正式服务器备份数据恢复/回滚到测试服务器正常 runtime”在当前脚本入口不完整，需要补齐。

## 验证结果

- RED：`python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k target_environment` -> FAIL，PowerShell 启动器缺少 `restore-data` 测试目标环境允许列表。
- RED：`python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment` -> FAIL，Linux-local 入口缺少 `restore-data` 测试目标环境允许列表。
- RED：`python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k target_environment` -> FAIL，`project_target_environment(config, "restore-data", "test")` 当前返回 blocked。
- GREEN：`python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k target_environment` -> PASS。
- GREEN：`python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py -k target_environment` -> PASS。
- GREEN：`python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py -k target_environment` -> PASS。
- GREEN：`python -X utf8 -m pytest script\tests\test_release_go_no_go_contract_docs.py -k restore_data_runbook` -> PASS。
- REGRESSION：`python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_backup_ops_linux_runtime_rollback_tooling.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_manifest_tooling.py script\tests\test_backup_ops_scheduling_tooling.py script\tests\test_release_go_no_go_contract_docs.py` -> PASS，72 passed。
- GREEN：`powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_rehearsal_evidence.ps1` -> PASS。
- GREEN：`powershell -NoProfile -ExecutionPolicy Bypass -File .\script\tests\test_backup_ops_manifest_ports.ps1` -> PASS。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\backup-disaster-recovery-readiness\scripts\validate_backup_disaster_recovery.py --evidence docs\recovery\backup-disaster-recovery.md` -> PASS。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-prod-backup-restore-to-test --mode preview` -> READY，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Blockers

- none.
