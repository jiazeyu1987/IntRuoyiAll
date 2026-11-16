-- release-target-preflight: migrationId=20260717_mes_workstation_current_process_rebind; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
NOT EXISTS (
    SELECT oldp.tenant_id,oldp.code FROM mes_md_workstation w
    JOIN mes_pro_process oldp ON oldp.id=w.process_id AND oldp.tenant_id=w.tenant_id AND oldp.deleted=b'1'
    JOIN mes_pro_process newp ON newp.tenant_id=oldp.tenant_id AND newp.code=oldp.code AND newp.deleted=b'0'
    WHERE w.deleted=b'0' AND oldp.code IS NOT NULL AND oldp.code<>''
    GROUP BY oldp.tenant_id,oldp.code HAVING COUNT(DISTINCT newp.id)>1
  )
  AND NOT EXISTS (
    SELECT 1 FROM mes_md_workstation w LEFT JOIN mes_pro_process p ON p.id=w.process_id AND p.tenant_id=w.tenant_id
    WHERE w.deleted=b'0' AND w.process_id IS NOT NULL AND p.id IS NULL
  )
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME IN ('mes_md_workstation_process_rebind_20260717','mes_route_process_workstation_rebind_20260717')
  )
THEN 'TARGET_PREFLIGHT_PASS:20260717_mes_workstation_current_process_rebind'
ELSE 'TARGET_PREFLIGHT_BLOCKED:review-required-before-data-migration' END;
