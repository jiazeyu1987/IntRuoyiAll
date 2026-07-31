# Database Schema Evidence

## Data Change Goal

将本地 `mes_kingdee_production_material_list` 白名单数据同步到测试服务器 `172.30.30.58` 的对应业务库和租户范围。

## Database Engine And Tooling

- Engine: MySQL / MariaDB compatible containerized database, exact versions to be confirmed by read-only probes.
- Tooling: `docker exec` local MySQL client and SSH remote `docker exec` MySQL client; SQL is passed through stdin without logging secrets.

## Affected Entities

- Business data table: `mes_kingdee_production_material_list`.
- Read-only prerequisite checks: `system_tenant`, `system_menu`, `system_role_menu`, `infra_job`, information schema columns/statistics.

## Data Safety Analysis

- No write is allowed until local and remote schemas, tenant IDs, row counts, unique keys, and backup path are verified.
- Upsert key must match the declared unique key: `tenant_id, source_bill_no, production_order_no, production_order_line_no, child_material_code`.
- Existing target rows outside the confirmed source business-key range must not be deleted.
- Directly copying local linkage IDs is unsafe because target master-data IDs do not align with local source IDs.
- Recomputing linkage IDs by code is also unsafe without an explicit decision because target item/work-order codes include duplicates and some dependencies are missing.

## Rollback Or Recovery Plan

- Before upsert, export the target table/range from the test server to a task-owned backup file.
- Recovery procedure is to restore the backed-up target rows or table snapshot if post-sync validation fails.

## BDD Scenarios

- BDD: Data-only production material list sync -> Given source and target schemas match, When target backup succeeds and upsert runs, Then target business-key count and hash match source.
- BDD: Missing prerequisite blocks write -> Given table, tenant, unique key, or backup is missing, When sync is requested, Then the operation stops before any remote write.

## RED Command And Expected Failure

- RED: safe-direct-upsert-preflight -> FAIL, expected reason: local `work_order_id`/`product_id`/`child_material_id`/`work_order_bom_id` do not align with target IDs, and target code-based remapping is ambiguous or incomplete.

## GREEN Command And Passing Result

- GREEN: local-remote-schema-readonly -> PASS, schema/unique-key hashes match.
- GREEN: remote-target-backup -> PASS, backup file exists, `gzip -t` passes, SHA256 recorded.
- GREEN: staging-load-verify -> PASS, staged source rows equal local count and hash, then staging table removed.

## Migration Verification

- No schema migration is planned in this data-only task. Existing migration file `20260613_mes_kingdee_production_material_list.sql` is the schema contract.

## Blockers

- Awaiting user confirmation for the safe upsert policy: sync list-detail business fields while not copying invalid local linkage IDs; unresolved target linkage IDs remain null or existing target mappings are preserved.
