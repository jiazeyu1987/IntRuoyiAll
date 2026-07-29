# Execution Log

## BDD Scenarios

BDD: smart scheduling seed collation matches live target -> Given 测试服 `system_codex_test_case` / `system_codex_test_checkpoint` 字符列使用 MySQL 8 目标排序规则, When release required SQL 执行 `20260726_system_codex_smart_scheduling_test_items.sql`, Then 临时表与真实表比较不得触发 `ERROR 1267 Illegal mix of collations`。

## Execution Evidence

- REQUEST: 维护仓仅测试服发布 r2 失败，`publish-test` operation `op-2026-07-29T044320236880700Z-fb6c43d9-09d3-435d-bf81-72f84370cdb4` 报 `20260726_system_codex_smart_scheduling_test_items.sql` line 313 collation mismatch。
- RED: `python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py -q` -> FAIL，新增 `test_smart_scheduling_test_items_temp_tables_match_live_target_collation` 断言缺少 `collate=utf8mb4_0900_ai_ci`。
- GREEN: 修改 `20260726_system_codex_smart_scheduling_test_items.sql` 中 `tmp_codex_smart_scheduling_case_seed`、`tmp_codex_smart_scheduling_case_ids`、`tmp_codex_smart_scheduling_checkpoint_seed` 的 DDL，显式 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`。
- GREEN: `python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py -q` -> PASS，`9 passed in 0.20s`。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260729-head-test-only-release\evidence\migration-policy-gate-r260729c.json` -> PASS。
- GREEN: git-push -> PASS，提交 `7d5677c7ba913d9037ae9f22d8245af0b5061e01` 与 `d15521bd5acc17c78ecfdee063f555039b87bc67` 已推送到 `origin/codex/20260729-test-release-sql-collation`，工作区除 pytest/cache 忽略目录外 clean。

## Problem Record

- 现象：`publish-test` r2 失败于 `20260726_system_codex_smart_scheduling_test_items.sql`，MySQL `ERROR 1267 Illegal mix of collations (utf8mb4_general_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT) for operation '='`。
- 阶段：测试服 required SQL 应用。
- 影响：测试服 `.env IMAGE_TAG` 已写为 r2，但 backend/frontend 实际容器仍为旧镜像，发布不能判定成功。
- 原因判断：该 seed SQL 的临时表没有显式 collation，真实测试库目标列为 `utf8mb4_0900_ai_ci`，默认临时列排序规则与目标列比较冲突。
- 处理动作：冻结失败证据，补 RED 测试，显式统一三个临时表 collation，运行定向回归和 migration policy gate。
- 结果：本地修复验证通过，必须提交后用新 releaseTag 重建重发。
- 是否可前置检查：是，release migration policy gate 应补强临时表/目标表 collation 比较扫描。
- 是否可自动化：是，SQL 静态测试可要求所有测试项 seed 临时表显式目标 collation。
- 下次如何避免：新增或修改 seed SQL 时先核对 live target column collation，临时表 DDL 不得只写 `DEFAULT CHARSET=utf8mb4`。
