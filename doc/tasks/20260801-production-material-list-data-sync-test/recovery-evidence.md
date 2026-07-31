# Recovery Evidence

## Recovery Scope And Data Inventory

- Scope: test server table/range backup for `mes_kingdee_production_material_list` before data-only upsert.
- Included dependencies: target tenant row identity and target table DDL/index metadata.

## Backup Frequency And Retention

- One task-owned pre-upsert backup snapshot.
- Retention path and duration to be recorded after backup creation.

## RTO And RPO

- RTO/RPO are not defined for this ad hoc test data sync. The task must not claim disaster recovery readiness; it only requires a task-local rollback backup before upsert.

## Restore Procedure

- Restore procedure will be recorded after backup format is selected and verified.

## Restore Test Evidence Or Blocker

- Pending. A backup existence/hash/row-count check is required before write; full destructive restore test is not planned unless validation fails.

## Owners

- Operator: current Codex task under user authorization.
- Target environment owner: test server `172.30.30.58`.

## Blockers

- Pending backup creation and verification.

