-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261026_edhr_lifecycle_directories; type=menu; riskLevel=low
-- Move the existing MES equipment directory as a whole. Its absolute URL and all descendant contracts remain unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_edhr_device_management_directory;
DELIMITER $$
CREATE PROCEDURE move_edhr_device_management_directory()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND parent_id=0 AND type=1
      AND path='/mes/edhr-batch-processing' AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=5300 AND name='设备管理' AND type=1
      AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='dv')
        OR (parent_id=900220 AND path='/mes/dv'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed MES equipment/eDHR directory';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND path='/mes/dv' AND id<>5300)
    OR (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND type=1 AND parent_id=900220
      AND id IN (991515,991516,991517))<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Equipment route conflict or missing lifecycle directories';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;

  UPDATE system_menu SET parent_id=900220,path='/mes/dv',sort=4,
    updater='edhr-device-menu',update_time=NOW() WHERE id=5300;
  UPDATE system_menu SET sort=CASE id WHEN 991515 THEN 5 WHEN 991516 THEN 6 WHEN 991517 THEN 7 END,
    updater='edhr-device-menu',update_time=NOW() WHERE id IN (991515,991516,991517);

  DROP TEMPORARY TABLE IF EXISTS tmp_edhr_device_descendants;
  CREATE TEMPORARY TABLE tmp_edhr_device_descendants (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_edhr_device_descendants
  WITH RECURSIVE descendants AS (
    SELECT id FROM system_menu WHERE id=5300 AND deleted=b'0'
    UNION ALL
    SELECT child.id FROM system_menu child JOIN descendants parent ON child.parent_id=parent.id
      WHERE child.deleted=b'0'
  ) SELECT DISTINCT id FROM descendants;
  -- Only existing equipment page/button holders gain the necessary eDHR navigation ancestor.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,900220,'edhr-device-menu',NOW(),'edhr-device-menu',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN tmp_edhr_device_descendants child ON child.id=source.menu_id
  WHERE source.deleted=b'0' AND NOT EXISTS (SELECT 1 FROM system_role_menu target
    WHERE target.deleted=b'0' AND target.role_id=source.role_id AND target.tenant_id=source.tenant_id AND target.menu_id=900220);
  UPDATE system_tenant_package package
  SET menu_ids=JSON_ARRAY_APPEND(CAST(package.menu_ids AS JSON),'$',900220),
    updater='edhr-device-menu',update_time=NOW()
  WHERE package.deleted=b'0' AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('900220' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_edhr_device_descendants child
      WHERE JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST(child.id AS JSON),'$'));
  COMMIT;
  DROP TEMPORARY TABLE tmp_edhr_device_descendants;
END$$
DELIMITER ;
CALL move_edhr_device_management_directory();
DROP PROCEDURE move_edhr_device_management_directory;
