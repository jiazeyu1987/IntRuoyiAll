-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_mes_edhr_final_archive_work_task; type=data; riskLevel=low
-- Goal:
--   Repair eDHR station-message templates that were previously written with
--   question-mark mojibake, and rebuild already-generated bad message content
--   from the stored template_params JSON.
-- Scope:
--   Only MES_EDHR_ARCHIVE_TASK_ASSIGNED and MES_EDHR_WORK_TASK_OVERDUE rows.
--   No fallback text is generated: rows without valid template_params remain
--   visible to the final fail-fast check.

DROP PROCEDURE IF EXISTS intruoyi_repair_mes_edhr_notify_template_garbled_text;
DELIMITER $$
CREATE PROCEDURE intruoyi_repair_mes_edhr_notify_template_garbled_text()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'system_notify_template'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing system_notify_template for garbled repair';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'system_notify_message'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing system_notify_message for garbled repair';
    END IF;

    IF (
        SELECT COUNT(*)
          FROM `system_notify_template`
         WHERE `code` IN ('MES_EDHR_ARCHIVE_TASK_ASSIGNED', 'MES_EDHR_WORK_TASK_OVERDUE')
           AND `deleted` = b'0'
    ) <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'missing eDHR notify template for garbled repair';
    END IF;

    UPDATE `system_notify_template`
       SET `name` = CASE `code`
             WHEN 'MES_EDHR_ARCHIVE_TASK_ASSIGNED' THEN 'eDHR最终归档任务通知'
             WHEN 'MES_EDHR_WORK_TASK_OVERDUE' THEN 'eDHR工作任务逾期提醒'
             ELSE `name`
           END,
           `nickname` = 'eDHR任务中心',
           `content` = CASE `code`
             WHEN 'MES_EDHR_ARCHIVE_TASK_ASSIGNED' THEN
               '工作到你了：请完成工单{workOrderCode}批次{batchCode}的最终归档。入口：{actionUrl}'
             WHEN 'MES_EDHR_WORK_TASK_OVERDUE' THEN
               '工作任务已逾期：工单{workOrderCode}批次{batchCode}的{processName}批记录应于{dueTime}前处理。入口：{actionUrl}'
             ELSE `content`
           END,
           `params` = CASE `code`
             WHEN 'MES_EDHR_ARCHIVE_TASK_ASSIGNED' THEN
               '["workOrderCode","batchCode","processName","actionUrl","workTaskId"]'
             WHEN 'MES_EDHR_WORK_TASK_OVERDUE' THEN
               '["workOrderCode","batchCode","processName","dueTime","actionUrl","workTaskId"]'
             ELSE `params`
           END,
           `updater` = 'edhr-notify-garbled-repair',
           `update_time` = NOW()
     WHERE `code` IN ('MES_EDHR_ARCHIVE_TASK_ASSIGNED', 'MES_EDHR_WORK_TASK_OVERDUE')
       AND `deleted` = b'0';

    UPDATE `system_notify_message`
       SET `template_nickname` = 'eDHR任务中心',
           `template_content` = CONCAT(
               '工作到你了：请完成工单',
               JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.workOrderCode')),
               '批次',
               JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.batchCode')),
               '的最终归档。入口：',
               JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.actionUrl'))
           ),
           `updater` = 'edhr-notify-garbled-repair',
           `update_time` = NOW()
     WHERE `template_code` = 'MES_EDHR_ARCHIVE_TASK_ASSIGNED'
       AND `deleted` = b'0'
       AND (LOCATE('??', `template_nickname`) > 0 OR LOCATE('??', `template_content`) > 0)
       AND JSON_VALID(`template_params`) = 1
       AND JSON_EXTRACT(`template_params`, '$.workOrderCode') IS NOT NULL
       AND JSON_EXTRACT(`template_params`, '$.batchCode') IS NOT NULL
       AND JSON_EXTRACT(`template_params`, '$.actionUrl') IS NOT NULL;

    UPDATE `system_notify_message`
       SET `template_nickname` = 'eDHR任务中心',
           `template_content` = CONCAT(
               '工作任务已逾期：工单',
               JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.workOrderCode')),
               '批次',
               JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.batchCode')),
               '的',
               JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.processName')),
               '批记录应于',
               JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.dueTime')),
               '前处理。入口：',
               JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.actionUrl'))
           ),
           `updater` = 'edhr-notify-garbled-repair',
           `update_time` = NOW()
     WHERE `template_code` = 'MES_EDHR_WORK_TASK_OVERDUE'
       AND `deleted` = b'0'
       AND (LOCATE('??', `template_nickname`) > 0 OR LOCATE('??', `template_content`) > 0)
       AND JSON_VALID(`template_params`) = 1
       AND JSON_EXTRACT(`template_params`, '$.workOrderCode') IS NOT NULL
       AND JSON_EXTRACT(`template_params`, '$.batchCode') IS NOT NULL
       AND JSON_EXTRACT(`template_params`, '$.processName') IS NOT NULL
       AND JSON_EXTRACT(`template_params`, '$.dueTime') IS NOT NULL
       AND JSON_EXTRACT(`template_params`, '$.actionUrl') IS NOT NULL;

    IF EXISTS (
        SELECT 1
          FROM `system_notify_template`
         WHERE `code` IN ('MES_EDHR_ARCHIVE_TASK_ASSIGNED', 'MES_EDHR_WORK_TASK_OVERDUE')
           AND `deleted` = b'0'
           AND (LOCATE('??', `name`) > 0
                OR LOCATE('??', `nickname`) > 0
                OR LOCATE('??', `content`) > 0)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'unrepairable eDHR notify template garbled text remains';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM `system_notify_message`
         WHERE `template_code` IN ('MES_EDHR_ARCHIVE_TASK_ASSIGNED', 'MES_EDHR_WORK_TASK_OVERDUE')
           AND `deleted` = b'0'
           AND (LOCATE('??', `template_nickname`) > 0
                OR LOCATE('??', `template_content`) > 0)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'unrepairable eDHR notify message garbled text remains';
    END IF;
END$$
DELIMITER ;

CALL intruoyi_repair_mes_edhr_notify_template_garbled_text();

DROP PROCEDURE IF EXISTS intruoyi_repair_mes_edhr_notify_template_garbled_text;
