# 测试服发布 SQL collation 阻塞修复执行日志

## BDD Scenarios

BDD: DCC Codex seed collation follows live target schema -> Given 测试服 `system_codex_test_case` 与 `system_codex_test_checkpoint` 字符列为 `utf8mb4_0900_ai_ci`, When required SQL `20260726_dcc_codex_test_items_seed.sql` 创建临时 seed 表并与目标表做名称、状态和检查点比较, Then 临时 seed 表字符串列必须使用相同 collation，发布不得触发 MySQL `ERROR 1267`。

BDD: Failed publish tag is not reused -> Given 上一轮 releaseTag 已在 required SQL 阶段失败且 `.env IMAGE_TAG` 与实际镜像漂移, When SQL 修复完成, Then 必须提交修复后使用新的 release worktree 和新的 releaseTag 重新构建发布，不得手工补旧 releaseTag。

## Execution Evidence

- GREEN: worktree-created -> PASS，`D:\IntRuoyiWorktree\r260729-sql-collation` 基于 `E:\IntRuoyi` HEAD `e56433700dc88a743c8707210f0da33edea41abc` 创建，分支 `codex/20260729-test-release-sql-collation`，主工作区并行改动未被触碰。
- GREEN: slot-reserved -> PASS，`reserve-worktree-slot.ps1` 分配 `int_main` slot `14`，frontend `8095`，backend `48095`。
- GREEN: experience-preflight -> PASS，已读取 `docs/database-rules.md`、`docs/backend-development.md`、`docs/worktree-restrictions.md`、`docs/powershell-memory.md`，命中数据修复临时表排序规则门禁。
- RED: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> FAIL，新增测试 `test_dcc_codex_test_items_seed_temp_tables_match_live_target_collation` 断言临时 seed 表必须包含 `collate=utf8mb4_0900_ai_ci`，当前 SQL 实际为 `collate=utf8mb4_unicode_ci`。
- GREEN: sql-collation-fix -> PASS，仅修改 `20260726_dcc_codex_test_items_seed.sql` 中 `tmp_dcc_codex_test_case_seed` 与 `tmp_dcc_codex_test_checkpoint_seed` 的 `COLLATE` 为 `utf8mb4_0900_ai_ci`，不修改真实表 schema、不改数据库默认 collation。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> PASS，5 passed。
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output ..\doc\tasks\20260729-test-release-sql-collation-fix\migration-policy-gate.json` -> PASS，`status=passed`，`migrationCount=390`。
- GREEN: `git diff --check -- IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py IntRuoyiBackend/sql/mysql/20260726_dcc_codex_test_items_seed.sql doc/tasks/20260729-test-release-sql-collation-fix` -> PASS，仅有 LF/CRLF 工作区提示，无 diff check 错误。
- GREEN: evidence-validators -> PASS，`validate_bug_regression.py` 与 `validate_database_schema.py` 均通过。
- GREEN: experience-consolidation -> PASS，`docs/database-rules.md` 既有“数据修复临时表排序规则门禁”覆盖本轮经验；已补充本任务 evidence，并在 `docs/experience-index.md` 关键词中加入 `20260726_dcc_codex_test_items_seed`。

## Problem Records

1. 现象：上一轮 `publish-test` 执行 `20260726_dcc_codex_test_items_seed.sql` 时报 MySQL `ERROR 1267 Illegal mix of collations`。
   阶段：required SQL 应用。
   影响：测试服发布失败，目标 releaseTag 未切换到实际容器。
   原因判断：`tmp_dcc_codex_test_case_seed` / `tmp_dcc_codex_test_checkpoint_seed` 以 `utf8mb4_unicode_ci` 创建，而测试服目标字符列为 `utf8mb4_0900_ai_ci`。
   处理动作：本轮先增加 RED 静态 SQL 合同，再修复 SQL。
   结果：定向 pytest 与 release migration policy gate 均通过。
   是否可前置检查：是。
   是否可自动化：是。
   下次如何避免：migration policy gate 增加 seed 临时表 collation 扫描，发布前用真实库 schema 核对。
