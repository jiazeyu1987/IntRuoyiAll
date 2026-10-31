-- release-target-preflight: migrationId=20261008_profile_business_config_menus; allowedEnvironments=test,backup,prod
-- Read-only data prerequisites; evaluated immediately before this migration.
-- S1 frontend atomicity, exact-scope backup and explicit DB-write approval remain mandatory external gates; active dynamic grants require separately approved source resync.
WITH defs(id,root_path,path,component,route,permission) AS(
 SELECT 991501,'/mes' COLLATE utf8mb4_unicode_ci,'pro/edhr-config' COLLATE utf8mb4_unicode_ci,'mes/pro/edhr-config/index' COLLATE utf8mb4_unicode_ci,'MesEdhrGlobalConfig' COLLATE utf8mb4_unicode_ci,'mes:pro-batch-record-execution:golden-finger' COLLATE utf8mb4_unicode_ci
 UNION ALL SELECT 991502,'/erp','sync/config','erp/sync/config/index','ErpTableAutoSyncConfig','mes:pro-batch-record-execution:golden-finger'
 UNION ALL SELECT 991503,'/dcc','registration-certificate/config','dcc/registration-certificate/config/index','DccRegistrationCertificateConfig','dcc:registration-certificate:config:query'
 UNION ALL SELECT 991504,'/infra','config/basic','infra/config/basic/index','InfraBasicConfig','infra:config:query'),
 roots AS(SELECT d.id,MIN(m.id) parent FROM defs d JOIN system_menu m ON m.deleted=0 AND m.status=0 AND m.type=1 AND m.visible=1 AND COALESCE(m.permission,'')='' AND m.parent_id=0 AND BINARY m.path=BINARY d.root_path GROUP BY d.id HAVING COUNT(*)=1)
SELECT CASE WHEN
  (FIND_IN_SET('STRICT_TRANS_TABLES',@@SESSION.sql_mode)>0 OR FIND_IN_SET('STRICT_ALL_TABLES',@@SESSION.sql_mode)>0)
  AND ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN('system_menu','system_role_menu','system_tenant_package') AND ENGINE='InnoDB')=3)
  AND (NOT EXISTS(SELECT 1 FROM system_tenant_package p WHERE p.deleted=0 AND
 CASE WHEN p.menu_ids IS NULL OR NOT JSON_VALID(p.menu_ids) THEN TRUE
 WHEN JSON_TYPE(CAST(p.menu_ids AS JSON))<>'ARRAY' THEN TRUE
 ELSE EXISTS(SELECT 1 FROM JSON_TABLE(p.menu_ids,'$[*]' COLUMNS(n FOR ORDINALITY)) j
 WHERE JSON_TYPE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']')))<>'INTEGER'
 OR CAST(JSON_UNQUOTE(JSON_EXTRACT(p.menu_ids,CONCAT('$[',j.n-1,']'))) AS DECIMAL(65,0)) NOT BETWEEN 0 AND 9223372036854775807) END))
  AND (NOT EXISTS(SELECT 1 FROM system_entitlement_grant WHERE deleted=0 AND status='ACTIVE' AND active_claim_count>0 AND permission_code IN('mes:pro-batch-record-execution:golden-finger','dcc:registration-certificate:config:query','infra:config:query','infra:config:update')))
  AND ((SELECT COUNT(*) FROM roots)=4)
  AND (NOT EXISTS(SELECT 1 FROM defs d JOIN roots r ON r.id=d.id JOIN system_menu m ON m.id=d.id OR (m.deleted=0 AND (BINARY m.component_name=BINARY d.route OR(m.parent_id=r.parent AND BINARY m.path=BINARY d.path))) WHERE NOT COALESCE(m.id=d.id AND m.deleted=0 AND m.status=0 AND m.type=2 AND m.parent_id=r.parent AND (BINARY m.path <=> BINARY d.path) AND (BINARY m.component <=> BINARY d.component) AND (BINARY m.component_name <=> BINARY d.route) AND (BINARY m.permission <=> BINARY d.permission) AND m.visible=1 AND m.keep_alive=0 AND (BINARY m.creator <=> BINARY 'profile-config-s1'),FALSE)))
  AND (NOT EXISTS(SELECT 1 FROM defs d WHERE NOT EXISTS(SELECT 1 FROM system_menu m WHERE m.deleted=0 AND m.status=0 AND BINARY m.permission=BINARY d.permission AND m.id NOT IN(991501,991502,991503,991504))))
THEN 'TARGET_PREFLIGHT_PASS:20261008_profile_business_config_menus' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261008_profile_business_config_menus' END;
