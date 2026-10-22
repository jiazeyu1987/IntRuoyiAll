# Activation Job Failure Isolation Review

Date: 2026-10-21

## Decision Requested

This is a read-only design review for Root. No production code was changed and no real job was run. The original V4 source diagnosis and its evidence limits are preserved in `scheduler-contract-source-review.md`.

The defect is confirmed: one `activateDue` RuntimeException currently exits `DccControlledFileActivationJob.execute` and prevents later due rows in that tenant from being attempted. The proposed per-record continuation is compatible with the repository's no-swallowed-exceptions rule only if the batch ends in a failure visible to Quartz and the original exceptions remain attached.

## Call Chain and Transaction Boundary

- `DccControlledFileActivationJob.execute` (`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/listener/DccControlledFileActivationJob.java`) is not annotated `@Transactional`.
- `dueVersions()` is a read query. Each `lifecycleService.activateDue(id)` call enters the proxied service method with `@Transactional(rollbackFor = Exception.class)` (`DccControlledFileLifecycleService.java`). Each failed record's transaction rolls back when its RuntimeException exits the proxy. Successful earlier/later calls commit independently because there is no surrounding job transaction.
- `JobHandlerInvoker.executeInternal` has no transaction annotation; it catches the handler throwable, writes a failed job-log result, then passes the exception to Quartz retry/failure handling. The invoker does not create an outer transaction.
- Critical boundary: `TenantJobAspect.around` catches `Throwable` inside each tenant execution, logs it, stores only the root-cause message, and returns a JSON result. Thus a job-level exception by itself does not reach `JobHandlerInvoker`; Quartz sees a normal return and can mark the run successful. Any approved fix must explicitly cross this aspect boundary.

## Existing Strict Propagation Mechanism

No existing strict-failure mechanism was found. `@TenantJob` has no propagation policy, `TenantJobParam` only encodes explicit tenant scope and the handler parameter, and the aspect's current behavior is to absorb tenant exceptions into its result map. Changing that behavior globally would affect all annotated jobs and is outside this defect's minimal scope.

## Approved Minimal Opt-In Design

1. Add `boolean failOnError() default false` to `@TenantJob`; set it true only on `DccControlledFileActivationJob`.
2. In the DCC handler, count only `activateDue(id) == true`; collect per-record `RuntimeException`s, log tenant/file/available definition identifiers, and continue. Do not catch `Error`, retry, change contract validation, or count false returns as success.
3. At scan end, throw an `IllegalStateException` summary only if per-record activation failures occurred. Include activated count and failed file IDs in its own message; attach each original RuntimeException with `addSuppressed`, not as cause, so Quartz root-cause extraction retains the summary. Let a `dueVersions()` exception propagate unchanged to the strict tenant aspect; it must not call `activateDue`.
4. In `TenantJobAspect`, when `failOnError=true`, collect all tenant `Throwable`s, continue processing all tenants, and throw one summary RuntimeException after the scan. Include each failing tenant and DCC summary; attach original throwables as suppressed without a cause. With default false, preserve existing behavior. This opt-in covers due-query failures and `Error` as well as record aggregates.
5. The summary must reach `JobHandlerInvoker` so the job log and Quartz outcome are failures; configured retry policy remains unchanged.

This is opt-in and does not alter other jobs unless they explicitly set `failOnError=true`. Files in scope are:

- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/listener/DccControlledFileActivationJob.java`
- `IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-tenant/src/main/java/cn/iocoder/yudao/framework/tenant/core/job/TenantJobAspect.java`
- `IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-tenant/src/main/java/cn/iocoder/yudao/framework/tenant/core/job/TenantJob.java`
- DCC job unit/integration test and `TenantJobAspectTest`.

No workflow Catalog, historical resource, database, Quartz configuration, or other job handler changes are proposed.

## BDD and RED/GREEN Design

- BDD: mixed due rows -> Given two bad and one good due row in order, When first two activations throw and the last succeeds, Then each row is attempted once, good row commits in H2, bad rows remain unchanged, and summary names both failed IDs and activated count 1.
- BDD: due query failure -> Given `dueVersions()` throws, When strict job runs, Then the same exception reaches the strict aspect unchanged and no `activateDue` call occurs.
- BDD: false is not success -> Given activation results false then true, When job runs, Then only true increments activated count.
- BDD: all-success compatibility -> Given no exceptions, When job runs, Then it returns the exact existing string.
- BDD: strict tenant propagation -> Given several tenants and one or more RuntimeException/Error failures, When strict aspect runs, Then all tenants are attempted and thrown summary names failing tenants and DCC summaries with original throwables suppressed.
- BDD: ordinary tenant compatibility -> Given `failOnError=false` and a tenant throws, When aspect runs, Then existing result-map behavior remains unchanged.
- RED: add mixed-failure/H2 transaction and strict aspect tests first; baseline must show first-error starvation and strict aspect's normal return. Cover due-query exception propagation, false counting, and ordinary behavior.
- GREEN: implement only annotation opt-in, DCC isolation/aggregation, and strict aspect aggregation; run focused tests plus `DccThreeControlledVersionActivationTest`, `DccTargetedActivationJobTest`, and `TenantJobAspectTest` serially.
- Regression: assert successful-only runs preserve the exact existing return string, ordinary tenant job errors keep prior behavior, existing activation audit/transaction rollback tests pass, and no `Error` catch was introduced.

Tests must use mocks or isolated H2 fixtures only. Do not invoke the real scheduler/job, touch runtime DB, restart a service, or run E2E in this review phase.

## Impact and Risks

- A failed due record no longer starves later records in the same tenant run. Successful records before and after it may commit; this is intentional partial progress, not an atomic batch. The final run still fails visibly.
- Under `@TenantJob`, other enabled tenants continue as they do today. If DCC has failures in one or more tenants, the opt-in aggregate must fail the Quartz run after tenant processing. This may cause the already configured Quartz retry behavior to rerun successful tenants, so DCC activation idempotency must be covered by existing and focused tests; retry count/interval are not changed here.
- Structured logs must not serialize full file objects or sensitive payloads. Log only identifiers, process-definition ID when already present in the due object, and exception.
- DCC catches `RuntimeException` per record only. Strict aspect catches tenant `Throwable`s only to continue other tenants, then propagates failure; `Error` is not converted to success.

## Evidence Paths

- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/listener/DccControlledFileActivationJob.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileLifecycleService.java`
- `IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-job/src/main/java/cn/iocoder/yudao/framework/quartz/core/handler/JobHandlerInvoker.java`
- `IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-tenant/src/main/java/cn/iocoder/yudao/framework/tenant/core/job/TenantJobAspect.java`
- `IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-tenant/src/main/java/cn/iocoder/yudao/framework/tenant/core/job/TenantJobParam.java`
- `IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-tenant/src/test/java/cn/iocoder/yudao/framework/tenant/core/job/TenantJobAspectTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccThreeControlledVersionActivationTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccTargetedActivationJobTest.java`
- `doc/tasks/20261021-e22-r5-historical-body-context/scheduler-contract-source-review.md`
