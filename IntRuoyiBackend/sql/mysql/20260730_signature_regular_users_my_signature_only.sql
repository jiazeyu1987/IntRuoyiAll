-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_signature_my_signature_admin_menu,20260721_admin_full_scope_role_standardization; type=menu; riskLevel=medium
-- 普通角色在电子签名入口只保留“我的签名”；治理、全量签名记录和用户授权只保留给正式管理员角色。

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_signature_regular_users_my_signature_only_20260730;
DELIMITER //
CREATE PROCEDURE ensure_signature_regular_users_my_signature_only_20260730()
BEGIN
  DECLARE v_signature_root_menu_id BIGINT DEFAULT 900218;
  DECLARE v_signature_records_menu_id BIGINT DEFAULT 900411;
  DECLARE v_signature_my_signature_menu_id BIGINT DEFAULT 900418;

  IF NOT EXISTS (
    SELECT 1
      FROM system_menu
     WHERE id = v_signature_root_menu_id
       AND path = '/signature-governance'
       AND status = 0
       AND deleted = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing electronic signature root menu 900218';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM system_menu
     WHERE id = v_signature_my_signature_menu_id
       AND path = 'my-signature'
       AND status = 0
       AND deleted = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing electronic signature my signature menu 900418';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM system_menu
     WHERE id = v_signature_records_menu_id
       AND path = 'signature-records'
       AND status = 0
       AND deleted = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing electronic signature records menu 900411';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_signature_admin_role_code;
  CREATE TEMPORARY TABLE tmp_signature_admin_role_code (
    role_code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO tmp_signature_admin_role_code(role_code)
  VALUES
    ('electronic_signature_admin'),
    ('audit_admin'),
    ('super_admin');

  DROP TEMPORARY TABLE IF EXISTS tmp_signature_regular_allowed_menu;
  CREATE TEMPORARY TABLE tmp_signature_regular_allowed_menu (
    menu_id BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO tmp_signature_regular_allowed_menu(menu_id)
  VALUES
    (900218),
    (900418);

  DROP TEMPORARY TABLE IF EXISTS tmp_signature_governance_scope_menu;
  CREATE TEMPORARY TABLE tmp_signature_governance_scope_menu (
    menu_id BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO tmp_signature_governance_scope_menu(menu_id)
  SELECT DISTINCT menu.id
    FROM system_menu menu
    LEFT JOIN system_menu parent_menu
      ON parent_menu.id = menu.parent_id
   WHERE menu.deleted = b'0'
     AND (
       menu.id IN (900411, 900413, 900417)
       OR menu.parent_id = v_signature_root_menu_id
       OR parent_menu.parent_id = v_signature_root_menu_id
       OR menu.permission IN (
         'signature-governance:policy:query',
         'signature-governance:policy:manage',
         'dcc:controlled-file:signature:manage',
         'mes:pro-batch-record-execution:signature-query'
       )
     )
     AND menu.id NOT IN (SELECT menu_id FROM tmp_signature_regular_allowed_menu);

  DROP TEMPORARY TABLE IF EXISTS tmp_signature_regular_role;
  CREATE TEMPORARY TABLE tmp_signature_regular_role (
    role_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, tenant_id)
  ) ENGINE=Memory;

  INSERT INTO tmp_signature_regular_role(role_id, tenant_id)
  SELECT DISTINCT role_menu.role_id, role_menu.tenant_id
    FROM system_role_menu role_menu
    JOIN system_role role
      ON role.id = role_menu.role_id
     AND role.tenant_id = role_menu.tenant_id
     AND role.deleted = b'0'
    LEFT JOIN tmp_signature_admin_role_code admin_role
      ON admin_role.role_code = role.code
   WHERE role_menu.menu_id = v_signature_root_menu_id
     AND role_menu.deleted = b'0'
     AND admin_role.role_code IS NULL;

  UPDATE system_role_menu role_menu
    JOIN system_role role
      ON role.id = role_menu.role_id
     AND role.tenant_id = role_menu.tenant_id
     AND role.deleted = b'0'
    JOIN tmp_signature_governance_scope_menu scope_menu
      ON scope_menu.menu_id = role_menu.menu_id
    LEFT JOIN tmp_signature_admin_role_code admin_role
      ON admin_role.role_code = role.code
     SET role_menu.deleted = b'1',
         role_menu.updater = 'signature-regular-my-only',
         role_menu.update_time = NOW()
   WHERE role_menu.deleted = b'0'
     AND admin_role.role_code IS NULL
     AND role_menu.menu_id NOT IN (SELECT menu_id FROM tmp_signature_regular_allowed_menu);

  UPDATE system_role_menu role_menu
    JOIN tmp_signature_regular_role regular_role
      ON regular_role.role_id = role_menu.role_id
     AND regular_role.tenant_id = role_menu.tenant_id
    JOIN tmp_signature_regular_allowed_menu allowed_menu
      ON allowed_menu.menu_id = role_menu.menu_id
     SET role_menu.deleted = b'0',
         role_menu.updater = 'signature-regular-my-only',
         role_menu.update_time = NOW();

  INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT regular_role.role_id,
         allowed_menu.menu_id,
         'signature-regular-my-only',
         NOW(),
         'signature-regular-my-only',
         NOW(),
         b'0',
         regular_role.tenant_id
    FROM tmp_signature_regular_role regular_role
    CROSS JOIN tmp_signature_regular_allowed_menu allowed_menu
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_role_menu existing
      WHERE existing.role_id = regular_role.role_id
        AND existing.tenant_id = regular_role.tenant_id
        AND existing.menu_id = allowed_menu.menu_id
        AND existing.deleted = b'0'
   );

  DROP TEMPORARY TABLE IF EXISTS tmp_signature_regular_role;
  DROP TEMPORARY TABLE IF EXISTS tmp_signature_governance_scope_menu;
  DROP TEMPORARY TABLE IF EXISTS tmp_signature_regular_allowed_menu;
  DROP TEMPORARY TABLE IF EXISTS tmp_signature_admin_role_code;
END//
DELIMITER ;

CALL ensure_signature_regular_users_my_signature_only_20260730();

DROP PROCEDURE IF EXISTS ensure_signature_regular_users_my_signature_only_20260730;

COMMIT;
