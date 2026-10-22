# Verification Report

## Status

in_progress

## Scope

## Root Cause

The native upload/revision path stores the real training artifact in `dcc_controlled_file.training_record_file_id` and advances BPM/status for manual distribution. Generic `dcc_controlled_file_training` and `dcc_controlled_file_distribution` rows are created only by the ordinary category-rule finalization path, so the native path legitimately has empty generic tables. The detail query previously exposed only those generic tables.

## Implementation

- Added native detail projection fields to `DccControlledFileRespVO` and the frontend response type.
- `DccControlledFileQueryServiceImpl` reads the bound training file record only when `needTraining=true` and the process key is native upload/revision.
- The same service derives distribution-node completion from the native controlled-file status, with `PENDING_MANUAL_DISTRIBUTION` remaining incomplete.
- The frontend displays the real training evidence file name and an explicit completed-distribution-node message while preserving ordinary table projections.

## Verification

- `DccControlledFileQueryServiceTest`: `162/162` passed, `0` failures, `0` errors, `0` skipped.
- `pnpm ts:check`: passed.
- DCC detail lifecycle timeline static contract: passed.
- DCC detail route snapshot summary static contract: passed.
- `git diff --check`: passed for task files.

## Runtime Note

The existing running service was not restarted per request. The new projection will appear after the normal backend reload/restart in the task runtime; no database or business data change is required.
