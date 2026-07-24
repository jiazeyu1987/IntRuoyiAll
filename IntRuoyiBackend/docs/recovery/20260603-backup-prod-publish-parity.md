# Backup Disaster Recovery Evidence: backup/prod publish parity

## Scope

This evidence covers the release-package deployment path used to promote a tested package to production and backup environments. It includes an authorized backup-server deployment attempt and records the remaining restore blocker.

## Inventory

- Application images: backend, frontend and OnlyOffice image tar in the release package.
- MySQL data: `ruoyi-vue-pro-current.sql` for with-data packages.
- Object storage: release package `minio/yudao` snapshot for with-data packages.
- Required SQL: packaged `required-sql` scripts.
- Runtime config: packaged `.env`, compose file and Website runtime assets.
- External dependencies: NAS release repository, SSH/SCP access, Docker/Compose, eDHR S3/Object Lock verifier settings, target MinIO container credentials.
- Backup target MinIO: `intruoyi-minio` on `172.30.30.59`, backed by `/mnt/intruoyi-data/runtime-data/minio`.

## Backup

The release package is the recovery artifact for deploy-release. `release-manifest.json` now provides the package `publishScope`, and deploy-release uses it to distinguish `with-data` from `code-only`.

## Retention

NAS release package retention is owned by the existing release repository process. This task did not change retention duration or deletion policy.

## RTO

RTO is not formally defined in the available project evidence. This remains a release-readiness blocker for disaster recovery commitments.

## RPO

RPO depends on the age and scope of the selected tested release package. A with-data package carries the MySQL dump and MinIO snapshot captured at build time; code-only packages carry no database/object restore data.

## Restore

Restore procedure for a tested with-data package:

1. Download the tested package from NAS.
2. Validate manifest `publishScope`.
3. Require MySQL dump and MinIO `yudao` snapshot for `with-data`.
4. Load application images.
5. Sync MinIO to the target environment using the declared remote MinIO container.
6. Reset/import MySQL when database sync is enabled.
7. Apply target-bound SQL and required SQL.
8. Start services and run health/readback gates.
9. Record prod or backup deployment history.

## Verification

Local verification passed:

- Runtime-control deploy argument tests.
- Publish script manifest/MinIO contract tests.
- Runtime-control script contract tests.
- PowerShell parser check.

Authorized backup-server verification passed for target MinIO readiness: container running, host health endpoint reachable, backend container reachability to `host.docker.internal:9000` reachable, and `yudao` bucket exists.

Authorized backup deploy-release of tested code-only package `26-06-02 20:13:57` reached service startup and health gates, including backend, frontend, OnlyOffice, Website and backend-to-MinIO reachability. It failed final restore/readback verification because showroom image readback returned JSON instead of `image/*`; this package has no MinIO snapshot and therefore cannot seed the newly provisioned backup MinIO.

## Blockers

- Live backup with-data restore/deploy verification requires a valid tested with-data release package. Current NAS prod history package `20260603_website_assets_cache_immutable` declares `publishScope=with-data` but lacks both `ruoyi-vue-pro-current.sql` and `minio/yudao`; corrected deploy logic rejects it.
- A code-only package cannot establish object-store RPO for a newly provisioned backup MinIO. An approved data baseline is required before code-only backup promotion can pass final file smoke.
- Formal RTO is not documented.

## Owners And Next Actions

- Release owner: approve whether to correct invalid NAS release manifests, build/test a new complete with-data package, or provide another tested complete release package.
- Data owner: confirm the approved source of the backup MinIO object baseline.
- Recovery gate: do not skip file smoke, do not deploy an untested with-data package, and do not rewrite release audit records without explicit approval.
