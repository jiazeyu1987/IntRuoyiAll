# Task: Repair int_main backend blocker

## Goal

Repair the current `int_main` backend blocker that prevents MES-related work on
this branch from completing compile and regression verification.

## Scope

- Backend repository only.
- Reproduce the current `int_main` backend compile or test failure.
- Isolate the minimal backend code or source-file issue that causes the failure.
- Implement the smallest fix without fallback behavior.
- Re-run focused backend verification for the affected MES slice.

## Previous Task Check

- Previous backend task: `doc/tasks/20260514-electronic-batch-record-doc-report-implementation/task.md`
- Status before this task: blocked.
- Impact: the pending electronic batch-record backend slice cannot be fully
  verified or promoted until the repository-level `int_main` backend blocker is
  removed.

## Milestones

- [x] B1: Check the latest backend task status before starting new backend work.
- [x] B2: Create this backend task document before production code changes.
- [x] B3: Record BDD scenarios and RED evidence for the current backend blocker.
- [x] B4: Implement the minimal backend fix for the reproduced blocker.
- [x] B5: Run focused backend verification and update evidence.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-erp,yudao-module-mes -am -DskipTests compile`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-erp,yudao-module-mes -am -Dtest=MesProBatchRecordDocParserTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed for the backend blocker-removal scope. The broken
`ErrorCodeConstants.java` source, missing batch-record report dependencies, and
batch-record regression-test isolation gap were repaired, and the focused MES
compile plus regression tests now pass on `int_main`.
