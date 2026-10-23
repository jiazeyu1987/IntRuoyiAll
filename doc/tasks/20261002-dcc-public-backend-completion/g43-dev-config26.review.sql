-- DEVELOPMENT_TEST_ONLY: exact26 operations, no QA approval/version row or business action.
-- Candidate version 2026-10-dcc-integration-01; policySHA 661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894; coverageSHA 3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d
-- Root-only actual execution after fresh local23306 identity, all-row backup/writer exclusion.
-- No preDROP, INSERT IGNORE/upsert/update/delete/--force. Helper collision stops before CALL.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone='+08:00';
DELIMITER $$
CREATE PROCEDURE `dcc_g43_dev26_config`()
BEGIN
  DECLARE changed_rows INT DEFAULT 0;
  DECLARE conflicts INT DEFAULT 0;
  DECLARE existing_rows INT DEFAULT 0;
  DECLARE lock_owned INT DEFAULT 0;
  DECLARE temporary_owned INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF temporary_owned=1 THEN DROP TEMPORARY TABLE `tmp_dcc_g43_dev26`; END IF;
    IF lock_owned=1 THEN DO RELEASE_LOCK('dcc:g43:dev26:tenant1'); END IF;
    RESIGNAL;
  END;
  IF @dcc_g43_dev_only IS NULL OR @dcc_g43_dev_only<>1 OR @dcc_g43_write_authorized IS NULL OR @dcc_g43_write_authorized<>1
    OR @dcc_g43_policy_sha IS NULL OR BINARY @dcc_g43_policy_sha<>BINARY '661af676e1406e86659806af8be8f46abd17d101d871d3af8d5b3b736873c894'
    OR @dcc_g43_coverage_sha IS NULL OR BINARY @dcc_g43_coverage_sha<>BINARY '3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d'
    OR DATABASE()<>'ruoyi-vue-pro' OR @@server_uuid<>'92ca05d0-aec8-11f1-a944-02b4e226a5ef' OR @@version<>'8.0.40' OR @@autocommit<>1
    OR @@character_set_connection<>'utf8mb4' OR @@collation_connection<>'utf8mb4_unicode_ci' OR @@session.time_zone<>'+08:00'
    OR FIND_IN_SET('STRICT_TRANS_TABLES',@@session.sql_mode)=0 OR FIND_IN_SET('NO_ENGINE_SUBSTITUTION',@@session.sql_mode)=0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact authorized local source development session required';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version','dcc_legacy_source_name_scope','dcc_legacy_source_name_evidence','dcc_source_name_reservation') AND ENGINE='InnoDB')<>5
    OR (SELECT COUNT(*) FROM infra_release_migration WHERE migration_id='20261003_dcc_legacy_source_name_occupancy' AND sha256='621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a' AND status='APPLIED' AND target_environment='test' AND deleted=b'0')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='actual source sidecar prerequisite missing';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='policy_version' AND COLUMN_TYPE='varchar(128)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='operation_id' AND COLUMN_TYPE='varchar(128)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='source_type' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='source_locator' AND COLUMN_TYPE='varchar(512)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='domain' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='subject_type' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='action_type' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='reason_policy' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='signature_policy' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='state_policy' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='retention_class' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='test_ids' AND COLUMN_TYPE='varchar(512)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='owner' AND COLUMN_TYPE='varchar(128)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1 OR
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME='applicability' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO' AND COLLATION_NAME='utf8mb4_unicode_ci')<>1
    OR (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND COLUMN_NAME IN ('active','deleted') AND COLUMN_TYPE='bit(1)' AND IS_NULLABLE='NO')<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact operation schema field types/collation missing';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND INDEX_NAME='uk_gxp_audit_policy_operation')<>3
    OR (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gxp_audit_policy_operation' AND INDEX_NAME='uk_gxp_audit_policy_operation' AND NON_UNIQUE=0 AND SUB_PART IS NULL AND ((SEQ_IN_INDEX=1 AND COLUMN_NAME='tenant_id') OR (SEQ_IN_INDEX=2 AND COLUMN_NAME='operation_id') OR (SEQ_IN_INDEX=3 AND COLUMN_NAME='policy_version')))<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact operation unique namespace index required';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE='gxp_audit_policy_operation')<>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='unexpected operation-table trigger blocks development insert';
  END IF;
  SELECT GET_LOCK('dcc:g43:dev26:tenant1',0) INTO lock_owned;
  IF lock_owned IS NULL OR lock_owned<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='development policy writer lock unavailable'; END IF;
  CREATE TEMPORARY TABLE `tmp_dcc_g43_dev26` (
 `policy_version` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `operation_id` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `source_type` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `source_locator` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `domain` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `subject_type` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `action_type` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `reason_policy` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `signature_policy` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `state_policy` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `retention_class` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `test_ids` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `owner` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 `applicability` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
 PRIMARY KEY (`operation_id`)
  ) ENGINE=InnoDB;
  SET temporary_owned=1;
  INSERT INTO `tmp_dcc_g43_dev26` (`policy_version`, `operation_id`, `source_type`, `source_locator`, `domain`, `subject_type`, `action_type`, `reason_policy`, `signature_policy`, `state_policy`, `retention_class`, `test_ids`, `owner`, `applicability`) VALUES
  -- exact-operation: dcc.controlled-file.activate
('2026-10-dcc-integration-01', 'dcc.controlled-file.activate', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordActivation', 'DCC', 'DCC_CONTROLLED_FILE', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[CC2-A-LEDGER-02, CC2-A-LEDGER-03]', 'dcc-a', 'GXP'),
  -- exact-operation: dcc.controlled-file.auto-obsolete
('2026-10-dcc-integration-01', 'dcc.controlled-file.auto-obsolete', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordAutomaticObsolete', 'DCC', 'DCC_CONTROLLED_FILE', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_VOIDED', 'GXP_CONTROLLED_DOCUMENT', '[CC2-A-LEDGER-02, CC2-A-LEDGER-03]', 'dcc-a', 'GXP'),
  -- exact-operation: dcc.controlled-file.control
('2026-10-dcc-integration-01', 'dcc.controlled-file.control', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordControl', 'DCC', 'DCC_CONTROLLED_FILE', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[CC2-A-LEDGER-01, CC2-A-LEDGER-03, CC2-A-LEDGER-06]', 'dcc-a', 'GXP'),
  -- exact-operation: dcc.controlled-file.legacy-name-occupancy.activate
('2026-10-dcc-integration-01', 'dcc.controlled-file.legacy-name-occupancy.activate', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccLegacySourceNameRegistrationService#activateVerifiedScope', 'DCC', 'DCC_LEGACY_SOURCE_NAME_SCOPE', 'ACTIVATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[LEGACY-ACT-01, LEGACY-ACT-02, LEGACY-ACT-03]', 'dcc-owner', 'GXP'),
  -- exact-operation: dcc.controlled-file.obsolete
('2026-10-dcc-integration-01', 'dcc.controlled-file.obsolete', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordApprovedObsolete', 'DCC', 'DCC_CONTROLLED_FILE', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_VOIDED', 'GXP_CONTROLLED_DOCUMENT', '[CC2-A-LEDGER-04, CC2-A-LEDGER-05]', 'dcc-a', 'GXP'),
  -- exact-operation: dcc.folder-template.delete
('2026-10-dcc-integration-01', 'dcc.folder-template.delete', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService#delete', 'DCC', 'DCC_PROJECT_CONFIGURATION', 'DELETE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_VOIDED', 'GXP_CONTROLLED_DOCUMENT', '[B-08]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.folder-template.save
('2026-10-dcc-integration-01', 'dcc.folder-template.save', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService#save', 'DCC', 'DCC_PROJECT_CONFIGURATION', 'CREATE_OR_UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_OR_PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-08]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-attributes.configure
('2026-10-dcc-integration-01', 'dcc.project-attributes.configure', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectAttributesController#configure', 'DCC', 'DCC_PROJECT_CONFIGURATION', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-04]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-file-placement.bind
('2026-10-dcc-integration-01', 'dcc.project-file-placement.bind', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService#bind', 'DCC', 'DCC_PROJECT_FILE_PLACEMENT', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-R03, B-PLACEMENT]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-folder.create
('2026-10-dcc-integration-01', 'dcc.project-folder.create', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService#save', 'DCC', 'DCC_PROJECT_FOLDER', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-R03, B-FOLDER-CREATE]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-folder.delete
('2026-10-dcc-integration-01', 'dcc.project-folder.delete', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService#delete', 'DCC', 'DCC_PROJECT_FOLDER', 'DELETE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_VOIDED', 'GXP_CONTROLLED_DOCUMENT', '[D2-02, CC2-B-FOLDER-DELETE]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-folder.update
('2026-10-dcc-integration-01', 'dcc.project-folder.update', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService#save', 'DCC', 'DCC_PROJECT_FOLDER', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-R03, B-FOLDER-UPDATE]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-product.approve
('2026-10-dcc-integration-01', 'dcc.project-product.approve', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateStateService#markApprovalDecision', 'DCC', 'DCC_PROJECT_PRODUCT_REQUEST', 'APPROVE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-REV-01, D2-03]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-product.complete
('2026-10-dcc-integration-01', 'dcc.project-product.complete', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateWriteService#writeApprovedRequest', 'DCC', 'DCC_PROJECT_PRODUCT_REQUEST', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-REV-01, D2-03]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-product.create
('2026-10-dcc-integration-01', 'dcc.project-product.create', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateServiceImpl#createRequest', 'DCC', 'DCC_PROJECT_PRODUCT_REQUEST', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-REV-01, D2-03]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-product.resubmit
('2026-10-dcc-integration-01', 'dcc.project-product.resubmit', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateServiceImpl#resubmitRejectedRequest', 'DCC', 'DCC_PROJECT_PRODUCT_REQUEST', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-01]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-product.retry
('2026-10-dcc-integration-01', 'dcc.project-product.retry', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateStateService#markRetryWriting', 'DCC', 'DCC_PROJECT_PRODUCT_REQUEST', 'RETRY', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-REV-01, D2-03]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-product.review
('2026-10-dcc-integration-01', 'dcc.project-product.review', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateServiceImpl#review', 'DCC', 'DCC_PROJECT_PRODUCT_REQUEST', 'REVIEW', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-REV-01, D2-03]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-product.reviewer-config
('2026-10-dcc-integration-01', 'dcc.project-product.reviewer-config', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerConfigurationService#save', 'DCC', 'DCC_PROJECT_CONFIGURATION', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_OR_PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[P07]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-product.write-failed
('2026-10-dcc-integration-01', 'dcc.project-product.write-failed', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateFailureService#markWriteFailed', 'DCC', 'DCC_PROJECT_PRODUCT_REQUEST', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[B-REV-01, D2-03]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-reference.cancel
('2026-10-dcc-integration-01', 'dcc.project-reference.cancel', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.relations.DccProjectReferenceService#cancel', 'DCC', 'DCC_PROJECT_REFERENCE', 'DELETE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_ABSENT', 'GXP_CONTROLLED_DOCUMENT', '[D-10]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.project-reference.create
('2026-10-dcc-integration-01', 'dcc.project-reference.create', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.relations.DccProjectReferenceService#create', 'DCC', 'DCC_PROJECT_REFERENCE', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[D-08]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.relation.arrange
('2026-10-dcc-integration-01', 'dcc.relation.arrange', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService#saveArrangements', 'DCC', 'DCC_RELATION_ARRANGEMENT', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[D-06]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.relation.controlled
('2026-10-dcc-integration-01', 'dcc.relation.controlled', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService#recordControlled', 'DCC', 'DCC_CONTROLLED_EVENT', 'CREATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'ABSENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[D-07]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.relation.notification.retry
('2026-10-dcc-integration-01', 'dcc.relation.notification.retry', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationNotificationRecoveryService#requestRetry', 'DCC', 'DCC_RELATION_DELIVERY', 'RETRY', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[D-07]', 'dcc-integration', 'GXP'),
  -- exact-operation: dcc.relation.replace
('2026-10-dcc-integration-01', 'dcc.relation.replace', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccControlledFileRelatedFileServiceImpl#replaceCurrentRelations', 'DCC', 'DCC_FILE_RELATION', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[D-01]', 'dcc-integration', 'GXP');
  IF (SELECT COUNT(*) FROM `tmp_dcc_g43_dev26`)<>26 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='candidate scope not exact26'; END IF;
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  START TRANSACTION;
  SELECT id FROM gxp_audit_policy_operation WHERE tenant_id=1 AND (operation_id IN ('dcc.controlled-file.activate', 'dcc.controlled-file.auto-obsolete', 'dcc.controlled-file.control', 'dcc.controlled-file.legacy-name-occupancy.activate', 'dcc.controlled-file.obsolete', 'dcc.folder-template.delete', 'dcc.folder-template.save', 'dcc.project-attributes.configure', 'dcc.project-file-placement.bind', 'dcc.project-folder.create', 'dcc.project-folder.delete', 'dcc.project-folder.update', 'dcc.project-product.approve', 'dcc.project-product.complete', 'dcc.project-product.create', 'dcc.project-product.resubmit', 'dcc.project-product.retry', 'dcc.project-product.review', 'dcc.project-product.reviewer-config', 'dcc.project-product.write-failed', 'dcc.project-reference.cancel', 'dcc.project-reference.create', 'dcc.relation.arrange', 'dcc.relation.controlled', 'dcc.relation.notification.retry', 'dcc.relation.replace') OR operation_id='dcc.controlled-file.publish') ORDER BY id FOR UPDATE;
  IF (SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id='dcc.controlled-file.publish')<>1
    OR (SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=1 AND BINARY operation_id=BINARY 'dcc.controlled-file.publish' AND BINARY policy_version=BINARY '2026-09-approved-01' AND active=b'1' AND deleted=b'0' AND BINARY applicability=BINARY 'GXP' AND BINARY source_locator=BINARY 'cn.iocoder.yudao.module.dcc.service.file.DccControlledFilePublishServiceImpl#publishControlledFile')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='preserved publish09 baseline conflict';
  END IF;
  SELECT COUNT(*) INTO conflicts FROM (
    SELECT candidate.operation_id FROM `tmp_dcc_g43_dev26` candidate JOIN gxp_audit_policy_operation existing ON existing.tenant_id=1 AND existing.operation_id=candidate.operation_id
    GROUP BY candidate.operation_id HAVING COUNT(*)<>1 OR SUM(existing.active=b'1' AND existing.deleted=b'0' AND BINARY existing.`policy_version` <=> BINARY candidate.`policy_version` AND
      BINARY existing.`operation_id` <=> BINARY candidate.`operation_id` AND
      BINARY existing.`source_type` <=> BINARY candidate.`source_type` AND
      BINARY existing.`source_locator` <=> BINARY candidate.`source_locator` AND
      BINARY existing.`domain` <=> BINARY candidate.`domain` AND
      BINARY existing.`subject_type` <=> BINARY candidate.`subject_type` AND
      BINARY existing.`action_type` <=> BINARY candidate.`action_type` AND
      BINARY existing.`reason_policy` <=> BINARY candidate.`reason_policy` AND
      BINARY existing.`signature_policy` <=> BINARY candidate.`signature_policy` AND
      BINARY existing.`state_policy` <=> BINARY candidate.`state_policy` AND
      BINARY existing.`retention_class` <=> BINARY candidate.`retention_class` AND
      BINARY existing.`test_ids` <=> BINARY candidate.`test_ids` AND
      BINARY existing.`owner` <=> BINARY candidate.`owner` AND
      BINARY existing.`applicability` <=> BINARY candidate.`applicability`)<>1
  ) invalid_rows;
  IF conflicts<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing namespace payload/version/state conflict'; END IF;
  SELECT COUNT(*) INTO existing_rows FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id IN ('dcc.controlled-file.activate', 'dcc.controlled-file.auto-obsolete', 'dcc.controlled-file.control', 'dcc.controlled-file.legacy-name-occupancy.activate', 'dcc.controlled-file.obsolete', 'dcc.folder-template.delete', 'dcc.folder-template.save', 'dcc.project-attributes.configure', 'dcc.project-file-placement.bind', 'dcc.project-folder.create', 'dcc.project-folder.delete', 'dcc.project-folder.update', 'dcc.project-product.approve', 'dcc.project-product.complete', 'dcc.project-product.create', 'dcc.project-product.resubmit', 'dcc.project-product.retry', 'dcc.project-product.review', 'dcc.project-product.reviewer-config', 'dcc.project-product.write-failed', 'dcc.project-reference.cancel', 'dcc.project-reference.create', 'dcc.relation.arrange', 'dcc.relation.controlled', 'dcc.relation.notification.retry', 'dcc.relation.replace');
  IF existing_rows NOT IN (0,26) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='partial26 namespace requires reviewed repair'; END IF;
  IF existing_rows=0 THEN
    INSERT INTO gxp_audit_policy_operation (`tenant_id`, `policy_version`, `operation_id`, `source_type`, `source_locator`, `domain`, `subject_type`, `action_type`, `reason_policy`, `signature_policy`, `state_policy`, `retention_class`, `test_ids`, `owner`, `applicability`, `active`, `deleted`)
    SELECT 1, candidate.`policy_version`, candidate.`operation_id`, candidate.`source_type`, candidate.`source_locator`, candidate.`domain`, candidate.`subject_type`, candidate.`action_type`, candidate.`reason_policy`, candidate.`signature_policy`, candidate.`state_policy`, candidate.`retention_class`, candidate.`test_ids`, candidate.`owner`, candidate.`applicability`, b'1', b'0' FROM `tmp_dcc_g43_dev26` candidate;
    SET changed_rows=ROW_COUNT();
    IF changed_rows<>26 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact26 insertion count failed'; END IF;
  END IF;
  IF (SELECT COUNT(*) FROM gxp_audit_policy_operation existing JOIN `tmp_dcc_g43_dev26` candidate ON existing.tenant_id=1 AND existing.operation_id=candidate.operation_id
      WHERE existing.active=b'1' AND existing.deleted=b'0' AND BINARY existing.`policy_version` <=> BINARY candidate.`policy_version` AND
      BINARY existing.`operation_id` <=> BINARY candidate.`operation_id` AND
      BINARY existing.`source_type` <=> BINARY candidate.`source_type` AND
      BINARY existing.`source_locator` <=> BINARY candidate.`source_locator` AND
      BINARY existing.`domain` <=> BINARY candidate.`domain` AND
      BINARY existing.`subject_type` <=> BINARY candidate.`subject_type` AND
      BINARY existing.`action_type` <=> BINARY candidate.`action_type` AND
      BINARY existing.`reason_policy` <=> BINARY candidate.`reason_policy` AND
      BINARY existing.`signature_policy` <=> BINARY candidate.`signature_policy` AND
      BINARY existing.`state_policy` <=> BINARY candidate.`state_policy` AND
      BINARY existing.`retention_class` <=> BINARY candidate.`retention_class` AND
      BINARY existing.`test_ids` <=> BINARY candidate.`test_ids` AND
      BINARY existing.`owner` <=> BINARY candidate.`owner` AND
      BINARY existing.`applicability` <=> BINARY candidate.`applicability`)<>26 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='post-insert exact26 payload failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE `tmp_dcc_g43_dev26`;
  SET temporary_owned=0;
  DO RELEASE_LOCK('dcc:g43:dev26:tenant1');
  SET lock_owned=0;
  SELECT 'DEVELOPMENT_TEST_ONLY' AS configuration_scope,changed_rows AS inserted_operations,0 AS quality_version_inserts,0 AS updated_operations,0 AS deleted_operations;
END$$
DELIMITER ;
CALL `dcc_g43_dev26_config`();
DROP PROCEDURE `dcc_g43_dev26_config`;
