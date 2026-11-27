-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260626_dcc_basic_data_global_submenu,20260829_registration_certificate_management_menu_hierarchy; type=menu; riskLevel=medium
-- Reuse project-code query/update permissions; only existing project administrators receive this entry.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS add_dcc_folder_template_menu;
DELIMITER $$
CREATE PROCEDURE add_dcc_folder_template_menu()
BEGIN
  DECLARE root_id BIGINT;
  DECLARE catalog_sort INT;
  DECLARE query_menu_id BIGINT;
  DECLARE update_menu_id BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  IF (SELECT COUNT(*) FROM system_menu WHERE path='/mdm' AND type=1 AND status=0 AND deleted=0)<>1
    OR (SELECT COUNT(*) FROM system_menu WHERE permission='dcc:project-code:query' AND status=0 AND deleted=0)<>1
    OR (SELECT COUNT(*) FROM system_menu WHERE permission='dcc:project-code:update' AND status=0 AND deleted=0)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Master data root or project-code permissions missing or duplicated';
  END IF;
  SELECT id INTO root_id FROM system_menu WHERE path='/mdm' AND type=1 AND status=0 AND deleted=0;
  SELECT id INTO query_menu_id FROM system_menu WHERE permission='dcc:project-code:query' AND status=0 AND deleted=0;
  SELECT id INTO update_menu_id FROM system_menu WHERE permission='dcc:project-code:update' AND status=0 AND deleted=0;
  IF (SELECT COUNT(*) FROM system_menu WHERE parent_id=root_id AND component='dcc/controlled-file/basic-data/product-catalog/index' AND status=0 AND deleted=0)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unique enabled product catalog menu required';
  END IF;
  SELECT sort INTO catalog_sort FROM system_menu WHERE parent_id=root_id AND component='dcc/controlled-file/basic-data/product-catalog/index' AND status=0 AND deleted=0;
  IF NOT EXISTS(SELECT 1 FROM system_menu WHERE parent_id=root_id AND deleted=0 AND status=0 AND sort>catalog_sort
      AND (path IN ('registration-certificate-management','/mdm/registration-certificate','registration-certificate') OR component='dcc/registration-certificate/index/index')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registration certificate menu must follow product catalog';
  END IF;
  IF EXISTS(SELECT 1 FROM system_menu WHERE id=2026112501 AND (deleted<>0 OR parent_id<>root_id OR path<>'folder-template' OR component<>'dcc/controlled-file/basic-data/folder-template/index'))
    OR EXISTS(SELECT 1 FROM system_menu WHERE deleted=0 AND id<>2026112501 AND (component='dcc/controlled-file/basic-data/folder-template/index' OR (parent_id=root_id AND path='folder-template'))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Folder template menu identity conflict';
  END IF;
  IF EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND (menu_ids IS NULL OR NOT JSON_VALID(menu_ids))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Invalid tenant package menu JSON';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM system_menu WHERE id=2026112501) THEN
    UPDATE system_menu SET sort=sort+1,updater='folder-template',update_time=NOW()
      WHERE parent_id=root_id AND deleted=0 AND sort>catalog_sort;
    INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
      VALUES(2026112501,'文件夹模板','',2,catalog_sort+1,root_id,'folder-template','ep:folder',
        'dcc/controlled-file/basic-data/folder-template/index','DccFolderTemplateBasicDataPage',0,1,1,1,'folder-template',NOW(),'folder-template',NOW(),0);
  END IF;
  INSERT INTO system_role_menu(role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
    SELECT r.id,2026112501,'folder-template',NOW(),'folder-template',NOW(),0,r.tenant_id
    FROM system_role r WHERE r.deleted=0 AND r.status=0
      AND r.code IN ('dcc_project_code_admin','tenant_admin','super_admin')
      AND EXISTS(SELECT 1 FROM system_role_menu m WHERE m.role_id=r.id AND m.tenant_id=r.tenant_id AND m.menu_id=query_menu_id AND m.deleted=0)
      AND EXISTS(SELECT 1 FROM system_role_menu m WHERE m.role_id=r.id AND m.tenant_id=r.tenant_id AND m.menu_id=update_menu_id AND m.deleted=0)
      AND NOT EXISTS(SELECT 1 FROM system_role_menu m WHERE m.role_id=r.id AND m.tenant_id=r.tenant_id AND m.menu_id=2026112501 AND m.deleted=0);
  UPDATE system_tenant_package SET menu_ids=JSON_ARRAY_APPEND(CAST(menu_ids AS JSON),'$',2026112501),updater='folder-template',update_time=NOW()
    WHERE deleted=0 AND JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(query_menu_id AS JSON),'$')
      AND JSON_CONTAINS(CAST(menu_ids AS JSON),CAST(update_menu_id AS JSON),'$')
      AND NOT JSON_CONTAINS(CAST(menu_ids AS JSON),'2026112501','$');
  COMMIT;
END$$
DELIMITER ;
CALL add_dcc_folder_template_menu();
DROP PROCEDURE add_dcc_folder_template_menu;
