# Database Schema Evidence

## Goal

修复 Codex 智能排产测试项 seed required SQL 的临时表排序规则，使其在 MySQL 8 目标库 `utf8mb4_0900_ai_ci` 字符列上可重复执行。

## Affected Entities

- `system_codex_test_case`
- `system_codex_test_checkpoint`
- `tmp_codex_smart_scheduling_case_seed`
- `tmp_codex_smart_scheduling_case_ids`
- `tmp_codex_smart_scheduling_checkpoint_seed`

## Data Safety

- 本次只修改迁移 SQL 源文件和静态测试，不直接写入测试服、正式服或备份服数据库。
- 已失败 releaseTag 不复用；修复后必须重新构建新 releaseTag。

## Rollback / Recovery

- 源码回滚：回退本次提交即可恢复旧 SQL。
- 运行环境恢复：若发布失败后的测试服 `.env` / 容器漂移需处理，必须另行取得用户授权，不在本修复中手工改库或改远端状态。

## BDD Scenarios

- BDD: Codex smart scheduling seed collation -> Given 目标库文本列可能为 `utf8mb4_0900_ai_ci`, When required SQL 使用临时 seed 表写入和校验测试项, Then 临时字符串列必须显式对齐目标 collation，避免 MySQL `ERROR 1267`。
- BDD: No database fallback -> Given 发布 required SQL 在测试服失败, When 修复 seed SQL, Then 不修改数据库默认 collation、不手工更新 migration/lock、不复用失败 releaseTag。

## Changes

- `sql/mysql/20260726_system_codex_smart_scheduling_test_items.sql`：三个 `tmp_codex_smart_scheduling_*` 临时表增加 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci`。
- `script/tests/test_codex_smart_scheduling_test_items_seed.py`：新增静态回归测试，断言临时表 collation 与目标测试管理文本列一致。

## Migration

- Migration tool: repository release migration policy gate over sql/mysql.
- Migration verification command: python -X utf8 script\\release\\run-release-migration-policy-gate.py --sql-root sql\\mysql --output D:\\IntRuoyiWorktree\\r260801b-frozen-smartseed-fix\\doc\\tasks\\20260801-smart-seed-collation-fix\\migration-policy-gate.json.
- Result: PASS, status=passed, migrationCount=403.

## Verification

- RED: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> FAIL, expected collation assertion failure。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 4 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix\doc\tasks\20260801-smart-seed-collation-fix\migration-policy-gate.json` -> PASS, status=`passed`, migrationCount=`403`。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py script/tests/test_dcc_codex_test_items_seed.py script/tests/test_codex_test_case_project_migration.py -q` -> PASS, 11 passed。

## Blockers

无当前 SQL 修复 blocker。测试服失败 releaseTag 仍不得复用，后续发布必须基于新提交重新构建新 releaseTag。
