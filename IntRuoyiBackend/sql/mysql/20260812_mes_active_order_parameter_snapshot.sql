-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_schema; type=schema; riskLevel=medium
-- C010 freezes canonical device parameter standards on every active-order process snapshot.

ALTER TABLE `mes_pro_process_pool_active_order_process_snapshot`
    ADD COLUMN `parameter_snapshot_json` LONGTEXT NULL COMMENT 'Canonical frozen device parameter rules',
    ADD COLUMN `parameter_snapshot_sha256` CHAR(64) NULL COMMENT 'SHA-256 of canonical parameter snapshot',
    ADD COLUMN `parameter_snapshot_state` VARCHAR(32) NOT NULL DEFAULT 'MISSING_LEGACY'
        COMMENT 'FROZEN or MISSING_LEGACY';
