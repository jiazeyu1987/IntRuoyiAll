# G19 verification report

ready_for_closeout.

`mvn -pl yudao-module-infra -am test -Dtest=JobStartupSyncRunnerTest -Dsurefire.failIfNoSpecifiedTests=false` passed with **7 tests / 0 failures / 0 errors / 0 skipped**, BUILD SUCCESS at 2026-10-03 12:02:37 +08:00 (`g19-job-startup-sync-green-final.log`). Main source and test compilation occurred in the reactor; standalone infra compile also passes (`g19-main-compile.log`).

Effective RED is preserved in `g19-job-startup-sync-red-effective.log`: removing the condition caused the explicit false property context test to discover `JobStartupSyncRunner`, proving the guard is behaviorally required.

`JobStartupSyncRunner` now uses `@ConditionalOnProperty` with prefix `yudao.local-job-control`, name `startup-sync-enabled`, `havingValue=true`, `matchIfMissing=true`. False omits the bean without touching either dependency; true and omitted property preserve sync; direct runner tests prove scheduler-disabled skip and original `SchedulerException` propagation.

No SQL, application YAML, services, runtime, database, frontend or Git actions were performed.
