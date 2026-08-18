-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260812_mes_pqc_dcc_qa_c00_postflight; type=schema; riskLevel=high
-- C015 structural bootstrap only. It creates storage for an approved ID mapping and never infers a relationship.

DROP PROCEDURE IF EXISTS bootstrap_mes_c015_route_dcc_qa_reconciliation;
DELIMITER $$
CREATE PROCEDURE bootstrap_mes_c015_route_dcc_qa_reconciliation()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
     WHERE table_schema = DATABASE() AND table_name = 'mes_md_item'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C015 bootstrap failed: mes_md_item base table is missing';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'mes_md_item' AND column_name = 'product_master_id'
  ) THEN
    ALTER TABLE `mes_md_item`
      ADD COLUMN `product_master_id` bigint DEFAULT NULL COMMENT 'MDM产品主数据ID' AFTER `id`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'mes_md_item' AND column_name = 'product_master_id'
       AND (data_type <> 'bigint' OR is_nullable <> 'YES')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C015 bootstrap failed: mes_md_item.product_master_id has a non-canonical definition';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM (
        SELECT non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
          FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = 'mes_md_item'
           AND index_name = 'idx_mes_md_item_product_master'
         GROUP BY non_unique
      ) product_master_index
     WHERE non_unique <> 1 OR columns_in_order <> 'tenant_id,product_master_id,deleted'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C015 bootstrap failed: idx_mes_md_item_product_master has a non-canonical signature';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'mes_md_item'
       AND index_name = 'idx_mes_md_item_product_master'
  ) THEN
    ALTER TABLE `mes_md_item`
      ADD KEY `idx_mes_md_item_product_master` (`tenant_id`, `product_master_id`, `deleted`);
  END IF;
END$$
DELIMITER ;

CALL bootstrap_mes_c015_route_dcc_qa_reconciliation();
DROP PROCEDURE IF EXISTS bootstrap_mes_c015_route_dcc_qa_reconciliation;
