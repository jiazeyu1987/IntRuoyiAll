-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- eDHR controlled final archive WORM guard.
-- Safe to run repeatedly after the base archive tables exist.

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_archive_worm_guard_prerequisites;
DELIMITER $$
CREATE PROCEDURE ensure_mes_batch_record_archive_worm_guard_prerequisites()
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution_archive'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution_archive; apply eDHR archive schema first';
  END IF;

  IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'mes_pro_batch_record_execution_archive_event'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing mes_pro_batch_record_execution_archive_event; apply eDHR archive schema first';
  END IF;
END$$
DELIMITER ;

CALL ensure_mes_batch_record_archive_worm_guard_prerequisites();

DROP PROCEDURE IF EXISTS ensure_mes_batch_record_archive_worm_guard_prerequisites;

DROP TRIGGER IF EXISTS `trg_execution_archive_sealed_no_update`;
DROP TRIGGER IF EXISTS `trg_execution_archive_sealed_no_delete`;
DROP TRIGGER IF EXISTS `trg_execution_archive_event_no_update`;
DROP TRIGGER IF EXISTS `trg_execution_archive_event_no_delete`;

DELIMITER $$
CREATE TRIGGER `trg_execution_archive_sealed_no_update`
BEFORE UPDATE ON `mes_pro_batch_record_execution_archive`
FOR EACH ROW
BEGIN
  IF OLD.archive_status = 'SEALED' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR SEALED execution archives are immutable';
  END IF;
END$$
CREATE TRIGGER `trg_execution_archive_sealed_no_delete`
BEFORE DELETE ON `mes_pro_batch_record_execution_archive`
FOR EACH ROW
BEGIN
  IF OLD.archive_status = 'SEALED' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'eDHR SEALED execution archives are immutable';
  END IF;
END$$
CREATE TRIGGER `trg_execution_archive_event_no_update`
BEFORE UPDATE ON `mes_pro_batch_record_execution_archive_event`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'eDHR archive events are append-only';
END$$
CREATE TRIGGER `trg_execution_archive_event_no_delete`
BEFORE DELETE ON `mes_pro_batch_record_execution_archive_event`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'eDHR archive events are append-only';
END$$
DELIMITER ;
