# Test Release CI/CD Evidence

## Environment

- Target environment: test only.
- Target server: `172.30.30.58`.
- Forbidden targets: production and backup servers.

## Commands

- Build: `publish-int-ruoyi.ps1 -Mode build-release -Component intruoyi ... -SkipDatabaseSync -SkipMinioSync`.
- Deploy: `publish-int-ruoyi.ps1 -Mode deploy-release -Environment test -Component intruoyi ...`.
- Regression: targeted pytest suites and the full migration policy gate.

## Secrets

- NAS credentials are read from the approved local NAS release configuration.
- SSH and runtime secrets are supplied by the existing controlled environment.
- No credential values are recorded in task documents or commits.

## Pipeline

- `release_preflight_plan.py` now uses a stable original-index priority queue for dependency ordering.
- Regression tests cover stable ordering and existing publish-script contracts.

## Verification

- Ordering and publish regression suite: `117 passed`.
- Migration policy gate: `passed`, `migrationCount=383`.
- Regenerated preflight order: cleanup migration before workstation binding.
- Failed test deployment was restored to the previous healthy runtime tag.

## Rollback

- Failed releaseTags are not reused.
- On partial deployment failure, release migration and operation metadata are closed as `FAILED`, `.env IMAGE_TAG` is restored, and unchanged running containers are reverified.
- The cleanup migration writes backup evidence before soft-deleting the confirmed invalid test data.

## Blockers

- `release-20260727-onlyoffice-test-r260727-1823` is invalid because its preflight plan executed workstation binding before cleanup.
- `release-20260727-onlyoffice-test-r260727-1948` is invalid because historical required SQL `20260709_mes_rt000006_batch_record_mapping.sql` requires an `RT000006` route and pressure-pump roles that are absent from the test database.
- Continuing requires an explicit business decision: formal no-op when the route is absent, or complete data reconstruction through a migration.
