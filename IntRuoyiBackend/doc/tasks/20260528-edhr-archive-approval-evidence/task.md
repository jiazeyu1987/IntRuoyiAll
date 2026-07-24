# Task: EDHR Archive Approval Snapshot Evidence

## Goal

Persist and expose `approvalSnapshotId` and `approvalSnapshotHash` through the eDHR archive evidence chain: archive DO/table, archive generation response, download response, existing sealed archive reuse, and focused backend tests.

## Milestones

- [completed] M0 Create backend task record before code changes.
- [completed] M1 Add RED backend tests for missing approval snapshot evidence in archive generation, reuse, download, and contract surface.
- [completed] M2 Implement minimal backend schema/DO/VO/service changes to pass tests without fallback behavior.
- [completed] M3 Run focused Maven and SQL contract verification.
- [completed] M4 Record GREEN/BLOCKED evidence and reviewer findings.

## BDD

BDD: Archive persists approval snapshot evidence -> Given an approved closed eDHR execution has an APPROVED approval snapshot, When the archive service generates a SEALED archive, Then the archive record and response include the approval snapshot id/hash and fail if the snapshot is missing or not approved.

BDD: Existing archive reuse preserves approval snapshot evidence -> Given a matching SEALED archive already exists for the same source and approval snapshot, When archive generation is requested without regenerate, Then the existing archive is returned with approval snapshot evidence and no renderer/file/signature side effect occurs.

BDD: Archive download exposes verification metadata -> Given a sealed archive is downloaded through the controlled endpoint, When file bytes match the archive checksum, Then the response includes sha256 plus approval snapshot id/hash for final evidence capture.

## Expected Verification

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionArchiveContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest script\tests\test_edhr_approval_archive_schema_contract_sql.py script\tests\test_edhr_archive_sql.py -q`

## Current Status

Completed for this backend slice. Backend archive approval snapshot evidence is implemented, focused Maven/SQL verification is GREEN, paired real UI E2E passed with archive id `8`, and final DB/hash verification passed against execution `32`.
