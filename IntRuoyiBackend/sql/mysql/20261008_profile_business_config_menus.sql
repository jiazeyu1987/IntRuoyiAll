-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_erp_kingdee_sync_menu,20260816_dcc_registration_certificate_menu,20260718_system_entitlement_management; type=menu; riskLevel=medium
-- NOT EXECUTED. Publish atomically with the S1 frontend, after exact scope backup,
-- menu schema/parent/grant checks and explicit database-write authorization.
-- No business configuration values, users, roles or existing permissions change.
DELIMITER $$
CREATE PROCEDURE migrate_profile_business_config_menus()
BEGIN
  DECLARE v_count INT;
  DECLARE v_id BIGINT;
  DECLARE v_parent BIGINT;
  DECLARE v_root VARCHAR(64);
  DECLARE v_name VARCHAR(64);
  DECLARE v_path VARCHAR(128);
  DECLARE v_component VARCHAR(255);
  DECLARE v_route_name VARCHAR(128);
  DECLARE v_permission VARCHAR(128);
  DECLARE v_done BOOLEAN DEFAULT FALSE;
  DECLARE c CURSOR FOR
    SELECT id, root_path, name, path, component, route_name, permission
    FROM profile_config_menu_plan ORDER BY id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  CREATE TEMPORARY TABLE profile_config_menu_plan (
    id BIGINT PRIMARY KEY,
    root_path VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    name VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    path VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    component VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    route_name VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    permission VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
  );
  INSERT INTO profile_config_menu_plan VALUES
    (991501, '/mes', 'eDHR配置', 'pro/edhr-config', 'mes/pro/edhr-config/index', 'MesEdhrGlobalConfig', 'mes:pro-batch-record-execution:golden-finger'),
    (991502, '/erp', '自动同步配置', 'sync/config', 'erp/sync/config/index', 'ErpTableAutoSyncConfig', 'mes:pro-batch-record-execution:golden-finger'),
    (991503, '/dcc', '注册证配置', 'registration-certificate/config', 'dcc/registration-certificate/config/index', 'DccRegistrationCertificateConfig', 'dcc:registration-certificate:config:query'),
    (991504, '/infra', '基础配置', 'config/basic', 'infra/config/basic/index', 'InfraBasicConfig', 'infra:config:query');

  -- Required for transactional rollback and to prevent VARCHAR truncation being treated as success.
  IF FIND_IN_SET('STRICT_TRANS_TABLES', @@SESSION.sql_mode)=0 AND FIND_IN_SET('STRICT_ALL_TABLES', @@SESSION.sql_mode)=0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S1 prerequisite: strict SQL mode required';
  END IF;
  SELECT COUNT(*) INTO v_count FROM information_schema.tables
    WHERE table_schema=DATABASE() AND table_name IN ('system_menu','system_role_menu','system_tenant_package') AND engine='InnoDB';
  IF v_count <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S1 prerequisite: all target tables must exist and use InnoDB';
  END IF;
  -- This migration publishes menu definitions only. Never silently omit dynamic grants.
  -- Missing entitlement table/columns also fails instead of assuming zero.
  IF EXISTS (SELECT 1 FROM system_entitlement_grant WHERE deleted=b'0' AND status='ACTIVE' AND active_claim_count>0
      AND permission_code IN ('mes:pro-batch-record-execution:golden-finger','dcc:registration-certificate:config:query','infra:config:query','infra:config:update')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S1 prerequisite: dynamic config grants require separately approved source resync';
  END IF;
  START TRANSACTION;
  IF EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted=b'0' AND (menu_ids IS NULL OR NOT JSON_VALID(menu_ids) OR JSON_TYPE(CAST(menu_ids AS JSON)) <> 'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S1 prerequisite: tenant package menu_ids must be valid JSON';
  END IF;
  OPEN c;
  config_loop: LOOP
    FETCH c INTO v_id, v_root, v_name, v_path, v_component, v_route_name, v_permission;
    IF v_done THEN LEAVE config_loop; END IF;
    SELECT COUNT(*), MIN(id) INTO v_count, v_parent FROM system_menu
      WHERE deleted=b'0' AND status=0 AND type=1 AND visible=b'1' AND (permission IS NULL OR permission='') AND parent_id=0 AND BINARY path=BINARY v_root;
    IF v_count <> 1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S1 prerequisite: unique active module root missing';
    END IF;
    IF EXISTS (SELECT 1 FROM system_menu WHERE
        (id=v_id OR (deleted=b'0' AND (BINARY component_name=BINARY v_route_name OR
        (parent_id=v_parent AND BINARY path=BINARY v_path)))) AND
        NOT COALESCE((id=v_id AND deleted=b'0' AND status=0 AND type=2 AND parent_id=v_parent AND
          (BINARY path <=> BINARY v_path) AND (BINARY component <=> BINARY v_component) AND
          (BINARY component_name <=> BINARY v_route_name) AND (BINARY permission <=> BINARY v_permission) AND
          visible=b'1' AND keep_alive=b'0' AND (BINARY creator <=> BINARY 'profile-config-s1')), FALSE)) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S1 prerequisite: menu identity collision or deployed menu drift';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND status=0 AND
      BINARY permission=BINARY v_permission AND id NOT IN (991501,991502,991503,991504)) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='S1 prerequisite: existing permission source missing';
    END IF;
    INSERT INTO system_menu
      (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
      SELECT v_id,v_name,v_permission,2,95,v_parent,v_path,'ep:setting',v_component,v_route_name,0,b'1',b'0',b'0','profile-config-s1','profile-config-s1',b'0'
      WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id=v_id);

    -- Authorization/package changes are deliberately excluded: use approved formal
    -- permission commands with real before/after audit, reason and idempotencyKey.
  END LOOP;
  CLOSE c;
  COMMIT;
  DROP TEMPORARY TABLE profile_config_menu_plan;
END$$
DELIMITER ;
CALL migrate_profile_business_config_menus();
DROP PROCEDURE migrate_profile_business_config_menus;
