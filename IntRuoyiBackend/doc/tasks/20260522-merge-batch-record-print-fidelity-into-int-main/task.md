# Task: Merge batch-record print-fidelity worktree into int_main backend

## Goal

Merge the committed backend history from
`codex/batch-record-print-fidelity-phase2` into `int_main` so the main backend
branch includes the batch-record report visual-fidelity work delivered in that
worktree, while keeping temporary task artifacts out of `int_main`.

## Scope

- Backend repository only
- Merge committed history from
  `D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2`
  into `int_main`
- Resolve branch-owned leftover tracked changes before merge
- Exclude temporary task artifacts and one-off helper files that should not land
  on `int_main`
- Run focused backend verification on the merged result

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-six-route-report-doc-consistency-review/task.md`
- Status before this task: blocked and partially superseded
- Impact: the old consistency-review task remained open for route-level evidence,
  but that blocker did not prevent merging the already committed batch-record
  backend history after branch hygiene and verification were completed

## Milestones

- [x] M1: Check the previous backend task state before new merge work.
- [x] M2: Create the merge task package before Git history changes.
- [x] M3: Classify branch leftovers into merge-worthy tracked work vs removable
  task artifacts.
- [x] M4: Commit the in-scope outstanding backend changes on the feature branch.
- [x] M5: Merge the backend feature branch into a clean `int_main`-based merge
  branch and resolve conflicts.
- [x] M6: Run focused backend verification on the merged result.
- [x] M7: Record evidence and complete the verified merge branch for `int_main`
  fast-forward.

## Expected Verification

- `git merge --no-ff codex/batch-record-print-fidelity-phase2`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\tmp\merge-batch-record-into-int-main-backend\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteBRecognizerTest,MesProBatchRecordRouteDRecognizerTest,MesProBatchRecordReportShapeRulesTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportServiceImplDbTest#recognizeFixedRoute_usesConfiguredWorkspaceSamplePath -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\tmp\merge-batch-record-into-int-main-backend\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`

## Current Status

Completed on the verified merge branch
`codex/20260522-merge-batch-record-into-int-main`. The batch-record
print-fidelity backend history has been merged on top of `int_main`, the
gateway and DB-test conflicts were resolved by preserving both the new pure
preview path behavior and the existing delete-all support, and the merged result
passed targeted MES regression tests plus full `yudao-server` packaging.
