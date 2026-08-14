-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_erp_kingdee_sync_runtime; type=schema; riskLevel=medium
-- 设计边界：只保存金蝶直接调拨单只读快照；不得写入 erp_stock_move / erp_stock_move_item，不触发本地库存流水或审批。

CREATE TABLE IF NOT EXISTS erp_kingdee_stock_move (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  source_form_id varchar(64) NOT NULL DEFAULT 'STK_TransferDirect' COMMENT 'ERP来源表单标识',
  source_fid varchar(64) NOT NULL COMMENT 'ERP单据FID',
  source_bill_no varchar(128) NOT NULL COMMENT 'ERP调拨单号',
  bill_date datetime DEFAULT NULL COMMENT '单据日期',
  document_status varchar(32) DEFAULT NULL COMMENT 'ERP单据状态',
  transfer_direct varchar(64) DEFAULT NULL COMMENT '调拨方向',
  transfer_biz_type varchar(128) DEFAULT NULL COMMENT '调拨业务类型',
  remark varchar(512) DEFAULT NULL COMMENT '备注',
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
  UNIQUE KEY uk_erp_kingdee_stock_move_source (tenant_id, source_form_id, source_fid, deleted),
  KEY idx_erp_kingdee_stock_move_bill_no (tenant_id, source_bill_no, deleted),
  KEY idx_erp_kingdee_stock_move_modify_time (tenant_id, source_modify_time, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP金蝶调拨单只读快照';

CREATE TABLE IF NOT EXISTS erp_kingdee_stock_move_item (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  stock_move_id bigint NOT NULL COMMENT '金蝶调拨单快照编号',
  source_form_id varchar(64) NOT NULL DEFAULT 'STK_TransferDirect' COMMENT 'ERP来源表单标识',
  source_fid varchar(64) NOT NULL COMMENT 'ERP单据FID',
  source_entry_id varchar(64) NOT NULL COMMENT 'ERP分录ID',
  source_line_key varchar(255) NOT NULL COMMENT 'ERP调拨分录唯一键',
  source_bill_no varchar(128) NOT NULL COMMENT 'ERP调拨单号',
  material_number varchar(64) NOT NULL COMMENT '物料编码',
  material_name varchar(255) NOT NULL COMMENT '物料名称',
  material_specification varchar(512) DEFAULT NULL COMMENT '规格型号',
  unit_name varchar(64) DEFAULT NULL COMMENT '单位',
  quantity decimal(24,6) NOT NULL COMMENT '调拨数量',
  from_warehouse_number varchar(64) DEFAULT NULL COMMENT '调出仓库编码',
  from_warehouse_name varchar(255) DEFAULT NULL COMMENT '调出仓库名称',
  to_warehouse_number varchar(64) DEFAULT NULL COMMENT '调入仓库编码',
  to_warehouse_name varchar(255) DEFAULT NULL COMMENT '调入仓库名称',
  from_stock_location varchar(255) DEFAULT NULL COMMENT '调出仓位',
  to_stock_location varchar(255) DEFAULT NULL COMMENT '调入仓位',
  lot_number varchar(128) DEFAULT NULL COMMENT '批号',
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
  UNIQUE KEY uk_erp_kingdee_stock_move_item_source (tenant_id, source_line_key, deleted),
  KEY idx_erp_kingdee_stock_move_item_move (tenant_id, stock_move_id, deleted),
  KEY idx_erp_kingdee_stock_move_item_material (tenant_id, material_number, deleted),
  KEY idx_erp_kingdee_stock_move_item_from_wh (tenant_id, from_warehouse_number, deleted),
  KEY idx_erp_kingdee_stock_move_item_to_wh (tenant_id, to_warehouse_number, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP金蝶调拨单分录只读快照';

INSERT INTO infra_job (
  name, status, handler_name, handler_param, cron_expression, retry_count, retry_interval,
  monitor_timeout, creator, create_time, updater, update_time, deleted
)
SELECT '每 10 分钟同步 ERP 金蝶调拨单', 2, 'kingdeeStockMoveSyncJob', '', '0 4/10 * * * ?', 3, 60, 0,
       '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM infra_job WHERE handler_name = 'kingdeeStockMoveSyncJob'
);

UPDATE infra_job
SET name = '每 10 分钟同步 ERP 金蝶调拨单',
    status = 2,
    handler_name = 'kingdeeStockMoveSyncJob',
    handler_param = '',
    cron_expression = '0 4/10 * * * ?',
    retry_count = 3,
    retry_interval = 60,
    monitor_timeout = 0,
    updater = '1',
    update_time = NOW(),
    deleted = b'0'
WHERE handler_name = 'kingdeeStockMoveSyncJob';

SET @erp_stock_parent_menu_id := (
  SELECT id
  FROM system_menu
  WHERE deleted = b'0'
    AND (path = 'stock' OR name = '库存管理')
  ORDER BY id
  LIMIT 1
);

INSERT INTO system_menu (
  id, name, permission, type, sort, parent_id, path, icon, component, component_name,
  status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 6030, '金蝶调拨单', '', 2, 80, @erp_stock_parent_menu_id,
       'kingdee-stock-move', 'ep:document', 'erp/stock/kingdeeStockMove/index',
       'ErpKingdeeStockMove', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE @erp_stock_parent_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE id = 6030 OR (parent_id = @erp_stock_parent_menu_id AND path = 'kingdee-stock-move')
  );

INSERT INTO system_menu (
  id, name, permission, type, sort, parent_id, path, icon, component, component_name,
  status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 6031, '金蝶调拨单查询', 'erp:kingdee-stock-move:query', 3, 1, 6030,
       '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
WHERE EXISTS (
    SELECT 1 FROM system_menu WHERE id = 6030 AND deleted = b'0'
  )
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE id = 6031 OR permission = 'erp:kingdee-stock-move:query'
  );

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT DISTINCT rm.role_id, menu_ids.menu_id, '1', NOW(), '1', NOW(), b'0', rm.tenant_id
FROM system_role_menu rm
JOIN system_menu source_menu
  ON source_menu.permission = 'erp:stock-move:query'
 AND source_menu.deleted = b'0'
JOIN (
  SELECT 6030 AS menu_id
  UNION ALL SELECT 6031
) menu_ids
WHERE rm.menu_id = source_menu.id
  AND rm.deleted = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu exists_rm
    WHERE exists_rm.role_id = rm.role_id
      AND exists_rm.menu_id = menu_ids.menu_id
      AND exists_rm.tenant_id = rm.tenant_id
      AND exists_rm.deleted = b'0'
  );
