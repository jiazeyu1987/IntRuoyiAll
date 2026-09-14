-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260730_mes_process_pool_foundation; type=schema; riskLevel=medium
-- Frozen per-material facts for one transactional frontline production feedback submission.

CREATE TABLE IF NOT EXISTS mes_pro_feedback_material (
  id bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  tenant_id bigint NOT NULL DEFAULT 0 COMMENT 'Tenant id',
  feedback_id bigint NOT NULL COMMENT 'MES production feedback id',
  active_order_id bigint NOT NULL COMMENT 'Frozen active order id',
  work_order_id bigint NOT NULL COMMENT 'Frozen work order id',
  route_id bigint NOT NULL COMMENT 'Frozen route id',
  route_version_id bigint NOT NULL COMMENT 'Frozen route version id',
  route_process_id bigint NOT NULL COMMENT 'Frozen route process id',
  process_id bigint NOT NULL COMMENT 'MES process id',
  material_id bigint NOT NULL COMMENT 'Formal MES material id',
  material_code varchar(128) NOT NULL COMMENT 'Material code snapshot',
  material_name varchar(255) NOT NULL COMMENT 'Material name snapshot',
  material_specification varchar(255) DEFAULT NULL COMMENT 'Material specification snapshot',
  bom_quantity decimal(24,6) NOT NULL COMMENT 'Frozen BOM usage ratio',
  output_quantity decimal(24,6) NOT NULL COMMENT 'Entered completion quantity',
  loss_quantity decimal(24,6) NOT NULL DEFAULT 0 COMMENT 'Entered loss quantity',
  loss_details_json longtext NOT NULL COMMENT 'Loss detail snapshot JSON',
  selected_device_json longtext DEFAULT NULL COMMENT 'Selected device snapshot JSON',
  device_parameter_readings_json longtext NOT NULL COMMENT 'Device parameter readings snapshot JSON',
  version int NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
  creator varchar(64) DEFAULT '' COMMENT 'Creator',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  updater varchar(64) DEFAULT '' COMMENT 'Updater',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  deleted bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted flag',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mes_feedback_material (tenant_id,feedback_id,material_id,deleted),
  KEY idx_mes_feedback_material_order_process (tenant_id,active_order_id,route_process_id,process_id),
  KEY idx_mes_feedback_material_feedback (tenant_id,feedback_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Frontline production feedback material facts';
