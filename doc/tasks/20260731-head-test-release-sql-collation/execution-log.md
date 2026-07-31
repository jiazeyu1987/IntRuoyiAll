# Execution Log

## User Intent

- 继续完成当前分支 HEAD 的仅测试服发布；未提交改动不得进入构建产物，只能发布测试服，不允许正式服/备份服/mark-tested/promote 动作。

## Bootstrap

- 2026-07-31：维护仓发布任务 `20260730-head-test-only-release` 已在 `publish-test` required-sql 阶段失败，失败 SQL 为 `20260726_dcc_codex_test_items_seed.sql`，MySQL `ERROR 1267 Illegal mix of collations`。
- 当前主程序仓 `E:\IntRuoyi` 分支 `int_main`，当前 HEAD `d1ffcef87e9a6af884cfe47bb0ad69b78febecfd`。
- 已读取 `AGENTS.md`、`docs/backend-development.md`、`docs/database-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`、`docs/worktree-restrictions.md`、`docs/release-backup-restore.md`、`docs/task-closeout-rules.md` 和 `docs/experience-index.md`。
- 目标文件 `IntRuoyiBackend/sql/mysql/20260726_dcc_codex_test_items_seed.sql` 与 `IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py` 当前无未提交差异。

## BDD

- BDD: required SQL collation compatibility -> Given 测试服真实表 `system_codex_test_case.name` / `system_codex_test_checkpoint.name` 使用 `utf8mb4_0900_ai_ci`，When seed SQL 用临时表中文名称与真实表名称做等值比较，Then 临时表字符串列或比较表达式必须使用一致 collation，避免 `ERROR 1267` 并保持租户、删除标记和 checkpoint 完整性校验不变。

## RED / GREEN / REGRESSION

- RED: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> FAIL, 新增 `test_dcc_codex_test_items_seed_temp_tables_match_target_text_collation` 断言旧 SQL 临时表仍为 `COLLATE=utf8mb4_unicode_ci`，无法证明与测试服目标列 `utf8mb4_0900_ai_ci` 对齐。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> PASS, 5 passed，seed SQL 临时表 collation 已对齐为 `utf8mb4_0900_ai_ci`。
- REGRESSION: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS, 400 migrations passed。

## Command Evidence

- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260726_dcc_codex_test_items_seed.sql` -> FAIL, 预期原因：单文件运行未带依赖 `20260724_system_codex_test_management`，policy gate 正确拒绝缺依赖上下文。
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260724_system_codex_test_management.sql --sql-file sql/mysql/20260726_dcc_codex_test_items_seed.sql` -> FAIL, 预期原因：仍未带 `20260724_system_codex_test_management` 的上游依赖 `20260721_admin_full_scope_role_standardization`。
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS, 使用完整 SQL 图校验依赖和元数据。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260731-head-test-release-sql-collation\bug-regression-evidence.md` -> PASS, Bug regression evidence is valid.
- `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence E:\IntRuoyi\doc\tasks\20260731-head-test-release-sql-collation\ci-cd-evidence.md` -> PASS, CI/CD environment evidence is valid.
- `git diff --check -- IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py IntRuoyiBackend/sql/mysql/20260726_dcc_codex_test_items_seed.sql doc/tasks/20260731-head-test-release-sql-collation` -> PASS.

## Issues

- P006 continuation: `20260726_dcc_codex_test_items_seed.sql` 临时表显式 `COLLATE=utf8mb4_unicode_ci`，与测试服目标列 `utf8mb4_0900_ai_ci` 比较时触发 `ERROR 1267`；本任务将以正式 SQL 修复和契约测试阻止复发。

## Staging Boundary

- 主程序仓存在并行任务 tracked 改动：`doc/tasks/20260731-mes-three-tab-test-sync/artifacts/preflight-report.json`、`doc/tasks/20260731-mes-three-tab-test-sync/artifacts/preflight-summary.md`、`doc/tasks/20260731-mes-three-tab-test-sync/tools/three_tab_sync_preflight.py`。
- 本任务不得暂存或提交上述并行任务文件；只允许暂存 SQL、目标 pytest 和 `doc/tasks/20260731-head-test-release-sql-collation/`。
