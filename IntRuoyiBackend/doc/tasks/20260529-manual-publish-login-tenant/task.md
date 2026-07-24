# 20260529-manual-publish-login-tenant

## Task Goal

Change manual showroom publish so the publish source tenant follows the current logged-in tenant instead of the preexisting `showroom_public_site_binding` tenant mapping.

## Milestones

- [x] Create task record before code changes.
- [x] Add RED regression tests for login-tenant-driven manual publish behavior.
- [x] Implement manual publish source resolution from current tenant and update the public site binding atomically.
- [x] Run targeted verification and record final status.

## Expected Verification

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomReleaseAdminPublishIntegrationTest,ShowroomTenantIsolationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Current Status

Completed.
