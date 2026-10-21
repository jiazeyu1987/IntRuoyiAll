-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260715_mes_edhr_template_config_menu_removal; type=menu; riskLevel=medium; requiresTargetPreflight=true
-- Only missing hidden legacy query entries/child ownership. August successors own
-- the final visible ordering and trace route; never restore the obsolete four-entry order.
-- Preserve all role/package grants and later visible menu labels.
SET NAMES utf8mb4;
DELIMITER $$
CREATE PROCEDURE release_20261022_trace_menu()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 IF (SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND ((id=900365 AND permission='mes:pro-batch-record-template:query') OR (id=900033 AND permission='mes:pro-edhr-batch-execution:query') OR (id=900025 AND permission='mes:pro-batch-record-execution:track') OR (id=900432 AND permission='mes:pro-edhr-form-fill-log:query') OR (id=900235 AND permission='mes:pro-edhr-change:query') OR (id=900260 AND permission='mes:pro-edhr-release:query')) )<>6 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='EDHR_TRACE_MENU_PERMISSION_IDENTITY_CONFLICT';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM system_menu WHERE id=900220 AND deleted=b'0') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='EDHR_TRACE_MENU_PARENT_MISSING';
 END IF;
 START TRANSACTION;
 UPDATE system_menu SET name=CASE id WHEN 900235 THEN 'eDHR变更查询' ELSE 'eDHR放行查询' END,type=3,parent_id=900025,sort=CASE id WHEN 900235 THEN 9 ELSE 10 END,path='',icon='',component='',component_name='',visible=b'0',updater='release-20261022-trace',update_time=NOW() WHERE id IN(900235,900260) AND deleted=b'0';
 UPDATE system_menu SET parent_id=900025,updater='release-20261022-trace',update_time=NOW() WHERE parent_id=900260 AND type=3 AND deleted=b'0';
 IF (SELECT COUNT(*) FROM system_menu WHERE id IN(900365,900033,900025,900432) AND parent_id=900220 AND type=2 AND visible=b'1' AND deleted=b'0')<>4
 OR (SELECT COUNT(*) FROM system_menu WHERE id IN(900235,900260) AND parent_id=900025 AND type=3 AND visible=b'0' AND path='' AND component='' AND component_name='' AND deleted=b'0')<>2
 OR EXISTS(SELECT 1 FROM system_menu WHERE parent_id=900260 AND type=3 AND deleted=b'0') THEN
  ROLLBACK;
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='EDHR_TRACE_MENU_FORWARD_POSTCONDITION_FAILED';
 END IF;
 COMMIT;
END$$
DELIMITER ;
CALL release_20261022_trace_menu();
DROP PROCEDURE release_20261022_trace_menu;
