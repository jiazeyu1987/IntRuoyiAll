-- release-target-preflight: migrationId=20260830_dcc_registration_certificate_notification_role_scope_backfill; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('dcc_registration_certificate','infra_job','mdm_enterprise','mdm_role_company_scope','system_role')) = 5
  AND (SELECT COUNT(*) FROM `infra_job` WHERE `handler_name` = 'registrationCertificateReminderDailyJob' AND `deleted` = b'0') = 1
  AND NOT EXISTS (
    SELECT 1
      FROM `dcc_registration_certificate` AS `certificate`
      LEFT JOIN `mdm_enterprise` AS `enterprise`
        ON `enterprise`.`id` = `certificate`.`owner_company_id`
       AND `enterprise`.`tenant_id` = `certificate`.`tenant_id`
       AND `enterprise`.`deleted` = b'0'
       AND `enterprise`.`type` = 'OWNED_COMPANY'
       AND `enterprise`.`status` = 'ENABLE'
     WHERE `certificate`.`deleted` = b'0'
       AND `certificate`.`status` = 'ACTIVE'
       AND `certificate`.`owner_company_id` IS NOT NULL
       AND `certificate`.`owner_company_id` > 0
       AND `enterprise`.`id` IS NULL
  )
  AND (
    NOT EXISTS (
      SELECT 1
        FROM `dcc_registration_certificate` AS `certificate`
        JOIN `mdm_enterprise` AS `enterprise`
          ON `enterprise`.`id` = `certificate`.`owner_company_id`
         AND `enterprise`.`tenant_id` = `certificate`.`tenant_id`
         AND `enterprise`.`deleted` = b'0'
         AND `enterprise`.`type` = 'OWNED_COMPANY'
         AND `enterprise`.`status` = 'ENABLE'
       WHERE `certificate`.`deleted` = b'0'
         AND `certificate`.`status` = 'ACTIVE'
         AND `certificate`.`owner_company_id` IS NOT NULL
         AND `certificate`.`owner_company_id` > 0
    )
    OR (
      NOT EXISTS (
        SELECT 1
          FROM `infra_job` AS `job`
         WHERE `job`.`handler_name` = 'registrationCertificateReminderDailyJob'
           AND `job`.`deleted` = b'0'
           AND (
             `job`.`handler_param` IS NULL
             OR TRIM(`job`.`handler_param`) = ''
             OR JSON_VALID(`job`.`handler_param`) <> 1
             OR COALESCE(JSON_TYPE(JSON_EXTRACT(`job`.`handler_param`, '$.roleIds')), '') <> 'ARRAY'
           )
      )
      AND EXISTS (
        SELECT 1
          FROM `infra_job` AS `job`
          JOIN JSON_TABLE(
                 `job`.`handler_param`,
                 '$.roleIds[*]' COLUMNS (
                   `role_id` bigint PATH '$'
                 )
               ) AS `role_param`
          JOIN `system_role` AS `role`
            ON `role`.`id` = `role_param`.`role_id`
           AND `role`.`deleted` = b'0'
           AND `role`.`status` = 0
         WHERE `job`.`handler_name` = 'registrationCertificateReminderDailyJob'
           AND `job`.`deleted` = b'0'
           AND JSON_VALID(`job`.`handler_param`) = 1
           AND `role_param`.`role_id` > 0
      )
      AND NOT EXISTS (
        SELECT 1
          FROM `infra_job` AS `job`
          JOIN JSON_TABLE(
                 `job`.`handler_param`,
                 '$.roleIds[*]' COLUMNS (
                   `role_id` bigint PATH '$'
                 )
               ) AS `role_param`
          LEFT JOIN `system_role` AS `role`
            ON `role`.`id` = `role_param`.`role_id`
           AND `role`.`deleted` = b'0'
           AND `role`.`status` = 0
         WHERE `job`.`handler_name` = 'registrationCertificateReminderDailyJob'
           AND `job`.`deleted` = b'0'
           AND JSON_VALID(`job`.`handler_param`) = 1
           AND (`role_param`.`role_id` IS NULL OR `role_param`.`role_id` <= 0 OR `role`.`id` IS NULL)
      )
    )
  )
THEN 'TARGET_PREFLIGHT_PASS:20260830_dcc_registration_certificate_notification_role_scope_backfill' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260830_dcc_registration_certificate_notification_role_scope_backfill' END;
