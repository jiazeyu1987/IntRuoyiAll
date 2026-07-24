# Task: Showroom Public Display Pure Frontstage Contract

## Goal

Expose the minimum public showroom display routes needed by the Website display-only frontend so it can read company, hall, product, and narration data without local fallback logic.

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-public-display-pure-frontstage\**`

## Non-Scope

- No schema changes
- No fallback or mock business data
- No admin route exposure

## Milestones

1. Audit the current anonymous display route coverage.
2. Add RED contract coverage for public frontstage routes.
3. Open the required display routes for anonymous reads.
4. Verify the affected contract tests.

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomDisplayCompanyAnonymousContractTest,ShowroomHttpApiIntegrationTest#publicFrontstageDisplayEndpointsShouldBeAnnotatedPermitAllExceptHome" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-public-display-pure-frontstage\backend-api-evidence.md`

## Current Status

- Status: Completed
- Completed work:
  - Opened anonymous access for `GET /showroom/display/hall/{hallId}`, `GET /showroom/display/product/{productId}`, and `GET /showroom/display/narration`.
  - Kept `GET /showroom/display/home` protected.
  - Updated public display contract tests to reflect the new frontstage route surface.
  - Preserved fail-fast behavior for missing live data.
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomDisplayCompanyAnonymousContractTest,ShowroomHttpApiIntegrationTest#publicFrontstageDisplayEndpointsShouldBeAnnotatedPermitAllExceptHome" "-Dsurefire.failIfNoSpecifiedTests=false" test`
