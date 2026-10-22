-- release-migration: allowedEnvironments=test; dependsOn=20260908_gxp_audit_trail_core; type=config; riskLevel=medium
-- Deliver the existing integration-02 signature-bind policy to local/test tenant 1 only.
-- Existing policies, historical audit records and business records remain unchanged.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_signature_bind_test_policy;
DELIMITER //
CREATE PROCEDURE ensure_dcc_signature_bind_test_policy()
BEGIN
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='signature.record.create'
          AND signature_policy='REQUIRED' AND active=b'1' AND deleted=b'0' AND applicability='GXP')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_SIGNATURE_BIND_EXISTING_SIGNATURE_POLICY_REQUIRED';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gxp_audit_policy_operation
                   WHERE tenant_id=1 AND operation_id='dcc.distribution.recipient.signature-bind') THEN
        INSERT INTO gxp_audit_policy_operation
          (tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,
           reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active,creator,updater)
        VALUES
          (1,'2026-10-dcc-integration-02','dcc.distribution.recipient.signature-bind','SERVICE_METHOD',
           'cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSignatureBindingService#bindElectronicReceipt',
           'DCC','DCC_DISTRIBUTION_RECIPIENT','CREATE','REQUIRED_CATEGORY_AND_TEXT','REQUIRED','ABSENT_TO_PRESENT',
           'GXP_CONTROLLED_DOCUMENT','[DccElectronicReceiptBindingTest, DccNativeUploadCompletionDatabaseTest]',
           'dcc-owner','GXP',b'1','20261018-e2e-signature-bind','20261018-e2e-signature-bind');
    END IF;
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='dcc.distribution.recipient.signature-bind')<>1
       OR (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='dcc.distribution.recipient.signature-bind'
          AND policy_version='2026-10-dcc-integration-02' AND source_type='SERVICE_METHOD'
          AND source_locator='cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSignatureBindingService#bindElectronicReceipt'
          AND domain='DCC' AND subject_type='DCC_DISTRIBUTION_RECIPIENT' AND action_type='CREATE'
          AND reason_policy='REQUIRED_CATEGORY_AND_TEXT' AND signature_policy='REQUIRED' AND state_policy='ABSENT_TO_PRESENT'
          AND retention_class='GXP_CONTROLLED_DOCUMENT'
          AND test_ids='[DccElectronicReceiptBindingTest, DccNativeUploadCompletionDatabaseTest]'
          AND owner='dcc-owner' AND applicability='GXP' AND active=b'1' AND deleted=b'0')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_SIGNATURE_BIND_TEST_POLICY_DRIFT';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_signature_bind_test_policy();
DROP PROCEDURE IF EXISTS ensure_dcc_signature_bind_test_policy;
