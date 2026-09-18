# DCC-STATIC-027 Verification Report

## Result

PASS: DCC-STATIC-027 is fixed in the scoped clean worktree through targeted backend logic and regression tests.

## Bug Summary

- Formal DCC metadata update could change the ACTIVE file row's file number, project, or taxonomy without synchronizing the Master logical identity fields.
- The stale Master identity allowed an OLD logical-key lookup to return a NEW active file as `matched=true`.

## Expected Behavior

- Metadata updates that form a complete new logical identity must validate uniqueness for `tenantId + dccProjectCodeId + fileTypeTaxonomyLeafId + normalizedFileNumber` in the same transaction before writing.
- Master must persist the authoritative project, taxonomy leaf, and normalized file number that correspond to the active file.
- Current-version reads must reject Master/ACTIVE identity mismatch instead of returning a false positive match.

## Reproduction

Reproduction command: `mvn -pl yudao-module-dcc '-Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest' test`.

- Before the fix, the new regression tests reproduced the static failure without E2E, service startup, database writes, remote access, or Git commit/push.
- The failure showed that OLD identity lookup could still flow through to NEW active-file details because the current-version path did not compare requested identity against the ACTIVE file.

## Root Cause

- `DccControlledFileMetadataUpdateServiceImpl` updated Master category, directory, file name, and file number, but omitted `dccProjectCodeId`, `fileTypeTaxonomyLeafId`, and `normalizedFileNumber`.
- Its conflict check still depended on category/directory/file name instead of the new DCC logical identity.
- `DccControlledFileWorkflowServiceImpl#getCurrentVersionByFileNumberInternal` trusted the Master pointer after lookup and did not revalidate the active file's project, taxonomy, or normalized file number against the requested identity.

## Regression Tests

- Added `DccControlledFileMetadataUpdateServiceTest#updateMetadata_identityChangeSyncsMasterLogicalKey`.
- Added `DccControlledFileMetadataUpdateServiceTest#updateMetadata_conflictingLogicalIdentityFailsBeforeWriting`.
- Added `DccControlledFileWorkflowServiceImplTest#getCurrentVersionByFileNumber_rejectsMasterActiveFileIdentityMismatch`.

## RED Evidence

RED: `mvn -pl yudao-module-dcc '-Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest' test` -> FAIL.

- `updateMetadata_identityChangeSyncsMasterLogicalKey`: `selectByNewLogicalIdentity` was not invoked, and the Master update payload kept `dccProjectCodeId`, `fileTypeTaxonomyLeafId`, and `normalizedFileNumber` null.
- `updateMetadata_conflictingLogicalIdentityFailsBeforeWriting`: no exception was thrown for a conflicting target logical identity.
- `getCurrentVersionByFileNumber_rejectsMasterActiveFileIdentityMismatch`: no exception was thrown, so OLD could still return the NEW active file.

## GREEN Evidence

GREEN: `mvn -pl yudao-module-dcc '-Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest' test` -> PASS, 142 tests, 0 failures, 0 errors, 0 skipped.

GREEN: `git diff --check` -> PASS; only Git line-ending normalization warnings were reported.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-027-metadata-identity-sync --mode preview --worktree-closeout off --json` -> PASS, keep only `task.md`, `execution-log.md`, and `verification-report.md`; delete/blocked/warnings were empty.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-027-metadata-identity-sync\verification-report.md` -> PASS, bug regression evidence valid.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-027-metadata-identity-sync --mode apply --worktree-closeout off --json` -> PASS, no paths deleted and no commit/merge/push/worktree removal performed.

## Risk And Regression Scope

- Scope is limited to DCC metadata update and current-version lookup identity consistency.
- No E2E was executed, no service was started or restarted, no database write was performed, no remote operation was performed, and no Git push was made; the later local `int_main` integration commit is recorded below.
- Shared bug file was not edited in this clean worktree because the referenced `docs/bugs/20260912-dcc-90-step-static-audit.md` exists only in the dirty `E:\IntRuoyi` working tree used as source evidence.

## Blockers And Follow-Up

- No implementation or verification blocker remains inside the requested scope.
- No cleanup blocker remains. Git push was intentionally not performed because the user did not authorize it for this delegated task; local `int_main` integration was later authorized and is recorded below.

## Int Main Integration Evidence

PASS: The scoped DCC-STATIC-027 fix has been integrated into local `int_main` in `E:\IntRuoyi`.

- Code commit: `38ddcc335 fix: sync DCC metadata logical identity`.
- Reverification on `int_main`: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest" test` -> PASS, 153 tests, 0 failures, 0 errors, 0 skipped.
- Static check on integrated files: `git -C E:\IntRuoyi diff --check -- <DCC-STATIC-027 files>` -> PASS.
- Runtime port contract guard: PASS for `int_main/int_main` frontend 8081 backend 48081.
- No E2E, service restart, database write, remote operation, or `git push` was performed.