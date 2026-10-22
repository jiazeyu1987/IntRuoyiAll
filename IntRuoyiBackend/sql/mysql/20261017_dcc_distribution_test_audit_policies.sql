-- release-migration: allowedEnvironments=test; dependsOn=20260908_gxp_audit_trail_core,20261011_dcc_distribution_outbox; type=config; riskLevel=medium
-- Local/test rollout of existing automatic distribution and recovery policies only.
-- Not quality approval or production adoption. Preserve all existing policies and audit facts.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_dcc_distribution_test_policy;
DELIMITER //
CREATE PROCEDURE ensure_dcc_distribution_test_policy(
    IN operation_value VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, IN locator_value VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, IN action_value VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN state_value VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, IN tests_value TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci)
BEGIN
    IF operation_value NOT IN ('dcc.distribution.batch.prepare','dcc.distribution.attempt.start',
                              'dcc.distribution.attempt.finish','dcc.distribution.batch.retry') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_DISTRIBUTION_TEST_POLICY_SCOPE_INVALID';
    END IF;
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=1
        AND operation_id IN ('dcc.distribution.batch.accept','dcc.distribution.batch.fail')
        AND active=b'1' AND deleted=b'0' AND applicability='GXP'
        AND reason_policy='REQUIRED_CATEGORY_AND_TEXT')<>2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_DISTRIBUTION_BASE_POLICIES_REQUIRED';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id=operation_value) THEN
        INSERT INTO gxp_audit_policy_operation
          (tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,
           reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active,creator,updater)
        VALUES
          (1,'2026-10-dcc-integration-02',operation_value,'SERVICE_METHOD',locator_value,'DCC',
           IF(operation_value LIKE 'dcc.distribution.attempt.%','DCC_DISTRIBUTION_ATTEMPT','DCC_DISTRIBUTION_BATCH'),
           action_value,'REQUIRED_CATEGORY_AND_TEXT','NOT_REQUIRED',state_value,'GXP_CONTROLLED_DOCUMENT',
           tests_value,'dcc-owner','GXP',b'1','20261017-e2e-unblock','20261017-e2e-unblock');
    END IF;
    IF (SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id=operation_value)<>1
       OR (SELECT COUNT(*) FROM gxp_audit_policy_operation WHERE tenant_id=1 AND operation_id=operation_value
        AND policy_version='2026-10-dcc-integration-02' AND source_type='SERVICE_METHOD' AND source_locator=locator_value
        AND domain='DCC' AND subject_type=IF(operation_value LIKE 'dcc.distribution.attempt.%','DCC_DISTRIBUTION_ATTEMPT','DCC_DISTRIBUTION_BATCH')
        AND action_type=action_value AND reason_policy='REQUIRED_CATEGORY_AND_TEXT' AND signature_policy='NOT_REQUIRED'
        AND state_policy=state_value AND retention_class='GXP_CONTROLLED_DOCUMENT' AND test_ids=tests_value
        AND owner='dcc-owner' AND applicability='GXP' AND active=b'1' AND deleted=b'0')<>1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_DISTRIBUTION_TEST_POLICY_DRIFT';
    END IF;
END//
DELIMITER ;
CALL ensure_dcc_distribution_test_policy('dcc.distribution.batch.prepare','cn.iocoder.yudao.module.dcc.service.file.DccDistributionBatchService#prepareAndDispatchBatch','UPDATE','PRESENT_TO_PRESENT','[IC10-PREPARE, IC10-LATE-FAILURE]');
CALL ensure_dcc_distribution_test_policy('dcc.distribution.attempt.start','cn.iocoder.yudao.module.dcc.service.file.DccDistributionDispatchAttemptService#startAttempt','CREATE','ABSENT_TO_PRESENT','[IC10-ATTEMPT-START, IC10-PLATFORM-IDEMPOTENCY]');
CALL ensure_dcc_distribution_test_policy('dcc.distribution.attempt.finish','cn.iocoder.yudao.module.dcc.service.file.DccDistributionDispatchAttemptService#finishAttempt','UPDATE','PRESENT_TO_PRESENT','[IC10-ATTEMPT-ACK, IC10-ACK-RECOVERY]');
CALL ensure_dcc_distribution_test_policy('dcc.distribution.batch.retry','cn.iocoder.yudao.module.dcc.service.file.DccDistributionTaskServiceImpl#retryAutomaticDistribution','UPDATE','PRESENT_TO_PRESENT','[IC31-ROSTER-RETRY, IC31-ROSTER-AUTHORIZATION]');
DROP PROCEDURE IF EXISTS ensure_dcc_distribution_test_policy;
