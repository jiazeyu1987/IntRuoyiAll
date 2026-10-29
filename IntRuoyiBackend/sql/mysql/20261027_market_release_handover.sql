-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261003_mes_release_task_notification; type=schema; riskLevel=medium
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS mes_pro_edhr_market_release_handover (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 batch_execution_id BIGINT NOT NULL,
 active_order_id BIGINT NOT NULL,
 release_transaction_id BIGINT NOT NULL,
 work_task_id BIGINT NOT NULL,
 original_user_id BIGINT NOT NULL,
 original_user_name VARCHAR(128) NOT NULL,
 recipient_user_id BIGINT NOT NULL,
 recipient_user_name VARCHAR(128) NOT NULL,
 original_candidate_snapshot TEXT NOT NULL,
 command_key VARCHAR(100) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 handed_at DATETIME NOT NULL,
 recalled_at DATETIME NULL,
 recall_reason VARCHAR(1000) NULL,
 recall_command_key VARCHAR(100) NULL,
 creator VARCHAR(64) DEFAULT '',updater VARCHAR(64) DEFAULT '',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted BIT(1) NOT NULL DEFAULT b'0',
 UNIQUE KEY uk_market_handover_command (tenant_id,command_key),
 UNIQUE KEY uk_market_recall_command (tenant_id,recall_command_key),
 KEY idx_market_handover_batch (tenant_id,batch_execution_id,id),
 KEY idx_market_handover_active (tenant_id,active_order_id),
 KEY idx_market_handover_recipient (tenant_id,recipient_user_id,batch_execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上市放行单人交接与回收记录';
DROP PROCEDURE IF EXISTS install_market_release_handover;
DELIMITER $$
CREATE PROCEDURE install_market_release_handover()
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='mes_pro_edhr_release_task_notify_delivery' AND column_name='event_revision') THEN
   ALTER TABLE mes_pro_edhr_release_task_notify_delivery ADD COLUMN event_revision BIGINT NOT NULL DEFAULT 0;
 END IF;
 IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
    AND table_name='mes_pro_edhr_release_task_notify_delivery' AND index_name='uk_mes_release_notify_recipient') THEN
   ALTER TABLE mes_pro_edhr_release_task_notify_delivery DROP INDEX uk_mes_release_notify_recipient;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
    AND table_name='mes_pro_edhr_release_task_notify_delivery' AND index_name='uk_mes_release_notify_revision') THEN
   ALTER TABLE mes_pro_edhr_release_task_notify_delivery ADD UNIQUE KEY uk_mes_release_notify_revision
      (tenant_id,work_task_id,event_type,user_id,event_revision);
 END IF;
 INSERT INTO system_notify_template
 (name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
 SELECT '上市放行交接通知','MES_MARKET_RELEASE_HANDED_OVER',2,'MES系统',
   '{originalName}已将工单{workOrderCode}、批次{batchCode}的上市放行交接给您。原因：{reason}。任务{workTaskId}，入口：{actionUrl}',
   '["originalName","workOrderCode","batchCode","reason","workTaskId","actionUrl"]',0,'上市放行单人交接','1',NOW(),'1',NOW(),b'0'
 WHERE NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_MARKET_RELEASE_HANDED_OVER');
 INSERT INTO system_notify_template
 (name,code,type,nickname,content,params,status,remark,creator,create_time,updater,update_time,deleted)
 SELECT '上市放行回收通知','MES_MARKET_RELEASE_RECALLED',2,'MES系统',
   '{originalName}已回收工单{workOrderCode}、批次{batchCode}的上市放行授权，您不能再查看或办理该批记录。原因：{reason}。原任务{workTaskId}。入口：{actionUrl}',
   '["originalName","workOrderCode","batchCode","reason","workTaskId","actionUrl"]',0,'上市放行授权回收','1',NOW(),'1',NOW(),b'0'
 WHERE NOT EXISTS (SELECT 1 FROM system_notify_template WHERE code='MES_MARKET_RELEASE_RECALLED');
 IF (SELECT COUNT(*) FROM system_notify_template WHERE code IN ('MES_MARKET_RELEASE_HANDED_OVER','MES_MARKET_RELEASE_RECALLED')
   AND deleted=b'0' AND status=0 AND JSON_LENGTH(params)=6
   AND JSON_CONTAINS(params,JSON_ARRAY('originalName','workOrderCode','batchCode','reason','workTaskId','actionUrl')))<>2 THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Market release handover notification templates missing or invalid';
 END IF;
END$$
DELIMITER ;
CALL install_market_release_handover();
DROP PROCEDURE install_market_release_handover;
