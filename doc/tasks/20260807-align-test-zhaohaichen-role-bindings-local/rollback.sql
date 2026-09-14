DROP PROCEDURE IF EXISTS codex_20260807_rollback_zhao_no_download;
DELIMITER $$
CREATE PROCEDURE codex_20260807_rollback_zhao_no_download()
proc: BEGIN
    DECLARE v_user_id BIGINT;
    DECLARE v_role_id BIGINT;
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_actor VARCHAR(64) DEFAULT 'codex-20260807-zhao-no-download-rollback';
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
       AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rollback user precondition failed';
    END IF;

    SELECT COUNT(*), MIN(id)
      INTO v_count, v_role_id
      FROM system_role
     WHERE tenant_id = 1
       AND code = 'wenkong_no_download'
       AND deleted = b'0';
    IF v_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rollback role precondition failed';
    END IF;

    SELECT COUNT(*)
      INTO v_count
      FROM system_user_role
     WHERE tenant_id = 1
       AND role_id = v_role_id
       AND user_id <> v_user_id
       AND deleted = b'0';
    IF v_count <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rollback role has unrelated active users';
    END IF;

    UPDATE system_user_role
       SET deleted = b'1', updater = v_actor, update_time = NOW()
     WHERE tenant_id = 1
       AND user_id = v_user_id
       AND role_id = v_role_id
       AND deleted = b'0';
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rollback user role count mismatch';
    END IF;

    UPDATE system_role_menu
       SET deleted = b'1', updater = v_actor, update_time = NOW()
     WHERE tenant_id = 1
       AND role_id = v_role_id
       AND deleted = b'0';
    IF ROW_COUNT() <> 10 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rollback role menu count mismatch';
    END IF;

    UPDATE system_role
       SET deleted = b'1', updater = v_actor, update_time = NOW()
     WHERE tenant_id = 1
       AND id = v_role_id
       AND deleted = b'0';
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rollback role count mismatch';
    END IF;

    COMMIT;
    SELECT 'ROLLED_BACK' AS tx_result, v_user_id AS user_id, v_role_id AS role_id;
END$$
DELIMITER ;
CALL codex_20260807_rollback_zhao_no_download();
DROP PROCEDURE codex_20260807_rollback_zhao_no_download;
