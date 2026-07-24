# Execution Log

## 2026-05-28

BDD: Domain trace API exposes complete snapshot -> Given a real batch-record execution has all required master-data links, When the backend detail API is called, Then the response returns trace status, hash, item list, versions, source references, and verification timestamp.

BDD: Domain trace blocks incomplete execution -> Given any required master-data link is missing or unverifiable, When domain trace verification or dependent submission/archive logic runs, Then the backend fails with explicit blockers and never returns default success.

RED: Pending backend worker test creation -> FAIL, backend domain trace behavior is not implemented yet.

GREEN: M0 backend task record -> PASS, backend repository task document exists before code changes.

BDD: Domain trace endpoint contract -> Given a backend caller has domain trace query or verify permission, When it calls `/mes/pro/batch-record-execution/domain-trace/detail`, `/page`, or `/verify`, Then the controller delegates to the domain trace service and enforces the locked permission codes.

BDD: Domain trace verification blocks missing master data -> Given a batch-record execution is missing required trace links such as work order, route process, workstation, batch report, batch code, or execution snapshot, When verification runs, Then the service persists/returns `BLOCKED` with explicit blockers and never returns default `VERIFIED`.

BDD: Domain trace expected hash is authoritative -> Given a caller submits `expectedDomainTraceHash`, When the current canonical domain trace hash differs, Then verification fails fast before writing a snapshot.

BDD: Domain trace verify is idempotent by canonical hash -> Given the same execution produces the same canonical `snapshotHash`, When verify is called more than once or a concurrent insert races on the unique snapshot key, Then the service reuses the persisted snapshot/items, updates the execution trace pointer, and never inserts duplicate snapshot/items.

BDD: Domain trace pointer update is transactional -> Given snapshot/items are persisted but the execution domain trace pointer update affects zero rows, When verify completes persistence, Then the service throws `PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED` and the transaction rolls back instead of leaving an orphaned trace.

RED: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, testCompile reports missing DomainTrace Controller/VO/Service/DO/Mapper classes.

RED: python -X utf8 -m pytest script\tests\test_edhr_domain_trace_schema_sql.py -q -> FAIL, `sql/mysql/20260528_edhr_domain_trace_schema.sql` and DomainTrace Java contract files are missing.

GREEN: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 6 tests passed; controller contract, service blockers, expected hash mismatch, and submit-gate verification behavior are covered.

GREEN: python -X utf8 -m pytest script\tests\test_edhr_domain_trace_schema_sql.py -q -> PASS, 5 tests passed; SQL fail-fast preconditions, execution projection columns, append-only snapshot/item schema, permissions, and Java contract fields are covered.

GREEN: python -X utf8 -m pytest script\tests\test_edhr_approval_archive_schema_contract_sql.py script\tests\test_edhr_field_audit_sql.py script\tests\test_edhr_domain_trace_schema_sql.py -q -> PASS, 28 tests passed; adjacent eDHR approval/archive and field-audit SQL contracts still pass.

BLOCKED: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, H2 test schema `yudao-module-mes/src/test/resources/sql/create_tables.sql` lacks new `domain_trace_snapshot_id`, `domain_trace_hash`, `domain_trace_status`, and `domain_trace_verified_at` columns. That file is outside this backend worker's permitted write scope, so the existing DB-backed regression cannot be made green in this slice without explicit scope expansion.

GREEN: Recheck 2026-05-28 00:57 -> python -X utf8 -m pytest script\tests\test_edhr_domain_trace_schema_sql.py -q -> PASS, 5 tests passed; `sql/mysql/20260528_edhr_domain_trace_schema.sql` exists and satisfies the domain trace SQL contract.

GREEN: Recheck 2026-05-28 00:57 -> mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 6 tests passed.

BLOCKED: Recheck 2026-05-28 00:58 -> mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, unchanged blocker: H2 test schema `yudao-module-mes/src/test/resources/sql/create_tables.sql` does not define the new `domain_trace_*` execution columns, so DB-backed legacy execution tests fail at MyBatis select with `org.springframework.jdbc.BadSqlGrammarException` / H2 `Column "domain_trace_snapshot_id" not found`.

BLOCKED: task-closeout-cleanup preview -> BLOCKED, linked worktree cannot be fast-forward merged into `int_main` and the preview classified current pending task files as uncommitted work. No cleanup apply, git stage, commit, merge, or worktree removal was performed.

BDD: Reviewer gate H2 schema regression -> Given the existing execution service regression test loads `yudao-module-mes/src/test/resources/sql/create_tables.sql`, When DomainTrace fields and tables are referenced by the updated execution/domain trace code, Then H2 test schema must expose the new execution `domain_trace_*` columns plus domain trace snapshot/item tables instead of treating the regression as blocked.

GREEN: Reviewer repair recheck 2026-05-28 01:06 -> mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 40 tests passed; H2 schema now includes the DomainTrace execution columns and required snapshot/item tables.

GREEN: Reviewer repair recheck 2026-05-28 01:06 -> python -X utf8 -m pytest script\tests\test_edhr_domain_trace_schema_sql.py -q -> PASS, 5 tests passed; production SQL domain trace contract remains satisfied.

GREEN: Main reviewer re-run 2026-05-28 01:10 -> python -X utf8 -m pytest script\tests\test_edhr_domain_trace_schema_sql.py -q -> PASS, 5 tests passed.

GREEN: Main reviewer re-run 2026-05-28 01:10 -> mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 40 tests passed; execution submit regression, DomainTrace service, and DomainTrace controller contracts are green.

GREEN: Main reviewer local DB migration 2026-05-28 -> Python `pymysql` applied `sql/mysql/20260528_edhr_domain_trace_schema.sql` to local MySQL `127.0.0.1:23306/ruoyi-vue-pro`; tenant 122 exists, DomainTrace execution columns, snapshot table, and two permissions are present.

GREEN: Main reviewer current backend package 2026-05-28 -> `mvn -pl yudao-server -am -DskipTests package` -> PASS, current `yudao-server.jar` built.

BLOCKED: Main reviewer current backend first runtime 2026-05-28 -> FAIL fast, missing explicit `dcc.signature.evidence.hmac-secret` and `dcc.signature.evidence.key-version`; startup rejected incomplete DCC signature evidence configuration.

GREEN: Main reviewer current backend runtime 2026-05-28 -> `yudao-server.jar` started on `http://127.0.0.1:48080` with local MySQL `23306`, Redis `26379`, and explicit DCC signature evidence test config; `/actuator/health` returned UP.

GREEN: Main reviewer logged-in API probe 2026-05-28 -> `tenant-id=122 / aoteman` on backend `48080`, `GET /admin-api/mes/pro/batch-record-execution/domain-trace/detail?executionId=9` -> PASS, returned canonical `status=BLOCKED`, `domainTraceHash`, blockers, and items for `BRE202605242206492170009`.

RED: Final reviewer idempotency compile gate 2026-05-28 -> `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, testCompile reported missing `selectByExecutionIdAndSnapshotHash`, proving the mapper contract could not express canonical-hash reuse yet.

RED: Final reviewer idempotency behavior gate 2026-05-28 -> after adding the mapper query contract, the same command -> FAIL, repeated verify still attempted a second snapshot insert and raised `DuplicateKeyException: Duplicate domain trace snapshot hash`.

GREEN: Final reviewer idempotency repair 2026-05-28 -> `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests passed; repeated verify reuses persisted snapshot/items and expected-hash mismatch still fails before writing.

RED: Final reviewer transaction/concurrency gate 2026-05-28 -> `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 2 tests failed: concurrent duplicate snapshot insert was not reloaded and execution pointer update returning 0 did not fail fast.

GREEN: Final reviewer transaction/concurrency repair 2026-05-28 -> `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 7 tests passed; duplicate insert races reload the same persisted snapshot/items, missing persisted items fail fast, and execution pointer update failure raises `PRO_BATCH_RECORD_DOMAIN_TRACE_PERSIST_FAILED`.

GREEN: Final reviewer backend focused suite 2026-05-28 -> `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 43 tests passed.

GREEN: Final reviewer backend SQL 2026-05-28 -> `python -X utf8 -m pytest script\tests\test_edhr_domain_trace_schema_sql.py -q` -> PASS, 5 tests passed.

GREEN: Final reviewer current backend package 2026-05-28 -> `mvn -pl yudao-server -am -DskipTests package` -> PASS, rebuilt current `yudao-server.jar` after the idempotency repair.

GREEN: Final reviewer current backend runtime 2026-05-28 -> rebuilt `yudao-server.jar` started on `http://127.0.0.1:48080` with dynamic datasource pointed at local MySQL `23306`, Redis `26379`, and explicit DCC signature evidence test config; `/actuator/health` returned HTTP 200.

INFO: Final reviewer environment residual 2026-05-28 -> backend logs show an unrelated scheduled DCC task querying missing local table `dcc_nas_acl_restore_plan`; this was not triggered by DomainTrace E2E and is recorded as a local environment residual risk rather than hidden.
