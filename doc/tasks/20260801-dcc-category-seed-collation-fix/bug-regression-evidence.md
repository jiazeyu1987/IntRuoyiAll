# Bug Regression Evidence

## Bug Summary

`publish-test` 执行 `20260731_dcc_file_category_match_rule_seed.sql` 时，在 line 131 调用存储过程后触发 MySQL `ERROR 1267`。先修复表默认 collation 后，r260801d 又暴露出目标列 collation 不一致：`dcc_file_category.name=utf8mb4_unicode_ci`，但 `dcc_file_category_match_rule.match_text/match_type=utf8mb4_0900_ai_ci`。

## Expected Behavior

DCC 文件分类匹配规则 seed SQL 应按真实目标列显式声明临时字符串列排序规则，所有 `category_name` / `match_text` / `match_type` 比较都能在目标 MySQL 8 collation 下执行。

## Reproduction

- RED: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> FAIL, `test_dcc_file_category_match_rule_seed_temp_table_matches_target_collation` 复现 SQL 未声明 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`。
- RED: `release-20260801-frozen-dcc-category-collation-r260801d-r1` publish-test -> FAIL, 表默认 `utf8mb4_unicode_ci` 修复后仍在 line 131 触发 `utf8mb4_0900_ai_ci` 与 `utf8mb4_unicode_ci` 混用。
- RED: 列级断言升级后，`python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> FAIL，当前 SQL 未分别声明 `category_name` / `match_text` / `match_type` 的目标列 collation。

## Root Cause

`tmp_dcc_file_category_match_rule_seed` 使用 `ENGINE=MEMORY`。只设置表默认 `utf8mb4_unicode_ci` 可对齐 `dcc_file_category.name`，但 `dcc_file_category_match_rule.match_text` 与 `match_type` 在测试服真实库为 `utf8mb4_0900_ai_ci`；最终完整性校验中 `rule_record.match_text/type = seed.match_text/type` 仍会混用 collation。

## Fix

保留表默认 `ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`；同时将 `category_name` 列显式设为 `utf8mb4_unicode_ci`，将 `match_text` 和 `match_type` 列显式设为 `utf8mb4_0900_ai_ci`，按真实目标列 collation 对齐。

## Verification

- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> PASS, 3 passed。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 8 passed。
- GREEN: migration policy gate -> PASS, `20260731_dcc_file_category_match_rule_seed.sql` sha256=`7e2e3cd8880f35af99bab05f7dfd1aa2b394e2564e3bc80c89e689e53c8eaa97`。
- GREEN: `git diff --check` -> PASS。
- GREEN: r2 目标测试 -> PASS, 3 passed。
- GREEN: r2 DCC seed + 智能排产 seed 相邻回归 -> PASS, 8 passed。
- GREEN: r2 migration policy gate -> PASS, `20260731_dcc_file_category_match_rule_seed.sql` sha256=`cce2f95c5e2a5d84b24b2d05580010a2ef6ca0a018279e8a7f7d6a70ed649321`。
- GREEN: r2 `git diff --check` -> PASS。

## Risk And Regression Scope

风险低：预计只调整临时表 collation，不改变插入数据、目标表结构、租户范围或删除/更新条件。回归覆盖 DCC 分类匹配规则 seed 与智能排产 seed 相邻 required SQL。

## Blockers

失败 releaseTag `release-20260801-frozen-smartseed-tempfix-r260801c-r1` 与 `release-20260801-frozen-dcc-category-collation-r260801d-r1` 不得复用，后续发布需新 releaseTag。
