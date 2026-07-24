# 任务：验证 with-data 发布包携带 DCC 文件数据

## 任务目标

根据根任务 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260604-dr-recovery-rollback-gap-audit` 和用户关于“带数据发布是否会带 DCC 文件数据”的问题，使用本机配置执行一次 `with-data` 发布包构建验证：发布包必须包含 MySQL dump、MinIO `yudao` 对象快照、DCC object inventory/hash 证据，并写入可复核 evidence。该验证只构建和上传发布包，不部署、不重启测试服/备份服/正式服。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260605-current-dr-readiness-doc-refresh/task.md`
- 状态：`completed`
- 处理：上一任务已完成并提交 `00f3ab4e3f`；本任务只执行发布包构建与证据检查，不修改服务器运行环境。

## BDD 场景

- BDD: with-data 发布包携带 DCC 文件对象 -> Given 本机 MinIO `yudao` bucket 中存在对象 / When 使用 NAS 配置构建 `with-data` 发布包 / Then 发布包中存在 `minio/yudao` 快照，DCC object inventory/hash 证据存在且列出对象。
- BDD: with-data 发布包携带数据库 dump -> Given 发布包未传 `-SkipDatabaseSync` / When 构建完成 / Then 发布包中存在 `ruoyi-vue-pro-current.sql`，manifest 标记 `publishScope=with-data`。
- BDD: evidence 可复核 -> Given 发布包构建完成 / When 文档合同测试读取 evidence JSON / Then evidence 必须记录 releaseTag、NAS 配置名、manifest、MinIO 快照、DCC inventory、对象数量和本地清理状态。

## Milestones

- [x] M1：确认上一任务 completed，确认 NAS config 与本机 Docker MinIO/MySQL 存在。
- [x] M2：新增 RED evidence 合同测试。
- [x] M3：执行 with-data 发布包构建并生成 evidence。
- [x] M4：运行 evidence 合同测试、发布包检查和 diff check。
- [x] M5：cleanup 预览并提交本任务改动。

## Expected Verification

- `python -m pytest script/tests/test_with_data_release_package_dcc_evidence.py -q`
- `powershell -ExecutionPolicy Bypass -File .\script\deploy\publish-int-ruoyi.ps1 -Mode build-release -Component backend -ReleaseTag <tag> -NasConfigPath .\runtime\runtime-control\nas-release-config\manual-backup-publish-20260601-005831.json -NasReleaseRoot Backup/ReleasePackage ...`
- `git diff --check -- script/tests/test_with_data_release_package_dcc_evidence.py doc/tasks/20260605-with-data-release-package-dcc-e2e`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺 NAS 配置、base image、MySQL、MinIO、manifest、对象快照或 DCC inventory 时均记录失败，不改为 code-only。
- `是否从根因和长期维护角度解决`：是。使用真实发布脚本和真实本机 MinIO/MySQL 验证 with-data 发布包内容。
- `是否存在临时补丁或绕过`：否。不部署、不重启远端；仅构建发布包并保留 evidence。

## 当前状态

completed

## Current Status

completed

## 完成内容

- 修复 `publish-int-ruoyi.ps1` 本机 DCC inventory SQL 执行路径：DCC 多行 SQL 改为经 `docker exec -i` stdin 输入，避免 `mysql -e` 将 SQL 中的 `-1` 误解析为命令参数。
- 修复 PowerShell 5.1 兼容问题：新增带 stdin 的进程执行 helper，并在缺少 `ProcessStartInfo.ArgumentList` 时使用安全命令行参数串。
- 修复 T4 管理策略 E2E fixture 根因：插入 `infra_file` 前同步向本机 MinIO `yudao/codex-e2e/` 写入真实 PDF fixture 对象，后续不会再生成 DB-only DCC 文件引用。
- 清理历史测试租户脏数据：软删 96 个 T4 历史受控文件、288 个 T4 `infra_file`、2 个 T2 上传 E2E 活跃遗留，以及相关分发/签收/签名/临时文件记录；清理范围仅限 `测试租户`。
- 真实构建 `with-data` 后端发布包 `20260605_with_data_dcc_verify_025547`，并上传到 NAS `Backup/ReleasePackage/20260605_with_data_dcc_verify_025547`。
- 生成 `with-data-release-package-evidence.json`，记录 MySQL dump、MinIO `yudao` 快照、DCC inventory/hash、NAS 上传一致性和本地临时包清理状态。

## 验证结果

- RED：`python -X utf8 -m pytest script/tests/test_with_data_release_package_dcc_evidence.py -q` -> FAIL，evidence JSON 不存在。
- RED：`python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py::test_t4_fixture_infra_file_insert_writes_matching_minio_object -q` -> FAIL，T4 fixture 只写 DB，未写 MinIO 对象。
- GREEN：`python -X utf8 -m pytest script/tests/test_dcc_screenshot_admin_policy_e2e.py::test_t4_fixture_infra_file_insert_writes_matching_minio_object -q` -> PASS。
- GREEN：DCC DB/MinIO 预检 -> PASS，活跃 DCC 引用 944 个，MinIO 缺失对象 0；活跃 DCC 引用缺失 `infra_file` 数 0。
- GREEN：`publish-int-ruoyi.ps1 -Mode build-release -Component backend -ReleaseTag 20260605_with_data_dcc_verify_025547 ...` -> PASS，进程完成后 NAS 包与本地包均为 4542 个文件、13911586307 bytes。
- GREEN：NAS 包关键文件存在 -> PASS，`release-manifest.json`、`manifest/dcc-object-inventory.json`、`ruoyi-vue-pro-current.sql`、`minio/yudao` 均存在。
- GREEN：本地临时发布包清理 -> PASS，`E:\Int\CacheData\IntRuoyi\publish-int-ruoyi\20260605_with_data_dcc_verify_025547` 不存在。
- GREEN：`python -X utf8 -m pytest script/tests/test_with_data_release_package_dcc_evidence.py script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_with_data_release_package_requires_dcc_object_inventory script/tests/test_dcc_screenshot_admin_policy_e2e.py::test_t4_fixture_infra_file_insert_writes_matching_minio_object -q` -> PASS，3 passed。
- GREEN：PowerShell parse `script/deploy/publish-int-ruoyi.ps1` -> PASS，`POWERSHELL_PARSE_OK`。
- GREEN：`git diff --check -- script/deploy/publish-int-ruoyi.ps1 script/e2e/dcc_screenshot_admin_policy_e2e.py script/tests/test_dcc_screenshot_admin_policy_e2e.py script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_with_data_release_package_dcc_evidence.py doc/tasks/20260605-with-data-release-package-dcc-e2e` -> PASS。
- GREEN：`task_closeout.py --task-id 20260605-with-data-release-package-dcc-e2e --mode preview` -> PASS，delete none，blocked none。
- GREEN：`task_closeout.py --task-id 20260605-with-data-release-package-dcc-e2e --mode apply` -> PASS，delete none，blocked none。

## Cleanup Keep

- `doc/tasks/20260605-with-data-release-package-dcc-e2e/task.md`
- `doc/tasks/20260605-with-data-release-package-dcc-e2e/execution-log.md`
- `doc/tasks/20260605-with-data-release-package-dcc-e2e/with-data-release-package-evidence.json`
