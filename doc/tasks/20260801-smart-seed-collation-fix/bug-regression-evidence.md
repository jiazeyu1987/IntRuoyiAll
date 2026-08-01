# Bug Regression Evidence

## Bug Summary

`publish-test` 执行 `20260726_system_codex_smart_scheduling_test_items.sql` 时，在 line 313 调用存储过程后触发 MySQL `ERROR 1267`，原因是临时 seed 表字符串列使用默认排序规则，与目标 `system_codex_test_*` 文本列的 `utf8mb4_0900_ai_ci` 比较不一致。

## Expected Behavior

智能排产 Codex 测试项 seed SQL 应显式声明临时字符串列排序规则，所有 `case_name` / `checkpoint_name` / `severity` / `status` 等比较都能在目标 MySQL 8 collation 下执行。

## Reproduction

- RED: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> FAIL, expected reason: 新增 collation 契约测试发现三个临时表缺少 `COLLATE=utf8mb4_0900_ai_ci`。

## Root Cause

`tmp_codex_smart_scheduling_case_seed` 和 `tmp_codex_smart_scheduling_checkpoint_seed` 只声明 `DEFAULT CHARSET=utf8mb4`，`tmp_codex_smart_scheduling_case_ids` 未声明 charset/collation；这些临时表字段与目标 `system_codex_test_case.name` / `system_codex_test_checkpoint` 文本列比较时，继承的排序规则可能与目标库 `utf8mb4_0900_ai_ci` 不一致。

## Fix

为三个 `tmp_codex_smart_scheduling_*` 临时表统一增加 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`，与测试管理目标文本列保持一致。

## Verification

- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 4 passed。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py script/tests/test_dcc_codex_test_items_seed.py script/tests/test_codex_test_case_project_migration.py -q` -> PASS, 11 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix\doc\tasks\20260801-smart-seed-collation-fix\migration-policy-gate.json` -> PASS。

## Risk And Regression Scope

风险低：仅调整临时表 collation，不改变插入数据、业务字段、目标表结构、租户范围或删除/更新条件。回归覆盖智能排产 seed、DCC Codex seed 和后续 `system_codex_test_case_project` 迁移依赖。
## Blockers

无当前修复 blocker。原测试服失败 releaseTag 不得复用，后续发布需新 releaseTag。
