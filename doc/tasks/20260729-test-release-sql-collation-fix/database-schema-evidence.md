# Database Schema Evidence

## Data

修复 Codex DCC 测试项 seed SQL 的临时表 collation 契约，避免与测试服真实目标表列比较时失败。受影响实体为 `system_codex_test_case`、`system_codex_test_checkpoint`、`tmp_dcc_codex_test_case_seed`、`tmp_dcc_codex_test_checkpoint_seed`。

## Migration

MySQL 8；release required SQL 由 `script/release/run-release-migration-policy-gate.py` 与发布脚本执行。`sql/mysql/20260726_dcc_codex_test_items_seed.sql` 中两个临时 seed 表的 `COLLATE` 从 `utf8mb4_unicode_ci` 改为 `utf8mb4_0900_ai_ci`。

## Safety

不修改真实表 schema、不删除数据、不改数据库默认 collation；仅调整 seed 临时表比较契约。

## Rollback

若验证失败，不发布；回滚代码提交即可。已失败 releaseTag 不复用。

## BDD

BDD: DCC Codex seed collation follows live target schema -> Given 测试服 `system_codex_test_case` 与 `system_codex_test_checkpoint` 字符列为 `utf8mb4_0900_ai_ci`, When required SQL 创建临时 seed 表并与目标表做名称、状态和检查点比较, Then 临时 seed 表字符串列必须使用相同 collation。

RED: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> FAIL，新增静态合同发现临时 seed 表仍为 `utf8mb4_unicode_ci`。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> PASS，5 passed。

## Verification

`python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260729-test-release-sql-collation-fix\migration-policy-gate.json` -> PASS，`status=passed`，`migrationCount=390`。

## Blockers

修复验证已通过；后续发布仍必须使用新 releaseTag，不得复用失败 releaseTag。
