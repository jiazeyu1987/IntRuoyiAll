# Database Schema Evidence

## Data Change Goal

- Add a durable, tenant-aware DCC file category match-rule table so project-code associated-file classification can be governed by data instead of hardcoded aliases only.
- Seed explicit OQ/PQ and component drawing rules that address the test-server `AMBIGUOUS` / `UNCLASSIFIED` gaps without directly rewriting `dcc_controlled_file`.

## Affected Entities

- New table: `dcc_file_category_match_rule`.
- Existing read path: `dcc_file_category` active categories.
- Existing write path: `dcc_controlled_file.fileTypeLevel2/fileTypeLevel3/fileTypeTaxonomyId` through the official project-code classification service only.

## Engine And Migration Tool

- Engine: MySQL/InnoDB.
- Migration location: `IntRuoyiBackend/sql/mysql`.
- Release gate: `script/release/run-release-migration-policy-gate.py`.

## Schema And Seed Changes

- `20260731_dcc_file_category_match_rule.sql`: creates rule table with `category_id`, `match_text`, `match_type`, `weight`, `active`, audit columns, unique key, and category lookup index.
- `20260731_dcc_file_category_match_rule_seed.sql`: seeds OQ/PQ text rules and drawing extension/text rules through a fail-fast stored procedure.
- Test fixture: `yudao-module-dcc/src/test/resources/sql/create_tables.sql` includes the new table and indexes.

## Data Safety

- Schema migration is additive: `CREATE TABLE IF NOT EXISTS`.
- Seed migration is idempotent through `NOT EXISTS` and the unique key.
- Seed migration fails fast with `SIGNAL` if required category names are missing, duplicated per tenant, or insertion is incomplete.
- No SQL updates, deletes, or backfills against `dcc_controlled_file`.

## Rollback Or Recovery

- Rollback for schema/seed deployment is to remove seeded `dcc_file_category_match_rule` rows and drop the additive table only through an explicit rollback task with backup approval.
- Runtime classification can be paused operationally by marking seeded rules inactive; this is not implemented as fallback code.

## BDD

- `BDD: 可维护规则消除 OQ/PQ 宽泛工艺歧义 -> Given 启用类别同时存在 OQ/PQ 验证类别和工序卡/作业指导书类别, When 文件名包含 OQ/PQ 明确验证方案或报告规则, Then 官方分类选择对应 OQ/PQ 类别并落入其阶段/文件类型, And 不因宽泛工艺关键词返回 AMBIGUOUS。`
- `BDD: 可维护规则识别图纸类未分类文件 -> Given 启用类别存在绑定文件类型的零配件图纸类别, When 项目代码关联文件名或标题包含受控图纸扩展名或图纸关键词, Then 分类结果写入零配件图纸的阶段/文件类型。`

## RED

- `RED: mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test -> FAIL, expected missing formal rule DO/Mapper/schema.`
- Command: `mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test`
- Result: `FAIL`
- Expected reason: the rule DO/Mapper/schema did not exist before implementation.

## GREEN

- `GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest,DccBaseSchemaTest#mysqlSchemaShouldSupportDccFileCategoryMatchRules" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS.`
- Command: `mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest,DccBaseSchemaTest#mysqlSchemaShouldSupportDccFileCategoryMatchRules" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: `PASS`; `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`.

## Migration Verification

- Command: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_sql.py -q`
- Result: `PASS`; `3 passed`.
- Command: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output doc\tasks\20260731-dcc-file-category-rules\migration-policy-gate.json`
- Result: `PASS`; `status=passed`, `migrationCount=401`, includes `20260731_dcc_file_category_match_rule` and `20260731_dcc_file_category_match_rule_seed`.

## Blockers

- None for this schema slice.
- No remote/test-server database migration was executed in this clean worktree task.
