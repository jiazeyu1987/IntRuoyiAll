-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261021_mes_process_gx_sequence,20261021_mes_route_create_sequence; type=data; riskLevel=medium
-- 为历史漏初始化的租户补齐缺失规则。已有规则、规则组成及流水记录均不修改。
DROP TEMPORARY TABLE IF EXISTS tmp_mes_missing_production_code_rules;
CREATE TEMPORARY TABLE tmp_mes_missing_production_code_rules (
  tenant_id BIGINT NOT NULL,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  prefix VARCHAR(8) NOT NULL,
  PRIMARY KEY (tenant_id, code)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
START TRANSACTION;
INSERT INTO tmp_mes_missing_production_code_rules (tenant_id, code, name, prefix)
SELECT t.id, definitions.code, definitions.name, definitions.prefix
FROM system_tenant t
CROSS JOIN (
  SELECT _utf8mb4'PRO_PROCESS_GX_CODE' COLLATE utf8mb4_unicode_ci AS code, '工序统一流水编码' AS name, 'GX-' AS prefix
  UNION ALL SELECT 'PRO_ROUTE_CODE', '工艺流程编码', 'GYLC-'
) definitions
WHERE t.deleted=0 AND NOT EXISTS (
  SELECT 1 FROM mes_md_auto_code_rule r
  WHERE r.tenant_id=t.id AND r.code=definitions.code AND r.deleted=0
);
INSERT INTO mes_md_auto_code_rule (tenant_id, code, name, max_length, padded, status)
SELECT tenant_id, code, name, 64, 0, 0 FROM tmp_mes_missing_production_code_rules;
INSERT INTO mes_md_auto_code_part (tenant_id, rule_id, sort, type, length, fix_character)
SELECT r.tenant_id, r.id, 1, 3, CHAR_LENGTH(m.prefix), m.prefix
FROM mes_md_auto_code_rule r JOIN tmp_mes_missing_production_code_rules m
  ON r.tenant_id=m.tenant_id AND r.code=m.code
WHERE r.deleted=0;
INSERT INTO mes_md_auto_code_part
  (tenant_id, rule_id, sort, type, length, serial_start_no, serial_step, cycle_flag)
SELECT r.tenant_id, r.id, 2, 4, 4, 1, 1, 0
FROM mes_md_auto_code_rule r JOIN tmp_mes_missing_production_code_rules m
  ON r.tenant_id=m.tenant_id AND r.code=m.code
WHERE r.deleted=0;
COMMIT;
DROP TEMPORARY TABLE tmp_mes_missing_production_code_rules;
