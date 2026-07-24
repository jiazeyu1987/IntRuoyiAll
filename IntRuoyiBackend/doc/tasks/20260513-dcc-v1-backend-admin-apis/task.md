# Task: DCC v1 backend admin APIs

## Goal

Implement the DCC backend admin APIs in the backend worktree for directories, access rules, file categories, approval positions, approval routes, and route preview on top of the committed backend foundation.

## Scope

- Add admin request and response VOs, service interfaces and implementations, converters, and controllers under `yudao-module-dcc`.
- Implement these backend endpoints:
  - `/dcc/directories/tree`
  - `/dcc/directories`
  - `/dcc/directories/{id}`
  - `/dcc/directories/{id}/access-rules`
  - `/dcc/file-categories`
  - `/dcc/file-categories/{id}`
  - `/dcc/file-categories/{id}/directory-binding`
  - `/dcc/approval-positions`
  - `/dcc/approval-positions/{id}/assignments`
  - `/dcc/approval-routes`
  - `/dcc/approval-routes/{categoryId}`
  - `/dcc/approval-routes/preview`
- Preserve fail-fast behavior for missing categories, directories, routes, and approver assignments.
- Leave BPM process start, controlled file submission, notifications, preview/download gates, and stamping behavior to later tasks.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-foundation/task.md`
- Status before this task: completed and committed.
- Impact: backend foundation is available for this admin-API slice.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document created before production code changes for this slice.
- [x] M3: BDD scenario and RED verification captured for one admin CRUD path and route preview failure path.
- [x] M4: Directory and access-rule admin APIs implemented.
- [x] M5: Category, position, and route admin APIs implemented.
- [x] M6: Route preview implemented with fail-fast missing-approver validation.
- [x] M7: GREEN verification captured with targeted Maven tests.
- [x] M8: Task status finalized and task-only changes committed after verification passes.

## Expected Verification

- Targeted Maven tests for directory, access-rule, category, position, route, and preview services/controllers.
- `mvn --% -f <backend-worktree-pom> -pl yudao-module-dcc -Dtest=<dcc-admin-tests> -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed. Backend admin APIs are implemented, verified, and committed for the next workflow slice.
