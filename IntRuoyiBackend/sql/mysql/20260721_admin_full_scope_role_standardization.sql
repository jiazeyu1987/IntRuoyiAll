-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_bpm_admin_role_assignment,20260721_approval_center_admin_role_scope,20260714_dcc_controlled_file_logs_consolidation,20260714_unified_signature_records_menu; type=menu; riskLevel=medium
-- Standardize full-scope permissions as explicit administrator roles and bind tenant 1 admin.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_admin_full_scope_role_standardization_20260721;
DELIMITER //
CREATE PROCEDURE ensure_admin_full_scope_role_standardization_20260721()
BEGIN
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;
  DECLARE v_menu_category_id BIGINT DEFAULT NULL;
  DECLARE v_bpm_admin_role_id BIGINT DEFAULT NULL;
  DECLARE v_approval_admin_role_id BIGINT DEFAULT NULL;
  DECLARE v_audit_admin_role_id BIGINT DEFAULT NULL;
  DECLARE v_preferred_audit_admin_role_id BIGINT DEFAULT 910312;
  DECLARE v_should_insert_audit_admin_role TINYINT DEFAULT 0;
  DECLARE v_insert_audit_admin_with_preferred_id TINYINT DEFAULT 0;

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

  SELECT id
    INTO v_bpm_admin_role_id
    FROM system_role
   WHERE code = 'bpm_admin'
     AND tenant_id = 1
     AND status = 0
     AND deleted = b'0'
   ORDER BY id
   LIMIT 1;

  IF v_bpm_admin_role_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing tenant 1 bpm_admin role';
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
    INTO v_audit_admin_role_id
    FROM system_role
   WHERE code = 'audit_admin'
     AND tenant_id = 1
   ORDER BY deleted ASC, id
   LIMIT 1;

  IF v_audit_admin_role_id IS NULL THEN
    SET v_should_insert_audit_admin_role = 1;
    IF NOT EXISTS (
      SELECT 1
        FROM system_role
       WHERE id = v_preferred_audit_admin_role_id
    ) THEN
      SET v_audit_admin_role_id = v_preferred_audit_admin_role_id;
      SET v_insert_audit_admin_with_preferred_id = 1;
    END IF;
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_audit_admin_expected_menu;
  CREATE TEMPORARY TABLE tmp_audit_admin_expected_menu (
    menu_id BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO tmp_audit_admin_expected_menu(menu_id)
  SELECT 108 UNION ALL
  SELECT 500 UNION ALL
  SELECT 501 UNION ALL
  SELECT 1040 UNION ALL
  SELECT 1042 UNION ALL
  SELECT 1043 UNION ALL
  SELECT 1045 UNION ALL
  SELECT 1083 UNION ALL
  SELECT 1078 UNION ALL
  SELECT 1088 UNION ALL
  SELECT 1082 UNION ALL
  SELECT 1084 UNION ALL
  SELECT 1085 UNION ALL
  SELECT 1086 UNION ALL
  SELECT 1089 UNION ALL
  SELECT 1093 UNION ALL
  SELECT 1107 UNION ALL
  SELECT 1108 UNION ALL
  SELECT 1109 UNION ALL
  SELECT 2130 UNION ALL
  SELECT 2141 UNION ALL
  SELECT 2142 UNION ALL
  SELECT 6800 UNION ALL
  SELECT 6818 UNION ALL
  SELECT 990225 UNION ALL
  SELECT 900220 UNION ALL
  SELECT 900356 UNION ALL
  SELECT 900357 UNION ALL
  SELECT 900218 UNION ALL
  SELECT 900411 UNION ALL
  SELECT 6815 UNION ALL
  SELECT 900026;

  DROP TEMPORARY TABLE IF EXISTS tmp_audit_admin_expected_permission;
  CREATE TEMPORARY TABLE tmp_audit_admin_expected_permission (
    permission VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO tmp_audit_admin_expected_permission(permission)
  SELECT 'system:operate-log:query' UNION ALL
  SELECT 'system:operate-log:export' UNION ALL
  SELECT 'system:login-log:query' UNION ALL
  SELECT 'infra:api-access-log:query' UNION ALL
  SELECT 'infra:api-error-log:query' UNION ALL
  SELECT 'dcc:controlled-file:audit:query' UNION ALL
  SELECT 'dcc:project-code-assignment:audit:query' UNION ALL
  SELECT 'mes:pro-edhr-flow-intervention:event-query' UNION ALL
  SELECT 'signature-governance:policy:query' UNION ALL
  SELECT 'mes:pro-batch-record-execution:signature-query';

  IF EXISTS (
    SELECT 1
      FROM tmp_audit_admin_expected_menu expected_menu
      LEFT JOIN system_menu menu
        ON menu.id = expected_menu.menu_id
       AND menu.status = 0
       AND menu.deleted = b'0'
     WHERE menu.id IS NULL
  ) OR EXISTS (
    SELECT 1
      FROM tmp_audit_admin_expected_permission expected_permission
      LEFT JOIN system_menu menu
        ON menu.permission = expected_permission.permission
       AND menu.status = 0
       AND menu.deleted = b'0'
     WHERE menu.id IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing enabled full-scope admin menu';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_audit_admin_restricted_menu;
  CREATE TEMPORARY TABLE tmp_audit_admin_restricted_menu (
    menu_id BIGINT NOT NULL PRIMARY KEY
  ) ENGINE=Memory;

  INSERT INTO tmp_audit_admin_restricted_menu(menu_id)
  SELECT DISTINCT menu.id
    FROM system_menu menu
    JOIN tmp_audit_admin_expected_permission expected_permission
      ON expected_permission.permission = menu.permission
   WHERE menu.status = 0
     AND menu.deleted = b'0';

  IF v_should_insert_audit_admin_role = 1 AND v_insert_audit_admin_with_preferred_id = 1 THEN
    INSERT INTO system_role (
      id, name, code, sort, category_id, data_scope, data_scope_dept_ids,
      status, type, remark, creator, create_time, updater, update_time,
      deleted, tenant_id
    )
    VALUES (
      v_audit_admin_role_id, '审计管理员', 'audit_admin', 910312, v_menu_category_id, 1, '',
      0, 2, '审计管理员角色；可全量查询、导出系统日志、操作审计、审批日志和签名证据账本，普通用户仍按本人相关或对象级授权查看审计信息。', 'codex', NOW(), 'codex', NOW(),
      b'0', 1
    );
  ELSEIF v_should_insert_audit_admin_role = 1 THEN
    INSERT INTO system_role (
      name, code, sort, category_id, data_scope, data_scope_dept_ids,
      status, type, remark, creator, create_time, updater, update_time,
      deleted, tenant_id
    )
    VALUES (
      '审计管理员', 'audit_admin', 910312, v_menu_category_id, 1, '',
      0, 2, '审计管理员角色；可全量查询、导出系统日志、操作审计、审批日志和签名证据账本，普通用户仍按本人相关或对象级授权查看审计信息。', 'codex', NOW(), 'codex', NOW(),
      b'0', 1
    );
    SET v_audit_admin_role_id = LAST_INSERT_ID();
  END IF;

  UPDATE system_role
     SET name = 'BPM管理员',
         remark = 'BPM管理员角色；可管理表单中心、流程模型、流程实例、流程任务和流程配置，并可查看或取消全量 BPM 流程实例。',
         updater = 'codex',
         update_time = NOW()
   WHERE id = v_bpm_admin_role_id;

  UPDATE system_role
     SET name = '审批中心管理员',
         remark = '审批中心管理员角色；可全量查看统一审批中心任务、详情、轨迹、导出和统计，普通用户仍只看本人发起、本人审批、抄送或授权相关审批信息。',
         updater = 'codex',
         update_time = NOW()
   WHERE id = v_approval_admin_role_id;

  UPDATE system_role
     SET name = '审计管理员',
         code = 'audit_admin',
         sort = 910312,
         category_id = v_menu_category_id,
         data_scope = 1,
         data_scope_dept_ids = '',
         status = 0,
         type = 2,
         remark = '审计管理员角色；可全量查询、导出系统日志、操作审计、审批日志和签名证据账本，普通用户仍按本人相关或对象级授权查看审计信息。',
         updater = 'codex',
         update_time = NOW(),
         deleted = b'0',
         tenant_id = 1
   WHERE id = v_audit_admin_role_id;

  UPDATE system_role_menu role_menu
    JOIN tmp_audit_admin_expected_menu expected_menu
      ON expected_menu.menu_id = role_menu.menu_id
     SET role_menu.deleted = b'0',
         role_menu.updater = 'codex',
         role_menu.update_time = NOW(),
         role_menu.tenant_id = 1
   WHERE role_menu.role_id = v_audit_admin_role_id;

  INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_audit_admin_role_id, expected_menu.menu_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM tmp_audit_admin_expected_menu expected_menu
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_role_menu existing
      WHERE existing.role_id = v_audit_admin_role_id
        AND existing.menu_id = expected_menu.menu_id
   );

  UPDATE system_role_menu role_menu
    JOIN system_role role
      ON role.id = role_menu.role_id
     AND role.tenant_id = 1
     AND role.deleted = b'0'
    JOIN tmp_audit_admin_restricted_menu restricted_menu
      ON restricted_menu.menu_id = role_menu.menu_id
     SET role_menu.deleted = b'1',
         role_menu.updater = 'codex',
         role_menu.update_time = NOW()
   WHERE role_menu.tenant_id = 1
     AND role_menu.deleted = b'0'
     AND role.code NOT IN ('audit_admin', 'super_admin');

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

  UPDATE system_user_role
     SET deleted = b'0',
         updater = 'codex',
         update_time = NOW(),
         tenant_id = 1
   WHERE user_id = v_admin_user_id
     AND role_id = v_approval_admin_role_id;

  INSERT INTO system_user_role (
    user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_admin_user_id, v_approval_admin_role_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM DUAL
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_user_role existing
      WHERE existing.user_id = v_admin_user_id
        AND existing.role_id = v_approval_admin_role_id
   );

  UPDATE system_user_role
     SET deleted = b'0',
         updater = 'codex',
         update_time = NOW(),
         tenant_id = 1
   WHERE user_id = v_admin_user_id
     AND role_id = v_audit_admin_role_id;

  INSERT INTO system_user_role (
    user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
  )
  SELECT v_admin_user_id, v_audit_admin_role_id, 'codex', NOW(), 'codex', NOW(), b'0', 1
    FROM DUAL
   WHERE NOT EXISTS (
     SELECT 1
       FROM system_user_role existing
      WHERE existing.user_id = v_admin_user_id
        AND existing.role_id = v_audit_admin_role_id
   );

  DROP TEMPORARY TABLE IF EXISTS tmp_audit_admin_expected_permission;
  DROP TEMPORARY TABLE IF EXISTS tmp_audit_admin_restricted_menu;
  DROP TEMPORARY TABLE IF EXISTS tmp_audit_admin_expected_menu;
END//
DELIMITER ;

CALL ensure_admin_full_scope_role_standardization_20260721();

DROP PROCEDURE IF EXISTS ensure_admin_full_scope_role_standardization_20260721;

COMMIT;
