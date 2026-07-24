# 执行日志：验证 with-data 发布包携带 DCC 文件数据

## BDD

- BDD: with-data 发布包携带 DCC 文件对象 -> Given 本机 MinIO `yudao` bucket 中存在对象 / When 使用 NAS 配置构建 `with-data` 发布包 / Then 发布包中存在 `minio/yudao` 快照，DCC object inventory/hash 证据存在且列出对象。
- BDD: with-data 发布包携带数据库 dump -> Given 发布包未传 `-SkipDatabaseSync` / When 构建完成 / Then 发布包中存在 `ruoyi-vue-pro-current.sql`，manifest 标记 `publishScope=with-data`。
- BDD: evidence 可复核 -> Given 发布包构建完成 / When 文档合同测试读取 evidence JSON / Then evidence 必须记录 releaseTag、NAS 配置名、manifest、MinIO 快照、DCC inventory、对象数量和本地清理状态。

## TDD Evidence

- RED: `python -m pytest script/tests/test_with_data_release_package_dcc_evidence.py -q` -> FAIL，`with-data-release-package-evidence.json` 不存在，证明尚无可复核的 with-data/DCC 入包 E2E 证据。
- RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_with_data_release_package_requires_dcc_object_inventory -q` -> FAIL，发布脚本缺少 stdin 输入 helper，`Invoke-LocalMySqlRaw` 仍使用 `mysql -e` 传多行 SQL，真实构建时 SQL 中 `-1` 被 mysql 解析成非法命令参数。
- GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_with_data_release_package_requires_dcc_object_inventory -q` -> PASS，`Invoke-LocalMySqlRaw` 改为 `docker exec -i` 并通过 stdin 传入 DCC SQL。
- RED: 真实 `with-data` 构建 `20260605_with_data_dcc_verify_023834` -> FAIL，PowerShell 5.1 环境中 `ProcessStartInfo.ArgumentList` 不可用，带 stdin helper 不能构造参数。
- GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_with_data_release_package_requires_dcc_object_inventory -q` -> PASS，新增 `ConvertTo-ProcessArgumentString`，缺少 `ArgumentList` 时使用安全参数串。
- RED: 真实 `with-data` 构建 -> FAIL，DCC inventory fail-fast 报告 `DCC_OBJECT_SNAPSHOT_MISSING`，`file id=9197255881690` 指向 `/codex-e2e/...original.pdf`，该对象不存在于 MinIO 快照。
- RED: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py::test_t4_fixture_infra_file_insert_writes_matching_minio_object -q` -> FAIL，T4 fixture 只写 `infra_file` 元数据，未写 MinIO 对象。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py::test_t4_fixture_infra_file_insert_writes_matching_minio_object -q` -> PASS，T4 fixture 插入 `infra_file` 前写入真实 MinIO PDF 对象。
- GREEN: 历史 T4 测试租户数据清理 -> PASS，软删 96 个 T4 受控文件、288 个 T4 `infra_file`，并软删 109 条分发接收、184 条分发、14 条签名子记录；剩余活跃 T4 `/codex-e2e/` 引用为 0。
- RED: 真实 `with-data` 构建 `20260605_with_data_dcc_verify_023834` -> FAIL，DCC inventory fail-fast 报告 `DCC_INFRA_FILE_MISSING`，旧 T2 上传 E2E 活跃行引用已软删 `infra_file id=9198354884974/9198354884975`。
- GREEN: 历史 T2 上传 E2E 测试租户数据清理 -> PASS，软删 2 个 `DCC-E2E-UP-*` 受控文件、2 个 master、4 条临时文件记录；剩余活跃 DCC 引用缺失 `infra_file` 数为 0。
- GREEN: DCC DB/MinIO 预检 -> PASS，活跃 DCC 引用 944 个，MinIO `yudao` 对象 4527 个，缺失对象 0。
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\deploy\publish-int-ruoyi.ps1 -Mode build-release -Component backend -ReleaseTag 20260605_with_data_dcc_verify_025547 -NasConfigPath .\runtime\runtime-control\nas-release-config\manual-backup-publish-20260601-005831.json -NasReleaseRoot Backup/ReleasePackage -BackendRuntimeBaseMode offline-tar ...` -> PASS，工具 30 分钟超时后子进程继续运行并完成；复核 NAS 包与本地包均为 4542 个文件、13911586307 bytes。
- GREEN: NAS 关键文件复核 -> PASS，`release-manifest.json`、`manifest/dcc-object-inventory.json`、`ruoyi-vue-pro-current.sql`、`minio/yudao` 均存在。
- GREEN: evidence 生成与本地临时包清理 -> PASS，`with-data-release-package-evidence.json` 记录 `minioYudaoObjectCount=4527`、`dccObjectInventoryObjectCount=944`、`dccObjectInventoryMissingObjectCount=0`，本地 release dir 已删除。
- GREEN: `python -X utf8 -m pytest script/tests/test_with_data_release_package_dcc_evidence.py script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_with_data_release_package_requires_dcc_object_inventory script/tests/test_dcc_screenshot_admin_policy_e2e.py::test_t4_fixture_infra_file_insert_writes_matching_minio_object -q` -> PASS，3 passed。
- GREEN: PowerShell parse `script/deploy/publish-int-ruoyi.ps1` -> PASS，`POWERSHELL_PARSE_OK`。
- GREEN: `git diff --check -- script/deploy/publish-int-ruoyi.ps1 script/e2e/dcc_screenshot_admin_policy_e2e.py script/tests/test_dcc_screenshot_admin_policy_e2e.py script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_with_data_release_package_dcc_evidence.py doc/tasks/20260605-with-data-release-package-dcc-e2e` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-with-data-release-package-dcc-e2e --mode preview` -> PASS，keep task/execution-log/evidence，delete none，blocked none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260605-with-data-release-package-dcc-e2e --mode apply` -> PASS，delete none，blocked none。
