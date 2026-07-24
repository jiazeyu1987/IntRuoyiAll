# Task: DCC v1 backend workflow APIs

## Goal

Implement the DCC backend workflow APIs in the backend worktree for controlled-file submission, page query, detail query, route snapshot persistence, BPM process start, and submitter withdraw.

## Scope

- Add workflow request and response VOs, services, controllers, and mapper helper methods under `yudao-module-dcc`.
- Implement these backend endpoints:
  - `/dcc/controlled-files/submit`
  - `/dcc/controlled-files/page`
  - `/dcc/controlled-files/{id}`
  - `/dcc/controlled-files/{id}/withdraw`
- Submission must derive the target directory from the category binding, resolve the active approval route, validate approvers, persist route snapshots, and start BPM with business key = controlled file id.
- Withdraw must cancel the process as the submitter and update DCC status locally.
- Leave BPM result listener finalization, stamp execution, inbox notification, and preview/download gate behavior to later tasks.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-admin-apis/task.md`
- Status before this task: completed and committed.
- Impact: directory, category, route, and route preview foundations are available for this workflow slice.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document and execution log created before production code changes for this slice.
- [x] M3: BDD scenario and RED verification captured for submit success and withdraw behavior.
- [x] M4: Submit workflow implemented with route validation, snapshot persistence, and BPM process creation.
- [x] M5: Page and detail workflow queries implemented.
- [x] M6: Withdraw workflow implemented with BPM process cancellation and local status update.
- [x] M7: GREEN verification captured with targeted Maven tests.
- [x] M8: Task status finalized and task-only changes committed after verification passes.

## Expected Verification

- Targeted Maven tests for submit, page, detail, and withdraw services.
- `mvn --% -f <backend-worktree-pom> -pl yudao-module-dcc -Dtest=<dcc-workflow-tests> -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed. Backend workflow APIs are implemented, verified, and committed for the next finalization slice.
