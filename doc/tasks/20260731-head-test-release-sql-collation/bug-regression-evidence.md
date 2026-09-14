# Bug Regression Evidence

## Bug

`publish-test` 在测试服 required-sql 阶段执行 `20260726_dcc_codex_test_items_seed.sql`，调用 `ensure_dcc_codex_test_items_seed()` 时报 `ERROR 1267 Illegal mix of collations`。

## Expected

Seed SQL 应在测试服真实表 collation 下执行，临时表中文名称与 `system_codex_test_case.name`、`system_codex_test_checkpoint.name` 做 `JOIN` / `=` / `NOT EXISTS` 比较时不得触发 collation 混用错误。

## Reproduction

`python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` 在新增 collation 断言后复现旧 SQL 使用 `utf8mb4_unicode_ci` 的问题。

## Root Cause

`tmp_dcc_codex_test_case_seed` 和 `tmp_dcc_codex_test_checkpoint_seed` 显式声明 `COLLATE=utf8mb4_unicode_ci`，而测试服目标列 `system_codex_test_case.name` / `system_codex_test_checkpoint.name` 为 `utf8mb4_0900_ai_ci`；等值比较缺少统一 collation。

## RED

RED: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> FAIL, `test_dcc_codex_test_items_seed_temp_tables_match_target_text_collation` 断言旧临时表未使用 `COLLATE=utf8mb4_0900_ai_ci`。

## GREEN

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> PASS, 5 passed。

## Verification

- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS, 400 migrations passed。
- `git diff --check -- IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py IntRuoyiBackend/sql/mysql/20260726_dcc_codex_test_items_seed.sql doc/tasks/20260731-head-test-release-sql-collation/task.md doc/tasks/20260731-head-test-release-sql-collation/execution-log.md doc/tasks/20260731-head-test-release-sql-collation/verification-report.md` -> PASS.

## Blockers

测试服重发尚未执行；当前证据只证明 SQL 根因修复和发布前 SQL policy gate 通过，不能证明最终测试服发布成功。

## Risk And Regression Scope

修复只修改两个临时表的默认 collation，不改变 seed 数据、租户范围、删除标记、checkpoint 数量校验或真实表 schema。
