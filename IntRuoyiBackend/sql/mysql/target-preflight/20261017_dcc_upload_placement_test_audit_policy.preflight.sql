-- release-target-preflight: migrationId=20261017_dcc_upload_placement_test_audit_policy; allowedEnvironments=test
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('gxp_audit_policy_operation'))=1)
 AND (NOT (NOT EXISTS (SELECT 1 FROM gxp_audit_policy_operation
                   WHERE tenant_id=1 AND operation_id='dcc.project-file-placement.bind'
                     AND active=b'1' AND deleted=b'0' AND applicability='GXP'
                     AND reason_policy='REQUIRED_CATEGORY_AND_TEXT')))
 AND (NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('ensure_dcc_upload_placement_test_policy')))
 THEN 'TARGET_PREFLIGHT_PASS:20261017_dcc_upload_placement_test_audit_policy' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261017_dcc_upload_placement_test_audit_policy:TARGET_PREREQUISITE_OR_CONFLICT' END;
