-- G59 DEVELOPMENT_TEST_ONLY exact one audit rule; not quality approval or a file repair.
-- Root sole operator; clone first/repeat then source exact protected scope after backup/writer fence.
-- Uses existing 2026-10-dcc-integration-01 unapproved development candidate; no quality registry write.
-- No pre-DROP, upsert/IGNORE, UPDATE/DELETE or --force. Original operation rows stay unchanged.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone='+08:00';
DELIMITER $$
CREATE PROCEDURE g59_development_repair_operation()
BEGIN
  DECLARE existing_rows INT DEFAULT 0;
  DECLARE matching_rows INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF @dcc_g59_development_only IS NULL OR @dcc_g59_development_only<>1
     OR @dcc_g59_config_authorized IS NULL OR @dcc_g59_config_authorized<>1
     OR @dcc_g59_writers_excluded IS NULL OR @dcc_g59_writers_excluded<>1
     OR @dcc_g59_expected_uuid IS NULL OR BINARY @@server_uuid<>BINARY @dcc_g59_expected_uuid
     OR @dcc_g59_expected_database IS NULL OR BINARY DATABASE()<>BINARY @dcc_g59_expected_database
     OR @dcc_g59_operator_reviewed_sql_sha IS NULL OR CHAR_LENGTH(@dcc_g59_operator_reviewed_sql_sha)<>64
     OR @@version<>'8.0.40' OR @@session.time_zone<>'+08:00'
     OR @@session.character_set_client<>'utf8mb4' OR @@autocommit<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G59 development identity/writer/authorization context required';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND ENGINE='InnoDB')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G59 formal audit operation table must exist';
  END IF;
  START TRANSACTION;
  SELECT COUNT(*) INTO existing_rows FROM gxp_audit_policy_operation
    WHERE tenant_id=1 AND operation_id='dcc.controlled-file.lifecycle-projection.repair';
  SELECT COUNT(*) INTO matching_rows FROM gxp_audit_policy_operation WHERE
      tenant_id=1 AND
      BINARY policy_version=BINARY '2026-10-dcc-integration-01' AND
      BINARY operation_id=BINARY 'dcc.controlled-file.lifecycle-projection.repair' AND
      BINARY source_type=BINARY 'SERVICE_METHOD' AND
      BINARY source_locator=BINARY 'cn.iocoder.yudao.module.dcc.service.file.DccLifecycleProjectionRepairService#repair' AND
      BINARY domain=BINARY 'DCC' AND
      BINARY subject_type=BINARY 'DCC_CONTROLLED_FILE' AND
      BINARY action_type=BINARY 'UPDATE' AND
      BINARY reason_policy=BINARY 'REQUIRED_CATEGORY_AND_TEXT' AND
      BINARY signature_policy=BINARY 'NOT_REQUIRED' AND
      BINARY state_policy=BINARY 'PRESENT_TO_PRESENT' AND
      BINARY retention_class=BINARY 'GXP_CONTROLLED_DOCUMENT' AND
      BINARY test_ids=BINARY '[G59-REPAIR-01, G59-REPAIR-02, G59-REPAIR-03]' AND
      BINARY owner=BINARY 'dcc-owner' AND
      BINARY applicability=BINARY 'GXP' AND
      active=1 AND
      deleted=0;
  IF existing_rows=0 THEN
    INSERT INTO gxp_audit_policy_operation (`tenant_id`,`policy_version`,`operation_id`,`source_type`,`source_locator`,`domain`,`subject_type`,`action_type`,`reason_policy`,`signature_policy`,`state_policy`,`retention_class`,`test_ids`,`owner`,`applicability`,`active`,`creator`,`updater`,`deleted`) VALUES (1,'2026-10-dcc-integration-01','dcc.controlled-file.lifecycle-projection.repair','SERVICE_METHOD','cn.iocoder.yudao.module.dcc.service.file.DccLifecycleProjectionRepairService#repair','DCC','DCC_CONTROLLED_FILE','UPDATE','REQUIRED_CATEGORY_AND_TEXT','NOT_REQUIRED','PRESENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','[G59-REPAIR-01, G59-REPAIR-02, G59-REPAIR-03]','dcc-owner','GXP',1,'g59-development-config','g59-development-config',0);
    IF ROW_COUNT()<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G59 audit rule insert incomplete'; END IF;
  ELSEIF existing_rows<>1 OR matching_rows<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='G59 existing audit rule conflicts; no overwrite';
  END IF;
  COMMIT;
END$$
DELIMITER ;
CALL g59_development_repair_operation();
DROP PROCEDURE g59_development_repair_operation;
