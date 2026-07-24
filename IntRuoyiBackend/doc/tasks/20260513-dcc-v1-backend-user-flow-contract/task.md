# Task: DCC v1 backend user-flow contract

## Goal

Close the backend contract gap that currently blocks the documented DCC frontend user flow by extending controlled-file submission metadata, controlled-file page filtering, and user-flow menu seeds in the backend worktree.

## Scope

- Extend `dcc_controlled_file` persistence to store upload metadata required by the approved frontend design:
  - effective date
  - submit remark
- Extend controlled-file request/response contracts so submit/detail/page can carry the new fields.
- Extend controlled-file page querying to support at least directory filtering needed by the browser page.
- Add DCC menu seed entries required for documented user-flow entry points:
  - `/dcc/controlled-file/mine`
  - `/dcc/controlled-file/approval-tasks`
- Update only the minimal backend tests, schema script, mapper query, and service logic required for this contract closure.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-finalization/task.md`
- Status before this task: completed and committed.
- Impact: final workflow states, preview/download gates, and stamping are available, so this task can focus only on user-flow contract closure.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document, execution log, backend API evidence, and database schema evidence created before production code changes.
- [x] M3: BDD scenarios and RED verification captured for metadata persistence and directory-based page filtering.
- [x] M4: Schema, DO, and API contracts extended for effective date, remark, and directory filter.
- [x] M5: Workflow service, mapper, and menu-seed behavior updated with targeted tests.
- [x] M6: Evidence validators and targeted Maven verification completed.
- [x] M7: Task-only backend changes committed after verification passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccBaseSchemaTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260513-dcc-v1-backend-user-flow-contract/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260513-dcc-v1-backend-user-flow-contract/database-schema-evidence.md`

## Current Status

Completed. Contract closure code, evidence, and task-only backend commit are complete.
