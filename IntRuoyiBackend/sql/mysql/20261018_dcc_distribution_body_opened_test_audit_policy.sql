-- release-migration: allowedEnvironments=test; dependsOn=20260908_gxp_audit_trail_core; type=config; riskLevel=medium
-- Deliver the existing integration-02 body-opened policy to local/test tenant 1 only.
-- Existing policies, historical audit records and business records remain unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_body_opened_test_policy;
DELIMITER //
CREATE PROCEDURE ensure_dcc_body_opened_test_policy()
BEGIN
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='signature.record.create'
          AND signature_policy='REQUIRED' AND active=b'1' AND deleted=b'0' AND applicability='GXP')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_BODY_OPENED_EXISTING_SIGNATURE_POLICY_REQUIRED';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gxp_audit_policy_operation
                   WHERE tenant_id=1 AND operation_id='dcc.distribution.recipient.body-opened') THEN
        INSERT INTO gxp_audit_policy_operation
          (tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,
           reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active,creator,updater)
        VALUES
          (1,'2026-10-dcc-integration-02','dcc.distribution.recipient.body-opened','SERVICE_METHOD',
           'cn.iocoder.yudao.module.dcc.service.file.DccDistributionReceiptServiceImpl#recordDistributionBodyOpened',
           'DCC','DCC_DISTRIBUTION_RECIPIENT','UPDATE','REQUIRED_CATEGORY_AND_TEXT','NOT_REQUIRED','PRESENT_TO_PRESENT',
           'GXP_CONTROLLED_DOCUMENT','[IC11-BODY-OPENED, IC11-READ-FAILURE-ROLLBACK]','dcc-owner','GXP',b'1',
           '20261018-e2e-body-opened','20261018-e2e-body-opened');
    END IF;
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='dcc.distribution.recipient.body-opened')<>1
       OR (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='dcc.distribution.recipient.body-opened'
          AND policy_version='2026-10-dcc-integration-02' AND source_type='SERVICE_METHOD'
          AND source_locator='cn.iocoder.yudao.module.dcc.service.file.DccDistributionReceiptServiceImpl#recordDistributionBodyOpened'
          AND domain='DCC' AND subject_type='DCC_DISTRIBUTION_RECIPIENT' AND action_type='UPDATE'
          AND reason_policy='REQUIRED_CATEGORY_AND_TEXT' AND signature_policy='NOT_REQUIRED' AND state_policy='PRESENT_TO_PRESENT'
          AND retention_class='GXP_CONTROLLED_DOCUMENT' AND test_ids='[IC11-BODY-OPENED, IC11-READ-FAILURE-ROLLBACK]'
          AND owner='dcc-owner' AND applicability='GXP' AND active=b'1' AND deleted=b'0')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_BODY_OPENED_TEST_POLICY_DRIFT';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_body_opened_test_policy();
DROP PROCEDURE IF EXISTS ensure_dcc_body_opened_test_policy;
