-- release-target-preflight: migrationId=20261023_mes_route_snapshot_legacy_summary_enforce; allowedEnvironments=test,backup,prod
-- The bound Java hook performs strict row/identity/reference/audit validation.
-- This support query verifies the already-created transactional structural prerequisites.
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME IN ('mes_pro_route_version','controlled_content_version_ref','controlled_content_transition_audit') AND ENGINE='InnoDB')=3
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_route_version'
    AND COLUMN_NAME IN ('id','tenant_id','route_id','version_no','active','lifecycle_status','route_snapshot_json',
      'route_snapshot_sha256','route_snapshot_format_version','remark','create_time','update_time','deleted',
      'approval_process_instance_id','source_route_version_id'))=15
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='controlled_content_version_ref'
    AND COLUMN_NAME IN ('id','tenant_id','content_type','content_key','native_master_id','native_version_id','canonical_status','domain_status',
      'active_unique_flag','open_candidate_unique_flag','last_transition_time'))=11
  AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='controlled_content_transition_audit'
    AND COLUMN_NAME IN ('tenant_id','version_ref_id','action','event_key','reason'))=5
THEN 'TARGET_PREFLIGHT_PASS:20261023_mes_route_snapshot_legacy_summary_enforce'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_mes_route_snapshot_legacy_summary_enforce' END AS result;
