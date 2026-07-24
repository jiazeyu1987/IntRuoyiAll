# Task: DCC v1 backend browser permission contract

## Goal

Close the backend permission gap that currently blocks the DCC browser slice by making the directory tree and controlled-file page honor current-user query visibility without breaking administrator maintenance or submitter my-file access.

## Scope

- Add a reusable DCC directory-query permission service for current-user directory visibility.
- Make `/dcc/directories/tree` return:
  - full enabled tree for users with DCC directory management visibility
  - query-visible directories plus required ancestors for ordinary users
- Make controlled-file page querying honor current-user directory query visibility for browser-style reads.
- Preserve requester-owned my-file listing behavior so submitters can still see their own records.
- Update only the minimal controller, service, mapper, and tests required for this permission contract closure.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-user-flow-contract/task.md`
- Status before this task: completed and committed.
- Impact: submit metadata, directory filtering input, and user-flow menu seeds are available, so this task can focus only on query-visibility enforcement for the browser slice.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document, execution log, and backend API evidence created before production code changes.
- [x] M3: BDD scenarios and RED verification captured for visible directory trees and browser-style file-page filtering.
- [x] M4: Directory-query permission helper and controller/service integration implemented.
- [x] M5: Controlled-file page permission filtering implemented with targeted tests.
- [x] M6: Evidence validator and targeted Maven verification completed.
- [x] M7: Task-only backend changes committed after verification passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccDirectoryAdminServiceImplTest,DccDirectoryAccessPermissionServiceTest,DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260513-dcc-v1-backend-browser-permission-contract/backend-api-evidence.md`

## Current Status

Completed. Browser permission contract code, evidence, and task-only backend commit are complete.
