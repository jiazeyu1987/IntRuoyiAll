# Execution Log

BDD: 缺少演练证据的真实备份点仍可用于恢复 -> Given `Backup/BackupPackage/<backupId>` 存在 manifest、checksum 和镜像标签 / When 恢复候选缺少 `manifest/rehearsal-report.json` 与 `manifest/现场快照.md` / Then 运行控制台仍应将该候选标记为 `AVAILABLE`。

BDD: 基础恢复证据缺失仍阻断 -> Given 备份点缺少 manifest、checksum 或镜像标签 / When 用户查询恢复候选 / Then 后端仍应返回 `BLOCKED` 并保留明确原因。

BDD: 备份点列表不再把演练证据作为可恢复条件 -> Given 备份点 manifest 与 checksum 有效但未演练 / When 查看 Backup 面板 / Then 备份点应显示为 `RECOVERABLE`，演练报告路径可为空。

CHANGE: 用户明确要求取消恢复演练限制，或默认恢复演练成功；本任务选择取消演练证据阻断，不采用默认成功，因为默认成功会伪造运维证据。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeRestoreCandidateServiceImplTest,RuntimeBackupDrillServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，13 tests 中 3 failures、1 error；失败原因是旧代码仍将缺少恢复演练报告或现场快照作为 BLOCKED / UNRECOVERABLE 条件。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRestoreCandidateServiceImplTest,RuntimeBackupDrillServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，13 tests，0 failures，0 errors。

VERIFY: `python -X utf8 C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260603-restore-rehearsal-gate.md` -> PASS。

VERIFY: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260603-restore-data-without-rehearsal-gate\backend-api-evidence.md` -> PASS。

VERIFY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-data-without-rehearsal-gate --mode preview` -> PASS，无删除项、无阻塞。
