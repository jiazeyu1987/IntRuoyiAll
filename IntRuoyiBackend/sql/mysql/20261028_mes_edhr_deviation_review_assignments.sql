-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260924_mes_edhr_deviation_management,20261027_mes_edhr_deviation_work_task_notify,20260718_mes_edhr_work_task_ownership,20260727_mes_edhr_work_task_responsibility_scope; type=schema; riskLevel=medium
-- Existing records deliberately receive no inferred people. An authorized handler must
-- revise the record and explicitly select the established five/six responsibility owners.
DROP PROCEDURE IF EXISTS ensure_mes_edhr_deviation_review_assignments;
DELIMITER $$
CREATE PROCEDURE ensure_mes_edhr_deviation_review_assignments()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'mes_pro_edhr_deviation_handling'
                   AND column_name = 'reviewer_assignments_json') THEN
    ALTER TABLE `mes_pro_edhr_deviation_handling`
      ADD COLUMN `reviewer_assignments_json` json DEFAULT NULL COMMENT '明确选择的签署节点责任人，纳入内容版本摘要';
  END IF;
END$$
DELIMITER ;
CALL ensure_mes_edhr_deviation_review_assignments();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_deviation_review_assignments;

INSERT INTO `system_notify_template`
(`name`, `code`, `type`, `nickname`, `content`, `params`, `status`, `remark`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'eDHR偏差签署与关闭任务通知', 'MES_EDHR_DEVIATION_REVIEW_ASSIGNED', 2, 'eDHR任务中心',
       '请处理{reason}。入口：{actionUrl}', '["reason","actionUrl","workTaskId"]', 0,
       '接收人由偏差处理记录明确选择；任务绑定处理正文版本和摘要',
       'edhr-deviation-review', NOW(), 'edhr-deviation-review', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_notify_template`
                  WHERE `code` = 'MES_EDHR_DEVIATION_REVIEW_ASSIGNED' AND `deleted` = b'0');
