# 任务：NAS 发布包流转后端与发布脚本

## 目标

将后端运行控制台和统一发布脚本改为 NAS 发布包流转：本机构建一次发布包存到 NAS，测试服部署该 ReleaseTag 并标记测试通过，正式服只能部署同一个已验证 ReleaseTag。

## 里程碑

- [x] M1：补充脚本和运行控制动作 RED 测试。
- [x] M2：实现 `build-release`、`deploy-release`、`mark-tested` 三种脚本模式。
- [x] M3：后端运行控制动作支持 ReleaseTag、NAS 管理配置复用、正式服 `-RequireTested` 门禁。
- [x] M4：运行 PowerShell、Python 和 Java 相关验证。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_ops_scripts.py -q`
- PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1`
- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeBackupDrillServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlCanonicalContractTest,RuntimeControlHighRiskActionContractTest" test`

## 当前状态

completed

## Current Status

completed

## 验证结果

- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_ops_scripts.py -q` -> PASS，23 passed。
- PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS。
- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeBackupDrillServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlCanonicalContractTest,RuntimeControlHighRiskActionContractTest" test` -> PASS，34 passed。

## Cleanup Candidates

- runtime/

## Cleanup Keep

- doc/tasks/20260528-nas-release-package-flow/task.md
- doc/tasks/20260528-nas-release-package-flow/execution-log.md
