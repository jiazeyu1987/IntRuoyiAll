# Backend API Evidence

## Scope

- Service scope: `DccProjectCodeServiceImpl#classifyAssociatedFileByName` and project-code associated-file category target resolution.
- Behavior change: official project-code classification now reads active, data-driven `dcc_file_category_match_rule` rows before falling back to existing category-name/alias scoring.
- API shape: no controller, request, response, enum, or route changes.

## Contract

- Classification still only writes target stage/file type after the user can see the associated file and the file requires classification.
- Valid existing category mappings are preserved; rules only influence the target selection for candidate files.
- Unsupported rule types fail fast with `IllegalStateException`.
- Blank rule text fails fast with `IllegalStateException`.

## Auth And Permissions

- Existing user visibility and project-code assignment checks remain unchanged.
- No new permissions, roles, menu entries, or tenant bindings are introduced.

## Config, Services, Fixtures, And Migrations

- New mapper/DO: `DccFileCategoryMatchRuleMapper`, `DccFileCategoryMatchRuleDO`.
- New schema/seed migrations: `20260731_dcc_file_category_match_rule.sql`, `20260731_dcc_file_category_match_rule_seed.sql`.
- Test fixture table added to `yudao-module-dcc/src/test/resources/sql/create_tables.sql`.

## BDD

- `BDD: 可维护规则消除 OQ/PQ 宽泛工艺歧义 -> Given 启用类别同时存在 OQ/PQ 验证类别和工序卡/作业指导书类别, When 文件名包含 OQ/PQ 明确验证方案或报告规则, Then 官方分类选择对应 OQ/PQ 类别并落入其阶段/文件类型, And 不因宽泛工艺关键词返回 AMBIGUOUS。`
- `BDD: 可维护规则识别图纸类未分类文件 -> Given 启用类别存在绑定文件类型的零配件图纸类别, When 项目代码关联文件名或标题包含受控图纸扩展名或图纸关键词, Then 分类结果写入零配件图纸的阶段/文件类型。`
- `BDD: 泛化同分仍显式歧义 -> Given 两个启用类别只有相同强度的泛化匹配规则, When 文件名同时命中两者且没有更明确规则, Then 分类结果仍为 AMBIGUOUS 并保留候选用于人工规则治理。`

## RED

- `RED: mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test -> FAIL, expected missing formal rule DO/Mapper.`
- Command: `mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test`
- Result: `FAIL`
- Expected reason: new tests referenced `DccFileCategoryMatchRuleDO` and `DccFileCategoryMatchRuleMapper`; production code did not yet define the formal rule model/mapper.

## GREEN

- `GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest,DccBaseSchemaTest#mysqlSchemaShouldSupportDccFileCategoryMatchRules" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS.`
- Command: `mvn -pl yudao-module-dcc -am "-Dtest=DccProjectCodeServiceImplTest,DccBaseSchemaTest#mysqlSchemaShouldSupportDccFileCategoryMatchRules" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: `PASS`
- Evidence: `BUILD SUCCESS`; `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`.

## Validation

- Evidence validators are required before cleanup; results are copied into `execution-log.md` and `verification-report.md`.

## Contract Verification

- Command: `python -X utf8 -m pytest script/tests/test_dcc_file_category_match_rule_sql.py -q`
- Result: `PASS`; `3 passed`.
- Command: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output doc\tasks\20260731-dcc-file-category-rules\migration-policy-gate.json`
- Result: `PASS`; `status=passed`, `migrationCount=401`.

## Observability

- No new logging side effects.
- Failure is explicit through exceptions for malformed rule rows and through migration `SIGNAL` checks for missing/ambiguous seed categories.

## Blockers

- None for the code/schema slice.
- Test-server data has not been modified in this code task; applying the new rules to runtime data requires a later authorized test-server deploy/migration/batch-recognition run.
