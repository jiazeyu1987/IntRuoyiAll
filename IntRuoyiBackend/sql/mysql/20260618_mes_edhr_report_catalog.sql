-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `mes_pro_edhr_dataset_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dataset_code` varchar(64) NOT NULL COMMENT '数据集编码',
  `dataset_name` varchar(128) NOT NULL COMMENT '数据集名称',
  `dataset_version` varchar(32) NOT NULL COMMENT '数据集版本',
  `status` varchar(32) NOT NULL COMMENT '状态：DRAFT、VALIDATED、PUBLISHED、DISABLED',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型',
  `source_object` varchar(128) NOT NULL COMMENT '来源对象',
  `source_owner` varchar(128) NOT NULL COMMENT '来源责任人',
  `field_schema_json` longtext NOT NULL COMMENT '字段定义JSON',
  `join_key_json` longtext NOT NULL COMMENT '关联键JSON',
  `sensitive_field_json` longtext DEFAULT NULL COMMENT '敏感字段JSON',
  `permission_policy_json` longtext NOT NULL COMMENT '权限策略JSON',
  `caliber_version` varchar(32) NOT NULL COMMENT '口径版本',
  `data_source_status` varchar(32) NOT NULL COMMENT '数据源状态：READY、ERROR',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_dataset_code_version` (`tenant_id`, `dataset_code`, `dataset_version`, `deleted`),
  KEY `idx_mes_pro_edhr_dataset_status` (`tenant_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR报表数据集定义';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_report_catalog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_code` varchar(64) NOT NULL COMMENT '报表编码',
  `report_name` varchar(128) NOT NULL COMMENT '报表名称',
  `report_category` varchar(64) NOT NULL COMMENT '报表分类',
  `business_purpose` varchar(500) NOT NULL COMMENT '业务用途',
  `primary_dimensions` varchar(500) NOT NULL COMMENT '主查询维度',
  `related_dimensions` varchar(500) NOT NULL COMMENT '关联维度',
  `data_source_summary` varchar(500) NOT NULL COMMENT '数据来源摘要',
  `permission_policy` varchar(500) NOT NULL COMMENT '权限策略',
  `export_policy` varchar(500) NOT NULL COMMENT '导出策略',
  `status` varchar(32) NOT NULL COMMENT '状态：ACTIVE、DISABLED',
  `acceptance_status` varchar(32) NOT NULL COMMENT '验收状态',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_report_catalog_code` (`tenant_id`, `report_code`, `deleted`),
  KEY `idx_mes_pro_edhr_report_catalog_category` (`tenant_id`, `report_category`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR统一追溯报表目录';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_report_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_code` varchar(64) NOT NULL COMMENT '报表编码',
  `report_name` varchar(128) NOT NULL COMMENT '报表名称',
  `report_type` varchar(32) NOT NULL COMMENT '报表类型：STANDARD、CUSTOM',
  `dataset_id` bigint NOT NULL COMMENT '数据集编号',
  `dataset_code` varchar(64) NOT NULL COMMENT '数据集编码',
  `dataset_version` varchar(32) NOT NULL COMMENT '数据集版本',
  `status` varchar(32) NOT NULL COMMENT '状态：DRAFT、PENDING、PUBLISHED、DISABLED、VOID',
  `caliber_version` varchar(32) NOT NULL COMMENT '口径版本',
  `field_caliber_json` longtext NOT NULL COMMENT '字段口径JSON',
  `filter_schema_json` longtext NOT NULL COMMENT '筛选条件JSON',
  `drilldown_target_json` longtext NOT NULL COMMENT '钻取目标JSON',
  `permission_summary_json` longtext NOT NULL COMMENT '权限摘要JSON',
  `data_source_status` varchar(32) NOT NULL COMMENT '数据源状态：READY、ERROR',
  `sample_query_json` longtext DEFAULT NULL COMMENT '样例查询JSON',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_edhr_report_definition_code_caliber` (`tenant_id`, `report_code`, `caliber_version`, `deleted`),
  KEY `idx_mes_pro_edhr_report_definition_dataset` (`tenant_id`, `dataset_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR报表定义';

CREATE TABLE IF NOT EXISTS `mes_pro_edhr_export_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_definition_id` bigint NOT NULL COMMENT '报表定义编号',
  `report_code` varchar(64) NOT NULL COMMENT '报表编码',
  `report_name` varchar(128) NOT NULL COMMENT '报表名称',
  `caliber_version` varchar(32) NOT NULL COMMENT '口径版本',
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型：EXPORT_AUDIT',
  `filter_snapshot_json` longtext NOT NULL COMMENT '筛选快照JSON',
  `permission_summary_json` longtext NOT NULL COMMENT '权限摘要JSON',
  `data_range_summary` varchar(500) NOT NULL COMMENT '数据范围摘要',
  `result_status` varchar(32) NOT NULL COMMENT '结果状态',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作人编号',
  `operator_username` varchar(64) DEFAULT NULL COMMENT '操作人',
  `occurred_at` datetime NOT NULL COMMENT '发生时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_mes_pro_edhr_export_audit_report` (`tenant_id`, `report_code`, `occurred_at`, `deleted`),
  KEY `idx_mes_pro_edhr_export_audit_result` (`tenant_id`, `result_status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='eDHR报表导出审计';

SET @mes_edhr_report_test_tenant_id := (
  SELECT `id`
  FROM `system_tenant`
  WHERE `name` = '测试租户'
    AND `deleted` = b'0'
);

UPDATE `mes_pro_edhr_dataset_definition` AS `legacy`
LEFT JOIN `mes_pro_edhr_dataset_definition` AS `target`
  ON `target`.`tenant_id` = @mes_edhr_report_test_tenant_id
 AND `target`.`dataset_code` = `legacy`.`dataset_code`
 AND `target`.`dataset_version` = `legacy`.`dataset_version`
 AND `target`.`deleted` = b'0'
SET `legacy`.`tenant_id` = @mes_edhr_report_test_tenant_id,
    `legacy`.`updater` = 'system',
    `legacy`.`update_time` = NOW()
WHERE @mes_edhr_report_test_tenant_id IS NOT NULL
  AND `legacy`.`tenant_id` = 0
  AND `legacy`.`dataset_code` IN ('EDHR_PRODUCTION_TRACE', 'EDHR_DHR_TRACE')
  AND `legacy`.`dataset_version` = 'V1.0'
  AND `legacy`.`deleted` = b'0'
  AND `target`.`id` IS NULL;

UPDATE `mes_pro_edhr_report_catalog` AS `legacy`
LEFT JOIN `mes_pro_edhr_report_catalog` AS `target`
  ON `target`.`tenant_id` = @mes_edhr_report_test_tenant_id
 AND `target`.`report_code` = `legacy`.`report_code`
 AND `target`.`deleted` = b'0'
SET `legacy`.`tenant_id` = @mes_edhr_report_test_tenant_id,
    `legacy`.`updater` = 'system',
    `legacy`.`update_time` = NOW()
WHERE @mes_edhr_report_test_tenant_id IS NOT NULL
  AND `legacy`.`tenant_id` = 0
  AND `legacy`.`report_code` IN (
    'PRODUCTION_TRACE', 'INSPECTION_TRACE', 'TRANSACTION_RECORD', 'WORK_REPORT_RECORD',
    'SCRAP_RECORD', 'DHR_TRACE', 'FORM_TRACE', 'RECORDBOOK_TRACE',
    'CONSUMPTION_RECORD', 'INVENTORY_LIST', 'INVENTORY_LEDGER', 'MESSAGE_RECORD'
  )
  AND `legacy`.`deleted` = b'0'
  AND `target`.`id` IS NULL;

UPDATE `mes_pro_edhr_report_definition` AS `legacy`
LEFT JOIN `mes_pro_edhr_report_definition` AS `target`
  ON `target`.`tenant_id` = @mes_edhr_report_test_tenant_id
 AND `target`.`report_code` = `legacy`.`report_code`
 AND `target`.`caliber_version` = `legacy`.`caliber_version`
 AND `target`.`deleted` = b'0'
SET `legacy`.`tenant_id` = @mes_edhr_report_test_tenant_id,
    `legacy`.`updater` = 'system',
    `legacy`.`update_time` = NOW()
WHERE @mes_edhr_report_test_tenant_id IS NOT NULL
  AND `legacy`.`tenant_id` = 0
  AND `legacy`.`report_code` IN ('PRODUCTION_TRACE', 'DHR_TRACE')
  AND `legacy`.`caliber_version` = 'CAL-V1'
  AND `legacy`.`deleted` = b'0'
  AND `target`.`id` IS NULL;

INSERT INTO `mes_pro_edhr_dataset_definition`
(`dataset_code`, `dataset_name`, `dataset_version`, `status`, `source_type`, `source_object`, `source_owner`,
 `field_schema_json`, `join_key_json`, `sensitive_field_json`, `permission_policy_json`, `caliber_version`,
 `data_source_status`, `failure_reason`, `published_at`, `remark`, `creator`, `updater`, `tenant_id`)
SELECT 'EDHR_PRODUCTION_TRACE', '生产追溯数据集', 'V1.0', 'PUBLISHED', 'TABLE', 'mes_pro_batch_record_execution', '生产/eDHR',
       '[{"field":"executionCode","label":"执行编码"},{"field":"workOrderCode","label":"工单"},{"field":"batchCode","label":"批次"}]',
       '[{"key":"batchCode","target":"DHR_TRACE"},{"key":"workOrderCode","target":"WORK_REPORT_RECORD"}]',
       '[]', '{"scope":"tenant_role","summary":"按测试租户菜单和角色权限过滤"}', 'CAL-V1',
       'READY', NULL, NOW(), '首切片生产追溯真实表汇总', 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (
  SELECT 1 FROM `mes_pro_edhr_dataset_definition`
  WHERE `dataset_code` = 'EDHR_PRODUCTION_TRACE'
    AND `dataset_version` = 'V1.0'
    AND `tenant_id` = @mes_edhr_report_test_tenant_id
    AND `deleted` = b'0'
);

INSERT INTO `mes_pro_edhr_dataset_definition`
(`dataset_code`, `dataset_name`, `dataset_version`, `status`, `source_type`, `source_object`, `source_owner`,
 `field_schema_json`, `join_key_json`, `sensitive_field_json`, `permission_policy_json`, `caliber_version`,
 `data_source_status`, `failure_reason`, `published_at`, `remark`, `creator`, `updater`, `tenant_id`)
SELECT 'EDHR_DHR_TRACE', 'DHR追溯数据集', 'V1.0', 'PUBLISHED', 'TABLE', 'mes_pro_edhr_batch_execution', 'eDHR',
       '[{"field":"executionCode","label":"DHR执行编码"},{"field":"batchCode","label":"批次"},{"field":"status","label":"状态"}]',
       '[{"key":"batchCode","target":"PRODUCTION_TRACE"}]',
       '[]', '{"scope":"tenant_role","summary":"按测试租户菜单和角色权限过滤"}', 'CAL-V1',
       'READY', NULL, NOW(), '首切片DHR追溯真实表汇总', 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (
  SELECT 1 FROM `mes_pro_edhr_dataset_definition`
  WHERE `dataset_code` = 'EDHR_DHR_TRACE'
    AND `dataset_version` = 'V1.0'
    AND `tenant_id` = @mes_edhr_report_test_tenant_id
    AND `deleted` = b'0'
);

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`,
 `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'PRODUCTION_TRACE', '生产追溯', '生产', '按产品、工单、批次、SN、工序、设备、人员和时间追溯生产执行结果',
       '产品,工单,批次,SN,工序,设备,人员,时间', '报工,消耗,事务,DHR',
       'mes_pro_batch_record_execution 等生产执行数据', '租户+角色+菜单+数据权限', '导出需记录筛选快照和权限摘要',
       'ACTIVE', 'FIRST_SLICE_READY', 10, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'PRODUCTION_TRACE' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'INSPECTION_TRACE', '检验追溯', '质量', '按检验类型、检验单、批次、SN、样本、检验项目和检验结论追溯质量数据',
       '检验类型,检验单,批次,SN,样本,检验项目,结论', '质量明细,业务对象', '质检单据与检验项目数据', '租户+角色+菜单+字段权限', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 20, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'INSPECTION_TRACE' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'TRANSACTION_RECORD', '事务记录', '生产', '按事务类型、触发对象、责任人、时间和状态追溯生产质量事务',
       '事务类型,触发对象,责任人,时间,状态', '生产,质量,DHR', '生产和质量事务数据', '租户+角色+菜单', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 30, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'TRANSACTION_RECORD' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'WORK_REPORT_RECORD', '报工记录', '生产', '按工单、工序、人员、设备、数量、时间和班次追溯报工',
       '工单,工序,人员,设备,数量,时间,班次', '生产执行,消耗', '报工记录数据', '租户+角色+菜单', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 40, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'WORK_REPORT_RECORD' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'SCRAP_RECORD', '报废记录', '质量', '按产品、批次、SN、工序、不良分类、报废原因、数量和责任人追溯报废',
       '产品,批次,SN,工序,不良分类,原因,数量,责任人', '质量事务,生产执行', '报废记录数据', '租户+角色+菜单', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 50, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'SCRAP_RECORD' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'DHR_TRACE', 'DHR追溯', 'eDHR', '按DHR目录、模板版本、批次、记录状态、签名、审批和归档状态追溯批记录',
       'DHR目录,模板版本,批次,状态,签名,审批,归档', '生产追溯,表单,记录本', 'mes_pro_edhr_batch_execution 等eDHR执行数据',
       '租户+角色+菜单+记录权限', '导出需记录口径版本和权限摘要', 'ACTIVE', 'FIRST_SLICE_READY', 60, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'DHR_TRACE' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'FORM_TRACE', '表单追溯', 'eDHR', '按表单模板、表单版本、填报人、审核人、业务对象和状态追溯独立表单',
       '表单模板,版本,填报人,审核人,业务对象,状态', '记录本,DHR,变更', '独立表单实例数据', '租户+角色+菜单', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 70, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'FORM_TRACE' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'RECORDBOOK_TRACE', '记录本追溯', 'eDHR', '按记录本、标签、填报周期、填报人和审核状态追溯记录本',
       '记录本,标签,周期,填报人,审核状态', '表单,DHR,变更', '记录本条目数据', '租户+角色+菜单+标签权限', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 80, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'RECORDBOOK_TRACE' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'CONSUMPTION_RECORD', '消耗记录', '仓储', '按工单、批次、SN、物料、用量、批号、库位和时间追溯投料消耗',
       '工单,批次,SN,物料,用量,批号,库位,时间', '库存流水,生产执行', '生产消耗数据', '租户+角色+菜单', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 90, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'CONSUMPTION_RECORD' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'INVENTORY_LIST', '库存列表', '仓储', '按物料、批号、库位、状态、数量和有效期追溯当前库存',
       '物料,批号,库位,状态,数量,有效期', '库存流水,消耗', '库存现存量数据', '租户+角色+菜单', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 100, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'INVENTORY_LIST' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'INVENTORY_LEDGER', '库存流水', '仓储', '按入库、出库、移库、调整、消耗和退料等类型追溯库存变化',
       '流水类型,物料,批号,库位,时间,业务对象', '库存列表,消耗,事务', '库存流水数据', '租户+角色+菜单', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 110, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'INVENTORY_LEDGER' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_catalog`
(`report_code`, `report_name`, `report_category`, `business_purpose`, `primary_dimensions`, `related_dimensions`, `data_source_summary`, `permission_policy`, `export_policy`, `status`, `acceptance_status`, `sort`, `creator`, `updater`, `tenant_id`)
SELECT 'MESSAGE_RECORD', '消息记录', '消息', '按业务对象、消息类型、接收人、发送状态、读取状态和时间追溯通知待办',
       '业务对象,消息类型,接收人,发送状态,读取状态,时间', '待办,审批,业务记录', '消息和待办数据', '租户+角色+消息权限', '导出需审计',
       'ACTIVE', 'CATALOG_ONLY', 120, 'system', 'system', @mes_edhr_report_test_tenant_id
WHERE NOT EXISTS (SELECT 1 FROM `mes_pro_edhr_report_catalog` WHERE `report_code` = 'MESSAGE_RECORD' AND `tenant_id` = @mes_edhr_report_test_tenant_id AND `deleted` = b'0');

INSERT INTO `mes_pro_edhr_report_definition`
(`report_code`, `report_name`, `report_type`, `dataset_id`, `dataset_code`, `dataset_version`, `status`,
 `caliber_version`, `field_caliber_json`, `filter_schema_json`, `drilldown_target_json`, `permission_summary_json`,
 `data_source_status`, `sample_query_json`, `published_at`, `remark`, `creator`, `updater`, `tenant_id`)
SELECT 'PRODUCTION_TRACE', '生产追溯', 'STANDARD', `dataset`.`id`, `dataset`.`dataset_code`, `dataset`.`dataset_version`, 'PUBLISHED',
       `dataset`.`caliber_version`, `dataset`.`field_schema_json`,
       '[{"field":"batchCode","label":"批次"},{"field":"workOrderCode","label":"工单"}]',
       '[{"target":"DHR_TRACE","label":"DHR追溯"}]', `dataset`.`permission_policy_json`,
       `dataset`.`data_source_status`, '{"query":"COUNT_BY_SOURCE_TABLE"}', NOW(), '首切片标准生产追溯只读查询', 'system', 'system', @mes_edhr_report_test_tenant_id
FROM `mes_pro_edhr_dataset_definition` `dataset`
WHERE `dataset`.`dataset_code` = 'EDHR_PRODUCTION_TRACE'
  AND `dataset`.`dataset_version` = 'V1.0'
  AND `dataset`.`tenant_id` = @mes_edhr_report_test_tenant_id
  AND `dataset`.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `mes_pro_edhr_report_definition`
    WHERE `report_code` = 'PRODUCTION_TRACE'
      AND `caliber_version` = 'CAL-V1'
      AND `tenant_id` = @mes_edhr_report_test_tenant_id
      AND `deleted` = b'0'
  );

INSERT INTO `mes_pro_edhr_report_definition`
(`report_code`, `report_name`, `report_type`, `dataset_id`, `dataset_code`, `dataset_version`, `status`,
 `caliber_version`, `field_caliber_json`, `filter_schema_json`, `drilldown_target_json`, `permission_summary_json`,
 `data_source_status`, `sample_query_json`, `published_at`, `remark`, `creator`, `updater`, `tenant_id`)
SELECT 'DHR_TRACE', 'DHR追溯', 'STANDARD', `dataset`.`id`, `dataset`.`dataset_code`, `dataset`.`dataset_version`, 'PUBLISHED',
       `dataset`.`caliber_version`, `dataset`.`field_schema_json`,
       '[{"field":"batchCode","label":"批次"},{"field":"status","label":"状态"}]',
       '[{"target":"PRODUCTION_TRACE","label":"生产追溯"}]', `dataset`.`permission_policy_json`,
       `dataset`.`data_source_status`, '{"query":"COUNT_BY_SOURCE_TABLE"}', NOW(), '首切片标准DHR追溯只读查询', 'system', 'system', @mes_edhr_report_test_tenant_id
FROM `mes_pro_edhr_dataset_definition` `dataset`
WHERE `dataset`.`dataset_code` = 'EDHR_DHR_TRACE'
  AND `dataset`.`dataset_version` = 'V1.0'
  AND `dataset`.`tenant_id` = @mes_edhr_report_test_tenant_id
  AND `dataset`.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `mes_pro_edhr_report_definition`
    WHERE `report_code` = 'DHR_TRACE'
      AND `caliber_version` = 'CAL-V1'
      AND `tenant_id` = @mes_edhr_report_test_tenant_id
      AND `deleted` = b'0'
  );

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900280, 'eDHR报表目录', '', 2, 280, 900220, '/mes/pro/feedback/edhr-report', 'ep:data-analysis', 'mes/pro/edhr-report/ReportPage', 'MesProEdhrReport', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900280 OR `path` = '/mes/pro/feedback/edhr-report');

UPDATE `system_menu`
SET `path` = '/mes/pro/feedback/edhr-report',
    `updater` = 'system',
    `update_time` = NOW()
WHERE `id` = 900280
  AND `path` = 'edhr-report'
  AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900281, 'eDHR报表查询', 'mes:pro-edhr-report:query', 3, 1, 900280, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900281 OR `permission` = 'mes:pro-edhr-report:query');

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 900282, 'eDHR报表导出审计', 'mes:pro-edhr-report:export', 3, 2, 900280, '', '', '', '', 0, b'1', b'0', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 900282 OR `permission` = 'mes:pro-edhr-report:export');

DROP PROCEDURE IF EXISTS ensure_mes_edhr_report_test_tenant_menus;

DELIMITER //
CREATE PROCEDURE ensure_mes_edhr_report_test_tenant_menus()
BEGIN
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_report_target_tenant`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_report_target_tenant` AS
  SELECT `tenant`.`id` AS `tenant_id`, `tenant`.`package_id`
  FROM `system_tenant` AS `tenant`
  WHERE `tenant`.`name` = '测试租户'
    AND `tenant`.`deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_report_target_tenant`) <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing unique 测试租户; cannot merge eDHR report menus';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_report_target_tenant` AS `target`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target`.`package_id`
     AND `package`.`deleted` = b'0'
    WHERE NOT JSON_VALID(`package`.`menu_ids`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid system_tenant_package.menu_ids JSON; cannot merge eDHR report menus';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_report_target_tenant` AS `target`
    JOIN `system_tenant_package` AS `package`
      ON `package`.`id` = `target`.`package_id`
     AND `package`.`deleted` = b'0'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing 测试租户 system_tenant_package; cannot merge eDHR report menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_report_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_report_menu_ids` AS
  SELECT `id` AS `menu_id`
  FROM `system_menu`
  WHERE `id` IN (900280, 900281, 900282)
    AND `deleted` = b'0';

  IF (SELECT COUNT(1) FROM `tmp_mes_edhr_report_menu_ids`) <> 3 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing eDHR report system_menu rows; cannot merge tenant package menu_ids';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_report_package_existing_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_report_package_existing_menu_ids` AS
  SELECT `package`.`id` AS `package_id`, CAST(`menu`.`menu_id` AS UNSIGNED) AS `menu_id`
  FROM `tmp_mes_edhr_report_target_tenant` AS `target`
  JOIN `system_tenant_package` AS `package`
    ON `package`.`id` = `target`.`package_id`
   AND `package`.`deleted` = b'0'
  JOIN JSON_TABLE(CAST(`package`.`menu_ids` AS JSON), '$[*]' COLUMNS (`menu_id` BIGINT PATH '$')) AS `menu`
  WHERE `package`.`deleted` = b'0';

  IF NOT EXISTS (
    SELECT 1
    FROM `tmp_mes_edhr_report_package_existing_menu_ids`
    WHERE `menu_id` = 900220
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Missing 测试租户 eDHR parent menu 900220; cannot merge report menus';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_report_package_merged_menu_ids`;
  CREATE TEMPORARY TABLE `tmp_mes_edhr_report_package_merged_menu_ids` AS
  SELECT DISTINCT `package_id`, `menu_id`
  FROM `tmp_mes_edhr_report_package_existing_menu_ids`
  UNION
  SELECT DISTINCT `target`.`package_id`, `menus`.`menu_id`
  FROM `tmp_mes_edhr_report_target_tenant` AS `target`
  JOIN `tmp_mes_edhr_report_menu_ids` AS `menus`;

  UPDATE `system_tenant_package` AS `package`
  JOIN `tmp_mes_edhr_report_target_tenant` AS `target`
    ON `target`.`package_id` = `package`.`id`
  JOIN (
    SELECT `package_id`, JSON_ARRAYAGG(`menu_id`) AS `menu_ids`
    FROM (
      SELECT DISTINCT `package_id`, `menu_id`
      FROM `tmp_mes_edhr_report_package_merged_menu_ids`
    ) AS `deduplicated`
    GROUP BY `package_id`
  ) AS `merged`
    ON `merged`.`package_id` = `package`.`id`
  SET `package`.`menu_ids` = `merged`.`menu_ids`,
      `package`.`updater` = 'system',
      `package`.`update_time` = NOW()
  WHERE `package`.`deleted` = b'0';

  INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `role`.`id`, `menus`.`menu_id`, 'system', NOW(), 'system', NOW(), b'0', `role`.`tenant_id`
  FROM `tmp_mes_edhr_report_target_tenant` AS `target`
  JOIN `system_role` AS `role`
    ON `role`.`tenant_id` = `target`.`tenant_id`
   AND `role`.`code` = 'tenant_admin'
   AND `role`.`deleted` = b'0'
  JOIN `tmp_mes_edhr_report_menu_ids` AS `menus`
  WHERE NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menus`.`menu_id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_report_package_merged_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_report_package_existing_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_report_menu_ids`;
  DROP TEMPORARY TABLE IF EXISTS `tmp_mes_edhr_report_target_tenant`;
END//
DELIMITER ;

CALL ensure_mes_edhr_report_test_tenant_menus();
DROP PROCEDURE IF EXISTS ensure_mes_edhr_report_test_tenant_menus;
