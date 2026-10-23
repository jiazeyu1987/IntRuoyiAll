-- release-migration: allowedEnvironments=test; dependsOn=20260908_gxp_audit_trail_core; type=config; riskLevel=medium
-- Local/test rollout of the existing G12 software policy only. Not quality approval or production adoption.
-- Preserve the strict bind operation and all existing policy/event rows. Do not replay the whole YAML.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_upload_placement_test_policy;
DELIMITER //
CREATE PROCEDURE ensure_dcc_upload_placement_test_policy()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM gxp_audit_policy_operation
                   WHERE tenant_id=1 AND operation_id='dcc.project-file-placement.bind'
                     AND active=b'1' AND deleted=b'0' AND applicability='GXP'
                     AND reason_policy='REQUIRED_CATEGORY_AND_TEXT') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_STRICT_PLACEMENT_POLICY_REQUIRED';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gxp_audit_policy_operation
                   WHERE tenant_id=1 AND operation_id='dcc.project-file-placement.upload-bind') THEN
        INSERT INTO gxp_audit_policy_operation
          (tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,
           reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active,creator,updater)
        VALUES
          (1,'2026-10-dcc-integration-02','dcc.project-file-placement.upload-bind','SERVICE_METHOD',
           'cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService#bindDerived',
           'DCC','DCC_PROJECT_FILE_PLACEMENT','CREATE','OPTIONAL_TEXT','NOT_REQUIRED','ABSENT_TO_PRESENT',
           'GXP_CONTROLLED_DOCUMENT','[G12-OPTIONAL-UPLOAD-PLACEMENT]','dcc-integration','GXP',b'1',
           '20261017-e2e-unblock','20261017-e2e-unblock');
    END IF;
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='dcc.project-file-placement.upload-bind')<>1
       OR (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='dcc.project-file-placement.upload-bind'
          AND policy_version='2026-10-dcc-integration-02' AND source_type='SERVICE_METHOD'
          AND source_locator='cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService#bindDerived'
          AND domain='DCC' AND subject_type='DCC_PROJECT_FILE_PLACEMENT' AND action_type='CREATE'
          AND reason_policy='OPTIONAL_TEXT' AND signature_policy='NOT_REQUIRED' AND state_policy='ABSENT_TO_PRESENT'
          AND retention_class='GXP_CONTROLLED_DOCUMENT' AND test_ids='[G12-OPTIONAL-UPLOAD-PLACEMENT]'
          AND owner='dcc-integration' AND applicability='GXP' AND active=b'1' AND deleted=b'0')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_UPLOAD_PLACEMENT_POLICY_DRIFT';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_upload_placement_test_policy();
DROP PROCEDURE IF EXISTS ensure_dcc_upload_placement_test_policy;
