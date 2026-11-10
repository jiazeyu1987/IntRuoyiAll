-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260816_mdm_enterprise_company_scope,20260816_dcc_registration_certificate_menu; type=menu; riskLevel=medium
-- Purpose: Add the existing authorized-company page and CRUD permissions below the verified MDM parent.
-- Scope: Only packages already containing the MDM parent/page, their tenant_admin roles, and tenant 1 super_admin.
-- No hierarchy retirement, MES menu changes, or user/role company-scope business assignments.
-- Execute only after separate review/authorization. Existing dependencies are reconciled SKIPPED_ALREADY_APPLIED.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS supplement_mdm_company_scope_menu_20261031;
DELIMITER $$
CREATE PROCEDURE supplement_mdm_company_scope_menu_20261031()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF (SELECT COUNT(*) FROM information_schema.tables
       WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
         AND table_name IN ('mdm_enterprise', 'mdm_user_company_scope', 'mdm_role_company_scope',
                            'system_menu', 'system_role', 'system_role_menu', 'system_tenant', 'system_tenant_package')) <> 8 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Required MDM company-scope/schema/menu tables are missing';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM system_menu
                  WHERE id = 990200 AND deleted = b'0' AND status = 0 AND visible = b'1'
                    AND type = 1 AND parent_id = 0 AND path = '/mdm' AND permission = ''
                    AND component IS NULL AND component_name IS NULL)
     OR EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND path = '/mdm' AND id <> 990200) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MDM parent 990200 /mdm contract/path conflict';
  END IF;

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0'
              AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN TRUE
                       ELSE JSON_TYPE(menu_ids) <> 'ARRAY' END) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant package menu_ids must be valid non-null JSON arrays';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_menus;
  CREATE TEMPORARY TABLE tmp_word14_scope_menus (
    id BIGINT NOT NULL PRIMARY KEY, name VARCHAR(64) NOT NULL, permission VARCHAR(128) NOT NULL,
    type INT NOT NULL, sort INT NOT NULL, parent_id BIGINT NOT NULL, path VARCHAR(128) NOT NULL,
    component VARCHAR(255) NOT NULL, component_name VARCHAR(128) NOT NULL
  ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
  INSERT INTO tmp_word14_scope_menus VALUES
    (990255, '授权公司', 'mdm:company-scope:query', 2, 16, 990200, 'company-scope', 'mdm/company-scope/index', 'MdmCompanyScope'),
    (990256, '授权公司新增', 'mdm:company-scope:create', 3, 10, 990255, '', '', ''),
    (990257, '授权公司修改', 'mdm:company-scope:update', 3, 20, 990255, '', '', ''),
    (990258, '授权公司删除', 'mdm:company-scope:delete', 3, 30, 990255, '', '', '');

  IF EXISTS (SELECT 1 FROM tmp_word14_scope_menus expected JOIN system_menu actual ON actual.id = expected.id
              WHERE NOT (actual.name <=> expected.name) OR NOT (actual.permission <=> expected.permission)
                 OR actual.type <> expected.type OR actual.sort <> expected.sort
                 OR actual.parent_id <> expected.parent_id OR NOT (actual.path <=> expected.path)
                 OR NOT (actual.component <=> expected.component) OR NOT (actual.component_name <=> expected.component_name)
                 OR actual.status <> 0 OR actual.visible <> b'1' OR actual.deleted <> b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Authorized-company reserved menu ID contract conflict';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu actual JOIN tmp_word14_scope_menus expected
                    ON actual.permission = expected.permission AND actual.id <> expected.id)
     OR EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND id <> 990255
                  AND ((parent_id = 990200 AND path = 'company-scope') OR path = '/mdm/company-scope'
                       OR component = 'mdm/company-scope/index' OR component_name = 'MdmCompanyScope')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Authorized-company permission/path/component conflict';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_packages;
  CREATE TEMPORARY TABLE tmp_word14_scope_packages AS
    SELECT DISTINCT package.id AS package_id FROM system_tenant_package package
      JOIN JSON_TABLE(CASE WHEN package.deleted = b'0' THEN package.menu_ids ELSE JSON_ARRAY() END,
                      '$[*]' COLUMNS(menu_id BIGINT PATH '$')) existing_menu ON 1 = 1
     WHERE package.deleted = b'0' AND existing_menu.menu_id IN (990200, 990255);
  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_roles;
  CREATE TEMPORARY TABLE tmp_word14_scope_roles AS
    SELECT DISTINCT tenant.id AS tenant_id, role.id AS role_id
      FROM system_tenant tenant JOIN system_role role ON role.tenant_id = tenant.id AND role.deleted = b'0'
       AND (role.code = 'tenant_admin' OR (tenant.id = 1 AND role.code = 'super_admin'))
      LEFT JOIN tmp_word14_scope_packages package ON package.package_id = tenant.package_id
     WHERE tenant.deleted = b'0' AND (tenant.id = 1 OR package.package_id IS NOT NULL);
  IF NOT EXISTS (SELECT 1 FROM tmp_word14_scope_roles WHERE tenant_id = 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tenant 1 administrative role missing for authorized-company maintenance';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_package_ids;
  CREATE TEMPORARY TABLE tmp_word14_scope_package_ids (
    package_id BIGINT NOT NULL, menu_id BIGINT NOT NULL, PRIMARY KEY(package_id, menu_id)
  );
  INSERT IGNORE INTO tmp_word14_scope_package_ids
    SELECT target.package_id, existing_menu.menu_id FROM tmp_word14_scope_packages target
      JOIN system_tenant_package package ON package.id = target.package_id
      JOIN JSON_TABLE(CASE WHEN package.deleted = b'0' THEN package.menu_ids ELSE JSON_ARRAY() END,
                      '$[*]' COLUMNS(menu_id BIGINT PATH '$')) existing_menu ON 1 = 1;
  INSERT IGNORE INTO tmp_word14_scope_package_ids
    SELECT target.package_id, menu.id FROM tmp_word14_scope_packages target CROSS JOIN tmp_word14_scope_menus menu;
  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_package_json;
  CREATE TEMPORARY TABLE tmp_word14_scope_package_json AS
    SELECT package_id, JSON_ARRAYAGG(menu_id) menu_ids FROM tmp_word14_scope_package_ids GROUP BY package_id;

  START TRANSACTION;
  INSERT INTO system_menu
    (id, name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
    SELECT expected.id, expected.name, expected.permission, expected.type, expected.sort, expected.parent_id,
           expected.path, IF(expected.type = 2, 'ep:connection', ''), expected.component, expected.component_name,
           0, b'1', b'1', b'1', 'word14-company-scope', NOW(), 'word14-company-scope', NOW(), b'0'
      FROM tmp_word14_scope_menus expected LEFT JOIN system_menu actual ON actual.id = expected.id
     WHERE actual.id IS NULL;

  UPDATE system_tenant_package package JOIN tmp_word14_scope_package_json merged ON merged.package_id = package.id
     SET package.menu_ids = merged.menu_ids, package.updater = 'word14-company-scope', package.update_time = NOW()
   WHERE package.deleted = b'0';
  UPDATE system_role_menu role_menu
    JOIN tmp_word14_scope_roles target ON target.tenant_id = role_menu.tenant_id AND target.role_id = role_menu.role_id
    JOIN tmp_word14_scope_menus menu ON menu.id = role_menu.menu_id
     SET role_menu.deleted = b'0', role_menu.updater = 'word14-company-scope', role_menu.update_time = NOW()
   WHERE role_menu.deleted = b'1';
  INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT target.role_id, menu.id, 'word14-company-scope', NOW(), 'word14-company-scope', NOW(), b'0', target.tenant_id
      FROM tmp_word14_scope_roles target CROSS JOIN tmp_word14_scope_menus menu
     WHERE NOT EXISTS (SELECT 1 FROM system_role_menu existing WHERE existing.tenant_id = target.tenant_id
                         AND existing.role_id = target.role_id AND existing.menu_id = menu.id AND existing.deleted = b'0');

  IF EXISTS (SELECT 1 FROM tmp_word14_scope_menus expected LEFT JOIN system_menu actual ON actual.id = expected.id
              WHERE actual.id IS NULL OR actual.deleted <> b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Authorized-company menu final verification failed';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_word14_scope_packages target
                  JOIN system_tenant_package actual ON actual.id = target.package_id
                  CROSS JOIN tmp_word14_scope_menus menu
                 WHERE NOT JSON_CONTAINS(actual.menu_ids, JSON_ARRAY(menu.id))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Authorized-company package final verification failed';
  END IF;
  IF EXISTS (SELECT 1 FROM tmp_word14_scope_roles target CROSS JOIN tmp_word14_scope_menus menu
                  LEFT JOIN system_role_menu actual ON actual.tenant_id = target.tenant_id
                   AND actual.role_id = target.role_id AND actual.menu_id = menu.id AND actual.deleted = b'0'
                 WHERE actual.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Authorized-company role final verification failed';
  END IF;
  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_package_json;
  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_package_ids;
  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_roles;
  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_packages;
  DROP TEMPORARY TABLE IF EXISTS tmp_word14_scope_menus;
END$$
DELIMITER ;
CALL supplement_mdm_company_scope_menu_20261031();
DROP PROCEDURE IF EXISTS supplement_mdm_company_scope_menu_20261031;
