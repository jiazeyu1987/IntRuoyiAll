-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261008_profile_business_config_menus,20260829_registration_certificate_management_menu_hierarchy; type=menu; riskLevel=medium
-- Move the existing configuration page under registration certificate management.
-- Preserve the page URL, route name and permission; grant only missing ancestors
-- to roles/packages that already hold this configuration page. Apply before startup.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS move_registration_certificate_config_20261125;
DELIMITER $$
CREATE PROCEDURE move_registration_certificate_config_20261125()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990253 AND deleted=0 AND status=0
      AND type=1 AND parent_id=990200 AND path='registration-certificate-management')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990200 AND deleted=0 AND status=0
      AND type=1 AND parent_id=0 AND path='/mdm') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing registration certificate management parent hierarchy';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_menu config WHERE config.id=991503
      AND config.deleted=0 AND config.status=0 AND config.type=2 AND config.visible=1
      AND config.component='dcc/registration-certificate/config/index'
      AND config.component_name='DccRegistrationCertificateConfig'
      AND config.permission='dcc:registration-certificate:config:query'
      AND ((config.parent_id=990253 AND config.path='/dcc/registration-certificate/config')
        OR (config.path='registration-certificate/config' AND EXISTS (
          SELECT 1 FROM system_menu root WHERE root.id=config.parent_id AND root.deleted=0
            AND root.status=0 AND root.type=1 AND root.parent_id=0 AND root.path='/dcc')))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed registration certificate configuration menu';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=0 AND id<>991503
      AND (path='/dcc/registration-certificate/config' OR component_name='DccRegistrationCertificateConfig')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registration certificate configuration route conflict';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=0
      AND (menu_ids IS NULL OR NOT JSON_VALID(menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu JSON';
  END IF;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=0
      AND JSON_TYPE(CAST(menu_ids AS JSON))<>'ARRAY') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tenant package menu JSON must be an array';
  END IF;

  UPDATE system_menu SET parent_id=990253, path='/dcc/registration-certificate/config', sort=30,
    updater='regcert-config-parent',update_time=NOW() WHERE id=991503;

  INSERT INTO system_role_menu (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT DISTINCT source.role_id,parent.id,'regcert-config-parent',NOW(),'regcert-config-parent',NOW(),0,source.tenant_id
    FROM system_role_menu source JOIN system_menu parent ON parent.id IN (990200,990253)
    WHERE source.deleted=0 AND source.menu_id=991503
      AND NOT EXISTS (SELECT 1 FROM system_role_menu target WHERE target.deleted=0
        AND target.role_id=source.role_id AND target.tenant_id=source.tenant_id AND target.menu_id=parent.id);
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',990200),
    updater='regcert-config-parent',update_time=NOW()
    WHERE deleted=0 AND JSON_CONTAINS(CAST(menu_ids AS JSON),'991503','$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'990200','$');
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',990253),
    updater='regcert-config-parent',update_time=NOW()
    WHERE deleted=0 AND JSON_CONTAINS(CAST(menu_ids AS JSON),'991503','$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'990253','$');
  COMMIT;
END$$
DELIMITER ;
CALL move_registration_certificate_config_20261125();
DROP PROCEDURE move_registration_certificate_config_20261125;
