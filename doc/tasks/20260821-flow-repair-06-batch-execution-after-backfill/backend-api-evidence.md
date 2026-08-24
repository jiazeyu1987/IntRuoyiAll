# Flow 6 Backend API Evidence

## Scope

`MesProductionReleaseBatchExecutionPortImpl#openOrCreate` now handles independent MANUAL, SCHEDULED, and PQC_INDEPENDENT entries by consuming only `sourceCredentialId` as a receipt identifier. It calls Flow 9 `MesIndependentBatchPrerequisiteReceiptService.verify` with the security tenant, entry type, and source snapshot hash before local Flow 6 contract validation and batch provisioning.

## Contract

- The caller-provided `independentReceipt` object is never trusted as proof of authenticity.
- Flow 9 reloads and verifies canonical payload hash, receipt hash, signature, issuer, tenant, validity window, revocation, entry type, and source snapshot.
- The verified Flow 9 object replaces the caller object before Flow 6 validation and Tx-B delegation.
- Missing or invalid formal verification fails fast; there is no default-success adapter or fallback.

## BDD

BDD: independent receipt reload -> Given an independent entry has only a receipt id and may contain a forged payload, When `openOrCreate` is called, Then Flow 9 verifies the id under the security tenant before local validation and only the verified object reaches Tx-B.

BDD: formal verification failure -> Given Flow 9 rejects the receipt, When `openOrCreate` is called, Then no batch mapper/provision service is allowed to create a batch and the formal error is propagated.

## TDD Evidence

RED: `mvn.cmd -Dflatten.skip=true -pl yudao-module-mes -am -Dtest=MesProductionReleaseBatchExecutionPortTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL; the new regression test could not compile because the 4-argument port constructor and Flow 9 verification seam were absent.

GREEN: isolated worktree targeted suite -> PASS, 39 tests, 0 failures, 0 errors; includes `MesProductionReleaseBatchExecutionPortTest` with the verified-object identity assertion.

GREEN: isolated worktree `mvn.cmd -Dflatten.skip=true -pl yudao-module-mes -am -DskipTests compile` -> PASS, exit code 0.

GREEN: main `int_main` targeted suite -> PASS, 39 tests, 0 failures, 0 errors.

GREEN: main `int_main` `mvn.cmd -Dflatten.skip=true -pl yudao-module-mes -am -DskipTests compile` -> PASS, exit code 0.

## Auth, Errors, and Observability

Tenant context is read from `TenantContextHolder`; the request tenant is still checked by the existing Flow 6 entry contract. Flow 9 owns signature/hash/revocation/expiry error mapping. Existing Flow 6 audit and provisioning records remain the only downstream state writers; this slice adds no fallback or alternate source.

## Validation

The isolated and mainline Maven commands above reached MES Surefire and reported 39/39 passing tests. The mainline compile reached all 24 reactor modules and exited 0. `git diff --check` and the branch runtime guard also passed.

## Verification

`git merge-base --is-ancestor 90455bdba int_main` returned success, and `git show --stat 90455bdba` contains only the Flow 6 port and its regression test.

## Blockers

Database migration apply/rollback, service runtime, and write E2E were not run. Full Flow 4 Tx-A, Flow 7 Tx-C, Flow 8 materials, and Flow 10 release runtime evidence remain cross-thread gates.
