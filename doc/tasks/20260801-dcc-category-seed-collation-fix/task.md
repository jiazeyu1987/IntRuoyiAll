# 20260801 DCC 分类匹配规则 Seed Collation 修复

## Task Goal

修复 `20260731_dcc_file_category_match_rule_seed.sql` 在测试服 `publish-test` required SQL 阶段因临时 seed 表默认排序规则与 `dcc_file_category` / `dcc_file_category_match_rule` 目标文本列不一致触发的 MySQL `ERROR 1267`，并用静态回归测试阻止同类 seed 再次发布失败。

## Milestones

1. 记录发布失败证据、任务目标和经验门禁。
2. 增加能复现临时表 collation 缺失的 RED 静态回归测试。
3. 最小修改 seed SQL，使临时表字符串列显式使用目标库通用 collation。
4. 运行目标测试、邻近 SQL 测试、migration policy gate 与证据校验。
5. 提交冻结修复分支后，用新 releaseTag 重建并仅发布测试服。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py -q` 先失败后通过。
- `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_seed_sql.py script/tests/test_codex_smart_scheduling_test_items_seed.py -q` 通过。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output <task evidence>` 通过。
- `git diff --check` 通过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；通过临时表 charset/collation 显式声明和静态测试覆盖 required SQL 发布失败根因。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- required SQL 使用临时表、派生表或过程变量与真实业务表字符列做 `JOIN`、`=`、`NOT EXISTS` 比较前，必须显式声明临时表/表达式 collation；不得依赖数据库或 MEMORY 引擎默认 collation。
- 发布 blocker 修复继续基于原冻结提交链路，不切到更新后的主线；失败 releaseTag 不复用，修复后重新构建新 releaseTag。
- 不手工修改测试库 migration 状态、operation lock 或业务数据来绕过发布失败。

## Current Status

verified_pending_release_rebuild

## Progress Log

- 2026-08-01：`release-20260801-frozen-smartseed-tempfix-r260801c-r1` 的 `publish-test` 已确认 `20260726_system_codex_smart_scheduling_test_items.sql` 通过，但在 `20260731_dcc_file_category_match_rule_seed.sql` line 131 触发 MySQL `ERROR 1267 Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_general_ci,IMPLICIT)`；operation 已失败并释放测试服发布锁，不能继续运行态验证。
- 2026-08-01：新增 `script/tests/test_dcc_file_category_match_rule_seed_sql.py` 静态回归测试，先复现临时表缺少 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` 的 RED 失败。
- 2026-08-01：已最小修改 `20260731_dcc_file_category_match_rule_seed.sql` 的 MEMORY 临时表声明，目标测试、相邻 seed 测试、migration policy gate 和 `git diff --check` 均通过；等待提交后用新 releaseTag 重建。
- 2026-08-01：`release-20260801-frozen-dcc-category-collation-r260801d-r1` 的 `publish-test` 再次失败，错误为 `utf8mb4_0900_ai_ci` 与 `utf8mb4_unicode_ci` 混用；只读确认真实目标列 `dcc_file_category.name=utf8mb4_unicode_ci`，`dcc_file_category_match_rule.match_text/match_type=utf8mb4_0900_ai_ci`。
- 2026-08-01：已升级测试为列级 collation 断言，并将临时表 `category_name`、`match_text`、`match_type` 分别对齐真实目标列；目标测试、相邻 seed 测试、migration policy gate 和 `git diff --check` 均通过；等待提交后用新 releaseTag 重建。
