# Execution Log

## User Intent

- 用户要求先修复测试服发布阻塞：`20260805_erp_nas_table_auto_sync.sql` 声明非法 `type=schema,job`，导致 migration policy gate 在 build-release 前失败。

## BDD

- BDD: NAS table auto sync release metadata -> Given `20260805_erp_nas_table_auto_sync.sql` creates schema tables and seeds an `infra_job`, When release migration metadata is parsed, Then `type` is a single allowed release enum and the job seed remains covered by SQL content tests.

## Milestone Updates

- completed: 任务记录已建立，适用门禁来自 release-migration 结构化字段与 dependsOn 后缀门禁。
- completed: 聚焦测试收紧为精确断言 NAS SQL 首行 `release-migration` 元数据。
- completed: SQL 首行从 `dependsOn=20260612_erp_kingdee_sync_runtime.sql; type=schema,job` 修复为 `dependsOn=20260612_erp_kingdee_sync_runtime; type=schema`。

## Verification Evidence

- RED: `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` -> FAIL, expected reason: first-line assertion showed `nc_runtime.sql; type=schema,job` instead of `nc_runtime; type=schema`.
- RED: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260805_erp_nas_table_auto_sync.sql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260612_erp_kingdee_sync_runtime.sql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate-red.json` -> FAIL, expected reason: `invalid type in ...20260805_erp_nas_table_auto_sync.sql: schema,job`.
- GREEN: `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` -> PASS, `4 passed`.
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate.json` -> PASS, `status=passed`, `migrationCount=443`, NAS migration `type=schema`, `dependsOn=["20260612_erp_kingdee_sync_runtime"]`.
- GREEN: `Select-String -Path 'IntRuoyiBackend\sql\mysql\*.sql' -Encoding utf8 -Pattern '^-- release-migration:.*type=.*job'` -> PASS, no output.
- GREEN: `Select-String -Path 'IntRuoyiBackend\sql\mysql\*.sql' -Encoding utf8 -Pattern '^-- release-migration:.*dependsOn=.*\.sql'` -> PASS, no output.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ..\doc\tasks\20260806-release-migration-metadata-fix\bug-regression-evidence.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ..\doc\tasks\20260806-release-migration-metadata-fix\database-schema-evidence.md` -> PASS.
- Experience consolidation: existing `docs/release-build-preflight-lessons.md#2026-07-19-release-migration-结构化字段与-dependson-后缀门禁` already covers this issue; no new long-term experience document created.

## Blockers

- 当前根工作区存在大量非本任务 dirty/untracked 改动；本任务仅修改 NAS SQL、其聚焦测试和任务记录。按项目 Git 门禁，最终提交/推送 closeout 需要先处理或授权基线提交这些既有非本任务改动。
