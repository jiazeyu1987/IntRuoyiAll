-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_erp_kingdee_sync_runtime; type=schema; riskLevel=medium
-- 生产补料单只读快照；不得写入本地库存、出库、审批或生产用料清单表。

DROP PROCEDURE IF EXISTS preflight_erp_production_replenishment_list_menu;

DELIMITER $$
CREATE PROCEDURE preflight_erp_production_replenishment_list_menu()
BEGIN
  DECLARE v_parent_count int DEFAULT 0;
  DECLARE v_parent_id bigint DEFAULT NULL;

  SELECT COUNT(*), MIN(id)
  INTO v_parent_count, v_parent_id
  FROM system_menu
  WHERE deleted = b'0' AND path = 'production';

  IF v_parent_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'ERP production parent menu must exist exactly once';
  END IF;

  IF EXISTS (
    SELECT 1 FROM system_menu
    WHERE id = 6034
      AND NOT (
        deleted = b'0'
        AND parent_id = v_parent_id
        AND path = 'replenishment-list'
        AND component = 'erp/production/replenishment-list/index'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'ERP production replenishment-list menu id 6034 is occupied';
  END IF;

  IF EXISTS (
    SELECT 1 FROM system_menu
    WHERE deleted = b'0'
      AND parent_id = v_parent_id
      AND path = 'replenishment-list'
      AND id <> 6034
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'ERP production replenishment-list sibling path is occupied';
  END IF;

  IF EXISTS (
    SELECT 1 FROM system_menu
    WHERE id = 6035
      AND NOT (
        deleted = b'0'
        AND parent_id = 6034
        AND permission = 'erp:production-replenishment-list:query'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'ERP production replenishment-list menu id 6035 is occupied';
  END IF;

  IF EXISTS (
    SELECT 1 FROM system_menu
    WHERE deleted = b'0'
      AND permission = 'erp:production-replenishment-list:query'
      AND id <> 6035
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'ERP production replenishment-list permission erp:production-replenishment-list:query is occupied';
  END IF;
END$$
DELIMITER ;

CALL preflight_erp_production_replenishment_list_menu();
DROP PROCEDURE IF EXISTS preflight_erp_production_replenishment_list_menu;

CREATE TABLE IF NOT EXISTS erp_kingdee_production_replenishment_list (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  source_form_id varchar(64) NOT NULL DEFAULT 'PRD_FeedMtrl' COMMENT 'ERP来源表单标识',
  source_fid varchar(64) NOT NULL COMMENT 'ERP单据FID',
  source_bill_no varchar(128) NOT NULL COMMENT 'ERP生产补料单号',
  bill_date datetime DEFAULT NULL COMMENT '单据日期',
  document_status varchar(32) DEFAULT NULL COMMENT 'ERP单据状态',
  stock_org_number varchar(64) DEFAULT NULL COMMENT '库存组织编码',
  stock_org_name varchar(255) DEFAULT NULL COMMENT '库存组织名称',
  production_org_number varchar(64) DEFAULT NULL COMMENT '生产组织编码',
  production_org_name varchar(255) DEFAULT NULL COMMENT '生产组织名称',
  owner_number varchar(64) DEFAULT NULL COMMENT '货主编码',
  owner_name varchar(255) DEFAULT NULL COMMENT '货主名称',
  department_number varchar(64) DEFAULT NULL COMMENT '补料部门编码',
  department_name varchar(255) DEFAULT NULL COMMENT '补料部门名称',
  description varchar(512) DEFAULT NULL COMMENT '备注',
  source_modify_time datetime DEFAULT NULL COMMENT 'ERP来源修改时间',
  last_sync_time datetime NOT NULL COMMENT '最后同步时间',
  raw_payload longtext DEFAULT NULL COMMENT 'ERP原始载荷快照',
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  tenant_id bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_kingdee_prod_replenishment_list_source (tenant_id, source_form_id, source_fid, deleted),
  KEY idx_erp_kingdee_prod_replenishment_list_bill_no (tenant_id, source_bill_no, deleted),
  KEY idx_erp_kingdee_prod_replenishment_list_modify_time (tenant_id, source_modify_time, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP金蝶生产补料单只读快照';

CREATE TABLE IF NOT EXISTS erp_kingdee_production_replenishment_list_item (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  production_replenishment_list_id bigint NOT NULL COMMENT '生产补料单快照编号',
  source_form_id varchar(64) NOT NULL DEFAULT 'PRD_FeedMtrl' COMMENT 'ERP来源表单标识',
  source_fid varchar(64) NOT NULL COMMENT 'ERP单据FID',
  source_entry_id varchar(64) NOT NULL COMMENT 'ERP分录ID',
  source_line_key varchar(255) NOT NULL COMMENT 'ERP补料分录唯一键',
  source_bill_no varchar(128) NOT NULL COMMENT 'ERP生产补料单号',
  material_number varchar(64) NOT NULL COMMENT '物料编码',
  material_name varchar(255) NOT NULL COMMENT '物料名称',
  material_specification varchar(512) DEFAULT NULL COMMENT '规格型号',
  unit_name varchar(64) DEFAULT NULL COMMENT '单位',
  requested_quantity decimal(24,6) NOT NULL COMMENT '申请数量',
  actual_quantity decimal(24,6) NOT NULL COMMENT '实发数量',
  base_actual_quantity decimal(24,6) DEFAULT NULL COMMENT '基本单位实发数量',
  warehouse_number varchar(64) DEFAULT NULL COMMENT '仓库编码',
  warehouse_name varchar(255) DEFAULT NULL COMMENT '仓库名称',
  stock_location_number varchar(128) DEFAULT NULL COMMENT '仓位编码',
  stock_location_name varchar(255) DEFAULT NULL COMMENT '仓位名称',
  lot_number varchar(128) DEFAULT NULL COMMENT '批号',
  production_order_no varchar(128) DEFAULT NULL COMMENT '生产订单编号',
  production_order_line_no int DEFAULT NULL COMMENT '生产订单行号',
  production_material_list_no varchar(128) DEFAULT NULL COMMENT '生产用料清单编号',
  production_material_list_line_no int DEFAULT NULL COMMENT '生产用料清单行号',
  workshop_number varchar(64) DEFAULT NULL COMMENT '车间编码',
  workshop_name varchar(255) DEFAULT NULL COMMENT '车间名称',
  stock_status_number varchar(64) DEFAULT NULL COMMENT '库存状态编码',
  stock_status_name varchar(255) DEFAULT NULL COMMENT '库存状态名称',
  source_modify_time datetime DEFAULT NULL COMMENT 'ERP来源修改时间',
  last_sync_time datetime NOT NULL COMMENT '最后同步时间',
  raw_payload longtext DEFAULT NULL COMMENT 'ERP原始载荷快照',
  creator varchar(64) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updater varchar(64) DEFAULT '',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  tenant_id bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_erp_kingdee_prod_replenishment_list_item_source (tenant_id, source_line_key, deleted),
  KEY idx_erp_kingdee_prod_replenishment_list_item_parent (tenant_id, production_replenishment_list_id, deleted),
  KEY idx_erp_kingdee_prod_replenishment_list_item_material (tenant_id, material_number, deleted),
  KEY idx_erp_kingdee_prod_replenishment_list_item_mo (tenant_id, production_order_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP金蝶生产补料单分录只读快照';

INSERT INTO infra_job (
  name, status, handler_name, handler_param, cron_expression, retry_count, retry_interval,
  monitor_timeout, creator, create_time, updater, update_time, deleted
)
SELECT '每 10 分钟同步 ERP 生产补料单列表', 2, 'kingdeeProductionReplenishmentListSyncJob', '', '0 6/10 * * * ?', 3, 60, 0,
       '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM infra_job WHERE handler_name = 'kingdeeProductionReplenishmentListSyncJob'
);

UPDATE infra_job
SET name = '每 10 分钟同步 ERP 生产补料单列表',
    status = 2,
    handler_param = '',
    cron_expression = '0 6/10 * * * ?',
    retry_count = 3,
    retry_interval = 60,
    monitor_timeout = 0,
    updater = '1',
    update_time = NOW(),
    deleted = b'0'
WHERE handler_name = 'kingdeeProductionReplenishmentListSyncJob';

SET @erp_production_parent_menu_id := (
  SELECT id FROM system_menu
  WHERE deleted = b'0' AND path = 'production'
  ORDER BY id LIMIT 1
);

INSERT INTO system_menu (
  id, name, permission, type, sort, parent_id, path, icon, component, component_name,
  status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 6034, '生产补料单列表', '', 2, 21, @erp_production_parent_menu_id,
       'replenishment-list', 'ep:list', 'erp/production/replenishment-list/index', 'ErpProductionReplenishmentList',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @erp_production_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE id = 6034 OR (parent_id = @erp_production_parent_menu_id AND path = 'replenishment-list')
  );

INSERT INTO system_menu (
  id, name, permission, type, sort, parent_id, path, icon, component, component_name,
  status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 6035, '生产补料单列表查询', 'erp:production-replenishment-list:query', 3, 1, 6034,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE EXISTS (SELECT 1 FROM system_menu WHERE id = 6034 AND deleted = b'0')
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE id = 6035 OR permission = 'erp:production-replenishment-list:query'
  );

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT DISTINCT rm.role_id, menu_ids.menu_id, '1', NOW(), '1', NOW(), b'0', rm.tenant_id
FROM system_role_menu rm
JOIN (
  SELECT 6034 AS menu_id
  UNION ALL SELECT 6035
) menu_ids
WHERE rm.menu_id = @erp_production_parent_menu_id
  AND rm.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu exists_rm
    WHERE exists_rm.role_id = rm.role_id
      AND exists_rm.menu_id = menu_ids.menu_id
      AND exists_rm.tenant_id = rm.tenant_id
      AND exists_rm.deleted = b'0'
  );
