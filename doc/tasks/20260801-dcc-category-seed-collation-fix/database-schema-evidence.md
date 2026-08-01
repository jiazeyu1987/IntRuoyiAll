# Database Schema Evidence

## Goal

修复 DCC 文件分类匹配规则 seed required SQL 的临时表排序规则，使其在测试服 MySQL 8 目标库上可重复执行。

## Affected Entities

- `dcc_file_category`
- `dcc_file_category_match_rule`
- `tmp_dcc_file_category_match_rule_seed`

## Data Safety

- 本次只修改迁移 SQL 源文件、静态测试和任务证据文档。
- 不直接写测试服、正式服或备份服数据库；失败 releaseTag 不复用。

## Rollback / Recovery

- 源码回滚：回退本次提交即可恢复旧 SQL。
- 运行环境恢复：如果发布失败导致 `.env` / migration 状态漂移，只记录证据并通过新 releaseTag 重发，不手工改库或改锁。

## BDD Scenarios

- BDD: DCC category match seed collation -> Given 目标表文本列使用 `utf8mb4_unicode_ci`, When required SQL 使用临时 seed 表比较分类名称、匹配文本和匹配类型, Then 临时表必须显式声明 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`。
- BDD: No database fallback -> Given 发布 required SQL 在测试服失败, When 修复 seed SQL, Then 不修改数据库默认 collation、不手工更新 migration/lock、不复用失败 releaseTag。

## Changes

- `20260731_dcc_file_category_match_rule_seed.sql` 的 `tmp_dcc_file_category_match_rule_seed` 临时表创建语句从 `ENGINE=MEMORY` 改为 `ENGINE=MEMORY DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`。
- 新增 `script/tests/test_dcc_file_category_match_rule_seed_sql.py`，覆盖 release metadata、临时表 collation、关键 JOIN/比较表达式和必需业务 seed 词。

## Migration

- Migration tool: repository release migration policy gate over `sql/mysql`.
- Migration verification command: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix\doc\tasks\20260801-dcc-category-seed-collation-fix\migration-policy-gate.json`。
- Result: PASS，migrationCount=403，`20260731_dcc_file_category_match_rule_seed.sql` sha256=`7e2e3cd8880f35af99bab05f7dfd1aa2b394e2564e3bc80c89e689e53c8eaa97`。

## Verification

- RED: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> FAIL, 缺少显式临时表 charset/collation。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` -> PASS, 3 passed。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 8 passed。
- GREEN: `git diff --check` -> PASS。

## Blockers

当前无源码修复 blocker；测试服发布需在修复提交后使用新 releaseTag 重建。
