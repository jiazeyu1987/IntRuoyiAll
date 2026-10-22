-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260718_mes_route_version_withdraw_reopen_permission_menu; type=schema; riskLevel=medium
-- 工艺库不建立工艺版本；既有流程/订单快照保持原样，由页面预览后启动无损初始化。
CREATE TABLE IF NOT EXISTS mes_pro_craft_sequence (
  tenant_id BIGINT NOT NULL,
  last_number BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mes_pro_craft (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  process_id BIGINT NOT NULL,
  parameters_json LONGTEXT NOT NULL,
  fingerprint CHAR(64) NOT NULL,
  creator VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_craft_code (tenant_id, code),
  KEY idx_craft_process (tenant_id, process_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mes_pro_craft_change (
  id BIGINT NOT NULL AUTO_INCREMENT,
  craft_id BIGINT NULL,
  change_type VARCHAR(24) NOT NULL,
  status VARCHAR(24) NOT NULL,
  before_json LONGTEXT NULL,
  after_json LONGTEXT NULL,
  candidate_ids_json LONGTEXT NOT NULL,
  candidate_hashes_json LONGTEXT NOT NULL,
  reason VARCHAR(512) NOT NULL,
  creator VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  tenant_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_craft_change (tenant_id, craft_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜单与工艺流程同级；按既有流程管理权限派生授权，避免全角色、全套餐授权。
DROP PROCEDURE IF EXISTS install_mes_craft_menu;
DELIMITER $$
CREATE PROCEDURE install_mes_craft_menu()
BEGIN
  DECLARE route_menu BIGINT;
  DECLARE craft_menu BIGINT;
  SELECT id INTO route_menu FROM system_menu
    WHERE component='mes/pro/route/index' AND deleted=0 LIMIT 1;
  IF route_menu IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='MES route menu is required for craft library';
  END IF;
  -- 既有候选配置接口已使用此权限，历史菜单迁移尚未登记；按路线编辑权补齐授权。
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show)
  SELECT '工艺路线版本编辑','mes:pro-route:version-update',3,17,route_menu,'','',0,1,1,1
  WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='mes:pro-route:version-update' AND deleted=0);
  INSERT INTO system_role_menu (role_id,menu_id,creator,updater,tenant_id)
  SELECT DISTINCT rm.role_id,target.id,'1','1',rm.tenant_id
  FROM system_role_menu rm
  JOIN system_menu source ON source.id=rm.menu_id AND source.deleted=0 AND source.permission='mes:pro-route:update'
  JOIN system_menu target ON target.deleted=0 AND target.permission='mes:pro-route:version-update'
  WHERE rm.deleted=0 AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
    WHERE existing.role_id=rm.role_id AND existing.menu_id=target.id AND existing.tenant_id=rm.tenant_id AND existing.deleted=0);
  INSERT INTO system_menu
    (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show)
    SELECT '工艺库','mes:pro-craft:query',2,sort+1,parent_id,'craft','ep:collection','mes/pro/craft/index','MesProCraft',0,1,1,1
    FROM system_menu WHERE id=route_menu
    AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='mes:pro-craft:query' AND deleted=0);
  SELECT id INTO craft_menu FROM system_menu WHERE permission='mes:pro-craft:query' AND deleted=0 LIMIT 1;
  INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,status,visible,keep_alive,always_show)
  SELECT p.name,p.permission,3,p.sort,craft_menu,'','',0,1,1,1
  FROM (
    SELECT '新增工艺' name,'mes:pro-craft:create' permission,1 sort
    UNION ALL SELECT '修改工艺并全部升级','mes:pro-craft:update',2
    UNION ALL SELECT '初始化工艺库','mes:pro-craft:initialize',3
  ) p WHERE NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.permission=p.permission AND m.deleted=0);

  INSERT INTO system_role_menu (role_id,menu_id,creator,updater,tenant_id)
  SELECT DISTINCT rm.role_id,target.id,'1','1',rm.tenant_id
  FROM system_role_menu rm
  JOIN system_menu source ON source.id=rm.menu_id AND source.deleted=0
  JOIN system_menu target ON target.deleted=0 AND (
    (target.permission='mes:pro-craft:query' AND rm.menu_id=route_menu)
    OR (target.permission='mes:pro-craft:create' AND source.permission='mes:pro-route:version-create')
    OR (target.permission IN ('mes:pro-craft:update','mes:pro-craft:initialize')
        AND source.permission='mes:pro-route:version-update'
        AND EXISTS (SELECT 1 FROM system_role_menu cr JOIN system_menu cm ON cm.id=cr.menu_id AND cm.deleted=0
          WHERE cr.role_id=rm.role_id AND cr.tenant_id=rm.tenant_id AND cr.deleted=0 AND cm.permission='mes:pro-route:version-create')
        AND EXISTS (SELECT 1 FROM system_role_menu sr JOIN system_menu sm ON sm.id=sr.menu_id AND sm.deleted=0
          WHERE sr.role_id=rm.role_id AND sr.tenant_id=rm.tenant_id AND sr.deleted=0 AND sm.permission='mes:pro-route:version-submit')))
  WHERE rm.deleted=0 AND NOT EXISTS (
    SELECT 1 FROM system_role_menu existing WHERE existing.role_id=rm.role_id
    AND existing.menu_id=target.id AND existing.tenant_id=rm.tenant_id AND existing.deleted=0);

  -- 只扩展已包含工艺流程菜单的套餐；保留既有菜单集合。
  UPDATE system_tenant_package p
  SET p.menu_ids=JSON_MERGE_PRESERVE(CAST(p.menu_ids AS JSON),
    (SELECT JSON_ARRAYAGG(m.id) FROM system_menu m
     WHERE m.deleted=0 AND m.permission IN ('mes:pro-route:version-update','mes:pro-craft:query','mes:pro-craft:create','mes:pro-craft:update','mes:pro-craft:initialize')
     AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(m.id AS JSON),'$')))
  WHERE p.deleted=0 AND JSON_VALID(p.menu_ids)
    AND JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(route_menu AS JSON),'$')
    AND EXISTS (SELECT 1 FROM system_menu m WHERE m.deleted=0
     AND m.permission IN ('mes:pro-route:version-update','mes:pro-craft:query','mes:pro-craft:create','mes:pro-craft:update','mes:pro-craft:initialize')
     AND NOT JSON_CONTAINS(CAST(p.menu_ids AS JSON),CAST(m.id AS JSON),'$'));
END$$
DELIMITER ;
CALL install_mes_craft_menu();
DROP PROCEDURE install_mes_craft_menu;
