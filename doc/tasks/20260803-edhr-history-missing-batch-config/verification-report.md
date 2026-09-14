# Verification Report

## Summary

- Implemented backend read-only history behavior so terminal eDHR history does not depend on current BATCH route-gate configuration.
- Preserved persisted batch-record execution snapshots in history even when the historical route snapshot or current route no longer has BATCH gate config.
- Added Java regression coverage and a lightweight static contract for the corrected behavior.

## Evidence

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsEmptyHistoryContentWhenArchivedBatchConfigMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before reaching MES because unrelated DCC tests failed to compile missing `FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS` / `getDefaultUnclassified`.
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsEmptyHistoryContentWhenArchivedBatchConfigMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL as expected with `ServiceException` code `1040750403` at `MesProEdhrBatchExecutionServiceImpl.buildTaskPredecessorRouteProcessIdMap`.
- RED: `node yudao-module-mes\src\test\js\edhr-history-missing-batch-config-static.spec.cjs` -> FAIL after correcting the expected behavior because production still called active `buildTaskGateMap` for history.
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsPersistedHistoryWhenArchivedRouteGateConfigMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL after gate fix because history execution review still queried current Jimu report and raised linked-report-missing.
- STATIC GREEN: `node yudao-module-mes\src\test\js\edhr-history-missing-batch-config-static.spec.cjs` -> PASS.
- JAVA GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsPersistedHistoryWhenArchivedRouteGateConfigMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS.
- REGRESSION GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getReviewTimeline_returnsBatchTasksSignaturesAndArchives" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS.
- RECHECK GREEN: `node yudao-module-mes\src\test\js\edhr-history-missing-batch-config-static.spec.cjs` -> PASS.

## Scope Notes

- The implementation does not catch and swallow BATCH configuration exceptions. It avoids invoking active gate calculation for terminal read-only history.
- Terminal history task events are explicitly read-only; active eDHR execution, new batch creation, active filling, closing, archiving, and non-target exceptions still use the existing fail-fast BATCH configuration validation.
- Closeout is not completed because the shared branch is behind `origin/int_main` and the working tree contains many unrelated concurrent dirty files; staging/committing/pushing was intentionally skipped to avoid mixing task ownership.
