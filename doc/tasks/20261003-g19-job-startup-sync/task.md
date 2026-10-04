# G19 JobStartupSyncRunner property guard

## Goal
Guard startup Quartz job synchronization behind `yudao.local-job-control.startup-sync-enabled`, defaulting to enabled for backward compatibility.

## BDD
- Given `startup-sync-enabled=false`, when the application context starts, then `JobStartupSyncRunner` is not registered and no startup sync is attempted.
- Given `startup-sync-enabled=true` or the property is absent, when startup sync runs, then the runner calls `JobService.syncJob()`.
- Given enabled startup sync and `syncJob()` throws `SchedulerException`, then the original exception propagates.

## Scope constraints
Only `JobStartupSyncRunner.java` and `JobStartupSyncRunnerTest.java` may change Java. No SQL, application configuration, services, runtime, database or Git actions.

## Current Status
ready_for_closeout — condition and focused tests pass.

## Expected Verification
Effective RED with the condition removed, GREEN with explicit false/true/missing property context tests plus exception propagation, and directed Maven reactor test.

## Design constraints
`@ConditionalOnProperty(prefix = "yudao.local-job-control", name = "startup-sync-enabled", havingValue = "true", matchIfMissing = true)` preserves existing startup behavior when omitted.

## Cleanup Keep

- doc/tasks/20261003-g19-job-startup-sync/task.md
- doc/tasks/20261003-g19-job-startup-sync/execution-log.md
- doc/tasks/20261003-g19-job-startup-sync/verification-report.md
- doc/tasks/20261003-g19-job-startup-sync/g19-delivery-fingerprints.json
- doc/tasks/20261003-g19-job-startup-sync/g19-test-counts.json
- doc/tasks/20261003-g19-job-startup-sync/g19-job-startup-sync-red-effective.log
- doc/tasks/20261003-g19-job-startup-sync/g19-job-startup-sync-green-final.log
- doc/tasks/20261003-g19-job-startup-sync/g19-main-compile.log
