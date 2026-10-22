-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_process_pool_active_order_authority; type=schema; riskLevel=medium
-- Preserve replaced execution cycles; do not rewrite historical business data.
DROP PROCEDURE IF EXISTS upgrade_mes_active_order_rework_cycle;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_active_order_rework_cycle()
BEGIN
  DECLARE actual_expression TEXT;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing active order prerequisite table';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND column_name='rework_source_active_order_id') THEN
    ALTER TABLE mes_pro_process_pool_active_order ADD COLUMN rework_source_active_order_id BIGINT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND column_name='rework_review_id') THEN
    ALTER TABLE mes_pro_process_pool_active_order ADD COLUMN rework_review_id BIGINT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND column_name='current_route_version_id') THEN
    ALTER TABLE mes_pro_process_pool_active_order ADD COLUMN current_route_version_id BIGINT GENERATED ALWAYS AS
      (CASE WHEN business_status IN ('REWORKED','VERSION_UPGRADED') THEN NULL ELSE route_version_id END) STORED;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND index_name='uk_mes_pp_active_order'
      AND column_name='route_version_id') THEN
    IF COALESCE((SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics
        WHERE table_schema=DATABASE() AND table_name='mes_pro_process_pool_active_order'
        AND index_name='uk_mes_pp_active_order' AND non_unique=0),'')
        <> 'tenant_id,work_order_id,route_id,route_version_id,deleted' THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Previous active order unique index is incompatible';
    END IF;
    ALTER TABLE mes_pro_process_pool_active_order
      DROP INDEX uk_mes_pp_active_order,
      ADD UNIQUE KEY uk_mes_pp_active_order (tenant_id,work_order_id,route_id,current_route_version_id,deleted);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND index_name='uk_mes_pp_rework_review') THEN
    ALTER TABLE mes_pro_process_pool_active_order
      ADD UNIQUE KEY uk_mes_pp_rework_review (tenant_id,rework_review_id,deleted);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND index_name='idx_mes_pp_order_cycle_history') THEN
    ALTER TABLE mes_pro_process_pool_active_order
      ADD KEY idx_mes_pp_order_cycle_history (tenant_id,work_order_id,route_id,route_version_id,deleted);
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order'
      AND column_name IN ('rework_source_active_order_id','rework_review_id')
      AND data_type='bigint' AND is_nullable='YES') <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Rework relation columns must be nullable BIGINT';
  END IF;
  -- MySQL metadata may escape literal quotes; compare the canonical expression.
  SELECT REPLACE(REGEXP_REPLACE(REPLACE(LOWER(generation_expression),'_utf8mb4',''), '[[:space:]`()]+', ''),CONCAT(CHAR(92),CHAR(39)),CHAR(39))
    INTO actual_expression FROM information_schema.columns WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND column_name='current_route_version_id'
      AND data_type='bigint' AND extra LIKE '%STORED GENERATED%';
  IF actual_expression IS NULL OR actual_expression <>
      'casewhenbusiness_statusin''reworked'',''version_upgraded''thennullelseroute_version_idend' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Active order current version expression is incompatible';
  END IF;
  IF COALESCE((SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='mes_pro_process_pool_active_order'
      AND index_name='uk_mes_pp_active_order' AND non_unique=0),'')
      <> 'tenant_id,work_order_id,route_id,current_route_version_id,deleted'
      OR (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND index_name='uk_mes_pp_active_order') <> 5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Active order current unique index is incompatible';
  END IF;
  IF COALESCE((SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='mes_pro_process_pool_active_order'
      AND index_name='uk_mes_pp_rework_review' AND non_unique=0),'') <> 'tenant_id,rework_review_id,deleted'
      OR (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND index_name='uk_mes_pp_rework_review') <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Rework review unique index is incompatible';
  END IF;
  IF COALESCE((SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='mes_pro_process_pool_active_order'
      AND index_name='idx_mes_pp_order_cycle_history' AND non_unique=1),'')
      <> 'tenant_id,work_order_id,route_id,route_version_id,deleted'
      OR (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE()
      AND table_name='mes_pro_process_pool_active_order' AND index_name='idx_mes_pp_order_cycle_history') <> 5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Active order cycle history index is incompatible';
  END IF;
END$$
DELIMITER ;
CALL upgrade_mes_active_order_rework_cycle();
DROP PROCEDURE upgrade_mes_active_order_rework_cycle;
