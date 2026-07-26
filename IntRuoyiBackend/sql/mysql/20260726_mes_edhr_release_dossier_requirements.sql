-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260725_mes_edhr_recordbook_global_setting; type=config-seed; riskLevel=medium
-- eDHR release dossier requirement switches. Missing or invalid runtime config must fail fast in backend.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_dossier_requirements_20260726;
DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_release_dossier_requirements_20260726()
BEGIN
  IF (
    SELECT COUNT(*)
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'infra_config'
  ) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing infra_config table';
  END IF;

  IF (
    SELECT COUNT(*)
      FROM infra_config
     WHERE config_key = 'mes.edhr.release.dossier.requirements'
       AND deleted = b'0'
  ) > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate eDHR release dossier requirement config';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM infra_config
     WHERE config_key = 'mes.edhr.release.dossier.requirements'
       AND deleted = b'0'
       AND JSON_VALID(value) = 0
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid eDHR release dossier requirement config JSON';
  END IF;

  IF EXISTS (
    SELECT 1
      FROM infra_config
     WHERE config_key = 'mes.edhr.release.dossier.requirements'
       AND deleted = b'0'
       AND (
         JSON_TYPE(JSON_EXTRACT(value, '$.incomingInspectionReportRequired')) <> 'BOOLEAN'
         OR JSON_TYPE(JSON_EXTRACT(value, '$.sterilizationReportRequired')) <> 'BOOLEAN'
         OR JSON_TYPE(JSON_EXTRACT(value, '$.finishedProductInspectionReportRequired')) <> 'BOOLEAN'
         OR JSON_TYPE(JSON_EXTRACT(value, '$.finishedProductInspectionRecordRequired')) <> 'BOOLEAN'
       )
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid eDHR release dossier requirement config fields';
  END IF;

  INSERT INTO infra_config (
    `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT
    'mes', 1, 'eDHR 放行资料限制开关',
    'mes.edhr.release.dossier.requirements',
    '{"incomingInspectionReportRequired":false,"sterilizationReportRequired":false,"finishedProductInspectionReportRequired":false,"finishedProductInspectionRecordRequired":false}',
    b'1',
    '金手指专用放行资料限制开关；开启后对应特殊节点必须完成且存在已保存 ADD 附件。',
    '20260726-edhr-release-dossier-requirements', NOW(),
    '20260726-edhr-release-dossier-requirements', NOW(), b'0'
  FROM DUAL
  WHERE NOT EXISTS (
    SELECT 1
      FROM infra_config
     WHERE config_key = 'mes.edhr.release.dossier.requirements'
       AND deleted = b'0'
  );
END//
DELIMITER ;

CALL ensure_mes_edhr_release_dossier_requirements_20260726();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_release_dossier_requirements_20260726;

COMMIT;
