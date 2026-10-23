# Activation job isolation test report

2026-10-21. Implementation subphase: ready_for_closeout for Root review only. Overall task remains blocked; no packaging, Git, real job, runtime DB, browser, or service actions authorized/performed.

## Implementation

Only three production files changed: `TenantJob.java` adds `failOnError=false`; `TenantJobAspect.java` propagates a tenant-summary IllegalStateException only for the opt-in annotation, with all tenant throwables suppressed; `DccControlledFileActivationJob.java` opts in and continues after individual RuntimeExceptions, then throws a summary containing activatedCount and failedFileIds with originals suppressed. Error exits the DCC scan immediately and strict tenant handling still reports failure. Due-query exceptions propagate unchanged. No cause is assigned to either aggregate, retaining the summary in Invoker root-cause extraction. No Invoker, Catalog, historical resource, schema or configuration changes.

Failure log contains tenantId/fileId/processDefinitionKey. The due object has no bound processDefinitionId field; no additional lookup or inferred definition identity was added.

## Actual RED

All commands ran serially from `IntRuoyiBackend` using PowerShell native argument stop-parsing (`mvn --% ...`).

- H2 RED: `mvn --% -pl yudao-module-dcc -am -Dtest=cn.iocoder.yudao.module.dcc.service.file.DccThreeControlledVersionActivationTest -Dsurefire.failIfNoSpecifiedTests=false test`, finished 11:21:26 +08:00, exit 1. Tests 11, failures 1, errors 0. Ten existing cases passed; only new `jobContinuesAfterBadDueRowsAndCommitsLaterGoodRow` failed: expected file6 ACTIVE, actual CONTROLLED_PENDING_EFFECTIVE. This is valid starvation RED.
- Tenant RED: `mvn --% -pl yudao-framework/yudao-spring-boot-starter-biz-tenant -Dtest=cn.iocoder.yudao.framework.tenant.core.job.TenantJobAspectTest test`, finished 11:22:24 +08:00, exit 1. Tests 6, failures 1, errors 1: strict test expected RuntimeException but nothing was thrown; annotation-default test reported absent failOnError method. Four existing tests passed.
- Job unit RED: `mvn --% -pl yudao-module-dcc -am -Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileActivationJobTest -Dsurefire.failIfNoSpecifiedTests=false test`, finished 11:23:02 +08:00, exit 1. Tests 3, failures 1, errors 0: aggregate failure-ID assertion failed; due-query propagation and false-count cases passed.

Earlier unsuccessful attempts are excluded from valid RED: one PowerShell property parsing failure; H2 fixture failures from missing effectiveContractResolver/distributionBatchService ports. The fixture was repaired before valid RED. First GREEN attempts also exposed missing unit-test tenant context and an unverified dueVersions mock invocation; both test-only issues were corrected.

## Final GREEN and regression

Command: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileActivationJobTest,DccThreeControlledVersionActivationTest,DccTargetedActivationJobTest,TenantJobAspectTest -Dsurefire.failIfNoSpecifiedTests=false test`

Finished 11:30:28 +08:00, exit 0, BUILD SUCCESS. Total 28 tests, failures 0, errors 0, skipped 0:

| Test | Count | Evidence |
| --- | ---: | --- |
| DccControlledFileActivationJobTest | 6 | real annotation opt-in; due-query same exception/no activation; false not counted; two bad summary; one bad/two good each once; Error abort |
| DccThreeControlledVersionActivationTest | 11 | real H2/Spring transaction proxy; good6 commit after two bad rows; existing lifecycle/concurrency/audit regression |
| DccTargetedActivationJobTest | 4 | targeted handler behavior unchanged |
| TenantJobAspectTest | 7 | strict multiple tenants incl. RuntimeException/Error; summary visible with originals suppressed; default false; ordinary legacy behavior |

Transaction proof boundary: new bad4/5 fail before writes because their master is missing. This test proves continuation, unchanged failed rows, and actual H2 good6 commit, not write-after-failure rollback by itself. The existing `lateActivationLedgerFailureRollsBackAllObsoleteRowsPointersRetentionAndAudits` passed in this final run and proves actual late-write rollback across version rows, master pointers, retention, audit and lifecycle event rows. Contract/distribution ports are mocked in the fixture; commit/rollback/state assertions use actual H2 and transaction manager.

## Preserved evidence and limits

Four exact final Surefire XML reports and `process-exit.json` are preserved in `activation-job-test-evidence/`. Process inspection after Maven exit found zero Maven/Surefire Java processes. No runtime scheduler execution is claimed; Quartz failed-status behavior is grounded in the unchanged Invoker call chain and strict-aspect propagation tests. Current configured retries remain unchanged and can rerun already committed records; existing idempotency/concurrency cases passed.

BDD/design: `activation-job-failure-isolation-review.md`. V4 source proof boundary: `scheduler-contract-source-review.md` (sealed, no further binlog hunt).
