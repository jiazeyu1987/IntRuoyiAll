-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260630_approval_center_role_visibility; type=data; riskLevel=medium
-- Ensure only the approval_admin role grants approval-center global visibility and tenant 1 admin owns that role.

SET NAMES utf8mb4;
START TRANSACTION;

DROP PROCEDURE IF EXISTS ensure_approval_center_admin_role_scope_20260721;
DELIMITER //
CREATE PROCEDURE ensure_approval_center_admin_role_scope_20260721()
BEGIN
  DECLARE v_admin_user_id BIGINT DEFAULT NULL;
  DECLARE v_approval_admin_role_id BIGINT DEFAULT NULL;

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

  UPDATE system_role
     SET name = '审批中心管理员',
         remark = '审批中心全量可见管理员角色',
         updater = 'codex',
         update_time = NOW()
   WHERE id = v_approval_admin_role_id;

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
END//
DELIMITER ;

CALL ensure_approval_center_admin_role_scope_20260721();

DROP PROCEDURE IF EXISTS ensure_approval_center_admin_role_scope_20260721;

COMMIT;
