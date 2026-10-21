-- release-target-preflight: migrationId=20261022_edhr_trace_menu_forward; allowedEnvironments=test,prod,backup
SELECT CASE WHEN
 (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND ((id=900365 AND permission='mes:pro-batch-record-template:query') OR (id=900033 AND permission='mes:pro-edhr-batch-execution:query') OR (id=900025 AND permission='mes:pro-batch-record-execution:track') OR (id=900432 AND permission='mes:pro-edhr-form-fill-log:query') OR (id=900235 AND permission='mes:pro-edhr-change:query') OR (id=900260 AND permission='mes:pro-edhr-release:query')))=6
 AND EXISTS(SELECT 1 FROM system_menu WHERE id=900220 AND deleted=b'0')
 AND (SELECT COUNT(*) FROM system_menu WHERE id IN(900365,900033,900025,900432) AND parent_id=900220 AND type=2 AND visible=b'1' AND deleted=b'0')=4
 AND NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='release_20261022_trace_menu')
 THEN 'TARGET_PREFLIGHT_PASS:20261022_edhr_trace_menu_forward' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_edhr_trace_menu_forward:MENU_IDENTITY_OR_INTERRUPTED_OPERATION' END;
