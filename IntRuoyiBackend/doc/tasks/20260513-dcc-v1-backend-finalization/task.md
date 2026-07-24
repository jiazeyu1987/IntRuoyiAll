# Task: DCC v1 backend finalization

## Goal

Implement the DCC backend finalization slice in the backend worktree for BPM result listening, PDF stamping, in-app notification, access logging, and controlled preview/download gates.

## Scope

- Add BPM result listener(s) under `yudao-module-dcc` for the `dcc-controlled-file-approval` process definition key.
- On approve: create or update DCC stamp state, generate stamped PDF, persist stamped-file linkage, update controlled-file status, and notify the submitter.
- On reject: update controlled-file status and notify the submitter.
- Add preview and download endpoints under `/dcc/controlled-files/{id}/preview` and `/dcc/controlled-files/{id}/download`.
- Enforce access gates with current directory access rules and controlled-file lifecycle status.
- Persist access logs for both allow and deny outcomes.
- Add MySQL-only notify-template seed data required by the DCC finalization slice.
- Leave frontend implementation and E2E browser automation to later tasks.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-workflow-apis/task.md`
- Status before this task: completed and committed.
- Impact: workflow submit/page/detail/withdraw is available for listener and preview/download integration.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document and execution log created before production code changes for this slice.
- [x] M3: BDD scenario and RED verification captured for approval finalization and preview/download access denial.
- [x] M4: BPM result listener implemented for approve and reject outcomes.
- [x] M5: PDF stamping and stamped-file persistence implemented for approve outcomes.
- [x] M6: Preview/download gate and access-log persistence implemented.
- [x] M7: Notify template seed and submitter notification integration implemented.
- [x] M8: GREEN verification captured with targeted Maven tests.
- [x] M9: Task status finalized and task-only changes committed after verification passes.

## Expected Verification

- Targeted Maven tests for listener, PDF stamping, access gate, and notification behavior.
- `mvn --% -f <backend-worktree-pom> -pl yudao-module-dcc -Dtest=<dcc-finalization-tests> -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed. Backend finalization code, verification, and task-only commit are complete.
