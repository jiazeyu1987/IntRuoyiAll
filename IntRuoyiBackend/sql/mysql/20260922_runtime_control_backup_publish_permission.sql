-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260523_infra_runtime_control_menu; type=permission; riskLevel=medium; requiresTargetPreflight=true
-- The publish action is intentionally separate from generic runtime-control operations.
-- Parent or permission identity conflicts fail without overwriting administrator data.
DROP PROCEDURE IF EXISTS migrate_runtime_backup_publish_permission_20260922;
DELIMITER $$
CREATE PROCEDURE migrate_runtime_backup_publish_permission_20260922()
BEGIN
    DECLARE v_parent_count INT DEFAULT 0;
    DECLARE v_parent_id BIGINT DEFAULT NULL;
    DECLARE v_permission_count INT DEFAULT 0;
    DECLARE v_permission_parent BIGINT DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT COUNT(*), MIN(`id`) INTO v_parent_count, v_parent_id
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `type` = 2
      AND `permission` = 'infra:runtime-control:query'
      AND `path` = 'runtime-control'
      AND `component` = 'infra/runtime-control/index';
    IF v_parent_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RELEASE_BACKUP_PUBLISH_PARENT_AMBIGUOUS';
    END IF;

    SELECT COUNT(*), MIN(`parent_id`) INTO v_permission_count, v_permission_parent
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `type` = 3
      AND `permission` = 'infra:runtime-control:publish-backup';
    IF v_permission_count > 1
       OR (v_permission_count = 1 AND v_permission_parent <> v_parent_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RELEASE_BACKUP_PUBLISH_PERMISSION_CONFLICT';
    END IF;

    IF v_permission_count = 0 THEN
        INSERT INTO `system_menu`
        (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`,
         `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`,
         `create_time`, `updater`, `update_time`, `deleted`)
        SELECT '运行控制台发布审查服务器', 'infra:runtime-control:publish-backup', 3, 5,
         v_parent_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
        WHERE NOT EXISTS (
            SELECT 1 FROM `system_menu`
            WHERE `deleted` = b'0'
              AND `type` = 3
              AND `permission` = 'infra:runtime-control:publish-backup'
        );
    END IF;

    SELECT COUNT(*) INTO v_permission_count
    FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `type` = 3
      AND `parent_id` = v_parent_id
      AND `permission` = 'infra:runtime-control:publish-backup';
    IF v_permission_count <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'RELEASE_BACKUP_PUBLISH_PERMISSION_MISSING';
    END IF;

    COMMIT;
END$$
DELIMITER ;
CALL migrate_runtime_backup_publish_permission_20260922();
DROP PROCEDURE migrate_runtime_backup_publish_permission_20260922;
