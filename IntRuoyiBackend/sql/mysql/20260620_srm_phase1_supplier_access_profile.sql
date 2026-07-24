-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260619_srm_d7_2_supplier_access_risk; type=schema; riskLevel=medium
-- SRM Phase 1 supplier portal profile: portal contact fields and qualification expiry date.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_srm_phase1_supplier_access_profile;

DELIMITER $$
CREATE PROCEDURE ensure_srm_phase1_supplier_access_profile()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'portal_contact_name'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `portal_contact_name` varchar(64) DEFAULT NULL COMMENT '门户联系人' AFTER `access_remark`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'portal_contact_phone'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `portal_contact_phone` varchar(32) DEFAULT NULL COMMENT '门户联系电话' AFTER `portal_contact_name`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'qualification_expire_date'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `qualification_expire_date` date DEFAULT NULL COMMENT '资质到期日' AFTER `portal_contact_phone`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'sample_test_status'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `sample_test_status` varchar(32) DEFAULT NULL COMMENT '样品测试状态' AFTER `qualification_expire_date`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'sample_audit_by'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `sample_audit_by` bigint DEFAULT NULL COMMENT '样品测试审核人编号' AFTER `sample_test_status`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'sample_audit_name'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `sample_audit_name` varchar(64) DEFAULT NULL COMMENT '样品测试审核人名称' AFTER `sample_audit_by`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'sample_audit_time'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `sample_audit_time` datetime DEFAULT NULL COMMENT '样品测试审核时间' AFTER `sample_audit_name`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'sample_audit_remark'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `sample_audit_remark` varchar(500) DEFAULT NULL COMMENT '样品测试审核意见' AFTER `sample_audit_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'trial_order_status'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `trial_order_status` varchar(32) DEFAULT NULL COMMENT '小批试用状态' AFTER `sample_audit_remark`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'trial_audit_by'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `trial_audit_by` bigint DEFAULT NULL COMMENT '小批试用审核人编号' AFTER `trial_order_status`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'trial_audit_name'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `trial_audit_name` varchar(64) DEFAULT NULL COMMENT '小批试用审核人名称' AFTER `trial_audit_by`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'trial_audit_time'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `trial_audit_time` datetime DEFAULT NULL COMMENT '小批试用审核时间' AFTER `trial_audit_name`;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'srm_supplier_access'
      AND COLUMN_NAME = 'trial_audit_remark'
  ) THEN
    ALTER TABLE `srm_supplier_access`
      ADD COLUMN `trial_audit_remark` varchar(500) DEFAULT NULL COMMENT '小批试用审核意见' AFTER `trial_audit_time`;
  END IF;
END$$
DELIMITER ;

CALL ensure_srm_phase1_supplier_access_profile();
DROP PROCEDURE IF EXISTS ensure_srm_phase1_supplier_access_profile;
