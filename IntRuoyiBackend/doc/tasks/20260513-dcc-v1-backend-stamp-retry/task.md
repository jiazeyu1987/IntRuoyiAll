# Task: DCC v1 backend stamp retry

## Goal

Add a backend retry path for `STAMP_FAILED` controlled files so an operator can explicitly re-run DCC stamping after the blocking condition is fixed.

## Scope

- Add one DCC backend operation for stamp retry.
- Restrict it to records in `STAMP_FAILED` with a valid original file.
- Reuse the existing finalization and PDF stamp path instead of creating a second implementation.
- Add the minimum targeted tests and evidence.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-approval-to-publish/task.md`
- Status before this task: completed and committed in `5f8cf6805b`.
- Impact: approval-to-publish is already working, so this task can focus only on manual recovery from stamp failure.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document, execution log, and backend evidence file created before production code changes.
- [x] M3: BDD scenario and RED verification captured for stamp retry behavior.
- [x] M4: Minimal backend retry endpoint and service logic implemented.
- [x] M5: Targeted backend verification completed.
- [x] M6: Task-only backend changes committed after verification passes.

## Expected Verification

- Targeted Maven verification for the touched DCC finalization tests
- Direct API verification against isolated backend `48082`

## Current Status

Completed. The backend now exposes an explicit DCC stamp-retry path for `STAMP_FAILED` records, and the isolated runtime can recover a failed-stamp file back to `STAMPED`.
