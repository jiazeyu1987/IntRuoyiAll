-- release-target-preflight: migrationId=20261113_mes_release_attachment_upload_roles; allowedEnvironments=test,backup,prod
-- Read-only baseline predicates from this migration; mutation-time guards remain authoritative.
WITH tmp_release_upload_packages AS (SELECT DISTINCT p.id FROM system_role r
JOIN system_tenant t ON t.id=r.tenant_id AND t.deleted=0 AND t.status=0
JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=0 AND p.status=0
WHERE r.deleted=0 AND r.status=0 AND r.code IN
('BATCH_ATTACHMENT_INCOMING_INSPECTION_REPORT_UPLOAD_1','BATCH_ATTACHMENT_STERILIZATION_REPORT_UPLOAD_1',
'BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_REPORT_UPLOAD_1','BATCH_ATTACHMENT_FINISHED_PRODUCT_INSPECTION_RECORD_UPLOAD_1'))
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND status=0
      AND ((id=900033 AND type=2 AND permission='mes:pro-edhr-batch-execution:query')
        OR (id=900034 AND parent_id=900033 AND type=3 AND permission='mes:pro-edhr-batch-execution:query')
        OR (id=900043 AND parent_id=900033 AND type=3 AND permission='mes:pro-edhr-batch-execution:upload')
        OR (id=900230 AND parent_id=991516 AND type=3 AND path='' AND component='' AND permission='mes:pro-edhr-work-task:query')
        OR (id=900231 AND parent_id=991516 AND type=3 AND permission='mes:pro-edhr-work-task:query'))) <> 5) THEN 'TARGET_PREFLIGHT_BLOCKED:20261113_mes_release_attachment_upload_roles:CONDITION_1'
 WHEN (EXISTS (SELECT 1 FROM system_tenant_package p JOIN tmp_release_upload_packages chosen ON chosen.id=p.id
      WHERE p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261113_mes_release_attachment_upload_roles:CONDITION_2'
 ELSE 'TARGET_PREFLIGHT_PASS:20261113_mes_release_attachment_upload_roles' END;
