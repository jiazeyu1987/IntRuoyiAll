-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261103_mes_project_production_codes; type=data; riskLevel=medium
-- 仅补齐活跃租户缺失的正式报工编号规则；保留已有配置和历史流水。
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS initialize_missing_mes_feedback_codes;
DELIMITER //
CREATE PROCEDURE initialize_missing_mes_feedback_codes()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  DROP TEMPORARY TABLE IF EXISTS tmp_missing_mes_feedback_codes;
  CREATE TEMPORARY TABLE tmp_missing_mes_feedback_codes (
    tenant_id BIGINT PRIMARY KEY,
    start_no BIGINT NOT NULL
  );
  INSERT INTO tmp_missing_mes_feedback_codes
  SELECT t.id, COALESCE((SELECT MAX(CAST(SUBSTRING(h.code,4) AS UNSIGNED))
    FROM (SELECT tenant_id,code COLLATE utf8mb4_bin AS code FROM mes_pro_feedback
          UNION ALL SELECT tenant_id,result COLLATE utf8mb4_bin FROM mes_md_auto_code_record) h
    WHERE h.tenant_id=t.id AND h.code REGEXP '^FB-[0-9]+$'),0)+1
  FROM system_tenant t WHERE t.deleted=0 AND NOT EXISTS (
    SELECT 1 FROM mes_md_auto_code_rule r WHERE r.tenant_id=t.id
      AND r.code='PRO_FEEDBACK_CODE' AND r.project_id IS NULL AND r.deleted=0
  );
  IF EXISTS (SELECT 1 FROM tmp_missing_mes_feedback_codes WHERE start_no>2147483647) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='FEEDBACK_CODE_SERIAL_OUT_OF_RANGE';
  END IF;
  INSERT INTO mes_md_auto_code_rule (tenant_id,code,name,max_length,padded,status)
  SELECT tenant_id,'PRO_FEEDBACK_CODE','生产报工单编码',64,0,0 FROM tmp_missing_mes_feedback_codes;
  INSERT INTO mes_md_auto_code_part (tenant_id,rule_id,sort,type,length,fix_character)
  SELECT r.tenant_id,r.id,1,3,3,'FB-' FROM mes_md_auto_code_rule r
  JOIN tmp_missing_mes_feedback_codes m ON m.tenant_id=r.tenant_id
  WHERE r.code='PRO_FEEDBACK_CODE' AND r.project_id IS NULL AND r.deleted=0;
  INSERT INTO mes_md_auto_code_part
    (tenant_id,rule_id,sort,type,length,serial_start_no,serial_step,cycle_flag)
  SELECT r.tenant_id,r.id,2,4,6,m.start_no,1,0 FROM mes_md_auto_code_rule r
  JOIN tmp_missing_mes_feedback_codes m ON m.tenant_id=r.tenant_id
  WHERE r.code='PRO_FEEDBACK_CODE' AND r.project_id IS NULL AND r.deleted=0;
  DROP TEMPORARY TABLE tmp_missing_mes_feedback_codes;
  COMMIT;
END//
DELIMITER ;
CALL initialize_missing_mes_feedback_codes();
DROP PROCEDURE initialize_missing_mes_feedback_codes;
