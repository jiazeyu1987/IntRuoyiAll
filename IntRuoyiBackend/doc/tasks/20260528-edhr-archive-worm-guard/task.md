# eDHR Archive WORM Guard Task

## Goal

Implement and verify eDHR archive immutability/WORM database gates.

## Scope

- `mes_pro_batch_record_execution_archive_event` must become append-only. UPDATE and DELETE must be rejected by the database.
- `mes_pro_batch_record_execution_archive` rows whose old value is `archive_status = 'SEALED'` must reject UPDATE and DELETE at the database layer.
- Non-SEALED archive rows must remain available for existing service-layer lifecycle changes.
- SQL migration must fail fast when required tables are missing.
- Migration must not mock, fallback, silently skip protection, or delete data.
- Verification must use strict TDD: RED, GREEN, REGRESSION.

## Known Context

- DomainTrace `snapshot` and `item` append-only triggers already exist in `sql/mysql/20260528_edhr_domain_trace_schema.sql`.
- Archive base schema exists in `sql/mysql/20260525_edhr_archive_schema.sql`.
- Archive main/event tables still need database-level WORM protection.

## Milestones

| Milestone | Status | Output |
| --- | --- | --- |
| M1 Documentation package | Completed | Task docs, BDD, TDD plan, evidence templates |
| M2 Schema contract RED | Completed | Failing contract test proving missing archive WORM guards |
| M3 SQL migration GREEN | Completed | Split fail-fast WORM guard migration from base archive schema |
| M4 Regression verification | Completed | Main reviewer checks passed and independent re-review passed |
| M5 Closeout | Completed | Evidence updated, cleanup preview recorded, final status |

## BDD

### BDD: Archive event rows are append-only

Given an existing row in `mes_pro_batch_record_execution_archive_event` When UPDATE or DELETE is attempted against that row Then the database rejects the operation
And the original event row remains unchanged.

### BDD: SEALED archive rows are immutable

Given an existing row in `mes_pro_batch_record_execution_archive` with old `archive_status = 'SEALED'` When UPDATE or DELETE is attempted against that row Then the database rejects the operation
And the original archive row remains unchanged.

### BDD: Non-SEALED archive rows remain editable by existing flow

Given an existing row in `mes_pro_batch_record_execution_archive` whose old `archive_status` is not `SEALED` When the existing service-layer process performs a required valid update Then the database allows the update
And no fallback path is introduced.

### BDD: Migration fails fast on missing prerequisites

Given the WORM migration is applied to a database missing a required archive table When the migration starts Then it fails before partial WORM object creation
And it does not mock, fallback, skip, or delete data.

## TDD Order

1. RED: write schema contract assertions first; run them before implementation and record the expected failure caused by missing archive WORM guards.
2. GREEN: implement the minimal SQL migration with fail-fast precondition checks and required triggers.
3. REGRESSION: run schema contract tests, SQL migration checks, and real DB verifier.

## Acceptance Commands

```powershell
python -X utf8 -m pytest script\tests\test_edhr_archive_sql.py -q
java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar" doc\tasks\20260528-edhr-archive-worm-guard\scripts\VerifyEdhrArchiveWormMysql.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 sql\mysql\20260525_edhr_archive_schema.sql sql\mysql\20260528_edhr_archive_worm_guard.sql
mvn -pl yudao-module-mes "-Dtest=MesBatchRecordBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Real DB Safety

- Use only test tenant data, dedicated test rows, or transaction rollback/cleanup.
- Do not modify the `芋道源码` tenant during development, debugging, or verification.
- Direct database rejection must be verified; service-only checks are insufficient.

## Current Status

`completed`

Independent reviewer failed the previous implementation because `sql/mysql/20260525_edhr_archive_schema.sql` embedded WORM prerequisite logic and triggers, while the new contract requires an independent `sql/mysql/20260528_edhr_archive_worm_guard.sql` migration. Worker repair intent:

- Keep `20260525_edhr_archive_schema.sql` as base table schema only.
- Add standalone WORM guard SQL with fail-fast prerequisite checks before trigger creation.
- Update the MySQL verifier to execute base schema first, WORM guard second, then verify normal WORM behavior and missing-table SQLSTATE `45000` behavior in temporary databases only.
- Do not claim final PASS; main reviewer owns final verification.

Worker-run verification:

- `python -X utf8 -m pytest script\tests\test_edhr_archive_sql.py -q` -> worker check passed, 3 tests.
- `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar" doc\tasks\20260528-edhr-archive-worm-guard\scripts\VerifyEdhrArchiveWormMysql.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 sql\mysql\20260525_edhr_archive_schema.sql sql\mysql\20260528_edhr_archive_worm_guard.sql` -> worker check passed; final reviewer verification still pending.

Main reviewer verification after worker repair:

- `python -X utf8 -m pytest script\tests\test_edhr_archive_sql.py -q` -> PASS.
- `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar" doc\tasks\20260528-edhr-archive-worm-guard\scripts\VerifyEdhrArchiveWormMysql.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 sql\mysql\20260525_edhr_archive_schema.sql sql\mysql\20260528_edhr_archive_worm_guard.sql` -> PASS; missing-table SQLSTATE `45000` confirmed for both prerequisite tables.
- `mvn -pl yudao-module-mes "-Dtest=MesBatchRecordBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.

Independent reviewer `019e6c1f-62c8-73f3-924c-fa6054232611` returned `final_decision: pass`, with `blocking_issues=[]` and `required_changes=[]`.

## Cleanup Keep

Keep:

- `doc/tasks/20260528-edhr-archive-worm-guard/`
- `doc/tasks/20260528-edhr-archive-worm-guard/scripts/VerifyEdhrArchiveWormMysql.java`
- `doc/tasks/20260528-edhr-archive-worm-guard/verification-report.md`
- `script/tests/test_edhr_archive_sql.py`
- `sql/mysql/20260525_edhr_archive_schema.sql`
- `sql/mysql/20260528_edhr_archive_worm_guard.sql`
- `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesBatchRecordBaseSchemaTest.java`

Closeout cleanup may only remove task-specific temporary artifacts created during later implementation or verification.
