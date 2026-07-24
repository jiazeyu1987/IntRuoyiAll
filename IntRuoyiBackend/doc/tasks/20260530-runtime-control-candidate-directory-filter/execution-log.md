# Execution Log

BDD: 回滚候选过滤普通目录 -> Given NAS `Backup` 根目录同时包含 `reference`、`26-05-30_00-11-31` 和真实备份点目录 / When 运行控制台加载回滚候选 / Then 服务端只返回真实备份点目录，普通目录不显示为已阻断候选。

BDD: 恢复候选过滤普通目录 -> Given NAS `Backup` 根目录同时包含普通目录和真实备份点目录 / When 运行控制台加载恢复候选 / Then 服务端只返回真实备份点目录，避免用非备份点制造 manifest/checksum 阻断信息。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> FAIL，`listRollbackCandidatesShouldIgnoreNonBackupPointDirectories` 和 `listRestoreCandidatesShouldIgnoreNonBackupPointDirectories` 均返回 3 个候选而不是 1 个，证明 `reference`、`26-05-30_00-11-31` 等普通目录被误纳入候选。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest" test` -> PASS，18 tests，新增回滚/恢复候选目录过滤用例均通过。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeBackupDrillServiceImplTest" test` -> PASS，22 tests，确认共享 NAS 备份点枚举入口未破坏备份点列表服务。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260530-runtime-control-candidate-directory-filter\bug-regression-evidence.md` -> PASS，缺陷证据满足 bug-regression contract。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260530-runtime-control-candidate-directory-filter --mode preview` -> PASS，cleanup 预览 keep `task.md`、`execution-log.md`、`bug-regression-evidence.md`，delete/blocked/warnings 均为 `<none>`。
