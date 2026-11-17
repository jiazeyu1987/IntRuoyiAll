-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261009_system_workbench_visibility_stable_keys,20260611_mes_edhr_work_task_flow; type=data; riskLevel=low
-- Preserve hidden deviation duties when their workbench source changes. Keep the
-- original visibility snapshot and immutable stable-key migration ledger intact.
-- Run with the application stopped for deployment; never rewrite an S4 generation.
DROP PROCEDURE IF EXISTS migrate_deviation_workbench_visibility_20261117;
DELIMITER $$
CREATE PROCEDURE migrate_deviation_workbench_visibility_20261117()
SQL SECURITY INVOKER
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF DATABASE() IS NULL OR
    (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME IN ('system_profile_workbench_task_visibility','system_workbench_visibility_context','mes_pro_edhr_work_task')
      AND ENGINE='InnoDB') <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing deviation workbench visibility prerequisites';
  END IF;
  START TRANSACTION;
  IF EXISTS (SELECT 1 FROM system_profile_workbench_task_visibility v
    JOIN mes_pro_edhr_work_task w ON w.tenant_id=v.tenant_id
      AND CAST(v.task_key AS BINARY)=CAST(CONCAT('EDHR_WORK_TASK:',w.id) AS BINARY)
    LEFT JOIN system_workbench_visibility_context c ON c.tenant_id=v.tenant_id AND c.user_id=v.user_id
    WHERE w.task_type IN ('DEVIATION_RECEIVE','DEVIATION_CORRECT','DEVIATION_VERIFY','DEVIATION_SIGN','DEVIATION_APPROVE','DEVIATION_CLOSE')
      AND c.user_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing personal workbench visibility context';
  END IF;
  INSERT INTO system_profile_workbench_task_visibility
    (user_id,task_key,task_type,source,business_id,detail,hidden_at,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT v.user_id,CONCAT('EDHR_DEVIATION_TASK:',w.id),'偏差','eDHR偏差任务',v.business_id,
         v.detail,v.hidden_at,v.creator,v.create_time,v.updater,v.update_time,v.deleted,v.tenant_id
  FROM system_profile_workbench_task_visibility v
  JOIN mes_pro_edhr_work_task w ON w.tenant_id=v.tenant_id
    AND CAST(v.business_id AS BINARY)=CAST(CAST(w.id AS CHAR) AS BINARY)
    AND CAST(v.task_key AS BINARY)=CAST(CONCAT('EDHR_WORK_TASK:',w.id) AS BINARY)
  WHERE w.task_type IN ('DEVIATION_RECEIVE','DEVIATION_CORRECT','DEVIATION_VERIFY','DEVIATION_SIGN','DEVIATION_APPROVE','DEVIATION_CLOSE')
    AND NOT EXISTS (SELECT 1 FROM system_profile_workbench_task_visibility target
      WHERE target.tenant_id=v.tenant_id AND target.user_id=v.user_id AND target.deleted=v.deleted
        AND CAST(target.task_key AS BINARY)=CAST(CONCAT('EDHR_DEVIATION_TASK:',w.id) AS BINARY));
  -- Invalidate existing personal cursors; task facts and recipients do not change.
  UPDATE system_workbench_visibility_context c
  SET c.visibility_version=c.visibility_version+1
  WHERE EXISTS (SELECT 1 FROM system_profile_workbench_task_visibility v
    JOIN mes_pro_edhr_work_task w ON w.tenant_id=v.tenant_id
      AND CAST(v.business_id AS BINARY)=CAST(CAST(w.id AS CHAR) AS BINARY)
      AND CAST(v.task_key AS BINARY)=CAST(CONCAT('EDHR_WORK_TASK:',w.id) AS BINARY)
    WHERE c.tenant_id=v.tenant_id AND c.user_id=v.user_id
      AND w.task_type IN ('DEVIATION_RECEIVE','DEVIATION_CORRECT','DEVIATION_VERIFY','DEVIATION_SIGN','DEVIATION_APPROVE','DEVIATION_CLOSE'));
  COMMIT;
END$$
DELIMITER ;
CALL migrate_deviation_workbench_visibility_20261117();
DROP PROCEDURE migrate_deviation_workbench_visibility_20261117;
