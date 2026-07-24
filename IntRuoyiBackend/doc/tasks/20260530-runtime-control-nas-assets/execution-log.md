# 执行日志

BDD: 发布包目录隔离 -> Given NAS 同时存在 `ReleasePackage` 和 `BackupPackage`, When 后端列出发布包或回滚候选, Then 只读取 `Backup/ReleasePackage`。
BDD: 备份点目录隔离 -> Given NAS 同时存在 `ReleasePackage` 和 `BackupPackage`, When 后端列出恢复候选或执行备份, Then 只读取或写入 `Backup/BackupPackage`。
BDD: 高危动作门禁 -> Given 用户执行正式上线、正式备份、回滚或恢复, When 缺责任人、`PROD` 或候选校验不通过, Then 后端拒绝执行命令并记录阻断原因。
BDD: 阻断动作留痕 -> Given 用户提交高危动作但缺少必填前置条件, When 后端拒绝执行, Then 操作记录状态为 `blocked`，日志写入阻断原因，并且不派发命令。
BDD: 备份源环境显式选择 -> Given 用户执行立即备份, When 请求携带 `targetEnvironment=test` 或 `prod`, Then 后端命令和 backup-ops 脚本按所选环境生成真实备份点。
BDD: 测试通过记录含结论 -> Given 测试服当前 releaseTag 可识别, When 用户标记测试通过, Then tested marker 包含验证结论、操作者和 releaseTag。
BDD: 正式服发布历史门禁 -> Given 发布包从未在正式服成功上线, When 用户选择回滚版本, Then 回滚候选状态为 `BLOCKED` 并提示缺少正式服发布历史。
BDD: 发布动作后端复核 -> Given 直接调用 `publish-test` 或 `promote-prod`, When 发布包缺 manifest、checksum 或 tested marker, Then 后端在派发脚本前阻断并记录 blocked 操作。

## 证据

- 2026-05-30: 创建后端任务文档，等待 RED 测试。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#defaultRuntimeControlPropertiesShouldSeparateReleaseAndBackupNasRoots+getReleasePackagesShouldListDirectoriesFromReleaseRepositoryInDescendingOrder+executeBuildReleaseShouldUseNasConfigSnapshotAndPersistAudit" test` -> FAIL, 旧实现仍使用 `Backup` 作为发布包和备份点根目录。
- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py::test_unified_publish_script_targets_test_and_production_with_explicit_confirmation script\tests\test_runtime_control_nas_backup_root.py::test_runtime_control_uses_nas_backup_points_root_config script\tests\test_backup_ops_tooling.py::test_backup_ops_example_configs_exist_without_inline_secrets -q` -> FAIL, 发布脚本、compose/yaml、backup-ops 示例配置仍使用旧目录。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#defaultRuntimeControlPropertiesShouldSeparateReleaseAndBackupNasRoots+getReleasePackagesShouldListDirectoriesFromReleaseRepositoryInDescendingOrder+executeBuildReleaseShouldUseNasConfigSnapshotAndPersistAudit" test` -> PASS。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py::test_unified_publish_script_targets_test_and_production_with_explicit_confirmation script\tests\test_runtime_control_nas_backup_root.py::test_runtime_control_uses_nas_backup_points_root_config script\tests\test_backup_ops_tooling.py::test_backup_ops_example_configs_exist_without_inline_secrets -q` -> PASS。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest,RuntimeBackupDrillServiceImplTest" test` -> PASS，48 个后端运行控制台相关单测通过。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_nas_backup_root.py script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_backup_ops_real_integration_tooling.py -q` -> PASS，65 个脚本与配置测试通过。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> FAIL, `targetEnvironment`、发布包完整性字段和 blocked 操作记录尚未实现。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> PASS，25 个运行控制台服务用例通过。
- RED: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py::test_backup_ops_launcher_projects_backup_now_source_environment_explicitly script\tests\test_backup_ops_linux_runtime_tooling.py::test_linux_runtime_projects_backup_now_source_environment_explicitly -q` -> FAIL, backup-ops 入口尚不支持显式目标环境投影。
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py::test_backup_ops_launcher_projects_backup_now_source_environment_explicitly script\tests\test_backup_ops_linux_runtime_tooling.py::test_linux_runtime_projects_backup_now_source_environment_explicitly script\tests\test_mark_tested_current_release_tooling.py -q` -> PASS。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest,RuntimeBackupDrillServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlCanonicalContractTest" test` -> PASS，61 个后端运行控制台相关单测通过。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_nas_backup_root.py script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_mark_tested_current_release_tooling.py script\tests\test_backup_ops_real_integration_tooling.py -q` -> PASS，69 个脚本与配置测试通过。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest" test` -> FAIL, 回滚候选尚未暴露并校验 `prod-latest.json` 正式服发布历史。
- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py::test_release_package_mode_builds_once_and_deploys_without_rebuild -q` -> FAIL, 正式服上线脚本尚未写入 `prod-latest.json` / `prod-deployments`。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeRollbackCandidateServiceImplTest" test` -> PASS，13 个回滚候选用例通过。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest,RuntimeBackupDrillServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlCanonicalContractTest" test` -> PASS，62 个后端运行控制台相关单测通过。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_nas_backup_root.py script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_mark_tested_current_release_tooling.py script\tests\test_backup_ops_real_integration_tooling.py -q` -> PASS，69 个脚本与配置测试通过。
- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> FAIL, 后端尚未自行复核发布包 manifest/checksum/tested。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> PASS，27 个运行控制台服务用例通过。
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest,RuntimeBackupDrillServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlCanonicalContractTest" test` -> PASS，64 个后端运行控制台相关单测通过。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_nas_backup_root.py script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_mark_tested_current_release_tooling.py script\tests\test_backup_ops_real_integration_tooling.py -q` -> PASS，69 个脚本与配置测试通过。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_nas_backup_root.py script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_backup_ops_real_integration_tooling.py -q` -> PASS，77 个发布与备份脚本测试通过。
- GREEN: real backup point `20260530-233026` -> PASS，测试租户真实“立即备份”成功写入 `Backup/BackupPackage/20260530-233026`，包含 MySQL、MinIO 对象、manifest 和 checksums。
- GREEN: real release package `26-05-31 00:33:43` -> PASS，测试租户真实“构建发布包”成功写入 `Backup/ReleasePackage/26-05-31_00-33-43`。
- MERGE: backend `codex/20260530-runtime-control-nas-assets` -> `int_main` -> PASS，merge commit `bf05e8126d`。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` on backend `int_main` -> PASS。
- CONFIG: backend `int_main` restarted on `http://127.0.0.1:48081` with explicit local DCC download encryption runtime config and restored SSH tunnel for real MySQL/Redis -> PASS。
- GREEN: `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` from frontend `int_main` with `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081` -> PASS，融合后 `芋道源码/admin` 真实只读 E2E 覆盖 AC-01 至 AC-11。
