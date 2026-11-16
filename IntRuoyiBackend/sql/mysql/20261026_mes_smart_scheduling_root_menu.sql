-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_smart_scheduling_tabs; type=menu; riskLevel=low
-- Promote the existing directory; preserve page URLs, menu IDs and permissions.
-- Existing system_role_menu and system_tenant_package grants remain valid: no new menu or grant is needed.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS promote_mes_smart_scheduling_root;
DELIMITER $$
CREATE PROCEDURE promote_mes_smart_scheduling_root()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900120 AND name='智能排产'
      AND type=1 AND permission='mes:pro-smart-scheduling:query'
      AND status=0 AND deleted=b'0'
      AND ((parent_id=5100 AND path='smart-scheduling')
        OR (parent_id=0 AND path IN ('smart-scheduling','/mes/smart-scheduling')))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing or changed smart scheduling directory 900120';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>900120
      AND path='/mes/smart-scheduling') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Smart scheduling root route is occupied';
  END IF;
  IF EXISTS (SELECT 1 FROM system_menu WHERE parent_id=900120 AND deleted=b'0'
      AND type<>3 AND path NOT LIKE '/%') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Smart scheduling child has a relative route';
  END IF;
  UPDATE system_menu SET parent_id=0,path='/mes/smart-scheduling',
    updater='scheduling-root',update_time=NOW()
  WHERE id=900120 AND (parent_id<>0 OR path<>'/mes/smart-scheduling');
  COMMIT;
END$$
DELIMITER ;
CALL promote_mes_smart_scheduling_root();
DROP PROCEDURE promote_mes_smart_scheduling_root;
