# Execution Log

## 2026-08-01

- User intent: 继续修复仅测试服发布阻塞，保持发布输入来自原冻结 worktree 修复分支，不引入未提交改动。
- BDD: DCC category match seed collation -> Given 目标表 `dcc_file_category` / `dcc_file_category_match_rule` 文本列可能使用 `utf8mb4_unicode_ci`, When required SQL 使用 MEMORY 临时 seed 表按分类名称、匹配文本和匹配类型写入规则, Then 临时表必须显式声明相同 charset/collation，避免 MySQL `ERROR 1267`。
- BDD: No database fallback -> Given required SQL 在测试服失败, When 修复 seed SQL, Then 不修改测试库默认 collation、不手工更新 migration/lock、不复用失败 releaseTag，而是提交源码修复并重建新 releaseTag。
- GREEN: frozen-source-guard -> PASS, 当前修复 worktree `D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix` 位于分支 `codex/20260801-smart-seed-collation-fix-frozen`，HEAD=`e3d3a8efdae5e17a7643192bf55a2c5ae21d1ff4`，该 HEAD 基于原冻结提交 `9420210f7ad4fb2519c179458fae0e823d082b54` 叠加发布 blocker 修复。

## TDD Evidence

- RED: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> FAIL, `test_dcc_file_category_match_rule_seed_temp_table_matches_target_collation` 断言 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` 不存在，2 passed / 1 failed。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> PASS, 3 passed。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 8 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix\doc\tasks\20260801-dcc-category-seed-collation-fix\migration-policy-gate.json` -> PASS, migrationCount=403，`20260731_dcc_file_category_match_rule_seed.sql` sha256=`7e2e3cd8880f35af99bab05f7dfd1aa2b394e2564e3bc80c89e689e53c8eaa97`。
- GREEN: `git diff --check` -> PASS，Git 仅提示目标 SQL 下次触碰时 LF 会被替换为 CRLF，未发现 whitespace error。

## Issues

### I001 DCC 分类匹配规则 seed 临时表 collation 继承 MEMORY 默认值

- 现象：`publish-test` 执行 `20260731_dcc_file_category_match_rule_seed.sql` 时，MySQL 返回 `ERROR 1267 (HY000) at line 131: Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_general_ci,IMPLICIT) for operation '='`。
- 阶段：测试服 `publish-test` required SQL 执行。
- 影响：测试服发布失败，后续容器重建、健康检查、HTTP 页面和版本说明验证不得继续。
- 原因判断：`tmp_dcc_file_category_match_rule_seed` 使用 `ENGINE=MEMORY` 但未声明 `DEFAULT CHARSET/COLLATE`，与目标表文本列比较时继承默认 `utf8mb4_general_ci`，而目标列为 `utf8mb4_unicode_ci`。
- 处理动作：新增静态 RED 测试，要求临时 seed 表显式 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`；随后修复 SQL 并重跑验证。
- 结果：源码修复和静态回归验证已通过，等待提交后用新 releaseTag 重新构建发布。
- 是否可前置检查：是。
- 是否可自动化：是，migration policy gate 可扫描 required SQL 中 MEMORY/TEMPORARY seed 表与真实文本列 JOIN 的 collation 声明。
- 下次如何避免：所有 required SQL seed 临时表必须在创建语句中显式声明目标 collation，不依赖 MEMORY 引擎默认值。
