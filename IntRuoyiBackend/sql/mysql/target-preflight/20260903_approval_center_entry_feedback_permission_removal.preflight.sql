-- release-target-preflight: migrationId=20260903_approval_center_entry_feedback_permission_removal; allowedEnvironments=test
-- Read-only data prerequisites; evaluated immediately before this migration.
SELECT CASE WHEN
  ((SELECT COUNT(*) FROM system_role WHERE code='approval_center_entry' AND tenant_id=1 AND status=0 AND deleted=0)=1)
  AND (EXISTS(SELECT 1 FROM system_menu WHERE id=900450 AND BINARY permission=BINARY 'mes:pro-feedback:frontline-pressure-pump:all-processes' AND type=3 AND parent_id=5550 AND status=0 AND deleted=0))
THEN 'TARGET_PREFLIGHT_PASS:20260903_approval_center_entry_feedback_permission_removal' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260903_approval_center_entry_feedback_permission_removal' END;
