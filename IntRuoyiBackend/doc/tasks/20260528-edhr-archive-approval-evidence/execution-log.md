# Execution Log: 20260528-edhr-archive-approval-evidence

BDD: Archive persists approval snapshot evidence -> Given an approved closed eDHR execution has an APPROVED approval snapshot, When the archive service generates a SEALED archive, Then the archive record and response include the approval snapshot id/hash and fail if the snapshot is missing or not approved.

BDD: Existing archive reuse preserves approval snapshot evidence -> Given a matching SEALED archive already exists for the same source and approval snapshot, When archive generation is requested without regenerate, Then the existing archive is returned with approval snapshot evidence and no renderer/file/signature side effect occurs.

BDD: Archive download exposes verification metadata -> Given a sealed archive is downloaded through the controlled endpoint, When file bytes match the archive checksum, Then the response includes sha256 plus approval snapshot id/hash for final evidence capture.

- RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected compile failure because archive DO, archive response and download response do not expose `approvalSnapshotId` / `approvalSnapshotHash`.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 19 tests / 0 failures.
- GREEN: `python -X utf8 -m pytest script\tests\test_edhr_approval_archive_schema_contract_sql.py script\tests\test_edhr_archive_sql.py -q` -> PASS, 19 tests.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ExecutionArchiveRendererTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests / 0 failures.
- BLOCKED: initial overall reviewer gate -> no commit because paired real mutating E2E was still missing fresh approve/reject draft records.
- GREEN: paired real UI E2E -> frontend `pnpm e2e:edhr:approval-tracking` PASS after tenant 122 fresh contexts were seeded; generated SEALED archive id `8` for execution `32` with `approvalSnapshotId=19` and approval snapshot hash evidence.
- GREEN: final root DB/hash verification -> `verify-edhr-final-db-hash.cjs` PASS for execution `32`, archive `8`, FIELD_CHANGE revision `1`, archive seal signature binding, approval snapshot binding, and downloaded PDF sha256.
- REGRESSION: final backend focused Maven -> `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS, 19 tests / 0 failures.
- REGRESSION: final backend renderer Maven -> `mvn -pl yudao-module-mes "-Dtest=ExecutionArchiveRendererTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS, 4 tests / 0 failures.
- REGRESSION: final backend SQL contract -> `python -X utf8 -m pytest script\tests\test_edhr_approval_archive_schema_contract_sql.py script\tests\test_edhr_archive_sql.py -q` PASS, 19 tests.
- RED: backend commit hook -> FAIL, `sql/mysql` changed without a changed `script/tests` SQL contract file in the same commit.
- GREEN: backend SQL archive schema contract includes approval snapshot columns in `script\tests\test_edhr_archive_sql.py`; `python -X utf8 -m pytest script\tests\test_edhr_approval_archive_schema_contract_sql.py script\tests\test_edhr_archive_sql.py -q` PASS, 19 tests.
