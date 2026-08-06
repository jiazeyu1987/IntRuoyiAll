# 20260806 release migration metadata fix

## Task Goal

修复 `20260805_erp_nas_table_auto_sync.sql` 的 `release-migration` 元数据，使测试服发布构建前 migration policy gate 不再因非法复合 `type=schema,job` 阻塞。

## Milestones

- [x] 建立任务记录、BDD 场景和适用门禁
- [x] 用聚焦测试复现非法复合 `type` 元数据
- [x] 最小修复 SQL 首行元数据并保持 NAS 自动同步语义不变
- [x] 运行目标 pytest、全量 migration policy gate 和证据校验
- [x] 记录验证结果与剩余阻塞

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate.json`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ..\doc\tasks\20260806-release-migration-metadata-fix\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ..\doc\tasks\20260806-release-migration-metadata-fix\database-schema-evidence.md`

## Current Status

ready_for_closeout

Implementation and required verification are complete. Final commit/push closeout is blocked by pre-existing unrelated dirty and untracked workspace changes that are outside this task scope.

## Applicable Gates

- Trigger: code-only / no-data 发布前执行全量 migration policy gate，SQL 首行把 `type` 写成复合值。
- Preflight check: 运行 migration policy gate 并用专项测试断言完整首行；`type` 只能是 `schema|data|menu|config|permission|seed` 中的单个枚举值。
- Blocker: 门禁输出 `invalid type ... schema,job`，或测试只检查存在 `release-migration` 但未锁定结构化字段。
- Verification: 先记录 RED，再修复 SQL 首行；目标 pytest 与全量 migration policy gate 均通过后，才能进入下一轮 build-release。
- Forbidden action: 不得跳过 schema/seed/job 类 SQL 元数据门禁；不得手工编辑 manifest 或远端迁移状态绕过。
- Evidence: `docs/release-build-preflight-lessons.md#2026-07-19-release-migration-结构化字段与-dependson-后缀门禁`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复发布迁移元数据契约并补聚焦测试。
- `是否存在临时补丁或绕过`：否。

## Verification Result

- RED: `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` failed before the fix because the SQL first line still had `dependsOn=20260612_erp_kingdee_sync_runtime.sql; type=schema,job`.
- RED: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260805_erp_nas_table_auto_sync.sql --sql-file E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260612_erp_kingdee_sync_runtime.sql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate-red.json` failed with `invalid type ... schema,job`.
- GREEN: `python -X utf8 -m pytest script/tests/test_erp_nas_table_auto_sync_sql.py -q` passed, `4 passed`.
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260806-release-migration-metadata-fix\migration-policy-gate.json` passed with `migrationCount=443`; NAS entry is `type=schema`, `dependsOn=["20260612_erp_kingdee_sync_runtime"]`.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ..\doc\tasks\20260806-release-migration-metadata-fix\bug-regression-evidence.md` passed.
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ..\doc\tasks\20260806-release-migration-metadata-fix\database-schema-evidence.md` passed.
- Scan: no remaining `release-migration` lines with `type=*job` or `dependsOn=*.sql` were found under `IntRuoyiBackend\sql\mysql`.
- Experience consolidation: existing `docs/release-build-preflight-lessons.md#2026-07-19-release-migration-结构化字段与-dependson-后缀门禁` already covers this class of issue; no new long-term experience document was created.
