-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261027_mes_edhr_deviation_work_task_notify,20261028_mes_edhr_deviation_review_assignments; type=data; riskLevel=low
-- Navigation stays in template parameters; machine URLs are never part of the message body.
DROP PROCEDURE IF EXISTS require_mes_edhr_deviation_notify_dependencies;
DELIMITER $$
CREATE PROCEDURE require_mes_edhr_deviation_notify_dependencies()
BEGIN
  IF (SELECT COUNT(*) FROM system_notify_template
      WHERE code IN ('MES_EDHR_DEVIATION_RECEIVE_ASSIGNED',
                     'MES_EDHR_DEVIATION_CORRECT_ASSIGNED',
                     'MES_EDHR_DEVIATION_REVIEW_ASSIGNED') AND deleted = b'0') <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '缺少 20261027/20261028 偏差通知模板，禁止执行可读历史通知迁移';
  END IF;
END$$
DELIMITER ;
CALL require_mes_edhr_deviation_notify_dependencies();
DROP PROCEDURE IF EXISTS require_mes_edhr_deviation_notify_dependencies;

UPDATE system_notify_template
SET content = '请{taskTitle}。生产订单：{workOrderCode}，批次：{batchCode}。办理说明：{reason}',
    params = '["workOrderCode","batchCode","processName","actionUrl","reason","workTaskId","taskTitle"]',
    updater = 'deviation-readable-notify', update_time = NOW()
WHERE code IN ('MES_EDHR_DEVIATION_RECEIVE_ASSIGNED','MES_EDHR_DEVIATION_CORRECT_ASSIGNED') AND deleted = b'0';

UPDATE system_notify_template
SET content = '请{taskTitle}。生产订单：{workOrderCode}，批次：{batchCode}。',
    params = '["taskTitle","reason","workOrderCode","batchCode","actionUrl","workTaskId"]',
    updater = 'deviation-readable-notify', update_time = NOW()
WHERE code = 'MES_EDHR_DEVIATION_REVIEW_ASSIGNED' AND deleted = b'0';

INSERT INTO system_notify_template
(name, code, type, nickname, content, params, status, remark, creator, create_time, updater, update_time, deleted)
SELECT '偏差任务逾期提醒', 'MES_EDHR_DEVIATION_WORK_TASK_OVERDUE', 2, 'eDHR任务中心',
       '偏差任务已逾期，请及时{taskTitle}。生产订单：{workOrderCode}，批次：{batchCode}，截止时间：{dueTime}。',
       '["taskTitle","reason","workOrderCode","batchCode","processName","dueTime","actionUrl","workTaskId"]',
       0, '偏差职责的专用逾期通知，不改变任务责任人或权限',
       'deviation-readable-notify', NOW(), 'deviation-readable-notify', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_notify_template
                  WHERE code = 'MES_EDHR_DEVIATION_WORK_TASK_OVERDUE' AND deleted = b'0');
