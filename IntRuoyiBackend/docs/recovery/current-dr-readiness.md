# Current Disaster Recovery Readiness

Current Gate: BLOCKED

This document is the current IntRuoyi disaster recovery readiness summary as of 2026-06-05. It supersedes older readiness interpretations for current gate decisions. Reviewers must not use old 2026-05-24 evidence as current readiness without reconciling it with the 2026-06-04 and 2026-06-05 recovery set, release package, owner matrix, and rollback evidence listed here.

## Scope

This readiness view covers the local Runtime Control and backup operations path for:

- application release package build, publish-test, mark-tested, promote-prod, and promote-backup;
- backup-now, restore-data, and rollback-app;
- MySQL data, MinIO `yudao` objects, DCC files stored in object storage, Redis recovery policy, runtime configuration, and release package manifests;
- backup server takeover through `promote-backup`.

This document does not claim production or backup server execution that has not been run in the current task. Server writes, real `rollback-app`, and real `promote-backup` remain authorization-gated.

## Inventory

Recoverable assets:

- Application images: backend, frontend, Website, and OnlyOffice when included in the release package.
- MySQL: `ruoyi-vue-pro-current.sql` in a complete `with-data` release package or the recovery set MySQL dump.
- MinIO/object storage: `minio/yudao` in a complete `with-data` release package and `recoverySet.minio.snapshotPath` in backup operations.
- DCC files: DCC object keys under MinIO/object storage must be included through the object snapshot and object-level inventory/hash contract added by backend commit `c26b3067a4`.
- Redis: recovery set must declare `recoverySet.redis.policy`; restore candidates without it are blocked by backend commit `693530e6da`.
- Runtime configuration: recovery set must declare `recoverySet.configuration.manifestPath` and `recoverySet.configuration.composePath`; missing values are blocked by commit `693530e6da`.
- Release package NAS repository: accessed through NAS Management configuration; secrets are not stored in this document.

Non-recoverable or externally controlled dependencies:

- NAS availability and credentials.
- SSH access to target servers.
- Docker/Compose availability on target servers.
- External S3/Object Lock verifier configuration for regulated archive evidence.
- Formal business RTO/RPO decisions and approval owners.

## Backup

There are two separate backup artifacts:

1. A `with-data` release package captures the application image tar, MySQL dump, required SQL, runtime assets, and MinIO `yudao` snapshot. If DCC files are stored in MinIO `yudao`, the package is expected to carry DCC file data through that object snapshot and the DCC inventory/hash manifest.
2. A backup operations recovery set captures MySQL, object storage, Redis policy, runtime configuration references, checksums, and program image tag for restore-data.

`code-only` packages do not carry MySQL or MinIO data and cannot prove DCC file recovery for a newly provisioned target.

## Retention

NAS release package retention is owned by the release repository process and is not formally redefined here. Backup recovery set retention and cleanup policy must preserve:

- `manifest.json`;
- checksum inventory;
- MySQL dump;
- MinIO/object snapshot;
- DCC object inventory/hash evidence;
- Redis policy and runtime configuration references.

Retention is not ready for a DCC 1W+ daily-growth commitment until the owner confirms duration, capacity budget, cleanup policy, and evidence retention responsibilities.

## RTO

RTO is not formally defined in the available current evidence. This blocks any claim that the system meets a business recovery-time commitment.

## RPO

RPO depends on the selected tested recovery set and the selected tested `with-data` release package.

- For restore-data, RPO is the timestamp and completeness of the chosen recovery set.
- For promote-backup, RPO is valid only when `releasePackageA` and `recoverySetA` are explicitly bound through `tested.json.recoverySet` and the current restore candidate hash.
- For `code-only` deploys, RPO for MySQL, MinIO, and DCC files is not established.

## Restore

Restore-data readiness requires:

1. Select an available `selectedRecoverySetCandidateId`.
2. Confirm `recoverySet.id`, `status=COMPLETE`, program image tag, Redis policy, runtime configuration manifest/compose paths, checksum hash, MySQL dump, and object snapshot.
3. Confirm the current runtime image tag matches the recovery set program image tag before restore.
4. Create a pre-restore snapshot of the target.
5. Restore MySQL and object storage according to the selected recovery set.
6. Verify backend health, frontend access, DCC file readback/download/preview, and critical business paths.

Application rollback readiness is separate. `rollback-app` only changes application `IMAGE_TAG` and restarts backend/frontend; it does not restore MySQL, MinIO, Redis, or DCC object data.

Backup server takeover readiness is also separate. `promote-backup` must deploy a tested release package and must recheck `tested.json.recoverySet` against the current restore candidate before dispatch.

## Verification

Completed evidence:

- DCC object inventory/hash for release package and deploy validation: backend commit `c26b3067a4`.
- restore-data version, Redis policy, and runtime configuration gate: backend commit `693530e6da`.
- Runtime Control default owner matrix and local capacity alert station-message owner: backend commit `be63394046`.
- Local promote-backup gate verified by `RuntimeControlServiceImplTest#executePromoteBackupShouldDeployOnlyVerifiedReleasePackageAndKeepProdGuard` and `#executePromoteBackupShouldBlockReleasePackageWithoutTestedMarkerBeforeDispatch`.
- 2026-06-04 single recovery set evidence records restore-data success to test and backup environments, but it does not prove application rollback or backup server takeover.

Not completed:

- Real frontend-path `rollback-app` on test and backup environments.
- Real `promote-backup` takeover with a unique `releasePackageA` and `recoverySetA`.
- Complete `with-data` package DCC readback/download/preview verification after restore or backup takeover.
- External webhook or real receiver evidence for operational alert routing.

## Blockers

- `rollback-app` E2E is still blocked by missing current-task authorization because it modifies target `IMAGE_TAG` and restarts backend/frontend.
- `promote-backup` is still blocked as a readiness claim until a real backup server takeover binds `releasePackageA` and `recoverySetA`.
- `with-data` DCC file recovery is not launch-ready until a complete tested with-data package or recovery set proves object snapshot, DCC inventory/hash, readback, download, preview, and critical business paths.
- RTO is not formally defined.
- Retention duration, capacity budget, and cleanup policy for DCC 1W+ daily-growth backups are not formally approved.
- External webhook/notification receiver evidence is not verified.

## Owners And Next Actions

- Release owner: approve and execute real test/backup `rollback-app`, or confirm it remains blocked.
- Release owner: approve a real backup server `promote-backup` rehearsal using a tested package with `tested.json.recoverySet`.
- Data owner: approve the recovery set or with-data package that establishes DCC file RPO.
- Operations owner: define RTO, retention duration, capacity budget, cleanup policy, and alert receiver evidence.
- Reviewer: keep the gate BLOCKED until the missing real execution evidence is attached.
