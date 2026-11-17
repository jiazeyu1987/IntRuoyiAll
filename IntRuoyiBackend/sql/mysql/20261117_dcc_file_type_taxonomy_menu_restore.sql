-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_dcc_file_type_taxonomy,20260829_registration_certificate_management_menu_hierarchy; type=menu; riskLevel=medium
-- 20260719 and 20260816 reused 990230. Never modify that registration-certificate menu.
-- Qualification follows 20260630_dcc_admin_full_config_menu: preserve existing category:manage grants.
-- Do not infer qualification from tenant-admin, project-admin, role names, or registration-certificate grants.
-- No taxonomy/category business data or user-role membership is created.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS restore_dcc_taxonomy_menu_20261117;
DELIMITER $$
CREATE PROCEDURE restore_dcc_taxonomy_menu_20261117()
BEGIN
 DECLARE target_id BIGINT DEFAULT 605072160;
 DECLARE parent_menu_id BIGINT;
 DECLARE node_id BIGINT;
 DECLARE node_count INT DEFAULT 0;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 IF (SELECT COUNT(*) FROM system_menu WHERE path='/mdm' AND parent_id=0 AND status=0 AND deleted=b'0')<>1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unique enabled /mdm root menu required';
 END IF;
 SELECT id INTO parent_menu_id FROM system_menu WHERE path='/mdm' AND parent_id=0 AND status=0 AND deleted=b'0';
 IF (SELECT COUNT(*) FROM system_menu WHERE component='dcc/controlled-file/basic-data/file-type-taxonomy/index' AND deleted=b'0')>1 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate taxonomy menu components require explicit reconciliation';
 END IF;
 IF EXISTS(SELECT 1 FROM system_menu WHERE component='dcc/controlled-file/basic-data/file-type-taxonomy/index' AND deleted=b'0') THEN
  SELECT id INTO target_id FROM system_menu WHERE component='dcc/controlled-file/basic-data/file-type-taxonomy/index' AND deleted=b'0';
 ELSEIF EXISTS(SELECT 1 FROM system_menu WHERE id=target_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reserved taxonomy menu ID 605072160 occupied';
 END IF;
 IF target_id=990230 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Legacy taxonomy ID 990230 requires explicit reconciliation';
 END IF;
 IF EXISTS(SELECT 1 FROM system_menu WHERE parent_id=parent_menu_id AND path='file-type-taxonomy' AND id<>target_id AND deleted=b'0') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Taxonomy route occupied by another menu';
 END IF;
 DROP TEMPORARY TABLE IF EXISTS dcc_taxonomy_qualified_roles;
 CREATE TEMPORARY TABLE dcc_taxonomy_qualified_roles(role_id BIGINT,tenant_id BIGINT,PRIMARY KEY(role_id,tenant_id));
 INSERT INTO dcc_taxonomy_qualified_roles
 SELECT DISTINCT r.id,r.tenant_id FROM system_role r
 JOIN system_role_menu rm ON rm.role_id=r.id AND rm.tenant_id=r.tenant_id AND rm.deleted=b'0'
 JOIN system_menu m ON m.id=rm.menu_id AND m.permission='dcc:controlled-file:category:manage' AND m.status=0 AND m.deleted=b'0'
 WHERE r.status=0 AND r.deleted=b'0';
 IF NOT EXISTS(SELECT 1 FROM dcc_taxonomy_qualified_roles) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='No existing active category-manage role; business authorization required';
 END IF;
 IF EXISTS(SELECT 1 FROM dcc_taxonomy_qualified_roles q LEFT JOIN system_tenant t ON t.id=q.tenant_id AND t.deleted=b'0'
           LEFT JOIN system_tenant_package p ON p.id=t.package_id AND p.deleted=b'0'
           WHERE q.tenant_id<>0 AND (t.id IS NULL OR (t.package_id<>0 AND (p.id IS NULL OR p.menu_ids IS NULL OR JSON_TYPE(p.menu_ids)<>'ARRAY')))) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Qualified tenant package missing or menu_ids is not a JSON array';
 END IF;
 INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
 SELECT target_id,'DCC文件分类','dcc:controlled-file:category:manage',2,22,parent_menu_id,'file-type-taxonomy','ep:collection-tag',
 'dcc/controlled-file/basic-data/file-type-taxonomy/index','DccFileTypeTaxonomyBasicDataPage',0,b'1',b'1',b'1','taxonomy-restore',NOW(),'taxonomy-restore',NOW(),b'0'
 WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE id=target_id);
 UPDATE system_menu SET parent_id=parent_menu_id,path='file-type-taxonomy',permission='dcc:controlled-file:category:manage',status=0,visible=b'1',updater='taxonomy-restore',update_time=NOW()
 WHERE id=target_id AND component='dcc/controlled-file/basic-data/file-type-taxonomy/index' AND deleted=b'0';
 WHILE node_count<2 DO
  SET node_id=IF(node_count=0,parent_menu_id,target_id);
  INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater)
  SELECT q.role_id,node_id,q.tenant_id,'taxonomy-restore','taxonomy-restore' FROM dcc_taxonomy_qualified_roles q
  WHERE NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=q.role_id AND rm.tenant_id=q.tenant_id AND rm.menu_id=node_id AND rm.deleted=b'0');
  UPDATE system_tenant_package p SET p.menu_ids=JSON_ARRAY_APPEND(p.menu_ids,'$',node_id),p.updater='taxonomy-restore',p.update_time=NOW()
  WHERE p.deleted=b'0' AND EXISTS(SELECT 1 FROM system_tenant t JOIN dcc_taxonomy_qualified_roles q ON q.tenant_id=t.id WHERE t.package_id=p.id AND t.deleted=b'0')
  AND NOT JSON_CONTAINS(p.menu_ids,CAST(node_id AS CHAR),'$');
  SET node_count=node_count+1;
 END WHILE;
 COMMIT;
 DROP TEMPORARY TABLE dcc_taxonomy_qualified_roles;
END$$
DELIMITER ;
CALL restore_dcc_taxonomy_menu_20261117();
DROP PROCEDURE restore_dcc_taxonomy_menu_20261117;
