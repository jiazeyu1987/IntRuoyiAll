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
- RED: publish-test r260801d -> FAIL, `release-20260801-frozen-dcc-category-collation-r260801d-r1` 在 `20260731_dcc_file_category_match_rule_seed.sql` line 131 再次触发 MySQL `ERROR 1267`，本次为 `utf8mb4_0900_ai_ci` 与 `utf8mb4_unicode_ci` 混用；operation=`op-2026-08-01T073030255051500Z-ba55c7d7-75c5-404a-bfe2-1e8f8035a6a1`。
- GREEN: live-column-collation-readonly -> PASS, 只读查询测试服目标列：`dcc_file_category.name=utf8mb4_unicode_ci`，`dcc_file_category_match_rule.match_text=utf8mb4_0900_ai_ci`，`dcc_file_category_match_rule.match_type=utf8mb4_0900_ai_ci`；未手工修改测试库。
- RED: column-level-collation-test -> FAIL, `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` 复现当前 SQL 未对 `category_name`、`match_text`、`match_type` 分别声明目标列 collation，2 passed / 1 failed。
- GREEN: column-level-collation-test -> PASS, `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> 3 passed。
- GREEN: column-level-adjacent-regression -> PASS, `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> 8 passed。
- GREEN: column-level-migration-policy-gate -> PASS, `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix\doc\tasks\20260801-dcc-category-seed-collation-fix\migration-policy-gate-r2.json` -> PASS, migrationCount=403，`20260731_dcc_file_category_match_rule_seed.sql` sha256=`cce2f95c5e2a5d84b24b2d05580010a2ef6ca0a018279e8a7f7d6a70ed649321`。
- GREEN: column-level-git-diff-check -> PASS, `git diff --check` 未发现 whitespace error。

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

### I002 DCC 分类匹配规则 seed 单表级 collation 不足

- 现象：`release-20260801-frozen-dcc-category-collation-r260801d-r1` 的 `publish-test` 再次在 `20260731_dcc_file_category_match_rule_seed.sql` line 131 失败，MySQL 返回 `ERROR 1267`，本次为 `utf8mb4_0900_ai_ci` 与 `utf8mb4_unicode_ci` 混用。
- 阶段：测试服 `publish-test` required SQL 执行。
- 影响：测试服发布失败，后续容器重建、健康检查、HTTP 页面和版本说明验证不得继续。
- 原因判断：临时表只设置了表默认 `utf8mb4_unicode_ci`，能对齐 `dcc_file_category.name`，但 `dcc_file_category_match_rule.match_text/match_type` 真实目标列为 `utf8mb4_0900_ai_ci`，插入完整性校验中比较 `rule_record.match_text/type = seed.match_text/type` 仍会混用 collation。
- 处理动作：只读查询真实列 collation；将静态测试升级为列级 collation 断言；将 `category_name` 显式设为 `utf8mb4_unicode_ci`，将 `match_text` 与 `match_type` 显式设为 `utf8mb4_0900_ai_ci`。
- 结果：目标测试、相邻 seed 回归、migration policy gate 与 `git diff --check` 均通过，等待提交后重建新 releaseTag。
- 是否可前置检查：是。
- 是否可自动化：是，required SQL gate 应基于目标表每个参与比较的列读取真实 collation，而不是只看表默认或第一个 JOIN 列。
- 下次如何避免：临时 seed 表与多个真实表文本列比较时，必须按列级目标 collation 建表或显式 `COLLATE` 比较；不得假设同一业务链路所有列使用同一 collation。
