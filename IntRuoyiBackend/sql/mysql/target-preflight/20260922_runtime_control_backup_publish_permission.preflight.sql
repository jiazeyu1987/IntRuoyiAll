-- release-target-preflight: migrationId=20260922_runtime_control_backup_publish_permission; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (
    SELECT COUNT(*) FROM `system_menu`
    WHERE `deleted` = b'0' AND `type` = 2
      AND `permission` = 'infra:runtime-control:query'
      AND `path` = 'runtime-control'
      AND `component` = 'infra/runtime-control/index'
  ) <> 1
  THEN 'TARGET_PREFLIGHT_BLOCKED:20260922_runtime_control_backup_publish_permission'
  WHEN (
    SELECT COUNT(*) FROM `system_menu`
    WHERE `deleted` = b'0' AND `type` = 3
      AND `permission` = 'infra:runtime-control:publish-backup'
  ) > 1
  THEN 'TARGET_PREFLIGHT_BLOCKED:20260922_runtime_control_backup_publish_permission'
  WHEN (
    SELECT COUNT(*) FROM `system_menu`
    WHERE `deleted` = b'0' AND `type` = 3
      AND `permission` = 'infra:runtime-control:publish-backup'
  ) = 1
   AND (
    SELECT MIN(`parent_id`) FROM `system_menu`
    WHERE `deleted` = b'0' AND `type` = 3
      AND `permission` = 'infra:runtime-control:publish-backup'
  ) <> (
    SELECT MIN(`id`) FROM `system_menu`
    WHERE `deleted` = b'0' AND `type` = 2
      AND `permission` = 'infra:runtime-control:query'
      AND `path` = 'runtime-control'
      AND `component` = 'infra/runtime-control/index'
  )
  THEN 'TARGET_PREFLIGHT_BLOCKED:20260922_runtime_control_backup_publish_permission'
  ELSE 'TARGET_PREFLIGHT_PASS:20260922_runtime_control_backup_publish_permission'
END;
