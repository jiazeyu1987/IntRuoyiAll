-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=medium
-- 自研 SRM 停止开发，后续采用第三方系统。仅撤销入口与授权，不删除业务数据或审计记录。
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS retire_internal_srm;
DELIMITER $$
CREATE PROCEDURE retire_internal_srm()
BEGIN
  DECLARE retired_menu_ids JSON;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS retired_srm_menu_ids;
    RESIGNAL;
  END;

  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0' AND NOT JSON_VALID(menu_ids)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid tenant package menu_ids JSON; cannot retire SRM';
  END IF;

  CREATE TEMPORARY TABLE retired_srm_menu_ids (id BIGINT PRIMARY KEY);
  START TRANSACTION;
  INSERT INTO retired_srm_menu_ids
  WITH RECURSIVE retired AS (
    SELECT id FROM system_menu
    WHERE TRIM(BOTH '/' FROM path) = 'srm'
       OR TRIM(LEADING '/' FROM path) LIKE 'srm/%'
       OR TRIM(LEADING '/' FROM component) LIKE 'srm/%'
       OR permission LIKE 'srm:%'
    UNION DISTINCT
    SELECT m.id FROM system_menu m JOIN retired p ON p.id = m.parent_id
  ) SELECT id FROM retired;

  UPDATE system_menu m JOIN retired_srm_menu_ids r ON r.id = m.id
  SET m.status = 1, m.visible = b'0', m.updater = 'srm-retirement', m.update_time = NOW();

  UPDATE system_role_menu rm JOIN retired_srm_menu_ids r ON r.id = rm.menu_id
  SET rm.deleted = b'1', rm.updater = 'srm-retirement', rm.update_time = NOW()
  WHERE rm.deleted = b'0';

  SELECT COALESCE(JSON_ARRAYAGG(id), JSON_ARRAY()) INTO retired_menu_ids FROM retired_srm_menu_ids;
  UPDATE system_tenant_package p
  SET p.menu_ids = (
    SELECT COALESCE(JSON_ARRAYAGG(j.menu_id), JSON_ARRAY())
    FROM JSON_TABLE(p.menu_ids, '$[*]' COLUMNS(menu_id BIGINT PATH '$')) j
    LEFT JOIN retired_srm_menu_ids r ON r.id = j.menu_id
    WHERE r.id IS NULL
  ), p.updater = 'srm-retirement', p.update_time = NOW()
  WHERE p.deleted = b'0' AND JSON_OVERLAPS(p.menu_ids, retired_menu_ids);

  UPDATE system_role SET status = 1, updater = 'srm-retirement', update_time = NOW()
  WHERE code = 'srm_admin' AND deleted = b'0';
  UPDATE system_role_category SET status = 1, updater = 'srm-retirement', update_time = NOW()
  WHERE code = 'srm' AND deleted = b'0';

  COMMIT;
  DROP TEMPORARY TABLE retired_srm_menu_ids;
END$$
DELIMITER ;
CALL retire_internal_srm();
DROP PROCEDURE retire_internal_srm;
