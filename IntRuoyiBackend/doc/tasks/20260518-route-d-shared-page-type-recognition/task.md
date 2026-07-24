# Task: Route D shared page-type header recognition

## Goal

Wire `MesProBatchRecordRouteDRecognizer` so Route D segment-title recognition reuses the shared page-type header rules instead of depending only on the fixed `PILOT_TEMPLATE_TITLES` list, without adding any single-template special case.

## Scope

- Allowed production code write scope: `yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteDRecognizer.java`
- Allowed test write scope: `yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteDRecognizerTest.java`
- Task documentation is updated only to satisfy repository policy.

## Previous Task Check

- Previous relevant task: `doc/tasks/20260517-batch-record-print-view-fidelity-phase2/task.md`
- Status before this task: blocked
- Blocker summary: the prior task is blocked by JMReport viewer chrome suppression and an external sibling frontend runtime path, which does not block Route D backend header recognition work in this worktree.

## Milestones

- [x] M1: Add a failing regression test proving Route D must split generic process-page headers that are not in the fixed title list.
- [x] M2: Implement the minimal recognizer change in `MesProBatchRecordRouteDRecognizer`.
- [x] M3: Run targeted verification and record RED/GREEN evidence.
- [x] M4: Mark the task completed with final verification results.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\worktrees\batch-record-print-fidelity-phase2\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordRouteDRecognizerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python tool/verify_tdd_compliance.py --task-dir doc/tasks/20260518-route-d-shared-page-type-recognition --all-changed`

## Current Status

Completed.

## Final Verification Result

- Regression reproduced first with the targeted Maven test command, failing on `route_d_expected_15_templates_actual_14`.
- After the minimal recognizer change, isolated compilation of the owned Route D sources and a direct JUnit Platform launch of `MesProBatchRecordRouteDRecognizerTest` passed with 4/4 tests green.
- The normal Maven rerun is still blocked by unrelated compile errors in other `yudao-module-mes` sources modified outside this task, so those errors were not changed here.
