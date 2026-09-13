# DCC-STATIC-016 Verification Report

## Result

PASS_READY_FOR_INT_MAIN_FUSION

## Bug

DCC-STATIC-016 from `docs/bugs/20260912-dcc-90-step-static-audit.md`: the product onboarding approval continuation entry was effectively tied to `dcc:project-code:create`, so an approval-only user with query/update rights could not reach pending product onboarding requests from the formal page.

## Expected

Users with `dcc:project-code:update` can open the product onboarding continuation entry, restore a pending request, and approve it. Users with only update permission must not gain the ability to create a new onboarding request, and create-only users must not gain the ability to approve.

## Reproduction

Static reproduction target: `node IntRuoyiFronted/tests/e2e/dcc-static-016-product-onboarding-approval-entry-static.spec.cjs` checks the frontend entry, submit guard, approval guard, pending-list recovery path, frontend API endpoint, backend controller permission contract, service contract, mapper status filter, and service unit coverage.

## Root Cause

The formal pending/approval backend path already separated create and update permissions, but the frontend entry carrying the pending list was hidden behind create-only permission. Approval-only users therefore had backend permission but no reachable continuation path.

## RED Evidence

- RED: `node IntRuoyiFronted/tests/e2e/dcc-static-016-product-onboarding-approval-entry-static.spec.cjs` -> FAIL, expected reason: `product onboarding entry must be reachable to approval-only users with update permission`.

## Fix Summary

- Added formal pending request query API: `GET /dcc/product-onboarding-requests/pending` with create-or-update permission.
- Added service and mapper pending-list query filtered by `PENDING_APPROVAL`.
- Updated the frontend entry so create or update permission holders can open product onboarding continuation.
- Kept creation submit guarded by `dcc:project-code:create` and approval guarded by `dcc:project-code:update`.
- Added pending-list recovery in the dialog and locked restored pending request fields to avoid converting approval continuation into an edit fallback.
- Updated only the DCC-STATIC-016 row and evidence in the shared bug audit document.

## GREEN Evidence

- GREEN: `node IntRuoyiFronted/tests/e2e/dcc-static-016-product-onboarding-approval-entry-static.spec.cjs` -> PASS.
- GREEN: `node IntRuoyiFronted/tests/e2e/dcc-project-code-product-onboarding-static.spec.js` -> PASS.
- GREEN: `mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- GREEN: `pnpm.cmd ts:check` from `IntRuoyiFronted` -> PASS.
- GREEN: `git diff --check` -> PASS, only CRLF working-copy warnings.
- GREEN: after conflict resolution, `node IntRuoyiFronted/tests/e2e/dcc-static-016-product-onboarding-approval-entry-static.spec.cjs` -> PASS.
- GREEN: after conflict resolution, `node IntRuoyiFronted/tests/e2e/dcc-project-code-product-onboarding-static.spec.js` -> PASS.
- GREEN: after conflict resolution, `mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- GREEN: after conflict resolution, `pnpm.cmd ts:check` -> PASS.
- GREEN: `git diff --cached --check` -> PASS.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS after Git common-dir slot 11 registration.

## Verification

Verified the DCC-STATIC-016 permission boundary by static contract, adjacent product onboarding frontend contract, backend DCC service unit test, frontend relaxed TypeScript check, whitespace check, and branch runtime port guard. No real E2E, service start/restart, or database write was performed.

## Blockers

None for DCC-STATIC-016 implementation and verification. Remaining audit items DCC-STATIC-017 to DCC-STATIC-021 and DCC-STATIC-024 to DCC-STATIC-027 are still open and outside this task; DCC-STATIC-022/023 were pre-existing fixed items preserved during conflict resolution.
