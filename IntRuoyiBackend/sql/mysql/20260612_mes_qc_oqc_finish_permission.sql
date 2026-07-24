-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=permission; riskLevel=low
-- OQC finish permission required by MesQcOqcController#finishOqc.
SET @oqc_parent_menu_id := (
    SELECT parent_id
    FROM system_menu
    WHERE permission = 'mes:qc-oqc:query'
      AND deleted = b'0'
    ORDER BY id
    LIMIT 1
);

SET @oqc_update_menu_id := (
    SELECT id
    FROM system_menu
    WHERE permission = 'mes:qc-oqc:update'
      AND deleted = b'0'
    ORDER BY id
    LIMIT 1
);

SET @missing_oqc_menu_message := IF(@oqc_parent_menu_id IS NULL OR @oqc_update_menu_id IS NULL,
    'OQC query/update menu permissions are required before adding mes:qc-oqc:finish',
    NULL);

DROP PROCEDURE IF EXISTS tmp_guard_oqc_finish_permission_menu;

DELIMITER $$
CREATE PROCEDURE tmp_guard_oqc_finish_permission_menu()
BEGIN
    IF @missing_oqc_menu_message IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'OQC query/update menu permissions are required before adding mes:qc-oqc:finish';
    END IF;
END$$
DELIMITER ;

CALL tmp_guard_oqc_finish_permission_menu();
DROP PROCEDURE tmp_guard_oqc_finish_permission_menu;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT
    '出货检验完成', 'mes:qc-oqc:finish', 3, 4, @oqc_parent_menu_id, '', '', '', NULL,
    0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu
    WHERE permission = 'mes:qc-oqc:finish'
      AND deleted = b'0'
);

SET @oqc_finish_menu_id := (
    SELECT id
    FROM system_menu
    WHERE permission = 'mes:qc-oqc:finish'
      AND deleted = b'0'
    ORDER BY id
    LIMIT 1
);

INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT DISTINCT
    rm.role_id, @oqc_finish_menu_id, 'system', NOW(), 'system', NOW(), b'0', rm.tenant_id
FROM system_role_menu rm
WHERE rm.menu_id = @oqc_update_menu_id
  AND rm.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu existing
      WHERE existing.role_id = rm.role_id
        AND existing.menu_id = @oqc_finish_menu_id
        AND existing.tenant_id = rm.tenant_id
        AND existing.deleted = b'0'
  );
