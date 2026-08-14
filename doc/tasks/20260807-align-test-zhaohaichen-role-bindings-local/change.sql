DROP PROCEDURE IF EXISTS codex_20260807_align_zhao_no_download;
DELIMITER $$
CREATE PROCEDURE codex_20260807_align_zhao_no_download()
proc: BEGIN
    DECLARE v_user_id BIGINT;
    DECLARE v_role_id BIGINT;
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_actor VARCHAR(64) DEFAULT 'codex-20260807-zhao-no-download';
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT COUNT(*), MIN(id)
      INTO v_count, v_user_id
      FROM system_users
     WHERE tenant_id = 1
       AND username = 'zhaohaichen'
       AND status = 0
       AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected exactly one enabled tenant 1 zhaohaichen user';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_user_role ur
      JOIN system_role r
        ON r.id = ur.role_id
       AND r.tenant_id = ur.tenant_id
       AND r.deleted = b'0'
       AND r.status = 0
     WHERE ur.tenant_id = 1
       AND ur.user_id = v_user_id
       AND ur.deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected active role count before change';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_user_role ur
      JOIN system_role r
        ON r.id = ur.role_id
       AND r.tenant_id = ur.tenant_id
       AND r.deleted = b'0'
       AND r.status = 0
     WHERE ur.tenant_id = 1
       AND ur.user_id = v_user_id
       AND ur.deleted = b'0'
       AND r.code = 'approval_center_entry';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'approval_center_entry baseline is missing';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_role
     WHERE tenant_id = 1
       AND code = 'wenkong_no_download';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wenkong_no_download already exists';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_menu
     WHERE id IN (6800, 6806, 6807, 6814, 6818, 900218, 900418, 990200, 990210, 990216)
       AND status = 0
       AND deleted = b'0';
    IF v_count <> 10 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Safe menu whitelist is incomplete';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_menu
     WHERE id IN (6800, 6806, 6807, 6814, 6818, 900218, 900418, 990200, 990210, 990216)
       AND permission IN (
           'dcc:controlled-file:directory:manage',
           'dcc:controlled-file:access-rule:manage',
           'dcc:controlled-file:category:manage',
           'dcc:controlled-file:download'
       );
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Safe menu whitelist contains a forbidden permission';
    END IF;

    INSERT INTO system_role (
        name, code, sort, category_id, data_scope, data_scope_dept_ids,
        status, type, remark, creator, updater, tenant_id
    ) VALUES (
        CONVERT(0xE69687E68EA7EFBC88E697A0E4B88BE8BDBDEFBC89 USING utf8mb4),
        'wenkong_no_download', 6801, 3, 3, '', 0, 2,
        'Dedicated DCC role without download or permission-management grants',
        v_actor, v_actor, 1
    );
    SET v_role_id = LAST_INSERT_ID();

    INSERT INTO system_role_menu (
        role_id, menu_id, creator, updater, tenant_id
    )
    SELECT v_role_id, m.id, v_actor, v_actor, 1
      FROM system_menu m
     WHERE m.id IN (6800, 6806, 6807, 6814, 6818, 900218, 900418, 990200, 990210, 990216)
       AND m.status = 0
       AND m.deleted = b'0';
    IF ROW_COUNT() <> 10 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Role menu insert count mismatch';
    END IF;

    INSERT INTO system_user_role (
        user_id, role_id, creator, updater, tenant_id
    ) VALUES (v_user_id, v_role_id, v_actor, v_actor, 1);
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User role insert count mismatch';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_user_role ur
      JOIN system_role r
        ON r.id = ur.role_id
       AND r.tenant_id = ur.tenant_id
     WHERE ur.tenant_id = 1
       AND ur.user_id = v_user_id
       AND ur.deleted = b'0'
       AND r.code IN ('doc_control', 'wenkong');
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'High privilege DCC role unexpectedly active';
    END IF;

    SELECT COUNT(DISTINCT r.code)
      INTO v_count
      FROM system_user_role ur
      JOIN system_role r
        ON r.id = ur.role_id
       AND r.tenant_id = ur.tenant_id
       AND r.deleted = b'0'
       AND r.status = 0
     WHERE ur.tenant_id = 1
       AND ur.user_id = v_user_id
       AND ur.deleted = b'0'
       AND r.code IN ('approval_center_entry', 'wenkong_no_download');
    IF v_count <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected active role set was not created';
    END IF;

    SELECT COUNT(DISTINCT m.id)
      INTO v_count
      FROM system_user_role ur
      JOIN system_role r
        ON r.id = ur.role_id
       AND r.tenant_id = ur.tenant_id
       AND r.deleted = b'0'
       AND r.status = 0
      JOIN system_role_menu rm
        ON rm.role_id = r.id
       AND rm.tenant_id = ur.tenant_id
       AND rm.deleted = b'0'
      JOIN system_menu m
        ON m.id = rm.menu_id
       AND m.deleted = b'0'
       AND m.status = 0
     WHERE ur.tenant_id = 1
       AND ur.user_id = v_user_id
       AND ur.deleted = b'0'
       AND m.id IN (6800, 900218, 990200);
    IF v_count <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target root menu count mismatch';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_role_menu rm
      JOIN system_menu m
        ON m.id = rm.menu_id
       AND m.deleted = b'0'
     WHERE rm.tenant_id = 1
       AND rm.role_id = v_role_id
       AND rm.deleted = b'0'
       AND m.permission IN (
           'dcc:controlled-file:directory:manage',
           'dcc:controlled-file:access-rule:manage',
           'dcc:controlled-file:category:manage',
           'dcc:controlled-file:download'
       );
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Forbidden permission entered no-download role';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM dcc_file_category_permission_rule
     WHERE tenant_id = 1
       AND subject_type = 'ROLE'
       AND subject_id = v_role_id
       AND action_type = 'DOWNLOAD'
       AND active = 1
       AND deleted = 0;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Category download rule entered no-download role';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM dcc_directory_access_rule
     WHERE tenant_id = 1
       AND subject_type = 'ROLE'
       AND subject_id = v_role_id
       AND can_download = 1
       AND active = 1
       AND deleted = 0;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Directory download rule entered no-download role';
    END IF;

    COMMIT;

    SELECT 'COMMITTED' AS tx_result,
           v_user_id AS user_id,
           v_role_id AS role_id,
           10 AS role_menu_count;
END$$
DELIMITER ;
CALL codex_20260807_align_zhao_no_download();
DROP PROCEDURE codex_20260807_align_zhao_no_download;
