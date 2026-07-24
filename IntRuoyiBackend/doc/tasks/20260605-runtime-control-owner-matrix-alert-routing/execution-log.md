# 执行日志：补齐运行控制台责任人矩阵与告警路由

## BDD

- BDD: 默认容量告警责任人可用 -> Given 本机或任一运行环境产生 `storage-capacity-warning` / When 告警服务查找必填责任人 / Then `local/test/backup/prod` 均有默认 `ops-owner`，站内信不因缺少责任人矩阵而阻塞。
- BDD: 默认备份恢复责任人可用 -> Given `backup-now`、`backup-scheduled`、`restore-data`、`rollback-app` 或 `rehearsal` 产生操作/告警 / When Runtime Control 校验责任人 / Then 目标环境有默认必填责任人和升级路径。
- BDD: 显式配置仍可覆盖默认责任人 -> Given 运维人员配置某环境/动作/角色责任人 / When 查询 owner matrix / Then 显式配置覆盖默认 ownerUserId/ownerName，但保持必填语义。

## TDD Evidence

- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest#defaultOpsOwnersShouldCoverCapacityBackupAndRehearsalRoutes,RuntimeOpsAlertServiceImplTest#createAlertShouldSendLocalCapacityWarningToDefaultOpsOwner" test` -> FAIL，默认矩阵没有 `local/storage-capacity-warning`，本机容量告警仍被 `BLOCKED`。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest#defaultOpsOwnersShouldCoverCapacityBackupAndRehearsalRoutes,RuntimeOpsAlertServiceImplTest#createAlertShouldSendLocalCapacityWarningToDefaultOpsOwner" test` -> PASS，默认矩阵覆盖容量、备份、恢复开始/结束、演练路由，本机容量告警发送给默认 ops owner。
- REGRESSION: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest,RuntimeOpsAlertServiceImplTest" test` -> PASS，15 tests。
- REGRESSION: `python -m pytest script/tests/test_release_go_no_go_contract_docs.py::test_g10_alert_routing_runbook_defines_webhook_and_evidence_contract script/tests/test_release_go_no_go_contract_docs.py::test_go_no_go_doc_defines_g11_owner_matrix_contract -q` -> PASS，2 tests。
- REGRESSION: `python -m pytest script/tests/test_release_readiness_g10_g11_contracts.py -q` -> PASS，11 tests。
- REGRESSION: `git diff --check -- yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeOpsResponsibilityServiceImpl.java yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeOpsResponsibilityServiceImplTest.java yudao-module-infra/src/test/java/cn/iocoder/yudao/module/infra/service/runtimecontrol/RuntimeOpsAlertServiceImplTest.java doc/tasks/20260605-runtime-control-owner-matrix-alert-routing` -> PASS，仅提示 Git 将在下次触碰时把 LF 替换为 CRLF。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-runtime-control-owner-matrix-alert-routing --mode preview` -> ready，keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
