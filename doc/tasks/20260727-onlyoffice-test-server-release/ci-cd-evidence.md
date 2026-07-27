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
- Code-only SQL selection preserves manifest dependency metadata and removes the full direct/transitive data dependency closure.
- Regression tests cover stable ordering, code-only dependency closure and existing publish-script contracts.

## Verification

- Ordering and publish regression suite: `117 passed`.
- Migration policy gate: `passed`, `migrationCount=383`.
- Regenerated preflight order: cleanup migration before workstation binding.
- Code-only required SQL regression: `125 passed`; `type=data` and direct/transitive dependents are filtered before remote MySQL execution, while missing migration type or dependency mappings fail fast.
- Real r3 manifest/preflight simulation: failed DCC seed and named MES data migrations excluded, independent schema retained.
- r4 package integrity: local and NAS Manifest v1 / legacy manifests matched, `3373` artifacts checked with missing `0`, size mismatch `0`, hash mismatch `0`, and no database dump, MinIO snapshot or runtime-data.
- OnlyOffice health-check quoting regression: targeted pytest `1 passed`, expanded publish regression `125 passed`, PowerShell parser passed, `git diff --check` passed, branch runtime port guard passed.
- r4 failed after container switch because the deploy validation command used nested `sh -lc` and dropped the URL argument; the lock was closed as `FAILED` and a fresh r5 releaseTag is required.

## Rollback

- Failed releaseTags are not reused.
- On partial deployment failure, release migration and operation metadata are closed as `FAILED`, `.env IMAGE_TAG` is restored, and unchanged running containers are reverified.
- The cleanup migration writes backup evidence before soft-deleting the confirmed invalid test data.

## Blockers

- `release-20260727-onlyoffice-test-r260727-1823` is invalid because its preflight plan executed workstation binding before cleanup.
- `release-20260727-onlyoffice-test-r260727-1948` is invalid because historical required SQL `20260709_mes_rt000006_batch_record_mapping.sql` requires an `RT000006` route and pressure-pump roles that are absent from the test database.
- `release-20260727-onlyoffice-test-r260727-codeonly-r3` is invalid because direct filtering left a seed dependent on skipped data in the APPLY queue.
- `release-20260727-onlyoffice-test-r260727-codeonly-r4` is invalid because deploy-release did not complete its final OnlyOffice reachability validation.
- Data migrations and their dependency descendants are no longer part of the code-only remote MySQL APPLY queue. A new r5 releaseTag is required for the final test-server deployment.
