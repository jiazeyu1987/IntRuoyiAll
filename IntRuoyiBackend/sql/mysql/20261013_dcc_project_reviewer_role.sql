-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20261013_dcc_project_single_role_review,20260626_dcc_basic_data_global_submenu; type=config; riskLevel=medium
-- New project single-stage reviewer role. Never assigns any user, OWNER, create/update/delete or global admin authority.
-- Existing requests, configured legacy reviewer and existing role memberships remain untouched.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP PROCEDURE IF EXISTS configure_dcc_project_reviewer_role;
DELIMITER //
CREATE PROCEDURE configure_dcc_project_reviewer_role()
BEGIN
  DECLARE query_menu BIGINT;
  DECLARE review_menu BIGINT;
  DECLARE mdm_menu BIGINT;
  DECLARE catalog_menu BIGINT;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
      AND TABLE_NAME IN ('system_tenant','system_role','system_menu','system_role_menu') AND ENGINE='InnoDB')<>4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Project reviewer system identity tables missing';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu WHERE permission='dcc:project-code:query' AND deleted=0 AND status=0)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Project reviewer requires exact existing project query menu';
  END IF;
  SELECT id INTO query_menu FROM system_menu WHERE permission='dcc:project-code:query' AND deleted=0 AND status=0;
  -- The 20260626 migration places both actual pages directly below the /mdm root, without fixed-ID inference.
  IF (SELECT COUNT(*) FROM system_menu WHERE path='/mdm' AND parent_id=0 AND type=1
      AND permission='' AND deleted=0 AND status=0 AND visible=1)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Project reviewer MDM root navigation missing or drift';
  END IF;
  SELECT id INTO mdm_menu FROM system_menu WHERE path='/mdm' AND parent_id=0 AND type=1 AND deleted=0 AND status=0 AND visible=1;
  IF (SELECT COUNT(*) FROM system_menu WHERE id=query_menu AND parent_id=mdm_menu AND type=2
      AND path='project-code' AND component='dcc/controlled-file/basic-data/project-code/index'
      AND component_name='DccProjectCodeBasicDataPage' AND visible=1)<>1
     OR (SELECT COUNT(*) FROM system_menu WHERE parent_id=mdm_menu AND type=2 AND path='product-catalog'
      AND component='dcc/controlled-file/basic-data/product-catalog/index' AND component_name='DccProductCatalogBasicDataPage'
      AND permission='' AND deleted=0 AND status=0 AND visible=1)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Project reviewer query/catalog page hierarchy or component drift';
  END IF;
  SELECT id INTO catalog_menu FROM system_menu WHERE parent_id=mdm_menu AND type=2 AND path='product-catalog'
      AND component='dcc/controlled-file/basic-data/product-catalog/index' AND component_name='DccProductCatalogBasicDataPage'
      AND permission='' AND deleted=0 AND status=0 AND visible=1;
  IF (SELECT COUNT(*) FROM system_menu WHERE permission='dcc:project-code:review' AND deleted=0)>1
     OR EXISTS(SELECT 1 FROM system_menu WHERE permission='dcc:project-code:review' AND deleted=0
          AND (type<>3 OR status<>0 OR parent_id<>query_menu)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Project review action menu identity drift';
  END IF;
  IF EXISTS(SELECT 1 FROM system_role WHERE code='dcc_project_reviewer' AND deleted=0
          GROUP BY tenant_id HAVING COUNT(*)<>1)
     OR EXISTS(SELECT 1 FROM system_role WHERE code='dcc_project_reviewer' AND deleted=0
          AND (BINARY code<>BINARY 'dcc_project_reviewer' OR name<>'项目审核员' OR type<>2 OR data_scope<>5)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Project reviewer role identity or scope drift';
  END IF;
  START TRANSACTION;
  INSERT INTO system_menu(name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
    SELECT '项目审核','dcc:project-code:review',3,9,query_menu,'','','','',0,1,1,1,'dcc-role-migration','dcc-role-migration',0
    WHERE NOT EXISTS(SELECT 1 FROM system_menu WHERE permission='dcc:project-code:review' AND deleted=0);
  SELECT id INTO review_menu FROM system_menu WHERE permission='dcc:project-code:review' AND deleted=0;
  INSERT INTO system_role(name,code,sort,data_scope,data_scope_dept_ids,status,type,remark,creator,updater,deleted,tenant_id)
    SELECT '项目审核员','dcc_project_reviewer',90,5,'',0,2,'新项目单环节审批；通过用户角色维护显式分配成员',
           'dcc-role-migration','dcc-role-migration',0,t.id
      FROM system_tenant t WHERE t.deleted=0 AND t.status=0
       AND NOT EXISTS(SELECT 1 FROM system_role r WHERE r.tenant_id=t.id AND r.code='dcc_project_reviewer' AND r.deleted=0);
  INSERT INTO system_role_menu(role_id,menu_id,creator,updater,deleted,tenant_id)
    SELECT r.id,m.id,'dcc-role-migration','dcc-role-migration',0,r.tenant_id
      FROM system_role r JOIN system_menu m ON m.id IN (mdm_menu,catalog_menu,query_menu,review_menu)
     WHERE r.code='dcc_project_reviewer' AND r.deleted=0 AND r.status=0
       AND NOT EXISTS(SELECT 1 FROM system_role_menu x WHERE x.tenant_id=r.tenant_id AND x.role_id=r.id AND x.menu_id=m.id AND x.deleted=0);
  COMMIT;
END//
DELIMITER ;
CALL configure_dcc_project_reviewer_role();
DROP PROCEDURE configure_dcc_project_reviewer_role;
