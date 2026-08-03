# Verification Report

## Summary

- Status: ready_for_closeout.
- Fix: added an idempotent DCC upload-size-policy default seed migration.
- Scope: backend SQL seed and SQL contract test only; no runtime fallback, exception swallowing, or frontend masking was introduced.

## Verification

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_upload_size_policy_seed_sql.py` -> FAIL because `20260803_dcc_upload_size_policy_default_seed.sql` was missing.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_upload_size_policy_seed_sql.py` -> PASS, 3 tests.
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260803-dcc-upload-size-policy-fix\migration-policy-gate.json` -> PASS, `migrationCount=420`.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccUploadSizePolicyServiceTest,DccControlledFileUploadApiTest#uploadPreviewFile_missingSizePolicy_throwsBeforeStorageOrTicket,DccControlledFileUploadApiTest#uploadPreviewFile_sourceDocx_successCreatesTicketAndDoesNotExposeFileId" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests, `BUILD SUCCESS`.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260803-dcc-upload-size-policy-fix\bug-regression-evidence.md` -> PASS before cleanup removed the temporary evidence file.
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `codex/dcc-upload-size-policy-fix/int_main`, frontend `8095`, backend `48095`.

## Root Cause

- DCC upload-preview correctly validates upload size before storage and fails fast when no effective policy exists.
- Existing release SQL created the `dcc_controlled_file_upload_policy` table but did not provide formal default effective policies for supported upload purposes.
- Real upload evidence from `doc/tasks/20260729-test-server-wangsiyu-file-upload-simulation/execution-log.md` showed `SOURCE` policy missing for upload categories with valid approval routes.

## Risk Notes

- The seed creates purpose-level defaults below category-specific rules, so stricter category or category-purpose policies still win.
- The seed only inserts when a tenant lacks an effective global or same-purpose purpose policy.
- Existing policies are not updated or deleted; if the exact seed policy code already exists but is invalid, the migration fails fast instead of silently overriding.
- Runtime note: the affected environment must apply `20260803_dcc_upload_size_policy_default_seed.sql` before users stop seeing the missing-policy message.
- Closeout blocker: linked worktree cleanup/merge is not complete because `E:\IntRuoyi` is dirty and cannot receive the ff-only merge.
