-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260818_dcc_registration_certificate_reminder,20260626_dcc_basic_data_global_submenu; type=menu; riskLevel=medium; requiresTargetPreflight=true; supersedes=20260816_dcc_registration_certificate_menu; applyOrder=10
-- Preserve taxonomy 990230 and print 990240. New registration list/renewal identities are 991601/991602.
-- Existing exact routes are left unchanged, including the later management hierarchy. No role/package grant is inherited.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP PROCEDURE IF EXISTS ensure_dcc_registration_menu_identity_20261023;
DELIMITER $$
CREATE PROCEDURE ensure_dcc_registration_menu_identity_20261023()
BEGIN
 DECLARE valid_identity BOOLEAN DEFAULT FALSE;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION
 BEGIN
  ROLLBACK;
  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_registration_identity;
  RESIGNAL;
 END;
 START TRANSACTION;
 CREATE TEMPORARY TABLE tmp_dcc_registration_identity(
 id bigint PRIMARY KEY,name varchar(64),permission varchar(128),type int,sort int,parent_id bigint,
 path varchar(255),component varchar(255),component_name varchar(128),visible bit
 ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
 INSERT INTO tmp_dcc_registration_identity(id,name,permission,type,sort,parent_id,path,component,component_name,visible) VALUES
(991601,'注册证','dcc:registration-certificate:query-current',2,22,990200,'registration-certificate','dcc/registration-certificate/index/index','DccRegistrationCertificateIndex',1),
(990231,'注册证详情','dcc:registration-certificate:query-current',2,1,990200,'registration-certificate/detail/:id','dcc/registration-certificate/detail/index','DccRegistrationCertificateDetail',0),
(990232,'注册证履历','dcc:registration-certificate:query-current',2,2,990200,'registration-certificate/history/:id','dcc/registration-certificate/history/index','DccRegistrationCertificateHistory',0),
(990233,'注册证新增','dcc:registration-certificate:create',3,10,991601,'','','',1),
(990234,'注册证修改','dcc:registration-certificate:update',3,20,991601,'','','',1),
(990235,'删除注册证草稿','dcc:registration-certificate:delete-draft',3,30,991601,'','','',1),
(990236,'注册证正式化','dcc:registration-certificate:formalize',3,40,991601,'','','',1),
(990237,'注册证访问申请','dcc:registration-certificate:access-request:create',3,50,991601,'','','',1),
(990246,'注册证访问审批','dcc:registration-certificate:access-request:approve',3,55,991601,'','','',1),
(990247,'注册证上传提交','dcc:registration-certificate:upload:create',3,56,991601,'','','',1),
(990248,'注册证上传审批','dcc:registration-certificate:upload:approve',3,57,991601,'','','',1),
(990238,'注册证配置查询','dcc:registration-certificate:config:query',3,60,991601,'','','',1),
(990239,'注册证配置修改','dcc:registration-certificate:config:update',3,70,991601,'','','',1),
(991602,'注册证延续上传','dcc:registration-certificate:renewal:upload',3,80,991601,'','','',1),
(990241,'注册证延续作废','dcc:registration-certificate:renewal:void',3,90,991601,'','','',1),
(990242,'注册证变更提交','dcc:registration-certificate:change:submit',3,100,991601,'','','',1),
(990243,'注册证作废','dcc:registration-certificate:void',3,110,991601,'','','',1),
(990244,'注册证支持文件上传','dcc:registration-certificate:supporting-document:upload',3,120,991601,'','','',1),
(990245,'注册证支持文件确认','dcc:registration-certificate:supporting-document:confirm',3,130,991601,'','','',1);
 WITH defs(id,name,permission,type,sort,parent_id,path,component,component_name,visible) AS (
SELECT 991601,'注册证','dcc:registration-certificate:query-current',2,22,990200,'registration-certificate','dcc/registration-certificate/index/index','DccRegistrationCertificateIndex',1
UNION ALL
SELECT 990231,'注册证详情','dcc:registration-certificate:query-current',2,1,990200,'registration-certificate/detail/:id','dcc/registration-certificate/detail/index','DccRegistrationCertificateDetail',0
UNION ALL
SELECT 990232,'注册证履历','dcc:registration-certificate:query-current',2,2,990200,'registration-certificate/history/:id','dcc/registration-certificate/history/index','DccRegistrationCertificateHistory',0
UNION ALL
SELECT 990233,'注册证新增','dcc:registration-certificate:create',3,10,991601,'','','',1
UNION ALL
SELECT 990234,'注册证修改','dcc:registration-certificate:update',3,20,991601,'','','',1
UNION ALL
SELECT 990235,'删除注册证草稿','dcc:registration-certificate:delete-draft',3,30,991601,'','','',1
UNION ALL
SELECT 990236,'注册证正式化','dcc:registration-certificate:formalize',3,40,991601,'','','',1
UNION ALL
SELECT 990237,'注册证访问申请','dcc:registration-certificate:access-request:create',3,50,991601,'','','',1
UNION ALL
SELECT 990246,'注册证访问审批','dcc:registration-certificate:access-request:approve',3,55,991601,'','','',1
UNION ALL
SELECT 990247,'注册证上传提交','dcc:registration-certificate:upload:create',3,56,991601,'','','',1
UNION ALL
SELECT 990248,'注册证上传审批','dcc:registration-certificate:upload:approve',3,57,991601,'','','',1
UNION ALL
SELECT 990238,'注册证配置查询','dcc:registration-certificate:config:query',3,60,991601,'','','',1
UNION ALL
SELECT 990239,'注册证配置修改','dcc:registration-certificate:config:update',3,70,991601,'','','',1
UNION ALL
SELECT 991602,'注册证延续上传','dcc:registration-certificate:renewal:upload',3,80,991601,'','','',1
UNION ALL
SELECT 990241,'注册证延续作废','dcc:registration-certificate:renewal:void',3,90,991601,'','','',1
UNION ALL
SELECT 990242,'注册证变更提交','dcc:registration-certificate:change:submit',3,100,991601,'','','',1
UNION ALL
SELECT 990243,'注册证作废','dcc:registration-certificate:void',3,110,991601,'','','',1
UNION ALL
SELECT 990244,'注册证支持文件上传','dcc:registration-certificate:supporting-document:upload',3,120,991601,'','','',1
UNION ALL
SELECT 990245,'注册证支持文件确认','dcc:registration-certificate:supporting-document:confirm',3,130,991601,'','','',1
)
SELECT (EXISTS(SELECT 1 FROM system_menu WHERE id=990200 AND deleted=0 AND type=1 AND parent_id=0 AND path='/mdm')
AND (SELECT COUNT(*) FROM system_menu WHERE deleted=0 AND (id=990200 OR path='/mdm'))=1
AND NOT EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND (menu_ids IS NULL OR JSON_VALID(menu_ids)=0))
AND NOT EXISTS(
 SELECT 1 FROM defs d JOIN system_menu m ON m.id=d.id
 WHERE NOT COALESCE(m.deleted=0 AND m.status=0 AND m.type=d.type
  AND BINARY m.permission=BINARY d.permission AND BINARY m.component=BINARY d.component
  AND BINARY m.component_name=BINARY d.component_name AND m.visible=d.visible
  AND ((m.parent_id=d.parent_id AND BINARY m.path=BINARY d.path)
    OR (d.type=2 AND m.parent_id=990253 AND BINARY m.path=BINARY CONCAT('/mdm/',d.path))),FALSE))
AND NOT EXISTS(
 SELECT 1 FROM system_menu m WHERE m.deleted=0
 AND ((m.permission='dcc:registration-certificate:query-current' AND m.id NOT IN(991601,990231,990232))
   OR EXISTS(SELECT 1 FROM defs d WHERE d.type=3 AND BINARY m.permission=BINARY d.permission AND m.id<>d.id)
   OR EXISTS(SELECT 1 FROM defs d WHERE d.type=2 AND (BINARY m.path=BINARY d.path OR BINARY m.path=BINARY CONCAT('/mdm/',d.path)) AND m.id<>d.id)))
AND NOT EXISTS(SELECT 1 FROM defs d JOIN system_role_menu rm ON rm.menu_id=d.id WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.id=d.id))
AND NOT EXISTS(SELECT 1 FROM defs d JOIN system_tenant_package p ON JSON_CONTAINS(p.menu_ids,CAST(d.id AS JSON),'$') WHERE p.deleted=0 AND NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.id=d.id))) INTO valid_identity;
 IF NOT COALESCE(valid_identity,FALSE) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Registration menu identity conflict; existing menu identities must be preserved';
 END IF;
 INSERT INTO system_menu(id,name,permission,type,sort,parent_id,path,icon,component,component_name,
  status,visible,keep_alive,always_show,creator,create_time,updater,update_time,deleted)
 SELECT d.id,d.name,d.permission,d.type,d.sort,d.parent_id,d.path,
  CASE WHEN d.id=991601 THEN 'ep:document-checked' WHEN d.id=990231 THEN 'ep:document' WHEN d.id=990232 THEN 'ep:clock' ELSE '' END,
  d.component,d.component_name,0,d.visible,1,IF(d.visible=0,0,1),
  'dcc-registration-menu-identity',NOW(),'dcc-registration-menu-identity',NOW(),0
 FROM tmp_dcc_registration_identity d WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.id=d.id);
 DROP TEMPORARY TABLE IF EXISTS tmp_dcc_registration_identity;
 COMMIT;
END$$
DELIMITER ;
CALL ensure_dcc_registration_menu_identity_20261023();
DROP PROCEDURE IF EXISTS ensure_dcc_registration_menu_identity_20261023;
