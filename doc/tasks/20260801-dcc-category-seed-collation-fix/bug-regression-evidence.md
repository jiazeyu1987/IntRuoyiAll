# Bug Regression Evidence

## Bug Summary

`publish-test` 执行 `20260731_dcc_file_category_match_rule_seed.sql` 时，在 line 131 调用存储过程后触发 MySQL `ERROR 1267`。根因是临时 seed 表未显式声明 collation，导致与目标 DCC 分类表文本列比较时出现 `utf8mb4_unicode_ci` 与 `utf8mb4_general_ci` 混用。

## Expected Behavior

DCC 文件分类匹配规则 seed SQL 应显式声明临时字符串列排序规则，所有 `category_name` / `match_text` / `match_type` 比较都能在目标 MySQL 8 collation 下执行。

## Reproduction

- RED: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> FAIL, `test_dcc_file_category_match_rule_seed_temp_table_matches_target_collation` 复现 SQL 未声明 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`。

## Root Cause

`tmp_dcc_file_category_match_rule_seed` 使用 `ENGINE=MEMORY`，但缺少 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`；临时列参与 `category.name = seed.category_name`、`existing.match_text = seed.match_text` 和 `existing.match_type = seed.match_type` 比较时与目标表 collation 不一致。

## Fix

将 `tmp_dcc_file_category_match_rule_seed` 的创建语句改为 `ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`，使临时表字符串列和目标 DCC 分类/匹配规则表比较时使用一致 collation。

## Verification

- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> PASS, 3 passed。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 8 passed。
- GREEN: migration policy gate -> PASS, `20260731_dcc_file_category_match_rule_seed.sql` sha256=`7e2e3cd8880f35af99bab05f7dfd1aa2b394e2564e3bc80c89e689e53c8eaa97`。
- GREEN: `git diff --check` -> PASS。

## Risk And Regression Scope

风险低：预计只调整临时表 collation，不改变插入数据、目标表结构、租户范围或删除/更新条件。回归覆盖 DCC 分类匹配规则 seed 与智能排产 seed 相邻 required SQL。

## Blockers

失败 releaseTag `release-20260801-frozen-smartseed-tempfix-r260801c-r1` 不得复用，后续发布需新 releaseTag。
