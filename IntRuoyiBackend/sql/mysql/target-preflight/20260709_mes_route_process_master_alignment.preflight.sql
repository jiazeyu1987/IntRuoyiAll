-- release-target-preflight: migrationId=20260709_mes_route_process_master_alignment; allowedEnvironments=test,backup,prod
-- Mirror the migration's required route/master/code checks without creating temporary tables.
WITH active_route_process AS (
  SELECT rp.id,rp.tenant_id,rp.process_id,p.id AS master_id,p.code,p.name
  FROM mes_pro_route_process rp
  JOIN mes_pro_route r ON r.id=rp.route_id AND r.tenant_id=rp.tenant_id AND r.deleted=b'0'
  LEFT JOIN mes_pro_process p ON p.id=rp.process_id AND p.tenant_id=rp.tenant_id AND p.deleted=b'0'
  WHERE rp.deleted=b'0'
), referenced_code AS (
  SELECT DISTINCT tenant_id,code FROM active_route_process WHERE code IS NOT NULL AND code<>''
)
SELECT CASE WHEN
  NOT EXISTS (SELECT 1 FROM active_route_process WHERE process_id IS NULL OR master_id IS NULL OR code IS NULL OR code='')
  AND NOT EXISTS (
    SELECT p.tenant_id,p.code FROM mes_pro_process p
    JOIN referenced_code rc ON rc.tenant_id=p.tenant_id AND rc.code=p.code
    WHERE p.deleted=b'0' GROUP BY p.tenant_id,p.code HAVING COUNT(DISTINCT p.name)>1
  ) THEN 'TARGET_PREFLIGHT_PASS:20260709_mes_route_process_master_alignment'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:route-process-master-or-code-ambiguous' END;
