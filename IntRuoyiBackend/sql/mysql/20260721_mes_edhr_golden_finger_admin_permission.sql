-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260523_mes_batch_record_execution_edhr_v1,20260707_system_role_category_management; type=permission; riskLevel=medium
-- Temporary eDHR golden-finger test permission. Requires explicit role + explicit permission binding.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_mes_edhr_golden_finger_admin_permission_20260721;
DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_golden_finger_admin_permission_20260721()
BEGIN
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;
  DECLARE v_parent_menu_id BIGINT DEFAULT NULL;
  DECLARE v_batch_record_category_id BIGINT DEFAULT NULL;
  DECLARE v_golden_finger_menu_id BIGINT DEFAULT NULL;
  DECLARE v_golden_finger_role_id BIGINT DEFAULT NULL;
  DECLARE v_preferred_menu_id BIGINT DEFAULT 900399;
  DECLARE v_preferred_role_id BIGINT DEFAULT 910399;

  IF (
    SELECT COUNT(*)
      FROM system_menu
     WHERE permission = 'mes:pro-batch-record-execution:golden-finger'
       AND deleted = b'0'
  ) > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate eDHR golden-finger permission menu';
  END IF;

  SELECT id
    INTO v_parent_menu_id
    FROM system_menu
   WHERE permission = 'mes:pro-batch-record-execution:update'
     AND status = 0
     AND deleted = b'0'
   ORDER BY id
   LIMIT 1;

  IF v_parent_menu_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing eDHR execution update permission menu';
  END IF;

  SELECT id
    INTO v_batch_record_category_id
    FROM system_role_category
   WHERE code = 'batch-record'
     AND tenant_id = 1
     AND status = 0
     AND deleted = b'0'
   ORDER BY id
   LIMIT 1;

  IF v_batch_record_category_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 batch-record role category';
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
    INTO v_golden_finger_menu_id
    FROM system_menu
   WHERE permission = 'mes:pro-batch-record-execution:golden-finger'
     AND deleted = b'0'
   ORDER BY id
   LIMIT 1;

  IF v_golden_finger_menu_id IS NULL THEN
    IF EXISTS (
      SELECT 1
        FROM system_menu
       WHERE id = v_preferred_menu_id
         AND deleted = b'0'
    ) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Preferred eDHR golden-finger menu id is already occupied';
    END IF;

    INSERT INTO system_menu (
      id, name, permission, type, sort, parent_id, path, icon, component, component_name,
      status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
    )
    VALUES (
      900399, '批记录金手指管理员', 'mes:pro-batch-record-execution:golden-finger',
      3, 399, v_parent_menu_id, '', '', '', '',
      0, b'1', b'1', b'1', '20260721-edhr-golden-finger', NOW(),
      '20260721-edhr-golden-finger', NOW(), b'0'
    );
    SET v_golden_finger_menu_id = v_preferred_menu_id;
  ELSE
    UPDATE system_menu
       SET name = '批记录金手指管理员',
           type = 3,
           sort = 399,
           parent_id = v_parent_menu_id,
           status = 0,
           visible = b'1',
           keep_alive = b'1',
           always_show = b'1',
           updater = '20260721-edhr-golden-finger',
           update_time = NOW()
     WHERE id = v_golden_finger_menu_id;
  END IF;

  SELECT id
    INTO v_golden_finger_role_id
    FROM system_role
   WHERE code = 'edhr_golden_finger_admin'
     AND tenant_id = 1
   ORDER BY deleted ASC, id
   LIMIT 1;

  IF v_golden_finger_role_id IS NULL THEN
    IF EXISTS (
      SELECT 1
        FROM system_role
       WHERE id = v_preferred_role_id
    ) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Preferred eDHR golden-finger role id is already occupied';
    END IF;

    INSERT INTO system_role (
      id, name, code, sort, category_id, data_scope, data_scope_dept_ids,
      status, type, remark, creator, create_time, updater, update_time, deleted, tenant_id
    )
    VALUES (
      v_preferred_role_id, '批记录金手指管理员', 'edhr_golden_finger_admin',
      910399, v_batch_record_category_id, 1, '',
      0, 2, '临时测试权限：放行前可代填并直接提交当前 eDHR 表单，绕过填写人和普通检查；不得绕过放行、关闭、作废或审批锁定，所有提交必须审计。',
      '20260721-edhr-golden-finger', NOW(), '20260721-edhr-golden-finger', NOW(), b'0', 1
    );
    SET v_golden_finger_role_id = v_preferred_role_id;
  ELSE
    UPDATE system_role
       SET name = '批记录金手指管理员',
           code = 'edhr_golden_finger_admin',
           sort = 910399,
           category_id = v_batch_record_category_id,
           data_scope = 1,
           data_scope_dept_ids = '',
           status = 0,
           type = 2,
           remark = '临时测试权限：放行前可代填并直接提交当前 eDHR 表单，绕过填写人和普通检查；不得绕过放行、关闭、作废或审批锁定，所有提交必须审计。',
           updater = '20260721-edhr-golden-finger',
           update_time = NOW(),
           deleted = b'0',
           tenant_id = 1
     WHERE id = v_golden_finger_role_id;
  END IF;

  UPDATE system_role_menu
     SET deleted = b'0',
         updater = '20260721-edhr-golden-finger',
         update_time = NOW(),
         tenant_id = 1
   WHERE role_id = v_golden_finger_role_id
     AND menu_id = v_golden_finger_menu_id;

  INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_golden_finger_role_id, v_golden_finger_menu_id,
         '20260721-edhr-golden-finger', NOW(),
         '20260721-edhr-golden-finger', NOW(), b'0', 1
    FROM DUAL
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_role_menu existing
      WHERE existing.role_id = v_golden_finger_role_id
        AND existing.menu_id = v_golden_finger_menu_id
   );

  UPDATE system_user_role
     SET deleted = b'0',
         updater = '20260721-edhr-golden-finger',
         update_time = NOW(),
         tenant_id = 1
   WHERE user_id = v_admin_user_id
     AND role_id = v_golden_finger_role_id;

  INSERT INTO system_user_role (
    user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_admin_user_id, v_golden_finger_role_id,
         '20260721-edhr-golden-finger', NOW(),
         '20260721-edhr-golden-finger', NOW(), b'0', 1
    FROM DUAL
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_user_role existing
      WHERE existing.user_id = v_admin_user_id
        AND existing.role_id = v_golden_finger_role_id
   );
END//
DELIMITER ;

CALL ensure_mes_edhr_golden_finger_admin_permission_20260721();

DROP PROCEDURE IF EXISTS ensure_mes_edhr_golden_finger_admin_permission_20260721;

COMMIT;
