-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260831_mes_frontline_feedback_material; type=schema; riskLevel=low
-- The batch-record material source has no BOM usage ratio. Preserve legacy values and allow new facts to leave it NULL.

ALTER TABLE `mes_pro_feedback_material`
  MODIFY COLUMN `bom_quantity` decimal(24,6) DEFAULT NULL
  COMMENT 'Legacy BOM usage ratio; null for batch-record material configuration';

-- Rollback (only when no row has NULL bom_quantity):
-- ALTER TABLE `mes_pro_feedback_material`
--   MODIFY COLUMN `bom_quantity` decimal(24,6) NOT NULL
--   COMMENT 'Frozen BOM usage ratio';
