# Flow 6 Bug Regression Evidence

## Bug

Independent batch callers could supply a complete-looking receipt object directly to Flow 6. That allowed the caller payload to reach local validation without a mandatory Flow 9 reload and signature verification by receipt id.

## Expected Behavior

Flow 6 must pass only `receiptId`, `entryType`, and `sourceSnapshotHash` to Flow 9 under the security tenant, replace the caller payload with Flow 9's verified record, and fail fast when formal verification rejects it.

## Root Cause

`MesProductionReleaseBatchExecutionPortImpl` validated the command directly and had no Flow 9 service dependency in the production constructor.

## Reproduction

Run the pre-fix targeted command: `mvn.cmd -Dflatten.skip=true -pl yudao-module-mes -am -Dtest=MesProductionReleaseBatchExecutionPortTest -Dsurefire.failIfNoSpecifiedTests=false test`. The newly added regression test fails at test compilation until the Flow 9 verification seam is added.

## Regression Test

`MesProductionReleaseBatchExecutionPortTest#independentEntryUsesVerifiedReceiptInsteadOfCallerPayload` asserts the exact object returned by Flow 9 is the object passed to both local validation and Tx-B.

## RED/GREEN

RED: targeted Maven test compile failed because the 4-argument constructor and verification call were absent.

GREEN: isolated worktree targeted suite passed 39/39; main `int_main` targeted suite passed 39/39.

## Verification

`git diff --check`, MES compile, targeted Surefire, branch runtime guard, and fast-forward containment all passed. No production service or database was started.

## Risk and Scope

Only the Flow 6 production-release batch port and its regression test changed. Active-order receipt behavior remains on the existing path. No schema, service runtime, or unrelated module changes were made.

## Follow-up Blockers

The full cross-thread runtime chain and database migration evidence still require Flow 4/7/8/9/10 owners and authorized environments.
