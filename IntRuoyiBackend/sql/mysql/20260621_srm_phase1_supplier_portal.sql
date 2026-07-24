-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260620_srm_phase1_supplier_access_profile; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `srm_supplier_portal_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '门户申请编号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `supplier_id` bigint DEFAULT NULL COMMENT '审核通过后关联的 ERP 供应商编号',
  `company_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '企业名称',
  `unified_social_credit_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一社会信用代码',
  `contact_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系邮箱',
  `qualification_attachment_urls` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '资质附件 URL',
  `qualification_expire_date` date DEFAULT NULL COMMENT '资质到期日',
  `bank_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开户行',
  `bank_account` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '银行账号',
  `bank_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开户地址',
  `application_status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请状态',
  `submitter_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提交人昵称',
  `submitted_time` datetime DEFAULT NULL COMMENT '提交时间',
  `audit_by` bigint DEFAULT NULL COMMENT '审核人用户编号',
  `audit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核人昵称',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核意见',
  `creator` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_srm_supplier_portal_application_tenant_user` (`tenant_id`,`user_id`,`deleted`),
  KEY `idx_srm_supplier_portal_application_tenant_status` (`tenant_id`,`application_status`,`submitted_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SRM 供应商门户注册申请';

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 991026, '门户审核', 'srm:supplier-portal:review', 2, 40, 991000, 'supplier-portal-review', 'ep:document', 'srm/supplier-portal/review/index', 'SrmSupplierPortalReview', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991026 OR `permission` = 'srm:supplier-portal:review');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 991027, '门户审核查询', 'srm:supplier-portal:review', 3, 10, 991026, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991027);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 991028, '门户审核操作', 'srm:supplier-portal:audit', 3, 20, 991026, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 991028 OR `permission` = 'srm:supplier-portal:audit');
