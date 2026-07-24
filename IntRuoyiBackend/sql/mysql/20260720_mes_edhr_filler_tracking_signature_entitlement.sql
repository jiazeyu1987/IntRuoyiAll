-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_system_entitlement_management,20260526_edhr_approval_archive_schema_contract; type=data; riskLevel=low
-- Extend MES_EDHR_FILLER_MINIMAL with read-only execution support panels required by the fill form.
-- Fail fast: updates the dynamic entitlement policy only, and never mutates static roles, users, or menus.

DROP PROCEDURE IF EXISTS intruoyi_update_mes_edhr_filler_tracking_signature_entitlement;

DELIMITER $$

CREATE PROCEDURE intruoyi_update_mes_edhr_filler_tracking_signature_entitlement()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'system_entitlement_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing system_entitlement_policy table for eDHR filler entitlement update';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM `system_entitlement_policy`
     WHERE `policy_code` = 'MES_EDHR_FILLER_MINIMAL'
       AND `status` = 0
       AND `deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing MES_EDHR_FILLER_MINIMAL policy for tracking/signature entitlement update';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM (
        SELECT 'mes:pro-batch-record-execution:track' AS permission_code
        UNION ALL
        SELECT 'mes:pro-batch-record-execution:signature-query'
      ) required_permission
      LEFT JOIN `system_menu` menu
        ON menu.`permission` = required_permission.`permission_code`
       AND menu.`status` = 0
       AND menu.`deleted` = b'0'
     WHERE menu.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR tracking/signature menu permission for filler entitlement update';
  END IF;

  UPDATE `system_entitlement_policy`
     SET `description` = 'Sources: EDHR_PROCESS_FORM_FILLER, EDHR_WORK_TASK_ASSIGNEE; includes fill form read-only tracking/signature support',
         `allowed_permission_codes_json` = JSON_ARRAY(
           'mes:pro-edhr-batch-execution:query',
           'mes:pro-edhr-batch-execution:update',
           'mes:pro-batch-record-execution:query',
           'mes:pro-batch-record-execution:update',
           'mes:pro-batch-record-execution:track',
           'mes:pro-batch-record-execution:signature-query',
           'mes:pro-edhr-work-task:query'
         ),
         `allowed_menu_refs_json` = JSON_ARRAY(
           JSON_OBJECT('permission', 'mes:pro-edhr-batch-execution:query'),
           JSON_OBJECT('permission', 'mes:pro-edhr-batch-execution:update'),
           JSON_OBJECT('permission', 'mes:pro-batch-record-execution:query'),
           JSON_OBJECT('permission', 'mes:pro-batch-record-execution:update'),
           JSON_OBJECT('permission', 'mes:pro-batch-record-execution:track'),
           JSON_OBJECT('permission', 'mes:pro-batch-record-execution:signature-query'),
           JSON_OBJECT('permission', 'mes:pro-edhr-work-task:query')
         ),
         `updater` = '20260720-filler-tracking-signature',
         `update_time` = NOW()
   WHERE `policy_code` = 'MES_EDHR_FILLER_MINIMAL'
     AND `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_filler_entitlement_affected_users`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_filler_entitlement_affected_users` AS
  SELECT DISTINCT claim.`tenant_id`, claim.`resolved_user_id`
    FROM `system_entitlement_claim` claim
   WHERE claim.`policy_code` = 'MES_EDHR_FILLER_MINIMAL'
     AND claim.`status` = 'ACTIVE'
     AND claim.`deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_filler_entitlement_grant_counts`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_filler_entitlement_grant_counts` AS
  SELECT claim.`tenant_id`,
         claim.`resolved_user_id`,
         claim.`policy_code`,
         COUNT(*) AS `active_claim_count`
    FROM `system_entitlement_claim` claim
    JOIN `tmp_mes_edhr_filler_entitlement_affected_users` affected_user
      ON affected_user.`tenant_id` = claim.`tenant_id`
     AND affected_user.`resolved_user_id` = claim.`resolved_user_id`
   WHERE claim.`policy_code` = 'MES_EDHR_FILLER_MINIMAL'
     AND claim.`status` = 'ACTIVE'
     AND claim.`deleted` = b'0'
   GROUP BY claim.`tenant_id`, claim.`resolved_user_id`, claim.`policy_code`;

  INSERT INTO `system_entitlement_grant`
  (`tenant_id`, `subject_type`, `subject_id`, `resolved_user_id`, `permission_code`, `menu_id`,
   `policy_code`, `active_claim_count`, `status`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT grant_count.`tenant_id`,
         'USER',
         grant_count.`resolved_user_id`,
         grant_count.`resolved_user_id`,
         allowed_permission.`permission_code`,
         menu.`id`,
         grant_count.`policy_code`,
         grant_count.`active_claim_count`,
         'ACTIVE',
         '20260720-filler-tracking-signature',
         NOW(),
         '20260720-filler-tracking-signature',
         NOW(),
         b'0'
    FROM `tmp_mes_edhr_filler_entitlement_grant_counts` grant_count
    JOIN `system_entitlement_policy` policy
      ON policy.`policy_code` = grant_count.`policy_code`
     AND policy.`status` = 0
     AND policy.`deleted` = b'0'
    JOIN JSON_TABLE(
           policy.`allowed_permission_codes_json`,
           '$[*]' COLUMNS (`permission_code` varchar(150) CHARACTER SET utf8mb4
             COLLATE utf8mb4_unicode_ci PATH '$')
         ) allowed_permission
    JOIN `system_menu` menu
      ON menu.`permission` = allowed_permission.`permission_code`
     AND menu.`status` = 0
     AND menu.`deleted` = b'0'
  WHERE TRUE
  ON DUPLICATE KEY UPDATE
         `active_claim_count` = VALUES(`active_claim_count`),
         `status` = 'ACTIVE',
         `updater` = '20260720-filler-tracking-signature',
         `update_time` = NOW(),
         `deleted` = b'0';

  INSERT INTO `system_entitlement_audit_event`
  (`tenant_id`, `event_type`, `source_type`, `source_key`, `policy_code`, `subject_type`, `subject_id`,
   `before_digest`, `after_digest`, `result_status`, `message`, `operator_user_id`, `operator_username`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT affected_user.`tenant_id`,
         'POLICY_GRANT_REFRESH',
         'EDHR_DYNAMIC_POLICY',
         'MES_EDHR_FILLER_MINIMAL',
         'MES_EDHR_FILLER_MINIMAL',
         'USER',
         affected_user.`resolved_user_id`,
         NULL,
         'MES_EDHR_FILLER_MINIMAL:tracking-signature',
         'PASS',
         'filler entitlement policy extended with tracking/signature grants',
         NULL,
         '20260720-filler-tracking-signature',
         '20260720-filler-tracking-signature',
         NOW(),
         '20260720-filler-tracking-signature',
         NOW(),
         b'0'
    FROM `tmp_mes_edhr_filler_entitlement_affected_users` affected_user;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_filler_entitlement_grant_counts`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_filler_entitlement_affected_users`;
END$$

DELIMITER ;

CALL intruoyi_update_mes_edhr_filler_tracking_signature_entitlement();

DROP PROCEDURE IF EXISTS intruoyi_update_mes_edhr_filler_tracking_signature_entitlement;
