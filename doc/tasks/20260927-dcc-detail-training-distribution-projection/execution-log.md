# Execution Log

## 2026-09-27

- Read repository, backend, frontend, E2E, runtime and closeout rules.
- Root cause investigation: three-workflow training evidence is stored on `dcc_controlled_file.training_record_file_id`; manual distribution advances the native BPM receive task and controlled-file status but does not create generic training/distribution rows. The query service only queried those generic tables.
- Planned RED/GREEN scope: query projection only, with no write-path or database changes.

### TDD

- RED: added QueryService assertions for native upload/revision training evidence and completed distribution. The first Maven run stopped before tests because the pre-existing `DccControlledFileAccessEventMapper` change in the dirty worktree references `List` without its import.
- Test-only unblock: temporarily added that missing import to compile the existing dirty source, ran the requested test, then removed the temporary import. The mapper remains outside this task's final diff.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> `162` tests, `0` failures, `0` errors, `0` skipped; `BUILD SUCCESS`.
- GREEN: `pnpm ts:check` -> exit `0`.
- GREEN: `node tests/e2e/dcc-detail-lifecycle-timeline-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/dcc-detail-route-snapshot-summary-static.spec.js` -> PASS.

### Root Cause And Fix

- `DccControlledFileWorkflowServiceImpl.uploadTrainingRecord` writes the native flow's real evidence to `dcc_controlled_file.training_record_file_id` and moves the native BPM task. It does not create `dcc_controlled_file_training` rows.
- `DccControlledFileFinalizationServiceImpl.createTrainingRecords` and `createDistributionRecords` belong to the generic category-rule finalization path. Native upload/revision manual release only changes the controlled-file status and triggers the `DISTRIBUTION` receive task, so the generic tables are correctly empty for these samples.
- `DccControlledFileQueryServiceImpl` now projects the native facts only for upload/revision: a valid bound training file produces `trainingRecordAvailable` and its file name; `PENDING_DOC_CONTROL_REVIEW` and terminal native states produce `distributionCompleted`. Generic `trainingStatuses` and `distributionStatuses` remain unchanged for ordinary flows.
- The detail page renders explicit native evidence/completion messages instead of claiming that no training/distribution event occurred. No synthetic rows, database writes, service restart, or business-data changes were used.
