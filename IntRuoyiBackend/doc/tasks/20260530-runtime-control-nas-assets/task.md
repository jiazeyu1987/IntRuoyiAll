# 任务：运行控制台 NAS 发布包与备份包隔离（后端）

## 目标

- 默认发布包根目录为 `Backup/ReleasePackage`。
- 默认备份点根目录为 `Backup/BackupPackage`。
- 发布、测试部署、测试通过、正式上线和应用回滚只读取发布包目录。
- 立即备份和恢复数据只读取备份点目录。
- 高危动作缺责任人、`PROD`、manifest、checksum、测试通过记录或现场快照时必须阻断。

## 里程碑

1. 补后端和脚本 RED 测试。
2. 修改运行控制台配置、脚本默认值和候选列表隔离。
3. 修改动作参数和校验，使按钮弹框输入能被后端严格验证。
4. 运行聚焦单测和脚本测试。

## 预期验证

- Maven 单测覆盖运行控制台服务、候选列表和默认配置。
- pytest 覆盖部署脚本、docker-compose、application-local、backup-ops 配置。

## 当前状态

已完成并已融合到后端 `int_main`。后端默认配置、部署脚本、docker-compose、application-local 和 backup-ops 配置均改为 `Backup/ReleasePackage` / `Backup/BackupPackage` 双目录模型，并补齐阻断操作留痕、备份目标环境、发布包 manifest/checksum 完整性筛选、后端发布动作复核、测试通过验证结论、正式服发布历史回滚门禁、测试服 NAS 挂载前置校验和远端 MinIO 对象写 NAS 的有限重试。

## 验证结果

- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlSpringWiringTest,RuntimeBackupDrillServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeControlCanonicalContractTest" test` -> PASS。
- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_nas_backup_root.py script\tests\test_backup_ops_tooling.py script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_mark_tested_current_release_tooling.py script\tests\test_backup_ops_real_integration_tooling.py -q` -> PASS。
- `mvn -pl yudao-server -am -DskipTests package` on backend `int_main` -> PASS。
- Restarted backend `int_main` on `http://127.0.0.1:48081` with explicit DCC download encryption runtime config and real MySQL/Redis SSH tunnel -> PASS。
- `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` from frontend `int_main` with `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081` -> PASS，`芋道源码/admin` 真实只读 E2E 覆盖 AC-01 至 AC-11。
