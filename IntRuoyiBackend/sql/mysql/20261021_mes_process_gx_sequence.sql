-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260512_mes_base_schema; type=schema; riskLevel=medium
-- 新建工序统一 GX- 流水号；使用独立规则，不继承旧 ER 规则的 Redis 计数。
-- 不更新任何既有工序或历史快照。编号服务同时读取既有 GX 编码及生成记录的最大值。
DROP PROCEDURE IF EXISTS upgrade_mes_process_gx_sequence;
DELIMITER $$
CREATE PROCEDURE upgrade_mes_process_gx_sequence()
BEGIN
  IF EXISTS (SELECT 1 FROM mes_pro_process WHERE LEFT(code, 3)='GX-'
      GROUP BY tenant_id, code HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate GX process codes including deleted rows; migration stopped';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_md_auto_code_rule
      WHERE code='PRO_PROCESS_GX_CODE' AND deleted=0
      GROUP BY tenant_id HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate PRO_PROCESS_GX_CODE rules; migration stopped';
  END IF;

  -- 只约束新编号空间，保留其他历史编码；防止手工修改与并发新增发生重号。
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process' AND COLUMN_NAME='gx_unique_code') THEN
    ALTER TABLE mes_pro_process ADD COLUMN gx_unique_code VARCHAR(64)
      GENERATED ALWAYS AS (CASE WHEN LEFT(code, 3)='GX-' THEN code ELSE NULL END) STORED;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME='mes_pro_process' AND INDEX_NAME='uk_mes_pro_process_gx_code') THEN
    ALTER TABLE mes_pro_process ADD UNIQUE KEY uk_mes_pro_process_gx_code (tenant_id, gx_unique_code);
  END IF;

  INSERT INTO mes_md_auto_code_rule (code, name, max_length, padded, status, tenant_id)
  SELECT 'PRO_PROCESS_GX_CODE', '工序统一流水编码', 64, 0, 0, tenants.tenant_id
  FROM (SELECT id AS tenant_id FROM system_tenant WHERE deleted=0
        UNION SELECT tenant_id FROM mes_pro_process
        UNION SELECT 0) tenants
  WHERE NOT EXISTS (SELECT 1 FROM mes_md_auto_code_rule r
      WHERE r.tenant_id=tenants.tenant_id AND r.code='PRO_PROCESS_GX_CODE' AND r.deleted=0);

  UPDATE mes_md_auto_code_rule SET max_length=64, padded=0, status=0
    WHERE code='PRO_PROCESS_GX_CODE' AND deleted=0;
  UPDATE mes_md_auto_code_part p JOIN mes_md_auto_code_rule r ON p.rule_id=r.id
    SET p.deleted=1 WHERE r.code='PRO_PROCESS_GX_CODE' AND r.deleted=0 AND p.deleted=0;
  INSERT INTO mes_md_auto_code_part (rule_id, sort, type, length, fix_character, tenant_id)
    SELECT id, 1, 3, 3, 'GX-', tenant_id FROM mes_md_auto_code_rule
    WHERE code='PRO_PROCESS_GX_CODE' AND deleted=0;
  INSERT INTO mes_md_auto_code_part
    (rule_id, sort, type, length, serial_start_no, serial_step, cycle_flag, tenant_id)
    SELECT id, 2, 4, 4, 1, 1, 0, tenant_id FROM mes_md_auto_code_rule
    WHERE code='PRO_PROCESS_GX_CODE' AND deleted=0;
END$$
DELIMITER ;
CALL upgrade_mes_process_gx_sequence();
DROP PROCEDURE upgrade_mes_process_gx_sequence;
