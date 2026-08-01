# Bug Regression Evidence

## Bug Summary

`publish-test` 执行 `20260726_system_codex_smart_scheduling_test_items.sql` 时，在 line 313 调用存储过程后先后触发 MySQL `ERROR 1267` 与 `ERROR 1137`。`ERROR 1267` 源于临时 seed 表字符串列默认排序规则与目标 `system_codex_test_*` 文本列 `utf8mb4_0900_ai_ci` 不一致；`ERROR 1137` 源于 checkpoint 数量校验在同一语句中重复读取同一 TEMPORARY TABLE。

## Expected Behavior

智能排产 Codex 测试项 seed SQL 应显式声明临时字符串列排序规则，所有 `case_name` / `checkpoint_name` / `severity` / `status` 等比较都能在目标 MySQL 8 collation 下执行；同时不应在同一语句内重复读取同一临时表。

## Reproduction

- RED: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> FAIL, expected reason: 新增 collation 契约测试发现三个临时表缺少 `COLLATE=utf8mb4_0900_ai_ci`。
- RED: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> FAIL, expected reason: 新增临时表重开契约测试发现 `) <> (SELECT COUNT(*) FROM tmp_codex_smart_scheduling_checkpoint_seed)` 风险形态。

## Root Cause

`tmp_codex_smart_scheduling_case_seed` 和 `tmp_codex_smart_scheduling_checkpoint_seed` 只声明 `DEFAULT CHARSET=utf8mb4`，`tmp_codex_smart_scheduling_case_ids` 未声明 charset/collation；这些临时表字段与目标 `system_codex_test_case.name` / `system_codex_test_checkpoint` 文本列比较时，继承的排序规则可能与目标库 `utf8mb4_0900_ai_ci` 不一致。

checkpoint 数量校验使用 `IF (SELECT COUNT(*) FROM tmp_codex_smart_scheduling_checkpoint_seed AS seed ... ) <> (SELECT COUNT(*) FROM tmp_codex_smart_scheduling_checkpoint_seed)`，MySQL 对 TEMPORARY TABLE 在同一语句内二次打开有限制，因此运行到存储过程时触发 `ERROR 1137 Can't reopen table: 'seed'`。

## Fix

为三个 `tmp_codex_smart_scheduling_*` 临时表统一增加 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`，与测试管理目标文本列保持一致；将 checkpoint 数量校验拆为 `v_expected_checkpoint_count` 与 `v_actual_checkpoint_count` 两个变量后再比较，避免重复打开同一临时表。

## Verification

- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 4 passed。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py script/tests/test_dcc_codex_test_items_seed.py script/tests/test_codex_test_case_project_migration.py -q` -> PASS, 11 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix\doc\tasks\20260801-smart-seed-collation-fix\migration-policy-gate.json` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 5 passed。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py script/tests/test_dcc_codex_test_items_seed.py script/tests/test_codex_test_case_project_migration.py -q` -> PASS, 12 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix\doc\tasks\20260801-smart-seed-collation-fix\migration-policy-gate.json` -> PASS，目标 SHA256=`e633f8ac1a008d6a46ebf94190614f6632b9094d66bb8242818ec3b0a78d934c`。

## Risk And Regression Scope

风险低：仅调整临时表 collation 和校验计数写法，不改变插入数据、业务字段、目标表结构、租户范围或删除/更新条件。回归覆盖智能排产 seed、DCC Codex seed 和后续 `system_codex_test_case_project` 迁移依赖。
## Blockers

无当前修复 blocker。原测试服失败 releaseTag 不得复用，后续发布需新 releaseTag。
