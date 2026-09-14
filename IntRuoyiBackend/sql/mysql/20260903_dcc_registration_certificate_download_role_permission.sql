-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260829_dcc_registration_certificate_upload_approver_role,20260816_dcc_registration_certificate_menu; type=permission; riskLevel=low
-- Allow the registration-certificate approver role to invoke the protected download endpoint.

START TRANSACTION;

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `role`.`id`, `menu`.`id`, 'dcc-reg-cert-download-role', NOW(),
       'dcc-reg-cert-download-role', NOW(), b'0', `role`.`tenant_id`
FROM `system_role` AS `role`
JOIN `system_menu` AS `menu`
  ON `menu`.`permission` = 'dcc:registration-certificate:access-request:create'
 AND `menu`.`status` = 0
 AND `menu`.`deleted` = b'0'
WHERE `role`.`code` = 'dcc_registration_certificate_approver'
  AND `role`.`status` = 0
  AND `role`.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` AS `existing`
    WHERE `existing`.`tenant_id` = `role`.`tenant_id`
      AND `existing`.`role_id` = `role`.`id`
      AND `existing`.`menu_id` = `menu`.`id`
      AND `existing`.`deleted` = b'0'
  );

COMMIT;
