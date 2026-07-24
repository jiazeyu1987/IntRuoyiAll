# 执行日志：运行控制回滚兼容性证据闭环

BDD: mark-tested 生成兼容证据 -> Given 测试服当前发布包已通过真实恢复集验证且发布包 manifest 为 code-only / When 操作员执行 `mark-tested` / Then 脚本写入 `tested.json` 与 `rollback-compatibility.json status=COMPATIBLE`，包含 `packageDirectoryName`、`checkedAt`、`summary` 和恢复集证据。

BDD: 非 app-only 证据保持阻塞 -> Given 发布包 manifest 为 `with-data` 或恢复集程序版本与发布包目录不一致 / When 操作员执行 `mark-tested` / Then 脚本写入 `BLOCKED` 兼容性证据，候选继续不可用。

BDD: 候选校验严格消费证据 -> Given `rollback-compatibility.json` 缺少 `packageDirectoryName`、`checkedAt` 或 `summary` / When 后端候选服务或回滚脚本扫描候选 / Then 候选保持 `BLOCKED` 或被跳过，不允许真实回滚动作继续。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_mark_tested_generates_formal_rollback_compatibility_evidence script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_mark_tested_blocks_rollback_compatibility_without_code_only_matching_recovery_set script/tests/test_backup_ops_tooling.py::test_rollback_tag_scan_requires_complete_compatibility_evidence_contract -q` -> FAIL, expected missing `New-RollbackCompatibilityEvidence`, `Write-NasRollbackCompatibilityEvidence`, and complete rollback compatibility field checks.

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeRollbackCandidateServiceImplTest#listRollbackCandidatesShouldBlockCompatibilityEvidenceWithoutPackageDirectoryName test` -> FAIL, expected backend candidate service still treated compatibility evidence without `packageDirectoryName` as available.

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_backup_ops_tooling.py::test_rollback_tag_scan_requires_complete_compatibility_evidence_contract script/tests/test_mark_tested_current_release_tooling.py -q` -> PASS, 59 passed.

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeRollbackCandidateServiceImplTest test` -> PASS, 15 tests.

GREEN: PowerShell parser check for `script/deploy/publish-int-ruoyi.ps1` and `script/backup-ops/scripts/modules/Infra/DockerOps.psm1` -> PASS.

GREEN: `git diff --check` -> PASS.

GREEN: task-closeout-cleanup preview -> PASS, no blocked cleanup paths; evidence helper files were preview-only and not applied.
