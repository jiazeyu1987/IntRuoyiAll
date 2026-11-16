-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260630_dcc_admin_full_config_menu; type=permission; riskLevel=medium
-- Establish the explicit tenant-1 document-control role prerequisite, without assigning users.
SET NAMES utf8mb4;
DELIMITER //
CREATE PROCEDURE release_dcc_document_control_role()
BEGIN
  DECLARE role_id_value BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF (SELECT COUNT(*) FROM system_tenant WHERE id=1 AND deleted=0 AND status=0)<>1
     OR (SELECT COUNT(*) FROM system_menu WHERE id IN (6800,6807) AND deleted=0 AND status=0)<>2
     OR (SELECT COUNT(*) FROM system_menu WHERE id=6807 AND permission='dcc:controlled-file:query' AND parent_id=6800 AND type=2)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Document control tenant and browser navigation required';
  END IF;
  IF (SELECT COUNT(*) FROM system_role WHERE tenant_id=1 AND code='doc_control')>1
     OR EXISTS(SELECT 1 FROM system_role WHERE tenant_id=1 AND code='doc_control'
       AND (BINARY code<>BINARY 'doc_control' OR deleted<>0 OR status<>0 OR type<>2)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Document control role namespace conflicts';
  END IF;
  START TRANSACTION;
  INSERT INTO system_role(name,code,sort,data_scope,data_scope_dept_ids,status,type,remark,creator,updater,deleted,tenant_id)
    SELECT '文控人员','doc_control',91,5,'',0,2,'受控文件浏览与下发；成员须通过用户角色管理显式分配',
      'release-role','release-role',0,1
    WHERE NOT EXISTS(SELECT 1 FROM system_role WHERE tenant_id=1 AND code='doc_control');
  SELECT id INTO role_id_value FROM system_role WHERE tenant_id=1 AND BINARY code=BINARY 'doc_control' AND deleted=0 AND status=0;
  INSERT INTO system_role_menu(role_id,menu_id,creator,updater,deleted,tenant_id)
    SELECT role_id_value,m.id,'release-role','release-role',0,1 FROM system_menu m
    WHERE m.id IN (6800,6807) AND NOT EXISTS(SELECT 1 FROM system_role_menu x
      WHERE x.role_id=role_id_value AND x.menu_id=m.id AND x.tenant_id=1 AND x.deleted=0);
  COMMIT;
END//
DELIMITER ;
CALL release_dcc_document_control_role();
DROP PROCEDURE release_dcc_document_control_role;
