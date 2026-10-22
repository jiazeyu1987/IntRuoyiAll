-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_mes_base_schema; type=schema; riskLevel=medium
-- 新增流程：同租户名称/编码唯一；GYLC 固定前缀、四位最小宽度、永不重置。
DROP PROCEDURE IF EXISTS upgrade_mes_route_create_sequence;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_route_create_sequence()
BEGIN
  IF EXISTS (SELECT 1 FROM mes_pro_route WHERE deleted=0 AND name IS NOT NULL
      GROUP BY tenant_id, TRIM(name) HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate live route names; resolve explicitly before migration';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_pro_route WHERE code IS NOT NULL
      GROUP BY tenant_id, code HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate route codes including deleted records; migration stopped';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_md_auto_code_rule WHERE code='PRO_ROUTE_CODE' AND deleted=0
      GROUP BY tenant_id HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate PRO_ROUTE_CODE rules; migration stopped';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_route' AND COLUMN_NAME='live_unique_name') THEN
    ALTER TABLE mes_pro_route ADD COLUMN live_unique_name VARCHAR(128)
      GENERATED ALWAYS AS (CASE WHEN deleted=0 THEN TRIM(name) ELSE NULL END) STORED;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_route' AND INDEX_NAME='uk_mes_pro_route_name_live') THEN
    ALTER TABLE mes_pro_route ADD UNIQUE KEY uk_mes_pro_route_name_live (tenant_id, live_unique_name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_route' AND INDEX_NAME='uk_mes_pro_route_code') THEN
    ALTER TABLE mes_pro_route ADD UNIQUE KEY uk_mes_pro_route_code (tenant_id, code);
  END IF;

  INSERT INTO mes_md_auto_code_rule
    (code, name, max_length, padded, status, tenant_id)
  SELECT 'PRO_ROUTE_CODE', '工艺流程编码', 64, 0, 0, tenants.tenant_id
  FROM (SELECT id AS tenant_id FROM system_tenant WHERE deleted=0
        UNION SELECT tenant_id FROM mes_pro_route
        UNION SELECT 0) tenants
  WHERE NOT EXISTS (SELECT 1 FROM mes_md_auto_code_rule r
      WHERE r.tenant_id=tenants.tenant_id AND r.code='PRO_ROUTE_CODE' AND r.deleted=0);

  UPDATE mes_md_auto_code_rule SET max_length=64, padded=0, status=0
    WHERE code='PRO_ROUTE_CODE' AND deleted=0;
  UPDATE mes_md_auto_code_part p JOIN mes_md_auto_code_rule r ON p.rule_id=r.id
    SET p.deleted=1 WHERE r.code='PRO_ROUTE_CODE' AND r.deleted=0 AND p.deleted=0;
  INSERT INTO mes_md_auto_code_part
    (rule_id, sort, type, length, fix_character, tenant_id)
    SELECT id, 1, 3, 5, 'GYLC-', tenant_id FROM mes_md_auto_code_rule
    WHERE code='PRO_ROUTE_CODE' AND deleted=0;
  INSERT INTO mes_md_auto_code_part
    (rule_id, sort, type, length, serial_start_no, serial_step, cycle_flag, tenant_id)
    SELECT id, 2, 4, 4, 1, 1, 0, tenant_id FROM mes_md_auto_code_rule
    WHERE code='PRO_ROUTE_CODE' AND deleted=0;
END$$
DELIMITER ;
CALL upgrade_mes_route_create_sequence();
DROP PROCEDURE upgrade_mes_route_create_sequence;
