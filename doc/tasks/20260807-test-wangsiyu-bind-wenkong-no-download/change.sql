DROP PROCEDURE IF EXISTS codex_20260807_bind_wangsiyu_wenkong_no_download;
DELIMITER $$
CREATE PROCEDURE codex_20260807_bind_wangsiyu_wenkong_no_download()
proc: BEGIN
    DECLARE v_user_id BIGINT;
    DECLARE v_role_id BIGINT;
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_actor VARCHAR(64) DEFAULT 'codex-20260807-wangsiyu-wenkong-no-download';
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
       AND username = 'wangsiyu'
       AND status = 0
       AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected exactly one enabled tenant 1 wangsiyu user';
    END IF;

    SELECT COUNT(*), MIN(id)
      INTO v_count, v_role_id
      FROM system_role
     WHERE tenant_id = 1
       AND code = 'wenkong_no_download'
       AND status = 0
       AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected exactly one enabled wenkong_no_download role';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_user_role
     WHERE tenant_id = 1
       AND user_id = v_user_id
       AND role_id = v_role_id
       AND deleted = b'0';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wangsiyu already has active wenkong_no_download binding';
    END IF;

    SELECT COUNT(DISTINCT m.id)
      INTO v_count
      FROM system_role_menu rm
      JOIN system_menu m
        ON m.id = rm.menu_id
       AND m.status = 0
       AND m.deleted = b'0'
     WHERE rm.tenant_id = 1
       AND rm.role_id = v_role_id
       AND rm.deleted = b'0'
       AND m.id IN (6800, 900218, 990200);
    IF v_count <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wenkong_no_download does not cover all target root menus';
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
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wenkong_no_download contains forbidden DCC permission';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_user_role ur
      JOIN system_role r
        ON r.id = ur.role_id
       AND r.tenant_id = ur.tenant_id
       AND r.status = 0
       AND r.deleted = b'0'
      JOIN system_role_menu rm
        ON rm.role_id = r.id
       AND rm.tenant_id = r.tenant_id
       AND rm.deleted = b'0'
      JOIN system_menu m
        ON m.id = rm.menu_id
       AND m.status = 0
       AND m.deleted = b'0'
     WHERE ur.tenant_id = 1
       AND ur.user_id = v_user_id
       AND ur.deleted = b'0'
       AND m.permission LIKE 'dcc:%';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wangsiyu already has active DCC permissions before safe binding';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM dcc_file_category_permission_rule
     WHERE tenant_id = 1
       AND subject_type IN ('ROLE', '3')
       AND subject_id = v_role_id
       AND action_type = 'DOWNLOAD'
       AND active = 1
       AND deleted = 0;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wenkong_no_download has category download rule';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM dcc_directory_access_rule
     WHERE tenant_id = 1
       AND subject_type IN ('ROLE', '3')
       AND subject_id = v_role_id
       AND can_download = 1
       AND active = 1
       AND deleted = 0;
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'wenkong_no_download has directory download rule';
    END IF;

    INSERT INTO system_user_role (
        user_id, role_id, creator, updater, tenant_id
    ) VALUES (
        v_user_id, v_role_id, v_actor, v_actor, 1
    );
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User role insert count mismatch';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_user_role ur
      JOIN system_role r
        ON r.id = ur.role_id
       AND r.tenant_id = ur.tenant_id
       AND r.status = 0
       AND r.deleted = b'0'
     WHERE ur.tenant_id = 1
       AND ur.user_id = v_user_id
       AND ur.deleted = b'0'
       AND r.code = 'wenkong_no_download';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected exactly one active no-download binding after insert';
    END IF;

    COMMIT;

    SELECT 'COMMITTED' AS tx_result,
           v_user_id AS user_id,
           v_role_id AS role_id,
           LAST_INSERT_ID() AS user_role_id;
END$$
DELIMITER ;
CALL codex_20260807_bind_wangsiyu_wenkong_no_download();
DROP PROCEDURE codex_20260807_bind_wangsiyu_wenkong_no_download;
