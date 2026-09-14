# Verification Report

## Current Result

列级 collation 源码修复已通过目标回归、相邻 SQL 回归、migration policy gate 和 diff 检查；等待形成新提交后使用新 releaseTag 重建并仅发布测试服。

## Evidence

- BLOCKER：`release-20260801-frozen-smartseed-tempfix-r260801c-r1` 的 `publish-test` 在 `20260731_dcc_file_category_match_rule_seed.sql` 触发 MySQL `ERROR 1267`。
- RED：`python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` 先失败，确认临时表缺少 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`。
- GREEN：目标 DCC seed 静态测试通过，3 passed。
- GREEN：DCC seed + 智能排产 seed 相邻回归通过，8 passed。
- GREEN：migration policy gate 通过，`20260731_dcc_file_category_match_rule_seed.sql` sha256=`7e2e3cd8880f35af99bab05f7dfd1aa2b394e2564e3bc80c89e689e53c8eaa97`。
- GREEN：`git diff --check` 通过。
- BLOCKER：`release-20260801-frozen-dcc-category-collation-r260801d-r1` 的 `publish-test` 继续在同一 SQL line 131 触发 MySQL `ERROR 1267`，本次为 `utf8mb4_0900_ai_ci` 与 `utf8mb4_unicode_ci` 混用。
- GREEN：只读查询测试服真实列 collation，`dcc_file_category.name=utf8mb4_unicode_ci`，`dcc_file_category_match_rule.match_text/match_type=utf8mb4_0900_ai_ci`。
- RED：列级 collation 断言升级后目标测试先失败，证明表默认 collation 修复不足。
- GREEN：列级修复后目标 DCC seed 静态测试通过，3 passed；DCC seed + 智能排产 seed 相邻回归通过，8 passed。
- GREEN：r2 migration policy gate 通过，`20260731_dcc_file_category_match_rule_seed.sql` sha256=`cce2f95c5e2a5d84b24b2d05580010a2ef6ca0a018279e8a7f7d6a70ed649321`。
- GREEN：r2 `git diff --check` 通过。

## Remaining Work

提交当前列级 collation 修复，随后使用新 releaseTag 重建并仅发布测试服；失败 releaseTag 不复用。
