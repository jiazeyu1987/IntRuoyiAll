-- release-target-preflight: migrationId=20261022_retire_fixed_route_initializers; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
 NOT EXISTS(SELECT 1 FROM mes_pro_route WHERE id=922067 OR code='RT000006')
 AND NOT EXISTS(SELECT 1 FROM mes_pro_route_process WHERE route_id=922067)
 AND NOT EXISTS(SELECT 1 FROM mes_pro_route_flow_config WHERE route_id=922067)
 AND NOT EXISTS(SELECT 1 FROM mes_pro_route_flow_process_config WHERE route_id=922067)
 AND NOT EXISTS(SELECT 1 FROM mes_pro_route_flow_process_batch_record WHERE route_id=922067)
 AND NOT EXISTS(
  SELECT 1 FROM mes_pro_route r
  JOIN mes_pro_route_process rp ON rp.route_id=r.id AND rp.tenant_id=r.tenant_id AND rp.deleted=b'0'
  LEFT JOIN mes_pro_process p ON p.id=rp.process_id AND p.tenant_id=rp.tenant_id AND p.deleted=b'0'
  LEFT JOIN mes_md_workstation w ON w.id=rp.workstation_id AND w.tenant_id=rp.tenant_id AND w.deleted=b'0'
  WHERE r.tenant_id=1 AND r.deleted=b'0' AND r.code IN('ROUTE-XLSX-00001','ROUTE-XLSX-00002')
  AND (p.id IS NULL OR w.id IS NULL OR NOT(w.process_id <=> rp.process_id))
 )
 AND NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='release_20261022_retire_fixed_route_initializers')
 THEN 'TARGET_PREFLIGHT_PASS:20261022_retire_fixed_route_initializers'
 ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_retire_fixed_route_initializers:FIXED_BUSINESS_IMPORT_OR_INTERRUPTED_OPERATION_REQUIRES_REVIEW' END;
