-- release-target-preflight: migrationId=20260802_role_requirement_matrix_m6_migration_preflight; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
NOT EXISTS (
    SELECT tenant_id,work_order_id,route_id,route_version_id FROM mes_pro_process_pool_active_order
    WHERE deleted=b'0' AND business_status='ACTIVE'
    GROUP BY tenant_id,work_order_id,route_id,route_version_id HAVING COUNT(*)>1
  )
  AND NOT EXISTS (
    SELECT 1 FROM mes_pro_process_pool_active_order WHERE deleted=b'0'
    AND (business_status IS NULL OR (business_status='ACTIVE' AND
      (route_id IS NULL OR route_version_id IS NULL OR erp_fixed_quantity_snapshot IS NULL)))
  )
  AND NOT EXISTS (
    SELECT 1 FROM mes_pqc_inspection_task WHERE deleted=b'0'
    AND (task_status IS NULL OR (task_status<>'CANCELLED' AND
      (active_order_id IS NULL OR work_order_id IS NULL OR route_id IS NULL OR route_version_id IS NULL
       OR route_process_id IS NULL OR process_id IS NULL OR regulation_version_id IS NULL OR planned_inspection_quantity IS NULL)))
  )
  AND NOT EXISTS (
    SELECT 1 FROM mes_pro_route_flow_process_batch_record WHERE deleted=b'0'
    AND form_slot_type='MAIN' AND record_category='BATCH_RECORD'
    AND (batch_record_report_id IS NULL OR batch_record_report_id='')
  )
  AND NOT EXISTS (
    SELECT tenant_id,route_process_id,use_type,report_sort FROM mes_pro_route_flow_process_batch_record
    WHERE deleted=b'0' AND form_slot_type='MAIN' AND record_category='BATCH_RECORD'
    GROUP BY tenant_id,route_process_id,use_type,report_sort HAVING COUNT(*)>1
  )
THEN 'TARGET_PREFLIGHT_PASS:20260802_role_requirement_matrix_m6_migration_preflight'
ELSE 'TARGET_PREFLIGHT_BLOCKED:review-required-before-data-migration' END;
