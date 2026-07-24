# 执行日志：运行控制台备份点改用 NAS 备份目录

- BDD: NAS 备份目录列出备份点 -> Given NAS 管理配置可访问且备份文件夹下存在完整备份点 / When 用户打开运行控制台备份演练 / Then 后端通过 NAS 服务列出备份点并返回 manifest 摘要。
- BDD: NAS 备份目录生成恢复候选 -> Given NAS 备份文件夹下存在带 deploy/image-tag.txt 的备份点 / When 用户请求恢复或回滚候选 / Then 后端从 NAS 目录读取候选数据，不读取本机路径。
- BDD: NAS 前置条件缺失 fail fast -> Given NAS 管理配置缺失或 NAS 备份文件夹不可访问 / When 用户请求备份点或候选 / Then 接口返回明确阻塞错误，不得切换到本地 `/mnt/nas/备份`。

- BLOCKED: superseded by NAS release package flow -> 用户要求发布包存储到 NAS 并按同一 ReleaseTag 从测试服流转到正式服；本任务只覆盖备份点读取 NAS，已被根仓库任务 `20260528-nas-release-package-flow` 吸收。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest#listBackupPointsShouldReadBackupFolderThroughNasBrowserService" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 缺少 `BackupOps.setNasBackupPointsRoot(...)` 与 `RuntimeBackupDrillServiceImpl(RuntimeControlProperties, NasBrowserService)`。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest#listBackupPointsShouldReadBackupFolderThroughNasBrowserService" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeBackupDrillServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeOpsGuideServiceImplTest,RuntimeControlServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeOpsResponsibilityServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 49 tests。
- GREEN: `python -m pytest script\tests\test_runtime_control_nas_backup_root.py -q` -> PASS。
- NOTE: `python -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> FAIL, 既有无关断言期望 `build-release` 动作，但当前 `RuntimeControlOperationAction.java` 不包含该动作。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260528-runtime-backup-nas-source\backend-api-evidence.md` -> PASS。
- GREEN: `git diff --check` -> PASS。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-runtime-backup-nas-source --mode preview` -> PASS, `backend-api-evidence.md` 已列入 Cleanup Keep，无删除项。
