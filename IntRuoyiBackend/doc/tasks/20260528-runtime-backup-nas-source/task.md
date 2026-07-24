# 任务：运行控制台备份点改用 NAS 备份目录

## 任务目标

- 将运行控制台备份点列表、备份点详情、恢复候选和回滚候选的备份来源改为 NAS 管理配置中的 NAS 服务器备份文件夹。
- 复用 NAS 管理页后端的 NAS 访问能力，避免 Windows 本地进程按本机路径读取 `/mnt/nas/备份`。
- 保持 fail-fast：NAS 配置缺失、备份目录不存在或不可读时必须暴露明确错误，不得回退到本地文件系统或 mock 数据。

## BDD 场景

- BDD: NAS 备份目录列出备份点 -> Given NAS 管理配置可访问且备份文件夹下存在完整备份点 / When 用户打开运行控制台备份演练 / Then 后端通过 NAS 服务列出备份点并返回 manifest 摘要。
- BDD: NAS 备份目录生成恢复候选 -> Given NAS 备份文件夹下存在带 deploy/image-tag.txt 的备份点 / When 用户请求恢复或回滚候选 / Then 后端从 NAS 目录读取候选数据，不读取本机路径。
- BDD: NAS 前置条件缺失 fail fast -> Given NAS 管理配置缺失或 NAS 备份文件夹不可访问 / When 用户请求备份点或候选 / Then 接口返回明确阻塞错误，不得切换到本地 `/mnt/nas/备份`。

## 里程碑

- [x] M1：确认运行控制台备份点/候选当前本地文件读取逻辑与 NAS 管理页可复用服务。
- [x] M2：补充 RED 测试，证明当前实现仍读取本机 `backupPointsRoot`。
- [x] M3：实现通过 NAS 服务读取备份目录、manifest、image-tag 的最小正式方案。
- [x] M4：运行目标测试、回归测试和 backend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览，更新任务记录并按策略提交本任务改动。

## 预期验证

- RED: 目标后端测试先失败，失败原因为运行控制台未调用 `NasBrowserService` 读取 NAS 备份文件夹。
- GREEN: 目标后端测试通过。
- GREEN: 运行控制台候选/备份点相关单元测试通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260528-runtime-backup-nas-source/backend-api-evidence.md` 通过。
- GREEN: `git diff --check` 通过。

## 当前状态

completed

## 阻塞与处置

- 2026-05-28：用户提出更大的“发布包存储到 NAS，控制台按同一 ReleaseTag 从测试流转到正式”的需求。本任务只覆盖运行控制台备份点读取 NAS，范围不足以单独完成新的发布包流转目标。
- 处置：本任务方向被根仓库任务 `doc/tasks/20260528-nas-release-package-flow` 吸收；后续 NAS 访问统一复用 NAS 管理配置，避免两套实现。
- 2026-05-28：本次按用户当前要求恢复并完成窄范围实现：运行控制台备份点、恢复候选、回滚候选改为通过 NAS 管理配置读取 NAS `备份` 文件夹。

## 当前发现

- 最近后端任务 `20260528-showroom-company-v8-bundle-fix` 已完成，可开始本任务。
- 初步定位到运行控制台备份点接口位于 `yudao-module-infra`，当前 `RuntimeBackupDrillServiceImpl` 与 `RuntimeOpsCandidateServiceImpl` 使用 `Path.of(backupPointsRoot)` 和 `Files.list/readString` 读取本机文件系统。
- 已补充 RED 测试：运行控制台备份点列表必须通过 `NasBrowserService` 读取 `备份` NAS 目录，而不是通过本机 `Path` 读取。
- 已实现 `RuntimeBackupNasRepository`，运行控制台备份点、恢复候选和回滚候选统一通过 NAS 管理配置访问 `backup-ops.nas-backup-points-root`。

## 验证结果

- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest#listBackupPointsShouldReadBackupFolderThroughNasBrowserService" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 缺少 `BackupOps.setNasBackupPointsRoot(...)` 与 `RuntimeBackupDrillServiceImpl(RuntimeControlProperties, NasBrowserService)`，证明生产代码尚未接入 NAS 读取。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest#listBackupPointsShouldReadBackupFolderThroughNasBrowserService" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeOpsGuideServiceImplTest,RuntimeControlServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeOpsResponsibilityServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，49 tests。
- GREEN: `python -m pytest script\tests\test_runtime_control_nas_backup_root.py -q` -> PASS。
- NOTE: `python -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> FAIL，既有无关断言期望 `build-release` 动作，但当前 `RuntimeControlOperationAction.java` 不包含该动作；本次未修改该既有差异文件。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260528-runtime-backup-nas-source\backend-api-evidence.md` -> PASS。
- GREEN: `git diff --check` -> PASS。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-runtime-backup-nas-source --mode preview` -> PASS，`backend-api-evidence.md` 已列入 Cleanup Keep，无删除项。

## 最终结果

- 运行控制台备份点列表、备份点详情、恢复候选和回滚候选已改为通过 NAS 管理配置读取 NAS 备份目录。
- 新增配置 `yudao.runtime-control.backup-ops.nas-backup-points-root`，本地和测试服默认指向 NAS 相对目录 `备份`。
- 原 backup-ops 脚本使用的 Linux `backupPointsRoot` 不再被运行控制台页面读取逻辑当成本机路径访问。

## Cleanup Keep

- doc/tasks/20260528-runtime-backup-nas-source/backend-api-evidence.md
