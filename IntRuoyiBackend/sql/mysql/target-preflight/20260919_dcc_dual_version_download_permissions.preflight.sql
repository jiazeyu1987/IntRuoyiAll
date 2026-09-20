-- release-target-preflight: migrationId=20260919_dcc_dual_version_download_permissions; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6807 AND `deleted` = b'0')
   AND (SELECT COUNT(*) FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:download-read-only' AND `deleted` = b'0') = 1
   AND (SELECT COUNT(*) FROM `system_menu` WHERE `permission` = 'dcc:controlled-file:download-editable' AND `deleted` = b'0') = 1
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6831 AND `permission` = 'dcc:controlled-file:download-read-only' AND `type` = 3 AND `parent_id` = 6807 AND `status` = 0 AND `deleted` = b'0')
   AND EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6832 AND `permission` = 'dcc:controlled-file:download-editable' AND `type` = 3 AND `parent_id` = 6807 AND `status` = 0 AND `deleted` = b'0')
  THEN 'TARGET_PREFLIGHT_PASS:20260919_dcc_dual_version_download_permissions'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260919_dcc_dual_version_download_permissions'
END;
