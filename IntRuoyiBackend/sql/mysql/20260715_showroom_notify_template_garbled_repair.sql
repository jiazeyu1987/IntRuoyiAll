-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_edhr_notify_template_garbled_repair; type=data; riskLevel=low
-- Goal:
--   Repair Showroom approval station-message templates that were previously
--   written with question-mark mojibake, and rebuild already-generated bad
--   message content only when the original target name is provable from JSON
--   params or Showroom business tables.
-- Scope:
--   Only SHOWROOM_APPROVAL_PENDING, SHOWROOM_APPROVAL_PUBLISHED and
--   SHOWROOM_APPROVAL_REJECTED rows.
--   No fallback text is generated: rows without valid source data remain
--   unchanged and visible to the final audit query.

DROP PROCEDURE IF EXISTS intruoyi_repair_showroom_notify_template_garbled_text;
DELIMITER $$
CREATE PROCEDURE intruoyi_repair_showroom_notify_template_garbled_text()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'system_notify_template'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing system_notify_template for Showroom garbled repair';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'system_notify_message'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing system_notify_message for Showroom garbled repair';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'showroom_change_request'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing showroom_change_request for Showroom garbled repair';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'showroom_product_revision'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing showroom_product_revision for Showroom garbled repair';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'showroom_company'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing showroom_company for Showroom garbled repair';
    END IF;

    IF (
        SELECT COUNT(*)
          FROM `system_notify_template`
         WHERE `code` IN ('SHOWROOM_APPROVAL_PENDING', 'SHOWROOM_APPROVAL_PUBLISHED', 'SHOWROOM_APPROVAL_REJECTED')
           AND `deleted` = b'0'
    ) <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing Showroom notify template for garbled repair';
    END IF;

    UPDATE `system_notify_template`
       SET `name` = CASE `code`
             WHEN 'SHOWROOM_APPROVAL_PENDING' THEN '展厅审批待办通知'
             WHEN 'SHOWROOM_APPROVAL_PUBLISHED' THEN '展厅发布完成通知'
             WHEN 'SHOWROOM_APPROVAL_REJECTED' THEN '展厅审批驳回通知'
             ELSE `name`
           END,
           `nickname` = '展厅系统',
           `content` = CASE `code`
             WHEN 'SHOWROOM_APPROVAL_PENDING' THEN
               '展厅{targetTypeText}【{targetName}】待{approvalStage}，点击查看对应内容。'
             WHEN 'SHOWROOM_APPROVAL_PUBLISHED' THEN
               '展厅{targetTypeText}【{targetName}】已审批通过并发布，点击查看对应内容。'
             WHEN 'SHOWROOM_APPROVAL_REJECTED' THEN
               '展厅{targetTypeText}【{targetName}】在{approvalStage}被驳回，原因：{rejectionReason}。点击后可继续修改原提交内容。'
             ELSE `content`
           END,
           `params` = CASE `code`
             WHEN 'SHOWROOM_APPROVAL_PENDING' THEN
               '["targetTypeText","targetName","approvalStage"]'
             WHEN 'SHOWROOM_APPROVAL_PUBLISHED' THEN
               '["targetTypeText","targetName"]'
             WHEN 'SHOWROOM_APPROVAL_REJECTED' THEN
               '["targetTypeText","targetName","approvalStage","rejectionReason"]'
             ELSE `params`
           END,
           `updater` = 'showroom-notify-garbled-repair',
           `update_time` = NOW()
     WHERE `code` IN ('SHOWROOM_APPROVAL_PENDING', 'SHOWROOM_APPROVAL_PUBLISHED', 'SHOWROOM_APPROVAL_REJECTED')
       AND `deleted` = b'0';

    UPDATE `system_notify_message` AS `m`
      LEFT JOIN `showroom_change_request` AS `cr`
        ON `cr`.`id` = CAST(JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.changeRequestId')) AS UNSIGNED)
       AND `cr`.`deleted` = b'0'
      LEFT JOIN `showroom_product_revision` AS `pr`
        ON JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
       AND `pr`.`id` = `cr`.`target_revision_id`
       AND `pr`.`deleted` = b'0'
      LEFT JOIN `showroom_company` AS `c`
        ON JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
       AND `c`.`id` = `cr`.`target_id`
       AND `c`.`deleted` = b'0'
       SET `m`.`template_nickname` = '展厅系统',
           `m`.`template_params` = JSON_SET(`m`.`template_params`, '$.targetName',
               CASE
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                      AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                   THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                   THEN `pr`.`name_cn`
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                   THEN `c`.`display_name`
                 ELSE NULL
               END),
           `m`.`template_content` = CASE `m`.`template_code`
             WHEN 'SHOWROOM_APPROVAL_PENDING' THEN CONCAT(
               '展厅',
               JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetTypeText')),
               '【',
               CASE
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                      AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                   THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                   THEN `pr`.`name_cn`
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                   THEN `c`.`display_name`
                 ELSE NULL
               END,
               '】待',
               JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.approvalStage')),
               '，点击查看对应内容。'
             )
             WHEN 'SHOWROOM_APPROVAL_PUBLISHED' THEN CONCAT(
               '展厅',
               JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetTypeText')),
               '【',
               CASE
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                      AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                   THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                   THEN `pr`.`name_cn`
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                   THEN `c`.`display_name`
                 ELSE NULL
               END,
               '】已审批通过并发布，点击查看对应内容。'
             )
             ELSE `m`.`template_content`
           END,
           `m`.`updater` = 'showroom-notify-garbled-repair',
           `m`.`update_time` = NOW()
     WHERE `m`.`template_code` IN ('SHOWROOM_APPROVAL_PENDING', 'SHOWROOM_APPROVAL_PUBLISHED')
       AND `m`.`deleted` = b'0'
       AND (LOCATE('??', `m`.`template_nickname`) > 0 OR LOCATE('??', `m`.`template_content`) > 0)
       AND JSON_VALID(`m`.`template_params`) = 1
       AND JSON_EXTRACT(`m`.`template_params`, '$.targetTypeText') IS NOT NULL
       AND JSON_EXTRACT(`m`.`template_params`, '$.targetName') IS NOT NULL
       AND JSON_EXTRACT(`m`.`template_params`, '$.targetType') IS NOT NULL
       AND JSON_EXTRACT(`m`.`template_params`, '$.changeRequestId') IS NOT NULL
       AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetTypeText'))) = 0
       AND (
             `m`.`template_code` = 'SHOWROOM_APPROVAL_PUBLISHED'
             OR (
                 JSON_EXTRACT(`m`.`template_params`, '$.approvalStage') IS NOT NULL
                 AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.approvalStage'))) = 0
             )
           )
       AND (
             CASE
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                    AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                 THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                 THEN `pr`.`name_cn`
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                 THEN `c`.`display_name`
               ELSE NULL
             END
           ) IS NOT NULL
       AND LOCATE('??',
             CASE
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                    AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                 THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                 THEN `pr`.`name_cn`
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                 THEN `c`.`display_name`
               ELSE NULL
             END
           ) = 0;

    UPDATE `system_notify_message` AS `m`
      LEFT JOIN `showroom_change_request` AS `cr`
        ON `cr`.`id` = CAST(JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.changeRequestId')) AS UNSIGNED)
       AND `cr`.`deleted` = b'0'
      LEFT JOIN `showroom_product_revision` AS `pr`
        ON JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
       AND `pr`.`id` = `cr`.`target_revision_id`
       AND `pr`.`deleted` = b'0'
      LEFT JOIN `showroom_company` AS `c`
        ON JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
       AND `c`.`id` = `cr`.`target_id`
       AND `c`.`deleted` = b'0'
       SET `m`.`template_nickname` = '展厅系统',
           `m`.`template_params` = JSON_SET(`m`.`template_params`, '$.targetName',
               CASE
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                      AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                   THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                   THEN `pr`.`name_cn`
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                   THEN `c`.`display_name`
                 ELSE NULL
               END),
           `m`.`template_content` = CONCAT(
               '展厅',
               JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetTypeText')),
               '【',
               CASE
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                      AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                   THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                   THEN `pr`.`name_cn`
                 WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                   THEN `c`.`display_name`
                 ELSE NULL
               END,
               '】在',
               JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.approvalStage')),
               '被驳回，原因：',
               JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.rejectionReason')),
               '。点击后可继续修改原提交内容。'
           ),
           `m`.`updater` = 'showroom-notify-garbled-repair',
           `m`.`update_time` = NOW()
     WHERE `m`.`template_code` = 'SHOWROOM_APPROVAL_REJECTED'
       AND `m`.`deleted` = b'0'
       AND (LOCATE('??', `m`.`template_nickname`) > 0 OR LOCATE('??', `m`.`template_content`) > 0)
       AND JSON_VALID(`m`.`template_params`) = 1
       AND JSON_EXTRACT(`m`.`template_params`, '$.targetTypeText') IS NOT NULL
       AND JSON_EXTRACT(`m`.`template_params`, '$.targetName') IS NOT NULL
       AND JSON_EXTRACT(`m`.`template_params`, '$.approvalStage') IS NOT NULL
       AND JSON_EXTRACT(`m`.`template_params`, '$.rejectionReason') IS NOT NULL
       AND JSON_EXTRACT(`m`.`template_params`, '$.targetType') IS NOT NULL
       AND JSON_EXTRACT(`m`.`template_params`, '$.changeRequestId') IS NOT NULL
       AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetTypeText'))) = 0
       AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.approvalStage'))) = 0
       AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.rejectionReason'))) = 0
       AND (
             CASE
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                    AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                 THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                 THEN `pr`.`name_cn`
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                 THEN `c`.`display_name`
               ELSE NULL
             END
           ) IS NOT NULL
       AND LOCATE('??',
             CASE
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                    AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                 THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                 THEN `pr`.`name_cn`
               WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                 THEN `c`.`display_name`
               ELSE NULL
             END
           ) = 0;

    IF EXISTS (
        SELECT 1
          FROM `system_notify_template`
         WHERE `code` IN ('SHOWROOM_APPROVAL_PENDING', 'SHOWROOM_APPROVAL_PUBLISHED', 'SHOWROOM_APPROVAL_REJECTED')
           AND `deleted` = b'0'
           AND (LOCATE('??', `name`) > 0
                OR LOCATE('??', `nickname`) > 0
                OR LOCATE('??', `content`) > 0)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'unrepairable Showroom notify template garbled text remains';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `system_notify_message` AS `m`
          LEFT JOIN `showroom_change_request` AS `cr`
            ON `cr`.`id` = CAST(JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.changeRequestId')) AS UNSIGNED)
           AND `cr`.`deleted` = b'0'
          LEFT JOIN `showroom_product_revision` AS `pr`
            ON JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
           AND `pr`.`id` = `cr`.`target_revision_id`
           AND `pr`.`deleted` = b'0'
          LEFT JOIN `showroom_company` AS `c`
            ON JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
           AND `c`.`id` = `cr`.`target_id`
           AND `c`.`deleted` = b'0'
         WHERE `m`.`template_code` IN ('SHOWROOM_APPROVAL_PENDING', 'SHOWROOM_APPROVAL_PUBLISHED', 'SHOWROOM_APPROVAL_REJECTED')
           AND `m`.`deleted` = b'0'
           AND (LOCATE('??', `m`.`template_nickname`) > 0 OR LOCATE('??', `m`.`template_content`) > 0)
           AND JSON_VALID(`m`.`template_params`) = 1
           AND JSON_EXTRACT(`m`.`template_params`, '$.targetTypeText') IS NOT NULL
           AND JSON_EXTRACT(`m`.`template_params`, '$.targetName') IS NOT NULL
           AND JSON_EXTRACT(`m`.`template_params`, '$.targetType') IS NOT NULL
           AND JSON_EXTRACT(`m`.`template_params`, '$.changeRequestId') IS NOT NULL
           AND (
                 CASE
                   WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                        AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                     THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
                   WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                     THEN `pr`.`name_cn`
                   WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                     THEN `c`.`display_name`
                   ELSE NULL
                 END
               ) IS NOT NULL
           AND LOCATE('??',
                 CASE
                   WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName')) IS NOT NULL
                        AND LOCATE('??', JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))) = 0
                     THEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetName'))
                   WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'PRODUCT'
                     THEN `pr`.`name_cn`
                   WHEN JSON_UNQUOTE(JSON_EXTRACT(`m`.`template_params`, '$.targetType')) = 'COMPANY'
                     THEN `c`.`display_name`
                   ELSE NULL
                 END
               ) = 0
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'unrepairable repairable Showroom notify message garbled text remains';
    END IF;
END$$
DELIMITER ;

CALL intruoyi_repair_showroom_notify_template_garbled_text();

DROP PROCEDURE IF EXISTS intruoyi_repair_showroom_notify_template_garbled_text;
