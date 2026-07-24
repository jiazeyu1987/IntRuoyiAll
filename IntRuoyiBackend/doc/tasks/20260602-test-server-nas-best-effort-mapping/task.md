# Task: Test server NAS best-effort principal mapping

## Task Goal

Map the remaining unmapped NAS ACL principals for test server transfer task 5 to the closest available DCC users, departments, roles, or posts so that NAS permission restore is no longer blocked by unmapped principals.

## Milestones

- [x] M1: Record the changed acceptance rule for best-effort mapping.
- [x] M2: Read the remaining unmapped NAS principals and DCC subject candidates from the test server.
- [x] M3: Generate a best-effort mapping plan for every remaining principal.
- [x] M4: Apply all remaining principal mappings to the test server mapping table.
- [x] M5: Verify snapshot summary and restore preview no longer report unmapped principal blockers.
- [x] M6: Re-run failed NAS transfer files after fixing stale task state.
- [x] M7: Apply permission restore for the final resolved directories and verify completion.

## Expected Verification

- Remaining unmapped principals before this task are counted.
- Every remaining principal receives a selected target.
- Snapshot summary reports `unmappedPrincipalCount=0`.
- Restore preview has no unmapped-principal blocker; any remaining blocker type must be reported separately.

## Current Status

completed

## Completed Work

- Applied 39 additional best-effort mappings with method `AUTO_BEST_EFFORT`.
- Mapping totals for tenant 1 are now:
  - `AUTO_EXACT`: 130
  - `AUTO_CONSERVATIVE`: 5
  - `AUTO_BEST_EFFORT`: 39
- Permission snapshot for transfer task 5 now has no unmapped principals, unsupported ACEs, or blockers.
- Submitted permission restore once after mapping, then re-ran it after the file transfer re-resolved directories.
- Found that the previous file failures were not caused by permission mapping. The first retry exposed stale directory IDs after old DCC directories had been soft-deleted.
- Re-queued task 5 directory and file items so directories were resolved again against the current DCC directory table.
- NAS transfer task 5 completed with 51 directories and 953 files completed.

## Final Verification

- Transfer task 5 API returned `status=COMPLETED`, `createdFileCount=953`, `failedFileCount=0`, `remainingPendingCount=0`, and no failures.
- Transfer item table returned `DIRECTORY/COMPLETED=51` and `FILE/COMPLETED=953`.
- Permission snapshot summary returned `snapshotStatus=CAPTURED`, `directorySnapshotCount=51`, `aceCount=2314`, `unmappedPrincipalCount=0`, `unsupportedAceCount=0`, `blockerCount=0`, `restoreSupported=true`.
- Permission restore preview returned `canRestore=true` with 0 blockers.
- Permission restore plan 2 returned `status=COMPLETED`, `completedDirectoryCount=51`, `failedDirectoryCount=0`, and no failure message.

## Operational Notes

- No server reboot was required.
- The old failure report was cleared after the successful rerun.
- The transient object storage error from the previous run was not present during the successful rerun; backend container access to `host.docker.internal:9000` was verified before re-queuing.
