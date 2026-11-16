-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261028_mes_edhr_record_change_tasks; type=schema; riskLevel=medium
-- No runtime writes are performed by development. Apply before deploying the decision API.
DROP PROCEDURE IF EXISTS upgrade_edhr_record_change_decisions;
DELIMITER $$
CREATE PROCEDURE upgrade_edhr_record_change_decisions()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_edhr_record_change_event' AND COLUMN_NAME='previous_change_event_id') THEN
    ALTER TABLE mes_pro_edhr_record_change_event ADD COLUMN previous_change_event_id BIGINT NULL COMMENT '上一退回申请轮次';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_edhr_record_change_event' AND COLUMN_NAME='decision_reason') THEN
    ALTER TABLE mes_pro_edhr_record_change_event ADD COLUMN decision_reason VARCHAR(500) NULL COMMENT '审批驳回或退回原因';
  END IF;
END$$
DELIMITER ;
CALL upgrade_edhr_record_change_decisions();
DROP PROCEDURE upgrade_edhr_record_change_decisions;

INSERT INTO system_notify_template
(name, code, type, nickname, content, params, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'eDHR记录变更已驳回终止', 'MES_EDHR_RECORD_CHANGE_REJECTED', 2, 'eDHR任务中心',
       '记录变更{changeCode}已驳回终止：{reason}。入口：{actionUrl}',
       '["changeCode","reason","actionUrl"]', 0, '记录变更正式审批结果', 'edhr-record-change', NOW(), 'edhr-record-change', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_EDHR_RECORD_CHANGE_REJECTED' AND deleted=b'0');

INSERT INTO system_notify_template
(name, code, type, nickname, content, params, status, remark, creator, create_time, updater, update_time, deleted)
SELECT 'eDHR记录变更已退回补正', 'MES_EDHR_RECORD_CHANGE_RETURNED', 2, 'eDHR任务中心',
       '记录变更{changeCode}已退回补正：{reason}。入口：{actionUrl}',
       '["changeCode","reason","actionUrl"]', 0, '记录变更正式审批结果', 'edhr-record-change', NOW(), 'edhr-record-change', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_EDHR_RECORD_CHANGE_RETURNED' AND deleted=b'0');
