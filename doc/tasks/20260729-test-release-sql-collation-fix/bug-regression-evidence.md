# Bug Regression Evidence

## Bug

`publish-test` 在 required SQL `20260726_dcc_codex_test_items_seed.sql` 阶段失败，MySQL 报 `ERROR 1267 Illegal mix of collations`。

## Expected

DCC Codex 测试项 seed SQL 在测试服真实 MySQL 8 schema 上可重复执行，不因临时表与目标列 collation 不一致失败。

## Reproduction

`python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py`

## Root Cause

发布 SQL 的 `tmp_dcc_codex_test_case_seed` 与 `tmp_dcc_codex_test_checkpoint_seed` 使用 `utf8mb4_unicode_ci`，而测试服真实 `system_codex_test_case` / `system_codex_test_checkpoint` 字符列为 `utf8mb4_0900_ai_ci`，过程内 `name` / `case_name` 等比较触发 collation mismatch。

## Regression Test

`script/tests/test_dcc_codex_test_items_seed.py` 新增 `test_dcc_codex_test_items_seed_temp_tables_match_live_target_collation`。

RED: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> FAIL，断言 `collate=utf8mb4_0900_ai_ci` 不存在，实际临时表 DDL 为 `collate=utf8mb4_unicode_ci`。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> PASS，5 passed。

## Verification

`python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260729-test-release-sql-collation-fix\migration-policy-gate.json` -> PASS，`status=passed`，`migrationCount=390`。

## Risk

required SQL seed，只影响 `system_codex_test_case` 与 `system_codex_test_checkpoint` 的 DCC Codex 测试项种子。

## Blockers

发布重试必须在修复提交后使用新 releaseTag。
