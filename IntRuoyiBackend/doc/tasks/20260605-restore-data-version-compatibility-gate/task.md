# 任务：restore-data 恢复前版本一致性门禁

## 任务目标

根据根任务 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260604-dr-recovery-rollback-gap-audit` 的 P0/F1 缺口，补齐 `restore-data` 在执行停服、导入数据库、覆盖对象文件前的恢复集一致性门禁。恢复集必须能证明程序镜像版本、Redis 恢复策略与运行配置范围一致；无正式兼容证明时采用精确 `IMAGE_TAG` 一致策略，缺失或不一致直接阻塞。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260605-backend-runtime-base-local-config/task.md`
- 状态：`blocked`
- 处理：已记录 Docker 构建内部基础镜像时 Ubuntu apt 源不可用，不能生成可信 tar、sha256、image id，也不能写入假环境变量。本任务不依赖该基础镜像配置产物，只修改恢复前门禁。

## BDD 场景

- BDD: 恢复集程序版本不一致必须阻塞 -> Given 目标运行时 `IMAGE_TAG` 与所选备份 `recoverySet.program.imageTag` 不一致 / When 执行 `restore-data` / Then 在停服、预恢复快照、数据库导入、对象覆盖前失败，并提示当前版本与恢复集版本。
- BDD: 恢复集缺少 Redis 或配置范围必须阻塞 -> Given 所选备份 `recoverySet` 缺少 `redis.policy` 或 `configuration.manifestPath`、`configuration.composePath`、`checksums.sha256` / When 执行 `restore-data` / Then 在任何高风险操作前失败，并提示缺失字段。
- BDD: 恢复集版本一致且范围完整才可继续 -> Given 目标运行时 `IMAGE_TAG` 与恢复集 `program.imageTag` 完全一致，且 Redis、配置、校验字段完整 / When 执行 `restore-data` / Then 允许进入既有预恢复快照、停服、导入、恢复对象、启动和验证流程。

## Milestones

- [x] M1：确认上一任务 blocked，不依赖其产物。
- [x] M2：写入 Linux `restore_data` RED 测试，证明版本不一致和范围缺失会在高风险操作前阻塞。
- [x] M3：实现 Linux 恢复前门禁。
- [x] M4：补齐 PowerShell `Invoke-RestoreDataUseCase` 同等契约和测试。
- [x] M5：运行受影响测试并记录 GREEN/REGRESSION 证据；Playwright 真实恢复/回滚演练移交父任务后续切片。
- [x] M6：cleanup 预览已通过；本任务改动随后单独提交。

## Expected Verification

- `python -m pytest script/tests/test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_program_image_mismatch_before_actions`
- `python -m pytest script/tests/test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_requires_recovery_scope_before_actions`
- `python -m pytest script/tests/test_backup_ops_linux_runtime_ports.py::test_linux_restore_data_blocks_manifest_without_recovery_set_before_actions`
- `python -m pytest script/tests/test_backup_ops_tooling.py::<PowerShell restore-data compatibility test>`
- `git diff --check -- script/backup-ops/linux/backup_ops_linux.py script/backup-ops/scripts/modules/UseCases/RestoreData.psm1 script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_backup_ops_tooling.py doc/tasks/20260605-restore-data-version-compatibility-gate`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少恢复集字段、当前运行时版本或版本不一致时直接失败。
- `是否从根因和长期维护角度解决`：是。把恢复前必须证明的程序版本、Redis 策略和配置范围固化为门禁，避免“只恢复数据成功但程序/配置不匹配”的长期风险。
- `是否存在临时补丁或绕过`：否。不增加兼容表默认放行，不允许自动忽略版本差异。

## 当前状态

completed

## Current Status

completed

## E2E Handoff

- 本任务不执行真实 `restore-data` 破坏性恢复；该路径必须在父任务后续 test/backup 恢复演练中通过 Playwright 真实用户路径验证。
- 禁止用 mock 路由、接口直调或假成功报告替代 Playwright 恢复/回滚演练。

## Cleanup Keep

- `doc/tasks/20260605-restore-data-version-compatibility-gate/task.md`
- `doc/tasks/20260605-restore-data-version-compatibility-gate/execution-log.md`
