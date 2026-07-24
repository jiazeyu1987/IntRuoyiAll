# 执行日志：修复运行控制台混滚版本缺少发布责任人

BDD: 默认发布责任人允许提交混滚版本 -> Given 运行控制台责任矩阵没有显式保存 `release-owner` / When 用户点击“混滚版本/回滚版本”并提交操作 / Then 后端应使用默认发布责任人 `admin` 通过责任人校验，并在操作记录中保留责任人信息。

BDD: 显式责任矩阵优先生效 -> Given 运维人员已为 `prod + rollback-app + release-owner` 保存责任人 / When 用户提交混滚版本/回滚版本 / Then 后端应使用显式责任人，不得被默认 `admin` 覆盖。

## 证据

- 2026-06-02：已将上一后端任务 `20260602-dcc-download-failure` 标记为 blocked，开始定位运行控制台默认发布责任人问题。
- 2026-06-03：用户要求继续当前任务；确认根因为 `RuntimeOpsResponsibilityServiceImpl#getOwnerMatrix()` 只返回显式责任矩阵，空矩阵下 `rollback-app/release-owner` 不存在，前端与后端均会阻断提交。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest#defaultReleaseOwnerShouldBeVisibleForProductionReleaseActions+executeRollbackWithDefaultReleaseOwnerShouldReachCandidateValidation+missingRequiredDataOwnerShouldBlockRestoreDataBeforeDispatch" test` -> FAIL，默认发布责任人矩阵实际 0 条，执行 `rollback-app` 仍先报“责任人”缺失。
- INFO: 已在 `RuntimeOpsResponsibilityServiceImpl` 合并默认发布责任人矩阵：`prod/promote-prod/release-owner`、`backup/promote-backup/release-owner`、`prod/rollback-app/release-owner` 默认 `ownerUserId=1`、`ownerName=admin`；显式非空发布责任人仍覆盖默认值。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest#defaultReleaseOwnerShouldBeVisibleForProductionReleaseActions+executeRollbackWithDefaultReleaseOwnerShouldReachCandidateValidation+missingRequiredDataOwnerShouldBlockRestoreDataBeforeDispatch" test` -> PASS，3 tests passed。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#executeRestoreDataShouldUseDetachedLinuxLocalRunnerWhenConfigured" test` -> PASS，1 test passed；确认恢复数据执行链路不受默认发布责任人改动影响。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsResponsibilityServiceImplTest,RuntimeControlServiceImplTest" test` -> PASS，37 tests passed。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-runtime-control-mixed-rollout-default-owner\bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-runtime-control-mixed-rollout-default-owner --mode preview` -> PASS，keep 为 `task.md`、`execution-log.md`、`bug-regression-evidence.md`，delete/blocked/warnings 均为空。
