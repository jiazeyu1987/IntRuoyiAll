# 执行日志：运行控制后端恢复集候选契约

- BDD: 后端屏蔽不完整恢复集 -> Given 备份 manifest 缺少 `recoverySet` 或必需组件 / When 查询恢复候选 / Then 候选状态为 `BLOCKED` 且不可执行。
- BDD: 后端暴露完整恢复集字段 -> Given 备份 manifest 包含完整 `recoverySet` / When 查询恢复候选 / Then 返回恢复集 ID、状态、程序版本、Redis 策略、配置清单、manifest hash 和组件摘要。
- BDD: 后端恢复动作使用恢复集候选 -> Given 操作员提交 `restore-data` 和 `selectedRecoverySetCandidateId` / When 后端校验通过 / Then 脚本参数投射恢复集 ID、hash、imageTag 和 Redis 策略。
- BDD: 后端屏蔽缺少兼容性证据的回滚包 -> Given 发布包没有 `rollback-compatibility.json` 或 `status != COMPATIBLE` / When 查询回滚候选或执行回滚 / Then 候选不可用且动作被阻断。

## Evidence

- RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCanonicalContractTest,RuntimeRestoreCandidateServiceImplTest,RuntimeRollbackCandidateServiceImplTest" test` -> FAIL, expected reason: missing `selectedRecoverySetCandidateId`, restore recovery-set response fields, and rollback compatibility evidence response fields.
- RED: `python -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> FAIL, expected reason: manifest lacked `recoverySet`, Linux restore did not block legacy manifest, rollback did not require compatible evidence.
- GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCanonicalContractTest,RuntimeRestoreCandidateServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeControlServiceImplTest,RuntimeControlHighRiskActionContractTest,RuntimeOpsGuideServiceImplTest,RuntimeOpsResponsibilityServiceImplTest" test` -> PASS, 84 tests.
- GREEN: `python -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py script/tests/test_backup_ops_manifest_tooling.py script/tests/test_mark_tested_current_release_tooling.py script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS, 122 tests.
- GREEN: backend API evidence validator -> PASS.
- GREEN: UTF-8 readback -> PASS.
- GREEN: task-closeout-cleanup preview -> PASS, keep only; no delete, blocked or warnings.
- REGRESSION: `git diff --check` -> PASS.
- GREEN: `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_VERIFY_TENANT='芋道源码'; $env:RUNTIME_CONTROL_E2E_VERIFY_USERNAME='admin'; $env:RUNTIME_CONTROL_E2E_VERIFY_PASSWORD='admin123'; node tests/e2e/runtime-control-yudao-admin-readonly.e2e.js` -> PASS, `AC-04 rollbackCandidates=22`, `AC-05 restoreCandidates=8`, `YUDAO_ADMIN_READONLY_PASS`; script asserted no non-GET runtime-control requests.
