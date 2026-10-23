-- REVIEW ONLY: not executed; exact25 tenant1 operation registration, separate from19SQL migration scope.
-- Source policy raw SHA256: 776905347c7726983db317eda110908f6762aa1351a8564e1e130da19b0d59df
-- Real quality approval variables intentionally unset; fill only from an actual confirmed registry record.
-- Requires fresh connection, policy-writer pause, backup and reviewed MySQL rehearsal. Never use --force.
-- Only permanent DML target is gxp_audit_policy_operation; no existing row is changed.
-- CREATE PROCEDURE collision fails; no preemptive DROP of an existing routine.
DELIMITER $$
CREATE PROCEDURE `dcc_gxp25_tenant1_config`(IN quality_reference text, IN quality_approver bigint)
BEGIN
  DECLARE changed_rows int DEFAULT 0;
  DECLARE conflict_rows int DEFAULT 0;
  DECLARE missing_rows int DEFAULT 0;
  DECLARE lock_owned int DEFAULT 0;
  DECLARE temporary_owned int DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF temporary_owned=1 THEN DROP TEMPORARY TABLE `tmp_dcc_gxp25_tenant1`; END IF;
    IF lock_owned=1 THEN DO RELEASE_LOCK('dcc:gxp25:tenant1'); END IF;
    RESIGNAL;
  END;
  IF DATABASE() IS NULL OR DATABASE() <> 'ruoyi-vue-pro' OR @dcc_gxp_tenant1_write_authorized IS NULL OR @dcc_gxp_tenant1_write_authorized <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='tenant1 exact25 config-write authorization required';
  END IF;
  IF quality_reference IS NULL OR TRIM(quality_reference)='' OR CHAR_LENGTH(quality_reference)>256 OR LOWER(quality_reference) LIKE '%pending%'
    OR LOWER(quality_reference) LIKE '%placeholder%' OR BINARY quality_reference=BINARY 'CODEX-IMPLEMENTATION-20260908'
    OR quality_approver IS NULL OR quality_approver<1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='real quality approval record required, not candidate PENDING';
  END IF;
  SELECT GET_LOCK('dcc:gxp25:tenant1',10) INTO lock_owned;
  IF lock_owned IS NULL OR lock_owned<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='policy config lock unavailable'; END IF;
  IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME IN ('gxp_audit_policy_operation','gxp_audit_policy_version') AND ENGINE='InnoDB')<>2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='transactional audit table engines required';
  END IF;
  CREATE TEMPORARY TABLE `tmp_dcc_gxp25_tenant1` (
  `policy_version` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `operation_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_locator` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `domain` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `action_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason_policy` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `signature_policy` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_policy` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `retention_class` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `test_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `applicability` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`operation_id`)
  ) ENGINE=InnoDB;
  SET temporary_owned=1;
  INSERT INTO `tmp_dcc_gxp25_tenant1` (`policy_version`, `operation_id`, `source_type`, `source_locator`, `domain`, `subject_type`, `action_type`, `reason_policy`, `signature_policy`, `state_policy`, `retention_class`, `test_ids`, `owner`, `applicability`) VALUES
  -- exact-operation: dcc.controlled-file.activate
('2026-10-dcc-integration-01', 'dcc.controlled-file.activate', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordActivation', 'DCC', 'DCC_CONTROLLED_FILE', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[CC2-A-LEDGER-02, CC2-A-LEDGER-03]', 'dcc-a', 'GXP'),
  -- exact-operation: dcc.controlled-file.auto-obsolete
('2026-10-dcc-integration-01', 'dcc.controlled-file.auto-obsolete', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordAutomaticObsolete', 'DCC', 'DCC_CONTROLLED_FILE', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_VOIDED', 'GXP_CONTROLLED_DOCUMENT', '[CC2-A-LEDGER-02, CC2-A-LEDGER-03]', 'dcc-a', 'GXP'),
  -- exact-operation: dcc.controlled-file.control
('2026-10-dcc-integration-01', 'dcc.controlled-file.control', 'SERVICE_METHOD', 'cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordControl', 'DCC', 'DCC_CONTROLLED_FILE', 'UPDATE', 'REQUIRED_CATEGORY_AND_TEXT', 'NOT_REQUIRED', 'PRESENT_TO_PRESENT', 'GXP_CONTROLLED_DOCUMENT', '[CC2-A-LEDGER-01, CC2-A-LEDGER-03, CC2-A-LEDGER-06]', 'dcc-a', 'GXP'),
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
  IF (SELECT COUNT(*) FROM `tmp_dcc_gxp25_tenant1`)<>25 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='candidate scope not25'; END IF;
  SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
  START TRANSACTION;
  -- Lock actual approval row and policy range; ordinary policy writers must be paused by Root.
  SELECT id FROM `gxp_audit_policy_version` WHERE tenant_id=1 AND BINARY policy_version=BINARY '2026-10-dcc-integration-01' FOR UPDATE;
  IF (SELECT COUNT(*) FROM `gxp_audit_policy_version` WHERE tenant_id=1
    AND BINARY policy_version=BINARY '2026-10-dcc-integration-01' AND BINARY policy_hash=BINARY '776905347c7726983db317eda110908f6762aa1351a8564e1e130da19b0d59df'
    AND BINARY approval_reference=BINARY quality_reference AND approved_by=quality_approver AND approved_at IS NOT NULL
    AND BINARY coverage_report_hash=BINARY '319fc04a677dcb8670b420c9791bf5e4584d6708e0e765e58dbb637d9ab6b558')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='exact-hash approved quality registry missing/conflicting';
  END IF;
  SELECT id FROM `gxp_audit_policy_operation` WHERE tenant_id=1
    AND (operation_id IN ('dcc.controlled-file.activate', 'dcc.controlled-file.auto-obsolete', 'dcc.controlled-file.control', 'dcc.controlled-file.obsolete', 'dcc.folder-template.delete', 'dcc.folder-template.save', 'dcc.project-attributes.configure', 'dcc.project-file-placement.bind', 'dcc.project-folder.create', 'dcc.project-folder.delete', 'dcc.project-folder.update', 'dcc.project-product.approve', 'dcc.project-product.complete', 'dcc.project-product.create', 'dcc.project-product.resubmit', 'dcc.project-product.retry', 'dcc.project-product.review', 'dcc.project-product.reviewer-config', 'dcc.project-product.write-failed', 'dcc.project-reference.cancel', 'dcc.project-reference.create', 'dcc.relation.arrange', 'dcc.relation.controlled', 'dcc.relation.notification.retry', 'dcc.relation.replace') OR operation_id='dcc.controlled-file.publish') ORDER BY id FOR UPDATE;
  IF (SELECT COUNT(*) FROM `gxp_audit_policy_operation` WHERE tenant_id=1 AND operation_id='dcc.controlled-file.publish')<>1
    OR (SELECT COUNT(*) FROM `gxp_audit_policy_operation` WHERE tenant_id=1 AND BINARY operation_id=BINARY 'dcc.controlled-file.publish'
      AND BINARY policy_version=BINARY '2026-09-approved-01' AND active=b'1' AND deleted=b'0' AND BINARY applicability=BINARY 'GXP'
      AND BINARY source_locator=BINARY 'cn.iocoder.yudao.module.dcc.service.file.DccControlledFilePublishServiceImpl#publishControlledFile')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='preserved publish baseline conflict';
  END IF;
  SELECT COUNT(*) INTO conflict_rows FROM (
    SELECT candidate.operation_id FROM `tmp_dcc_gxp25_tenant1` candidate
    JOIN `gxp_audit_policy_operation` existing ON existing.tenant_id=1 AND existing.operation_id=candidate.operation_id
    GROUP BY candidate.operation_id HAVING COUNT(*)<>1 OR SUM(existing.active=b'1' AND existing.deleted=b'0' AND
      BINARY existing.`policy_version` <=> BINARY candidate.`policy_version` AND
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
  ) conflicts;
  IF conflict_rows<>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing policy payload conflict; no overwrite allowed'; END IF;
  SELECT COUNT(*) INTO missing_rows FROM `tmp_dcc_gxp25_tenant1` candidate
    WHERE NOT EXISTS (SELECT 1 FROM `gxp_audit_policy_operation` existing WHERE existing.tenant_id=1 AND existing.operation_id=candidate.operation_id);
  IF missing_rows>0 THEN
  INSERT INTO `gxp_audit_policy_operation` (`tenant_id`, `policy_version`, `operation_id`, `source_type`, `source_locator`, `domain`, `subject_type`, `action_type`, `reason_policy`, `signature_policy`, `state_policy`, `retention_class`, `test_ids`, `owner`, `applicability`, `active`, `deleted`)
  SELECT 1, candidate.`policy_version`, candidate.`operation_id`, candidate.`source_type`, candidate.`source_locator`, candidate.`domain`, candidate.`subject_type`, candidate.`action_type`, candidate.`reason_policy`, candidate.`signature_policy`, candidate.`state_policy`, candidate.`retention_class`, candidate.`test_ids`, candidate.`owner`, candidate.`applicability`, b'1', b'0'
  FROM `tmp_dcc_gxp25_tenant1` candidate
  WHERE NOT EXISTS (SELECT 1 FROM `gxp_audit_policy_operation` existing
    WHERE existing.tenant_id=1 AND existing.operation_id=candidate.operation_id);
  SET changed_rows=ROW_COUNT();
  END IF;
  IF changed_rows<0 OR changed_rows>25 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='insert count outside25 scope'; END IF;
  IF (SELECT COUNT(*) FROM `gxp_audit_policy_operation` existing JOIN `tmp_dcc_gxp25_tenant1` candidate
    ON existing.tenant_id=1 AND existing.operation_id=candidate.operation_id
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
      BINARY existing.`applicability` <=> BINARY candidate.`applicability`)<>25 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='post-insert exact25 payload validation failed';
  END IF;
  COMMIT;
  DROP TEMPORARY TABLE `tmp_dcc_gxp25_tenant1`;
  SET temporary_owned=0;
  DO RELEASE_LOCK('dcc:gxp25:tenant1');
  SELECT changed_rows AS inserted_operations, 0 AS updated_operations, 0 AS deleted_operations;
END$$
DELIMITER ;
CALL `dcc_gxp25_tenant1_config`(@dcc_gxp_quality_approval_reference, @dcc_gxp_quality_approved_by);
DROP PROCEDURE `dcc_gxp25_tenant1_config`;
