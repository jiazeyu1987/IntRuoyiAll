# Code-only Required SQL Regression Evidence

## Bug Summary

Code-only test-server releases skipped the database dump and MinIO sync but still executed every pending `required SQL` migration. Unrelated `type=data` migrations could therefore inspect or modify test-server business data and block an OnlyOffice release.

## Expected Behavior

When `publishScope=code-only`, `type=data` required SQL and every direct or transitive dependent must not enter the remote MySQL APPLY queue. Independent non-data migrations remain eligible, and missing manifest type or dependency metadata must fail fast.

## Reproduction

- Build a code-only release with `-SkipDatabaseSync -SkipMinioSync`.
- Deploy a package whose preflight plan contains an unapplied `type=data` migration such as `20260709_mes_rt000006_batch_record_mapping`.
- Before this fix, the deploy script passed every `APPLY` item to remote MySQL and failed on the missing `RT000006` route.
- After direct data filtering was added, r3 skipped RT000006 but still selected `20260720_dcc_obsolete_approval_bpm_seed`, whose declared dependency `20260719_dcc_obsolete_form_policy_seed` is `type=data`; it failed because the dependent seed ran without its skipped prerequisite.

## Root Cause

The first defect was that deploy entries did not preserve `type`, so every preflight `APPLY` item reached the SQL executor. The second defect was that direct data filtering did not preserve `dependsOn` or compute the data dependency closure, leaving seed/menu/schema descendants eligible after their data prerequisite was removed.

## Regression Test

`script/tests/test_code_only_required_sql_contract.py`

## TDD Evidence

- RED: `python -X utf8 -m pytest script\tests\test_code_only_required_sql_contract.py -q` -> FAIL, `2 failed`; manifest type fields and scope-aware apply filtering were absent.
- GREEN: `python -X utf8 -m pytest script\tests\test_code_only_required_sql_contract.py -q` -> PASS, `2 passed`.
- GREEN: targeted release regression suite -> PASS, `123 passed`.
- RED: dependency-closure regression -> FAIL, `3 failed, 1 passed`; `dependsOn` was not preserved, transitive dependents remained selected, and missing dependency metadata did not fail fast.
- GREEN: dependency-closure regression -> PASS, `4 passed`.
- GREEN: expanded targeted release regression suite -> PASS, `125 passed`.
- GREEN: migration policy gate -> PASS, `migrationCount=383`.

## Verification

- Deploy entries preserve `type`, `migrationId`, `dependsOn`, `file`, and `sha256` from manifest required SQL.
- Code-only filtering runs before `Info "Applying required database SQL: $fileName"` and logs `Skipping data required database SQL for code-only release`.
- Direct and transitive dependents log `Skipping required database SQL with data dependency for code-only release`.
- APPLY items or dependency IDs missing from manifest required SQL fail fast before remote MySQL execution.
- Recomputing the r3 real package selected 9 independent APPLY migrations, excluded the failed seed and the three named MES data migrations, and retained the independent form-policy schema migration.

## Risk And Scope

The change only narrows the remote SQL execution queue for `publishScope=code-only` while keeping dependency closure valid. Full data releases retain the existing APPLY behavior. Database dumps and MinIO synchronization remain disabled by `SkipDatabaseSync` and `SkipMinioSync`.

## Blockers

The old release tags through r3 remain invalid. A new r4 package and release tag are required for test-server deployment.
