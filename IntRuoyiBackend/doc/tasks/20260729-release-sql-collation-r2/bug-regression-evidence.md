# Bug Regression Evidence

## Bug Summary

测试服发布 `release-20260729-sqlfix-test-r260729b-r2` 时，required SQL `20260726_system_codex_smart_scheduling_test_items.sql` 在存储过程执行阶段失败，报 MySQL `ERROR 1267 Illegal mix of collations`。

## Expected Behavior

测试项 seed SQL 可以在 MySQL 8 测试库中重复执行；临时表与 `system_codex_test_case`、`system_codex_test_checkpoint` 比较时不因排序规则不同失败。

## Reproduction

维护仓发布 operation `op-2026-07-29T044320236880700Z-fb6c43d9-09d3-435d-bf81-72f84370cdb4` 在测试服执行该 SQL 失败。任务证据冻结于维护仓 `doc/tasks/20260729-head-test-only-release/evidence/publish-test-log-tail-sanitized-r260729b-r2.txt` 与 `test-server-failure-freeze-r260729b-r2.out`。

## Root Cause

`tmp_codex_smart_scheduling_case_seed`、`tmp_codex_smart_scheduling_case_ids`、`tmp_codex_smart_scheduling_checkpoint_seed` 未显式声明 `COLLATE=utf8mb4_0900_ai_ci`，与测试服真实目标表字符列比较时触发 collation mismatch。

## Regression Test

新增 `test_smart_scheduling_test_items_temp_tables_match_live_target_collation`，要求三个临时表 DDL 均包含 `collate=utf8mb4_0900_ai_ci`，且不得出现 `utf8mb4_general_ci` / `utf8mb4_unicode_ci`。

## RED

RED:

`python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py -q` -> FAIL，断言 `collate=utf8mb4_0900_ai_ci` 不在临时表 DDL 中。

## GREEN

GREEN:

`python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py -q` -> PASS，`9 passed`。

## Verification

Verification: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260729-head-test-only-release\evidence\migration-policy-gate-r260729c.json` -> PASS。

## Regression Scope

- SQL seed 临时表排序规则。
- DCC test item seed 前一轮 collation 修复保持通过。
- release migration policy gate 保持通过。

## Blockers And Follow-Up

无本地修复 blocker。发布层必须使用包含本提交的新 releaseTag 重建重发，不得复用失败的 r2 releaseTag。
