-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260630_dcc_admin_full_config_menu,20260714_dcc_distribution_training_menu_retire; type=menu; riskLevel=low
-- G70 local tenant-1 delivery only: one permission button plus one exact enabled doc_control grant.
-- Parent 6800 is already granted. Never enable retired 6808 or grant administrator page 6819.
-- Run in a fresh dedicated connection with writers fenced; stop on error, never mysql --force.
-- No old row UPDATE/DELETE, role/user/category/tenant-package changes or business schema DDL.
SET NAMES utf8mb4;
DELIMITER $$
CREATE PROCEDURE g70_dcc_native_distribute()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE v_exact INT DEFAULT 0;
  DECLARE v_menu_id BIGINT DEFAULT NULL;
  DECLARE v_role_id BIGINT DEFAULT NULL;
  DECLARE v_menu_writes INT DEFAULT 0;
  DECLARE v_grant_writes INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;
  SELECT COUNT(*) INTO v_count FROM system_menu
    WHERE id = 6800 AND type = 1 AND status = 0 AND deleted = b'0';
  IF v_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 enabled DCC ancestor 6800 required';
  END IF;
  SELECT COUNT(*) INTO v_count FROM system_role
    WHERE tenant_id = 1 AND code = 'doc_control' AND status = 0 AND deleted = b'0';
  SELECT COUNT(*) INTO v_exact FROM system_role
    WHERE tenant_id = 1 AND id = 910233 AND BINARY code = BINARY 'doc_control' AND status = 0 AND deleted = b'0';
  IF v_count <> 1 OR v_exact <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 exact unique tenant1 doc_control role910233 required';
  END IF;
  SELECT id INTO v_role_id FROM system_role WHERE tenant_id = 1 AND id = 910233
    AND BINARY code = BINARY 'doc_control' AND status = 0 AND deleted = b'0' FOR UPDATE;
  IF NOT (v_role_id = 910233) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 role identity changed';
  END IF;
  SELECT COUNT(*) INTO v_count FROM system_role_menu
    WHERE tenant_id = 1 AND role_id = v_role_id AND menu_id = 6800 AND deleted = b'0';
  IF v_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 existing DCC ancestor role grant required';
  END IF;

  -- Namespace count deliberately uses the column collation to reject case/trailing-space collisions.
  SELECT COUNT(*) INTO v_count FROM system_menu WHERE permission = 'dcc:controlled-file:distribute';
  SELECT COUNT(*) INTO v_exact FROM system_menu
    WHERE BINARY permission = BINARY 'dcc:controlled-file:distribute'
      AND BINARY name = BINARY '文控下发' AND type = 3 AND sort = 99 AND parent_id = 6800
      AND BINARY path = BINARY '' AND BINARY icon = BINARY ''
      AND BINARY component = BINARY '' AND BINARY component_name = BINARY ''
      AND status = 0 AND visible = b'1' AND keep_alive = b'0' AND always_show = b'0' AND deleted = b'0';
  IF v_count = 0 THEN
    IF EXISTS (SELECT 1 FROM system_menu WHERE id = 6839) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 reserved menu6839 is occupied';
    END IF;
    INSERT INTO system_menu
      (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,
       creator,create_time,updater,update_time,deleted)
    VALUES (6839,'文控下发','dcc:controlled-file:distribute',3,99,6800,'','','','',0,b'1',b'0',b'0',
      'dcc-native-distribution-seed',CURRENT_TIMESTAMP,'dcc-native-distribution-seed',CURRENT_TIMESTAMP,b'0');
    SET v_menu_writes = ROW_COUNT();
    IF v_menu_writes <> 1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 menu insert rowcount mismatch';
    END IF;
    SET v_menu_id = 6839;
  ELSEIF v_count = 1 AND v_exact = 1 THEN
    -- An exact formally UI-created permission is the same business identity; preserve its ID/audit bytes.
    SELECT id INTO v_menu_id FROM system_menu WHERE BINARY permission = BINARY 'dcc:controlled-file:distribute'
      AND deleted = b'0' FOR UPDATE;
    IF v_menu_id IS NULL OR v_menu_id <= 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 existing menu identity invalid';
    END IF;
  ELSE
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 distribution menu namespace or payload conflict';
  END IF;

  SELECT COUNT(*) INTO v_count FROM system_role_menu WHERE role_id = v_role_id AND menu_id = v_menu_id;
  SELECT COUNT(*) INTO v_exact FROM system_role_menu
    WHERE tenant_id = 1 AND role_id = v_role_id AND menu_id = v_menu_id AND deleted = b'0';
  IF v_count = 0 THEN
    INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    VALUES (v_role_id,v_menu_id,'dcc-native-distribution-seed',CURRENT_TIMESTAMP,
      'dcc-native-distribution-seed',CURRENT_TIMESTAMP,b'0',1);
    SET v_grant_writes = ROW_COUNT();
    IF v_grant_writes <> 1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 grant insert rowcount mismatch';
    END IF;
  ELSEIF v_count <> 1 OR v_exact <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 existing role grant identity conflict';
  END IF;

  SELECT COUNT(*) INTO v_count FROM system_role_menu
    WHERE tenant_id = 1 AND role_id = v_role_id AND menu_id = v_menu_id AND deleted = b'0';
  IF v_count <> 1 OR v_menu_writes > 1 OR v_grant_writes > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'G70 exact postflight grant required';
  END IF;
  COMMIT;
  SELECT 'G70_NATIVE_DISTRIBUTE_MENU_ONLY' AS scope, v_menu_id AS menu_id, v_role_id AS role_id,
    1 AS tenant_id, v_menu_writes AS menu_insert_count, v_grant_writes AS grant_insert_count;
END$$
DELIMITER ;
CALL g70_dcc_native_distribute();
DROP PROCEDURE g70_dcc_native_distribute;
