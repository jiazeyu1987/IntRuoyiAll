# Task: DCC v1 backend foundation

## Goal

Create the backend foundation for the independent DCC file system v1 in the backend worktree only, following the repo's existing business-module patterns.

## Scope

- Register `yudao-module-dcc` in the root reactor and `yudao-server` so the backend build compiles.
- Add MySQL-only schema and seed SQL for the DCC foundation tables and menu/permission entries defined in this task.
- Scaffold the backend DCC module packages needed for later slices without adding admin APIs, BPM runtime integration, notifications, or stamping behavior.
- Add DCC foundation enums, error-code constants, data objects, mappers, and one minimal service path with strict TDD evidence.
- Leave unrelated worktree changes untouched and do not write outside `D:\\ProjectPackage\\Int\\IntRuoyi\\ruoyi-vue-pro\\.worktrees\\dcc-v1-backend`.

## Previous Task Check

- Previous backend task: `doc/tasks/20260512-report-route-sweep/task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this DCC foundation slice.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document and execution log created before production code changes.
- [x] M3: BDD scenario and RED verification captured for DCC foundation wiring and one service/mapper path.
- [x] M4: `yudao-module-dcc` module scaffolding, build wiring, enums, DOs, mappers, and minimal service path added.
- [x] M5: MySQL foundation schema and seed SQL added for tables and permissions in scope.
- [x] M6: GREEN verification captured with targeted Maven tests / compile checks.
- [x] M7: Task status finalized and task-only changes committed after verification passes.

## Expected Verification

- `mvn "-pl yudao-server -am" "-Dtest=DccModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn "-pl yudao-module-dcc -am" "-Dtest=DccFileDirectoryServiceImplTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn "-pl yudao-server -am" "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true" package`

## Current Status

Completed. Backend foundation is implemented, verified, and ready as the base for the next DCC backend slice.
