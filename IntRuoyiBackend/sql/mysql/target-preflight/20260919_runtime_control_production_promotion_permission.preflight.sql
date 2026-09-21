-- release-target-preflight: migrationId=20260919_runtime_control_production_promotion_permission; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (
    SELECT COUNT(*) FROM `system_menu`
    WHERE `deleted` = b'0' AND `type` = 2
      AND `permission` = 'infra:runtime-control:query'
      AND `path` = 'runtime-control'
      AND `component` = 'infra/runtime-control/index'
  ) = 1
  AND (
    (SELECT COUNT(*) FROM `system_menu`
     WHERE `deleted` = b'0' AND `type` = 3
       AND `permission` = 'infra:runtime-control:promote-prod') = 0
    OR (
      (SELECT COUNT(*) FROM `system_menu`
       WHERE `deleted` = b'0' AND `type` = 3
         AND `permission` = 'infra:runtime-control:promote-prod') = 1
      AND
      (SELECT MIN(`parent_id`) FROM `system_menu`
       WHERE `deleted` = b'0' AND `type` = 3
         AND `permission` = 'infra:runtime-control:promote-prod') =
      (SELECT MIN(`id`) FROM `system_menu`
       WHERE `deleted` = b'0' AND `type` = 2
         AND `permission` = 'infra:runtime-control:query'
         AND `path` = 'runtime-control'
         AND `component` = 'infra/runtime-control/index')
    )
  )
  THEN 'TARGET_PREFLIGHT_PASS:20260919_runtime_control_production_promotion_permission'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:20260919_runtime_control_production_promotion_permission'
END;
