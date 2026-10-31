-- release-target-preflight: migrationId=20261028_mes_edhr_record_change_approver_role; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (NOT (NOT EXISTS (SELECT 1 FROM system_tenant
                 WHERE id=1 AND name='芋道源码' AND status=0 AND deleted=b'0')))
  AND (NOT ((SELECT package_id FROM system_tenant WHERE id=1) IS NULL OR (SELECT package_id FROM system_tenant WHERE id=1)<>0))
  AND (NOT (NOT EXISTS (SELECT 1 FROM system_role_category
                 WHERE id=5 AND tenant_id=1 AND name='批记录' AND status=0 AND deleted=b'0')))
  AND (NOT (NOT EXISTS (SELECT 1 FROM system_menu WHERE id=1200 AND parent_id=0
                  AND path='/approval-center' AND permission='' AND type=1 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=1207 AND parent_id=1200
                  AND permission='' AND type=2 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=1221 AND parent_id=1207
                  AND permission='bpm:task:query' AND type=3 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900231
                  AND permission='mes:pro-edhr-work-task:query' AND type=3 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900236
                  AND permission='mes:pro-edhr-change:query' AND type=3 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900240
                  AND permission='mes:pro-edhr-change:approve' AND type=3 AND status=0 AND deleted=b'0')))
  AND (NOT ((SELECT COUNT(*) FROM system_role WHERE tenant_id=1 AND code='edhr_record_change_approver')>1))
  AND (NOT (EXISTS (SELECT 1 FROM system_role WHERE tenant_id=1 AND code='edhr_record_change_approver'
              AND (deleted<>b'0' OR status<>0 OR type<>2 OR name<>'eDHR记录变更审批人'
                   OR category_id IS NULL OR category_id<>5 OR data_scope<>5
                   OR data_scope_dept_ids IS NULL OR data_scope_dept_ids<>''))))
  AND (NOT ((SELECT MAX(id) FROM system_role WHERE tenant_id=1 AND code='edhr_record_change_approver' AND deleted=b'0') IS NOT NULL AND EXISTS (
      SELECT 1 FROM system_role_menu WHERE role_id=(SELECT MAX(id) FROM system_role WHERE tenant_id=1 AND code='edhr_record_change_approver' AND deleted=b'0') AND deleted=b'0'
       AND (tenant_id<>1 OR menu_id NOT IN (1200,1207,1221,900231,900236,900240)))))
THEN 'TARGET_PREFLIGHT_PASS:20261028_mes_edhr_record_change_approver_role' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261028_mes_edhr_record_change_approver_role:BASELINE_CONFLICT' END;
