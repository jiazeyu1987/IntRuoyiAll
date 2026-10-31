-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261023_mes_team_leader_permission_identity,20261023_mes_active_order_upgrade_permission_identity,20260808_mes_active_order_release_application; type=menu; riskLevel=medium; applyOrder=10; supersedes=20260915_mes_team_leader_cleanup_permission_menu; requiresTargetPreflight=true
-- Preserve the eDHR button IDs and all existing eDHR grants; attach only validated team-leader permissions.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_mes_team_leader_cleanup_identity;
DELIMITER //
CREATE PROCEDURE ensure_mes_team_leader_cleanup_identity()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF NOT COALESCE(((SELECT COUNT(*) FROM system_menu WHERE id=900436 AND deleted=0 AND status=0 AND type=2
 AND permission='mes:pro-process-pool-team-leader:query'
 AND path='/mes/pro/process-pool/production-leader'
 AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage')=1 AND (SELECT COUNT(*) FROM system_menu WHERE id=991613 AND permission='mes:pro-process-pool-team-leader:maintain' AND type=3 AND deleted=0 AND status=0 AND parent_id=900436)=1
 AND (SELECT COUNT(*) FROM system_menu WHERE permission='mes:pro-process-pool-team-leader:maintain' AND deleted=0)=1
 AND (SELECT COUNT(*) FROM system_menu WHERE id=900451 AND permission='mes:pro-process-pool-team-leader:release-apply' AND type=3 AND deleted=0 AND status=0 AND parent_id IN(900310,900436))=1
 AND (SELECT COUNT(*) FROM system_menu WHERE permission='mes:pro-process-pool-team-leader:release-apply' AND deleted=0)=1
 AND (SELECT COUNT(*) FROM system_menu WHERE id=900452 AND permission='mes:pro-process-pool-team-leader:version-upgrade' AND type=3 AND deleted=0 AND status=0 AND parent_id=900436)=1), FALSE) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Team leader cleanup identity mismatch';
  END IF;
  UPDATE system_menu SET parent_id=900436,updater='team-leader-identity',update_time=NOW()
  WHERE id=900451 AND parent_id<>900436;
  UPDATE system_menu SET name='生产组长维护',sort=4,visible=1,updater='team-leader-identity',update_time=NOW()
  WHERE id=991613 AND (name<>'生产组长维护' OR sort<>4 OR visible<>1);
  COMMIT;
END//
DELIMITER ;
CALL ensure_mes_team_leader_cleanup_identity();
DROP PROCEDURE IF EXISTS ensure_mes_team_leader_cleanup_identity;
