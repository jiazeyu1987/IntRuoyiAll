-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_lifecycle; type=config; riskLevel=medium
-- Prepared only. The release operator supplies explicitly approved session variables in this same connection.
-- @dcc_activation_cron, @dcc_activation_retry_count, @dcc_activation_retry_interval, @dcc_activation_monitor_timeout
-- No inferred business cadence and no fixed job ID. Existing active/paused runtime choices are never overwritten.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS register_dcc_controlled_file_activation_job;
DELIMITER //
CREATE PROCEDURE register_dcc_controlled_file_activation_job()
BEGIN
 DECLARE job_count INT DEFAULT 0;
 IF @dcc_activation_cron IS NULL OR TRIM(@dcc_activation_cron)='' OR CHAR_LENGTH(@dcc_activation_cron)>32
   OR @dcc_activation_retry_count IS NULL OR @dcc_activation_retry_count<0
   OR @dcc_activation_retry_interval IS NULL OR @dcc_activation_retry_interval<0
   OR @dcc_activation_monitor_timeout IS NULL OR @dcc_activation_monitor_timeout<0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Approved activation cron/retry/monitor settings must be supplied explicitly';
 END IF;
 SELECT COUNT(*) INTO job_count FROM infra_job WHERE handler_name='dccControlledFileActivationJob' AND deleted=b'0';
 IF job_count>1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Activation handler name is not unique';
 END IF;
 IF job_count=1 AND EXISTS(SELECT 1 FROM infra_job WHERE handler_name='dccControlledFileActivationJob' AND deleted=b'0'
   AND (cron_expression<>@dcc_activation_cron OR retry_count<>@dcc_activation_retry_count
     OR retry_interval<>@dcc_activation_retry_interval OR monitor_timeout<>@dcc_activation_monitor_timeout
     OR status NOT IN (1,2) OR (handler_param IS NOT NULL AND TRIM(handler_param)<>''))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing activation registration differs from approved configuration; review via official job controls';
 END IF;
 IF job_count=0 THEN
  INSERT INTO infra_job(name,status,handler_name,handler_param,cron_expression,retry_count,retry_interval,monitor_timeout,creator,updater)
  VALUES('DCC受控文件按日期生效',2,'dccControlledFileActivationJob',NULL,@dcc_activation_cron,@dcc_activation_retry_count,
   @dcc_activation_retry_interval,@dcc_activation_monitor_timeout,'dcc-activation-registration','dcc-activation-registration');
 END IF;
END//
DELIMITER ;
CALL register_dcc_controlled_file_activation_job();
DROP PROCEDURE IF EXISTS register_dcc_controlled_file_activation_job;
-- Paused SQL registration alone is not Quartz registration or runtime activation.
-- After authorized runtime checks, use official job controls and verify qrtz_job_details/qrtz_triggers before enabling.
