-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- CRM base schema repair for MySQL.
-- Generated from yudao-module-crm DO annotations and fields.
-- Safe to run repeatedly: creates missing tables only and does not delete data.

CREATE TABLE IF NOT EXISTS `crm_business` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `follow_up_status` bit(1) DEFAULT NULL,
  `contact_last_time` datetime DEFAULT NULL,
  `contact_next_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `status_type_id` bigint DEFAULT NULL,
  `status_id` bigint DEFAULT NULL,
  `end_status` int DEFAULT NULL,
  `end_remark` varchar(512) DEFAULT NULL,
  `deal_time` datetime DEFAULT NULL,
  `total_product_price` decimal(24,6) DEFAULT NULL,
  `discount_percent` decimal(24,6) DEFAULT NULL,
  `total_price` decimal(24,6) DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmBusinessDO';

CREATE TABLE IF NOT EXISTS `crm_business_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `business_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_price` decimal(24,6) DEFAULT NULL,
  `business_price` decimal(24,6) DEFAULT NULL,
  `count` decimal(24,6) DEFAULT NULL,
  `total_price` decimal(24,6) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_product_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmBusinessProductDO';

CREATE TABLE IF NOT EXISTS `crm_business_status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type_id` bigint DEFAULT NULL,
  `name` varchar(128) DEFAULT NULL,
  `percent` int DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_status_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmBusinessStatusDO';

CREATE TABLE IF NOT EXISTS `crm_business_status_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `dept_ids` longtext,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_business_status_type_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmBusinessStatusTypeDO';

CREATE TABLE IF NOT EXISTS `crm_clue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `follow_up_status` bit(1) DEFAULT NULL,
  `contact_last_time` datetime DEFAULT NULL,
  `contact_last_content` varchar(512) DEFAULT NULL,
  `contact_next_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `transform_status` bit(1) DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `mobile` varchar(32) DEFAULT NULL,
  `telephone` varchar(32) DEFAULT NULL,
  `qq` varchar(64) DEFAULT NULL,
  `wechat` varchar(64) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `area_id` int DEFAULT NULL,
  `detail_address` varchar(512) DEFAULT NULL,
  `industry_id` int DEFAULT NULL,
  `level` int DEFAULT NULL,
  `source` int DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_clue_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmClueDO';

CREATE TABLE IF NOT EXISTS `crm_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `contact_last_time` datetime DEFAULT NULL,
  `contact_last_content` varchar(512) DEFAULT NULL,
  `contact_next_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `mobile` varchar(32) DEFAULT NULL,
  `telephone` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `qq` bigint DEFAULT NULL,
  `wechat` varchar(64) DEFAULT NULL,
  `area_id` int DEFAULT NULL,
  `detail_address` varchar(512) DEFAULT NULL,
  `sex` int DEFAULT NULL,
  `master` bit(1) DEFAULT NULL,
  `post` varchar(128) DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_contact_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmContactDO';

CREATE TABLE IF NOT EXISTS `crm_contact_business` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contact_id` bigint DEFAULT NULL,
  `business_id` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_contact_business_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmContactBusinessDO';

CREATE TABLE IF NOT EXISTS `crm_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `no` varchar(64) DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `business_id` bigint DEFAULT NULL,
  `contact_last_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `process_instance_id` varchar(64) DEFAULT NULL,
  `audit_status` int DEFAULT NULL,
  `order_date` datetime DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `total_product_price` decimal(24,6) DEFAULT NULL,
  `discount_percent` decimal(24,6) DEFAULT NULL,
  `total_price` decimal(24,6) DEFAULT NULL,
  `sign_contact_id` bigint DEFAULT NULL,
  `sign_user_id` bigint DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_contract_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmContractDO';

CREATE TABLE IF NOT EXISTS `crm_contract_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `notify_enabled` bit(1) DEFAULT NULL,
  `notify_days` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_contract_config_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmContractConfigDO';

CREATE TABLE IF NOT EXISTS `crm_contract_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_price` decimal(24,6) DEFAULT NULL,
  `contract_price` decimal(24,6) DEFAULT NULL,
  `count` decimal(24,6) DEFAULT NULL,
  `total_price` decimal(24,6) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_contract_product_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmContractProductDO';

CREATE TABLE IF NOT EXISTS `crm_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `follow_up_status` bit(1) DEFAULT NULL,
  `contact_last_time` datetime DEFAULT NULL,
  `contact_last_content` varchar(512) DEFAULT NULL,
  `contact_next_time` datetime DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `owner_time` datetime DEFAULT NULL,
  `lock_status` bit(1) DEFAULT NULL,
  `deal_status` bit(1) DEFAULT NULL,
  `mobile` varchar(32) DEFAULT NULL,
  `telephone` varchar(32) DEFAULT NULL,
  `qq` varchar(64) DEFAULT NULL,
  `wechat` varchar(64) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `area_id` int DEFAULT NULL,
  `detail_address` varchar(512) DEFAULT NULL,
  `industry_id` int DEFAULT NULL,
  `level` int DEFAULT NULL,
  `source` int DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_customer_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmCustomerDO';

CREATE TABLE IF NOT EXISTS `crm_customer_limit_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` int DEFAULT NULL,
  `user_ids` longtext,
  `dept_ids` longtext,
  `max_count` int DEFAULT NULL,
  `deal_count_enabled` bit(1) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_customer_limit_config_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmCustomerLimitConfigDO';

CREATE TABLE IF NOT EXISTS `crm_customer_pool_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) DEFAULT NULL,
  `contact_expire_days` int DEFAULT NULL,
  `deal_expire_days` int DEFAULT NULL,
  `notify_enabled` bit(1) DEFAULT NULL,
  `notify_days` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_customer_pool_config_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmCustomerPoolConfigDO';

CREATE TABLE IF NOT EXISTS `crm_follow_up_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` int DEFAULT NULL,
  `biz_id` bigint DEFAULT NULL,
  `type` int DEFAULT NULL,
  `content` varchar(512) DEFAULT NULL,
  `next_time` datetime DEFAULT NULL,
  `pic_urls` longtext,
  `file_urls` longtext,
  `business_ids` longtext,
  `contact_ids` longtext,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_follow_up_record_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmFollowUpRecordDO';

CREATE TABLE IF NOT EXISTS `crm_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` int DEFAULT NULL,
  `biz_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `level` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_permission_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmPermissionDO';

CREATE TABLE IF NOT EXISTS `crm_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `no` varchar(64) DEFAULT NULL,
  `unit` int DEFAULT NULL,
  `price` decimal(24,6) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_product_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmProductDO';

CREATE TABLE IF NOT EXISTS `crm_product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `parent_id` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_product_category_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmProductCategoryDO';

CREATE TABLE IF NOT EXISTS `crm_receivable` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(64) DEFAULT NULL,
  `plan_id` bigint DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `contract_id` bigint DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `return_time` datetime DEFAULT NULL,
  `return_type` int DEFAULT NULL,
  `price` decimal(24,6) DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `process_instance_id` varchar(64) DEFAULT NULL,
  `audit_status` int DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_receivable_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmReceivableDO';

CREATE TABLE IF NOT EXISTS `crm_receivable_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `period` int DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `contract_id` bigint DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `return_time` datetime DEFAULT NULL,
  `return_type` int DEFAULT NULL,
  `price` decimal(24,6) DEFAULT NULL,
  `receivable_id` bigint DEFAULT NULL,
  `remind_days` int DEFAULT NULL,
  `remind_time` datetime DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_crm_receivable_plan_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CrmReceivablePlanDO';
