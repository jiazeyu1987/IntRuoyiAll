-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_approval_center_workflow_menu_consolidation; type=menu; riskLevel=medium
-- Restrict Approval Center workflow management navigation to the BPM admin role and assign tenant 1 admin.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_bpm_admin_role_assignment_20260718;
DELIMITER //
CREATE PROCEDURE ensure_bpm_admin_role_assignment_20260718()
BEGIN
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;
  DECLARE v_preferred_bpm_admin_role_id BIGINT DEFAULT 910311;
  DECLARE v_bpm_admin_role_id BIGINT DEFAULT NULL;
  DECLARE v_should_insert_bpm_admin_role TINYINT DEFAULT 0;
  DECLARE v_insert_bpm_admin_with_preferred_id TINYINT DEFAULT 0;
  DECLARE v_approval_entry_role_id BIGINT DEFAULT NULL;
  DECLARE v_approval_admin_role_id BIGINT DEFAULT NULL;
  DECLARE v_menu_category_id BIGINT DEFAULT NULL;

  SELECT id
    INTO v_bpm_admin_role_id
    FROM system_role
   WHERE code = 'bpm_admin'
     AND tenant_id = 1
   ORDER BY deleted ASC, id
   LIMIT 1;

  IF v_bpm_admin_role_id IS NULL THEN
    SET v_should_insert_bpm_admin_role = 1;
    IF NOT EXISTS (
      SELECT 1
        FROM system_role
       WHERE id = v_preferred_bpm_admin_role_id
    ) THEN
      SET v_bpm_admin_role_id = v_preferred_bpm_admin_role_id;
      SET v_insert_bpm_admin_with_preferred_id = 1;
    END IF;
  END IF;

  SELECT id
    INTO v_admin_user_id
    FROM system_users
   WHERE username = 'admin'
     AND tenant_id = 1
     AND status = 0
     AND deleted = b'0'
   ORDER BY id
   LIMIT 1;

  IF v_admin_user_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled tenant 1 admin user';
  END IF;

  SELECT id
    INTO v_approval_entry_role_id
    FROM system_role
   WHERE code = 'approval_center_entry'
     AND tenant_id = 1
     AND status = 0
     AND deleted = b'0'
   ORDER BY id
   LIMIT 1;

  IF v_approval_entry_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 approval_center_entry role';
  END IF;

  SELECT id
    INTO v_approval_admin_role_id
    FROM system_role
   WHERE code = 'approval_admin'
     AND tenant_id = 1
     AND status = 0
     AND deleted = b'0'
   ORDER BY id
   LIMIT 1;

  IF v_approval_admin_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 approval_admin role';
  END IF;

  SELECT id
    INTO v_menu_category_id
    FROM system_role_category
   WHERE code = 'menu'
     AND tenant_id = 1
     AND deleted = b'0'
   ORDER BY id
   LIMIT 1;

  IF v_menu_category_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 menu role category';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_bpm_admin_expected_menu;
  CREATE TEMPORARY TABLE tmp_bpm_admin_expected_menu (
    menu_id BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO tmp_bpm_admin_expected_menu(menu_id)
  SELECT 1200 UNION ALL
  SELECT 1186 UNION ALL
  SELECT 1193 UNION ALL
  SELECT 1194 UNION ALL
  SELECT 1195 UNION ALL
  SELECT 1197 UNION ALL
  SELECT 1198 UNION ALL
  SELECT 1199 UNION ALL
  SELECT 2913 UNION ALL
  SELECT 1187 UNION ALL
  SELECT 1188 UNION ALL
  SELECT 1189 UNION ALL
  SELECT 1190 UNION ALL
  SELECT 1191 UNION ALL
  SELECT 1192 UNION ALL
  SELECT 2714 UNION ALL
  SELECT 2715 UNION ALL
  SELECT 2716 UNION ALL
  SELECT 2717 UNION ALL
  SELECT 2718 UNION ALL
  SELECT 1209 UNION ALL
  SELECT 1210 UNION ALL
  SELECT 1211 UNION ALL
  SELECT 1212 UNION ALL
  SELECT 1213 UNION ALL
  SELECT 2731 UNION ALL
  SELECT 2732 UNION ALL
  SELECT 2733 UNION ALL
  SELECT 2734 UNION ALL
  SELECT 2735 UNION ALL
  SELECT 605071200 UNION ALL
  SELECT 605071201 UNION ALL
  SELECT 605071202 UNION ALL
  SELECT 605071203 UNION ALL
  SELECT 605071204 UNION ALL
  SELECT 605071205 UNION ALL
  SELECT 605071206 UNION ALL
  SELECT 605071207 UNION ALL
  SELECT 605071208 UNION ALL
  SELECT 605071209 UNION ALL
  SELECT 605071210 UNION ALL
  SELECT 605071211 UNION ALL
  SELECT 605071212 UNION ALL
  SELECT 605071213 UNION ALL
  SELECT 605071214 UNION ALL
  SELECT 605071215 UNION ALL
  SELECT 605071216 UNION ALL
  SELECT 605071217;

  IF EXISTS (
    SELECT 1
      FROM tmp_bpm_admin_expected_menu expected_menu
      LEFT JOIN system_menu menu
        ON menu.id = expected_menu.menu_id
       AND menu.status = 0
       AND menu.deleted = b'0'
     WHERE menu.id IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled BPM workflow management menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_approval_center_entry_menu;
  CREATE TEMPORARY TABLE tmp_approval_center_entry_menu (
    menu_id BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO tmp_approval_center_entry_menu(menu_id)
  SELECT 1200 UNION ALL
  SELECT 1207 UNION ALL
  SELECT 1208 UNION ALL
  SELECT 1201 UNION ALL
  SELECT 2713 UNION ALL
  SELECT 1221;

  IF EXISTS (
    SELECT 1
      FROM tmp_approval_center_entry_menu entry_menu
      LEFT JOIN system_menu menu
        ON menu.id = entry_menu.menu_id
       AND menu.status = 0
       AND menu.deleted = b'0'
     WHERE menu.id IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled approval center entry menu';
  END IF;

  IF NOT EXISTS (
    SELECT 1
      FROM system_menu
     WHERE id = 1186
       AND parent_id = 1200
       AND path = 'manager'
       AND deleted = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'BPM workflow management menu is not under approval center';
  END IF;

  IF v_should_insert_bpm_admin_role = 1 AND v_insert_bpm_admin_with_preferred_id = 1 THEN
    INSERT INTO system_role (
      id, name, code, sort, category_id, data_scope, data_scope_dept_ids,
      status, type, remark, creator, create_time, updater, update_time,
      deleted, tenant_id
    )
    VALUES (
      v_bpm_admin_role_id, 'BPM管理员', 'bpm_admin', 910311, v_menu_category_id, 1, '',
      0, 2, '审批中心流程管理菜单及 BPM 配置维护权限', 'codex', NOW(), 'codex', NOW(),
      b'0', 1
    );
  ELSEIF v_should_insert_bpm_admin_role = 1 THEN
    INSERT INTO system_role (
      name, code, sort, category_id, data_scope, data_scope_dept_ids,
      status, type, remark, creator, create_time, updater, update_time,
      deleted, tenant_id
    )
    VALUES (
      'BPM管理员', 'bpm_admin', 910311, v_menu_category_id, 1, '',
      0, 2, '审批中心流程管理菜单及 BPM 配置维护权限', 'codex', NOW(), 'codex', NOW(),
      b'0', 1
    );
    SET v_bpm_admin_role_id = LAST_INSERT_ID();
  END IF;

  UPDATE system_role
     SET name = 'BPM管理员',
         code = 'bpm_admin',
         sort = 910311,
         category_id = v_menu_category_id,
         data_scope = 1,
         data_scope_dept_ids = '',
         status = 0,
         type = 2,
         remark = '审批中心流程管理菜单及 BPM 配置维护权限',
         updater = 'codex',
         update_time = NOW(),
         deleted = b'0',
         tenant_id = 1
   WHERE id = v_bpm_admin_role_id;

  UPDATE system_role_menu role_menu
    JOIN tmp_bpm_admin_expected_menu expected_menu
      ON expected_menu.menu_id = role_menu.menu_id
     SET role_menu.deleted = b'0',
         role_menu.updater = 'codex',
         role_menu.update_time = NOW(),
         role_menu.tenant_id = 1
   WHERE role_menu.role_id = v_bpm_admin_role_id;

  INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_bpm_admin_role_id, expected_menu.menu_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM tmp_bpm_admin_expected_menu expected_menu
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_role_menu existing
      WHERE existing.role_id = v_bpm_admin_role_id
        AND existing.menu_id = expected_menu.menu_id
   );

  UPDATE system_role_menu role_menu
    JOIN tmp_approval_center_entry_menu entry_menu
      ON entry_menu.menu_id = role_menu.menu_id
     SET role_menu.deleted = b'0',
         role_menu.updater = 'codex',
         role_menu.update_time = NOW(),
         role_menu.tenant_id = 1
   WHERE role_menu.role_id IN (v_approval_entry_role_id, v_approval_admin_role_id, v_bpm_admin_role_id);

  INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT target_role.role_id, entry_menu.menu_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM (
      SELECT v_approval_entry_role_id AS role_id
      UNION ALL SELECT v_approval_admin_role_id
      UNION ALL SELECT v_bpm_admin_role_id
    ) target_role
    CROSS JOIN tmp_approval_center_entry_menu entry_menu
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_role_menu existing
      WHERE existing.role_id = target_role.role_id
        AND existing.menu_id = entry_menu.menu_id
   );

  UPDATE system_role_menu
     SET deleted = b'0',
         updater = 'codex',
         update_time = NOW(),
         tenant_id = 1
   WHERE role_id = v_approval_admin_role_id
     AND menu_id = 1222;

  INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_approval_admin_role_id, 1222, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM DUAL
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_role_menu existing
      WHERE existing.role_id = v_approval_admin_role_id
        AND existing.menu_id = 1222
   );

  UPDATE system_role_menu role_menu
    JOIN system_role role
      ON role.id = role_menu.role_id
     AND role.tenant_id = 1
     AND role.deleted = b'0'
    JOIN tmp_bpm_admin_expected_menu expected_menu
      ON expected_menu.menu_id = role_menu.menu_id
     SET role_menu.deleted = b'1',
         role_menu.updater = 'codex',
         role_menu.update_time = NOW()
   WHERE role_menu.tenant_id = 1
     AND role_menu.deleted = b'0'
     AND role_menu.menu_id <> 1200
     AND role.code NOT IN ('bpm_admin', 'super_admin');

  UPDATE system_user_role
     SET deleted = b'0',
         updater = 'codex',
         update_time = NOW(),
         tenant_id = 1
   WHERE user_id = v_admin_user_id
     AND role_id = v_bpm_admin_role_id;

  INSERT INTO system_user_role (
    user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_admin_user_id, v_bpm_admin_role_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM DUAL
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_user_role existing
      WHERE existing.user_id = v_admin_user_id
        AND existing.role_id = v_bpm_admin_role_id
   );

  DROP TEMPORARY TABLE IF EXISTS tmp_bpm_admin_expected_menu;
  DROP TEMPORARY TABLE IF EXISTS tmp_approval_center_entry_menu;
END//
DELIMITER ;

CALL ensure_bpm_admin_role_assignment_20260718();

DROP PROCEDURE IF EXISTS ensure_bpm_admin_role_assignment_20260718;

COMMIT;
