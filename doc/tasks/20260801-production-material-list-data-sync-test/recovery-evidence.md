# Recovery Evidence

## Recovery Scope And Data Inventory

- Scope: test server table/range backup for `mes_kingdee_production_material_list` before data-only upsert.
- Included dependencies: target tenant row identity and target table DDL/index metadata.

## Backup Frequency And Retention

- One task-owned pre-upsert backup snapshot.
- Backup path: `/var/lib/docker/intruoyi-data/runtime-data/task-backups/20260801-production-material-list-data-sync-test/mes_kingdee_production_material_list_before_20260801-005623.sql.gz`.
- Retention duration: task-owned test-server backup retained until user confirms cleanup or the task is closed with a replacement retention decision.

## RTO And RPO

- RTO/RPO are not defined for this ad hoc test data sync. The task must not claim disaster recovery readiness; it only requires a task-local rollback backup before upsert.

## Restore Procedure

- Restore target table from the gzip-compressed mysqldump after stopping any active conflicting sync/write task:
  `gzip -dc <backup.sql.gz> | docker exec -i -e MYSQL_PWD=<redacted> intruoyi-mysql mysql -uroot ruoyi-vue-pro`
- Secrets are not stored in this evidence file; use the test server runtime `.env` source for restore execution.

## Restore Test Evidence Or Blocker

- Backup existence/hash/gzip verification passed.
- Full destructive restore test is not run because the target table has not yet been modified; if upsert proceeds and validation fails, restore execution becomes the next required step.

## Verification

- GREEN: remote-target-backup -> PASS, gzip backup created at `/var/lib/docker/intruoyi-data/runtime-data/task-backups/20260801-production-material-list-data-sync-test/mes_kingdee_production_material_list_before_20260801-005623.sql.gz`.
- GREEN: gzip -t backup -> PASS.
- GREEN: sha256sum backup -> PASS, `a35afce295013118dab19761130eeeb553aced1800215b8d7043e7ee58752a7e`.

## Owners

- Operator: current Codex task under user authorization.
- Target environment owner: test server `172.30.30.58`.

## Blockers

- Awaiting user confirmation on safe upsert linkage policy before any target data modification.
