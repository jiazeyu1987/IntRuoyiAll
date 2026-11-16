-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_signature_regular_users_my_signature_only; type=menu; riskLevel=low
-- Move electronic signature under system management; preserve URLs, descendants and permissions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_signature_under_system;
DELIMITER $$
CREATE PROCEDURE move_signature_under_system()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=1 AND name='系统管理'
      AND parent_id=0 AND path='/system' AND type=1 AND status=0 AND deleted=b'0')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900218 AND name='电子签名'
      AND parent_id IN (0,1) AND path='/signature-governance'
      AND component='signature-governance/index' AND type=2 AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed system/signature menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0'
      AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu scope';
  END IF;

  UPDATE system_menu SET parent_id=1,updater='signature-system-menu',update_time=NOW()
  WHERE id=900218 AND parent_id<>1;

  DROP TEMPORARY TABLE IF EXISTS tmp_signature_system_descendants;
  CREATE TEMPORARY TABLE tmp_signature_system_descendants (id BIGINT PRIMARY KEY);
  INSERT INTO tmp_signature_system_descendants
  WITH RECURSIVE descendants AS (
    SELECT id FROM system_menu WHERE id=900218 AND deleted=b'0'
    UNION ALL
    SELECT child.id FROM system_menu child JOIN descendants parent ON child.parent_id=parent.id
      WHERE child.deleted=b'0'
  ) SELECT DISTINCT id FROM descendants;
  -- Only existing signature page/button holders gain only the system navigation ancestor.
  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT DISTINCT source.role_id,1,'signature-system-menu',NOW(),'signature-system-menu',NOW(),b'0',source.tenant_id
  FROM system_role_menu source JOIN tmp_signature_system_descendants child ON child.id=source.menu_id
  WHERE source.deleted=b'0' AND NOT EXISTS (SELECT 1 FROM system_role_menu target
    WHERE target.deleted=b'0' AND target.role_id=source.role_id AND target.tenant_id=source.tenant_id AND target.menu_id=1);
  UPDATE system_tenant_package package
  SET menu_ids=JSON_ARRAY_APPEND(CAST(package.menu_ids AS JSON),'$',1),
    updater='signature-system-menu',update_time=NOW()
  WHERE package.deleted=b'0' AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST('1' AS JSON),'$')
    AND EXISTS (SELECT 1 FROM tmp_signature_system_descendants child
      WHERE JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST(child.id AS JSON),'$'));
  COMMIT;
  DROP TEMPORARY TABLE tmp_signature_system_descendants;
END$$
DELIMITER ;
CALL move_signature_under_system();
DROP PROCEDURE move_signature_under_system;
