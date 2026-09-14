# Database Schema Evidence

## Data

- Data change goal: NAS transfer task 持久化 `dcc_project_code_id`，让 NAS 异步任务可在执行阶段按 DCC 项目代码重新计算产品编号。
- Affected entities: `dcc_controlled_file_nas_transfer_task`、DCC base schema、DCC test schema fixture、`DccControlledFileNasTransferTaskDO`。
- Database engine: MySQL migration files under `IntRuoyiBackend/sql/mysql` plus H2/MySQL-like test fixture under `yudao-module-dcc/src/test/resources/sql/create_tables.sql`。

## Migration

- Added migration: `IntRuoyiBackend/sql/mysql/20260728_dcc_nas_transfer_project_code.sql`。
- Updated base schema: `20260513_dcc_base_schema.sql` and `20260523_dcc_nas_transfer_task.sql` include `dcc_project_code_id` on NAS transfer tasks.
- Updated tests: `DccBaseSchemaTest` and `create_tables.sql` validate schema consistency for DCC tests.

## Safety

- Additive nullable column only; no destructive migration and no historical data rewrite.
- Existing `product_master_id` remains nullable for old NAS task/display compatibility, but new writes persist `null`.
- No production SQL execution was performed in this task.

## Rollback

- Rollback plan for the migration file: drop `dcc_project_code_id` from `dcc_controlled_file_nas_transfer_task` only if the DCC/NAS code change is also rolled back.
- No data backfill rollback is required because the migration is additive and does not mutate existing rows.

## BDD

- `BDD: NAS transfer task preserves project code source -> Given 用户创建 NAS transfer / When 任务异步执行 / Then task row has dcc_project_code_id and execution writes productCode/projectName from DCC project code, productMasterId null。`

## RED

- `RED: DccBaseSchemaTest before schema update -> FAIL expected, NAS transfer task schema lacked dcc_project_code_id required by new DO/service contract。`

## GREEN

- `GREEN: mvn -pl yudao-module-dcc "-Dtest=DccBaseSchemaTest" test included in target suite -> PASS, DccBaseSchemaTest Tests run: 30, Failures: 0, Errors: 0。`
- `GREEN: full DCC target suite -> PASS, includes DccBaseSchemaTest and NAS transfer tests。`

## Verification

- Migration verification was static/test-fixture based; no live DB migration applied.
- DCC target tests verify task persistence and execution contract with `dccProjectCodeId`.

## Blockers

- None. Live migration apply/down was not required; local runtime schema preflight and DCC tests passed.
