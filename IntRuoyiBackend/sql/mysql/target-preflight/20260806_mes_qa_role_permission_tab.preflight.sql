-- release-target-preflight: migrationId=20260806_mes_qa_role_permission_tab; allowedEnvironments=test,prod,backup
-- Read-only original prerequisite/identity checks; no menu resurrection, grants, or fixture creation.
SELECT CASE WHEN
 NOT COALESCE((EXISTS (
    SELECT 1
      FROM `system_tenant_package`
     WHERE `deleted` = b'0'
       AND NOT JSON_VALID(`menu_ids`)
  )),TRUE)
 AND NOT COALESCE((NOT EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900434
       AND `status` = 0
       AND `deleted` = b'0'
  )),TRUE)
 AND NOT COALESCE((EXISTS (
    SELECT 1
      FROM `system_menu`
     WHERE `id` = 900434
       AND `deleted` = b'0'
       AND NOT (
         `name` = 'QA'
         AND `path` = '/mes/pro/process-pool/qa-regulation'
         AND `component` = 'mes/pro/processpool/QaRegulationPage'
         AND `component_name` = 'MesProProcessPoolQaRegulation'
       )
  )),TRUE)
 AND NOT EXISTS(SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_qa_role_permission_tab_20260806')
 AND EXISTS(SELECT 1 FROM system_tenant WHERE id=1 AND deleted=b'0')
 AND EXISTS(SELECT 1 FROM system_users WHERE username='admin' AND tenant_id=1 AND status=0 AND deleted=b'0')
 AND NOT EXISTS(SELECT 1 FROM system_tenant t LEFT JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0' WHERE t.deleted=b'0' AND (t.id=1 OR JSON_CONTAINS(p.menu_ids,CAST('900434' AS JSON),'$')) AND (SELECT COUNT(*) FROM system_role_category c WHERE c.tenant_id=t.id AND c.code='menu' AND c.status=0 AND c.deleted=b'0')<>1)
 AND NOT EXISTS(SELECT 1 FROM system_role WHERE code='qa' GROUP BY tenant_id HAVING COUNT(*)>1)
 THEN 'TARGET_PREFLIGHT_PASS:20260806_mes_qa_role_permission_tab' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260806_mes_qa_role_permission_tab:MENU_ROLE_PREREQUISITE_OR_IDENTITY_CONFLICT' END;
