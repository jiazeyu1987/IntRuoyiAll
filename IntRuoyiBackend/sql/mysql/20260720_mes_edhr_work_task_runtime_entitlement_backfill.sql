-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_system_entitlement_management,20260718_mes_edhr_work_task_ownership,20260719_mes_edhr_release_lifecycle_adapter,20260720_mes_edhr_filler_tracking_signature_entitlement,20260720_mes_edhr_approval_reviewer_runtime_entitlement; type=data; riskLevel=medium
-- Backfill dynamic runtime entitlements for active eDHR work tasks.
-- Fail fast: uses the entitlement ledger only, and never mutates static roles or role-menu bindings.

DROP PROCEDURE IF EXISTS intruoyi_backfill_mes_edhr_work_task_runtime_entitlement;

DELIMITER $$

CREATE PROCEDURE intruoyi_backfill_mes_edhr_work_task_runtime_entitlement()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'mes_pro_edhr_work_task'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_edhr_work_task table for runtime entitlement backfill';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'system_entitlement_claim'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing system_entitlement_claim table for runtime entitlement backfill';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM (
        SELECT 'MES_EDHR_FILLER_MINIMAL' COLLATE utf8mb4_unicode_ci AS policy_code
        UNION ALL
        SELECT 'MES_EDHR_APPROVAL_REVIEWER_MINIMAL' COLLATE utf8mb4_unicode_ci AS policy_code
        UNION ALL
        SELECT 'MES_EDHR_RELEASE_APPROVER_MINIMAL' COLLATE utf8mb4_unicode_ci AS policy_code
      ) required_policy
      LEFT JOIN `system_entitlement_policy` policy
        ON policy.`policy_code` = required_policy.`policy_code`
       AND policy.`status` = 0
       AND policy.`deleted` = b'0'
     WHERE policy.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR runtime entitlement policy';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `system_entitlement_policy` policy
      JOIN JSON_TABLE(
             policy.`allowed_permission_codes_json`,
             '$[*]' COLUMNS (`permission_code` varchar(150) CHARACTER SET utf8mb4
               COLLATE utf8mb4_unicode_ci PATH '$')
           ) allowed_permission
      LEFT JOIN `system_menu` menu
        ON menu.`permission` = allowed_permission.`permission_code`
       AND menu.`status` = 0
       AND menu.`deleted` = b'0'
     WHERE policy.`policy_code` IN ('MES_EDHR_FILLER_MINIMAL', 'MES_EDHR_APPROVAL_REVIEWER_MINIMAL',
                                     'MES_EDHR_RELEASE_APPROVER_MINIMAL')
       AND policy.`status` = 0
       AND policy.`deleted` = b'0'
       AND menu.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR runtime entitlement menu';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM `mes_pro_edhr_work_task` task
     WHERE task.`task_type` IN ('FILL', 'REWORK', 'REVIEW', 'APPROVE', 'RELEASE_APPROVE')
       AND task.`status` IN ('TODO', 'OVERDUE')
       AND task.`deleted` = b'0'
       AND (task.`candidate_user_snapshot` IS NULL OR TRIM(task.`candidate_user_snapshot`) = '')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Active eDHR runtime work task has empty candidate pool';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_runtime_entitlement_candidates`;
  CREATE TEMPORARY TABLE `tmp_edhr_runtime_entitlement_candidates` AS
  SELECT DISTINCT
         task.`tenant_id`,
         task.`id` AS `task_id`,
         CONCAT('WORK_TASK|', task.`id`) AS `source_key`,
         CAST(task.`id` AS char) AS `source_version`,
         CASE
           WHEN task.`task_type` = 'RELEASE_APPROVE' THEN 'MES_EDHR_RELEASE_APPROVER_MINIMAL' COLLATE utf8mb4_unicode_ci
           WHEN task.`task_type` IN ('REVIEW', 'APPROVE') THEN 'MES_EDHR_APPROVAL_REVIEWER_MINIMAL' COLLATE utf8mb4_unicode_ci
           ELSE 'MES_EDHR_FILLER_MINIMAL' COLLATE utf8mb4_unicode_ci
         END AS `policy_code`,
         candidate.`resolved_user_id`,
         CONCAT(
           'taskType=', task.`task_type`,
           ';status=', task.`status`,
           ';candidateSourceType=', IFNULL(task.`candidate_source_type`, ''),
           ';candidateSourceId=', IFNULL(CAST(task.`candidate_source_id` AS char), ''),
           ';candidateUserSnapshot=', task.`candidate_user_snapshot`,
           ';responsibilitySourceType=', IFNULL(task.`responsibility_source_type`, ''),
           ';responsibilitySourceKey=', IFNULL(task.`responsibility_source_key`, ''),
           ';ownershipLocked=', IF(task.`ownership_locked` = b'1', 'true', 'false')
         ) AS `source_digest`
    FROM `mes_pro_edhr_work_task` task
    JOIN JSON_TABLE(
           CONCAT('[', REPLACE(task.`candidate_user_snapshot`, ' ', ''), ']'),
           '$[*]' COLUMNS (`resolved_user_id` bigint PATH '$')
         ) candidate
   WHERE task.`task_type` IN ('FILL', 'REWORK', 'REVIEW', 'APPROVE', 'RELEASE_APPROVE')
     AND task.`status` IN ('TODO', 'OVERDUE')
     AND task.`deleted` = b'0';

  IF EXISTS (
    SELECT 1
      FROM `tmp_edhr_runtime_entitlement_candidates` candidate
      LEFT JOIN `system_users` users
        ON users.`tenant_id` = candidate.`tenant_id`
       AND users.`id` = candidate.`resolved_user_id`
       AND users.`status` = 0
       AND users.`deleted` = b'0'
     WHERE users.`id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing or disabled eDHR runtime work task user';
  END IF;

  INSERT INTO `system_entitlement_claim`
  (`tenant_id`, `source_type`, `source_key`, `source_version`, `source_digest`, `policy_code`,
   `subject_type`, `subject_id`, `resolved_user_id`, `status`, `effective_at`, `revoked_at`,
   `last_sync_status`, `last_sync_message`, `operator_user_id`, `operator_username`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT candidate.`tenant_id`,
         'EDHR_WORK_TASK_ASSIGNEE',
         candidate.`source_key`,
         candidate.`source_version`,
         candidate.`source_digest`,
         candidate.`policy_code`,
         'USER',
         candidate.`resolved_user_id`,
         candidate.`resolved_user_id`,
         'ACTIVE',
         NOW(),
         NULL,
         'PASS',
         'runtime work task entitlement backfilled',
         NULL,
         'runtime-entitlement-backfill',
         'runtime-entitlement-backfill',
         NOW(),
         'runtime-entitlement-backfill',
         NOW(),
         b'0'
    FROM `tmp_edhr_runtime_entitlement_candidates` candidate
  ON DUPLICATE KEY UPDATE
         `source_version` = VALUES(`source_version`),
         `source_digest` = VALUES(`source_digest`),
         `status` = 'ACTIVE',
         `effective_at` = IF(`status` = 'ACTIVE', `effective_at`, NOW()),
         `revoked_at` = NULL,
         `last_sync_status` = 'PASS',
         `last_sync_message` = 'runtime work task entitlement backfilled',
         `operator_user_id` = NULL,
         `operator_username` = 'runtime-entitlement-backfill',
         `updater` = 'runtime-entitlement-backfill',
         `update_time` = NOW(),
         `deleted` = b'0';

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_runtime_entitlement_affected_users`;
  CREATE TEMPORARY TABLE `tmp_edhr_runtime_entitlement_affected_users` AS
  SELECT DISTINCT `tenant_id`, `resolved_user_id`
    FROM `tmp_edhr_runtime_entitlement_candidates`;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_runtime_entitlement_grant_counts`;
  CREATE TEMPORARY TABLE `tmp_edhr_runtime_entitlement_grant_counts` AS
  SELECT claim.`tenant_id`,
         claim.`resolved_user_id`,
         claim.`policy_code`,
         COUNT(*) AS `active_claim_count`
    FROM `system_entitlement_claim` claim
    JOIN `tmp_edhr_runtime_entitlement_affected_users` affected_user
      ON affected_user.`tenant_id` = claim.`tenant_id`
     AND affected_user.`resolved_user_id` = claim.`resolved_user_id`
   WHERE claim.`status` = 'ACTIVE'
     AND claim.`deleted` = b'0'
     AND claim.`policy_code` IN ('MES_EDHR_FILLER_MINIMAL', 'MES_EDHR_APPROVAL_REVIEWER_MINIMAL',
                                 'MES_EDHR_RELEASE_APPROVER_MINIMAL')
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
         'runtime-entitlement-backfill',
         NOW(),
         'runtime-entitlement-backfill',
         NOW(),
         b'0'
    FROM `tmp_edhr_runtime_entitlement_grant_counts` grant_count
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
         `updater` = 'runtime-entitlement-backfill',
         `update_time` = NOW(),
         `deleted` = b'0';

  INSERT INTO `system_entitlement_audit_event`
  (`tenant_id`, `event_type`, `source_type`, `source_key`, `policy_code`, `subject_type`, `subject_id`,
   `before_digest`, `after_digest`, `result_status`, `message`, `operator_user_id`, `operator_username`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`)
  SELECT candidate.`tenant_id`,
         'BACKFILL',
         'EDHR_WORK_TASK_ASSIGNEE',
         candidate.`source_key`,
         candidate.`policy_code`,
         'USER',
         candidate.`resolved_user_id`,
         NULL,
         candidate.`source_digest`,
         'PASS',
         'runtime work task entitlement backfilled',
         NULL,
         'runtime-entitlement-backfill',
         'runtime-entitlement-backfill',
         NOW(),
         'runtime-entitlement-backfill',
         NOW(),
         b'0'
    FROM `tmp_edhr_runtime_entitlement_candidates` candidate;

  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_runtime_entitlement_grant_counts`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_runtime_entitlement_affected_users`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_edhr_runtime_entitlement_candidates`;
END$$

DELIMITER ;

CALL intruoyi_backfill_mes_edhr_work_task_runtime_entitlement();

DROP PROCEDURE IF EXISTS intruoyi_backfill_mes_edhr_work_task_runtime_entitlement;
