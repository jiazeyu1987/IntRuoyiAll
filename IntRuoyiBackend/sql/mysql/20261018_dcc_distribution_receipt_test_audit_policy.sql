-- release-migration: allowedEnvironments=test; dependsOn=20260908_gxp_audit_trail_core; type=config; riskLevel=medium
-- Deliver the existing integration-02 receipt policy to local/test tenant 1 only.
-- No quality approval or production adoption; never update existing policy or signature rows.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_receipt_test_policy;
DELIMITER //
CREATE PROCEDURE ensure_dcc_receipt_test_policy()
BEGIN
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='signature.record.create'
          AND signature_policy='REQUIRED' AND active=b'1' AND deleted=b'0' AND applicability='GXP')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_RECEIPT_EXISTING_SIGNATURE_POLICY_REQUIRED';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gxp_audit_policy_operation
                   WHERE tenant_id=1 AND operation_id='signature.dcc.distribution-receipt.create') THEN
        INSERT INTO gxp_audit_policy_operation
          (tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,
           reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active,creator,updater)
        VALUES
          (1,'2026-10-dcc-integration-02','signature.dcc.distribution-receipt.create','SERVICE_METHOD',
           'cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl#sign',
           'SIGNATURE','SIGNATURE_RECORD','CREATE','OPTIONAL_TEXT','REQUIRED','PRESENT_TO_PRESENT',
           'GXP_ELECTRONIC_SIGNATURE','[RA01, RA03, RA04]','qa-owner','GXP',b'1',
           '20261018-e2e-receipt','20261018-e2e-receipt');
    END IF;
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='signature.dcc.distribution-receipt.create')<>1
       OR (SELECT COUNT(*) FROM gxp_audit_policy_operation
        WHERE tenant_id=1 AND operation_id='signature.dcc.distribution-receipt.create'
          AND policy_version='2026-10-dcc-integration-02' AND source_type='SERVICE_METHOD'
          AND source_locator='cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl#sign'
          AND domain='SIGNATURE' AND subject_type='SIGNATURE_RECORD' AND action_type='CREATE'
          AND reason_policy='OPTIONAL_TEXT' AND signature_policy='REQUIRED' AND state_policy='PRESENT_TO_PRESENT'
          AND retention_class='GXP_ELECTRONIC_SIGNATURE' AND test_ids='[RA01, RA03, RA04]'
          AND owner='qa-owner' AND applicability='GXP' AND active=b'1' AND deleted=b'0')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_RECEIPT_TEST_POLICY_DRIFT';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_receipt_test_policy();
DROP PROCEDURE IF EXISTS ensure_dcc_receipt_test_policy;
