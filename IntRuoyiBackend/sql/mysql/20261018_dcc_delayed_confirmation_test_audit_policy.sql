-- release-migration: allowedEnvironments=test; dependsOn=20260908_gxp_audit_trail_core; type=config; riskLevel=medium
-- Deliver the existing integration-02 software policy to local/test tenant 1 only.
-- Not quality approval or production adoption. Preserve all existing policies and business evidence.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_delayed_confirmation_test_policy;
DELIMITER //
CREATE PROCEDURE ensure_dcc_delayed_confirmation_test_policy()
BEGIN
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id IN ('dcc.controlled-file.control','signature.record.create')
          AND active=b'1' AND deleted=b'0' AND applicability='GXP')<>2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_DELAYED_EXISTING_AUDIT_POLICIES_REQUIRED';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gxp_audit_policy_operation
                   WHERE tenant_id=1 AND operation_id='dcc.controlled-file.delayed-effective-confirm') THEN
        INSERT INTO gxp_audit_policy_operation
          (tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,
           reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active,creator,updater)
        VALUES
          (1,'2026-10-dcc-integration-02','dcc.controlled-file.delayed-effective-confirm','SERVICE_METHOD',
           'cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordDelayedConfirmation',
           'DCC','DCC_CONTROLLED_FILE','CONFIRM','REQUIRED_CATEGORY_AND_TEXT','REQUIRED','PRESENT_TO_PRESENT',
           'GXP_DCC_SIGNATURE','[G01-CONFIRM-IDENTITY, G01-CONFIRM-ROLLBACK, G01-CONFIRM-REPLAY]',
           'dcc-workflow-owner','GXP',b'1','20261018-e2e-unblock','20261018-e2e-unblock');
    END IF;
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='dcc.controlled-file.delayed-effective-confirm')<>1
       OR (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='dcc.controlled-file.delayed-effective-confirm'
          AND policy_version='2026-10-dcc-integration-02' AND source_type='SERVICE_METHOD'
          AND source_locator='cn.iocoder.yudao.module.dcc.service.file.DccWorkflowFileStateAudit#recordDelayedConfirmation'
          AND domain='DCC' AND subject_type='DCC_CONTROLLED_FILE' AND action_type='CONFIRM'
          AND reason_policy='REQUIRED_CATEGORY_AND_TEXT' AND signature_policy='REQUIRED' AND state_policy='PRESENT_TO_PRESENT'
          AND retention_class='GXP_DCC_SIGNATURE'
          AND test_ids='[G01-CONFIRM-IDENTITY, G01-CONFIRM-ROLLBACK, G01-CONFIRM-REPLAY]'
          AND owner='dcc-workflow-owner' AND applicability='GXP' AND active=b'1' AND deleted=b'0')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_DELAYED_CONFIRMATION_POLICY_DRIFT';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_delayed_confirmation_test_policy();
DROP PROCEDURE IF EXISTS ensure_dcc_delayed_confirmation_test_policy;
