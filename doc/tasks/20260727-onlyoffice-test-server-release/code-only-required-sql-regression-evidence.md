# Code-only Required SQL Regression Evidence

## Bug Summary

Code-only test-server releases skipped the database dump and MinIO sync but still executed every pending `required SQL` migration. Unrelated `type=data` migrations could therefore inspect or modify test-server business data and block an OnlyOffice release.

## Expected Behavior

When `publishScope=code-only`, `type=data` required SQL must not enter the remote MySQL APPLY queue. Non-data migrations remain eligible, and missing manifest type metadata must fail fast.

## Reproduction

- Build a code-only release with `-SkipDatabaseSync -SkipMinioSync`.
- Deploy a package whose preflight plan contains an unapplied `type=data` migration such as `20260709_mes_rt000006_batch_record_mapping`.
- Before this fix, the deploy script passed every `APPLY` item to remote MySQL and failed on the missing `RT000006` route.

## Root Cause

The deploy script loaded required SQL from the release manifest without preserving `type` and passed every preflight `APPLY` item directly to the SQL executor. The code-only scope was only applied to full database and MinIO synchronization.

## Regression Test

`script/tests/test_code_only_required_sql_contract.py`

## TDD Evidence

- RED: `python -X utf8 -m pytest script\tests\test_code_only_required_sql_contract.py -q` -> FAIL, `2 failed`; manifest type fields and scope-aware apply filtering were absent.
- GREEN: `python -X utf8 -m pytest script\tests\test_code_only_required_sql_contract.py -q` -> PASS, `2 passed`.
- GREEN: targeted release regression suite -> PASS, `123 passed`.
- GREEN: migration policy gate -> PASS, `migrationCount=383`.

## Verification

- Deploy entries preserve `type`, `migrationId`, `file`, and `sha256` from manifest required SQL.
- Code-only filtering runs before `Info "Applying required database SQL: $fileName"` and logs `Skipping data required database SQL for code-only release`.
- APPLY items missing from manifest required SQL fail fast before remote MySQL execution.

## Risk And Scope

The change only narrows the remote SQL execution queue for `publishScope=code-only`. Full data releases retain the existing APPLY behavior. Database dumps and MinIO synchronization remain disabled by `SkipDatabaseSync` and `SkipMinioSync`.

## Blockers

The old release tags remain invalid. A new package and release tag are required for test-server deployment.
