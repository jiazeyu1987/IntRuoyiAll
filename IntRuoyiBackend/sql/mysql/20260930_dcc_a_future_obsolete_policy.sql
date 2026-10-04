-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_workflow_bpmn_v4,20260930_dcc_a_signature_workflow_round; type=seed; riskLevel=medium
-- A controlled future version uses its own explicit state policy; no UPLOAD/REVISION or ACTIVE runtime fallback.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS validate_dcc_a_future_obsolete_policy;
DELIMITER $$
CREATE PROCEDURE validate_dcc_a_future_obsolete_policy()
BEGIN
  IF (SELECT COUNT(*) FROM bpm_business_approval_policy
      WHERE tenant_id IN (1,122) AND BINARY data_domain=BINARY 'DCC' AND BINARY system_code=BINARY 'DCC'
        AND BINARY object_type=BINARY 'CONTROLLED_FILE' AND BINARY action_code=BINARY 'OBSOLETE'
        AND BINARY object_state=BINARY 'ACTIVE' AND status='PUBLISHED' AND deleted=b'0'
        AND policy_mode='BPM_REQUIRED' AND BINARY process_definition_key=BINARY 'dcc-controlled-file-obsolete'
        AND BINARY effect_executor_code=BINARY 'DCC_OBSOLETE') <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC future obsolete requires exact published obsolete policies for both tenants';
  END IF;
  IF EXISTS (SELECT 1 FROM bpm_business_approval_policy
      WHERE tenant_id IN (1,122) AND BINARY data_domain=BINARY 'DCC' AND BINARY system_code=BINARY 'DCC'
        AND BINARY object_type=BINARY 'CONTROLLED_FILE' AND BINARY action_code=BINARY 'OBSOLETE'
        AND BINARY object_state=BINARY 'CONTROLLED_PENDING_EFFECTIVE' AND status='PUBLISHED' AND deleted=b'0'
        AND (policy_mode<>'BPM_REQUIRED' OR process_definition_key IS NULL
          OR BINARY process_definition_key<>BINARY 'dcc-controlled-file-obsolete'
          OR BINARY effect_executor_code<>BINARY 'DCC_OBSOLETE')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC future obsolete policy conflicts with its formal workflow';
  END IF;
END$$
DELIMITER ;
CALL validate_dcc_a_future_obsolete_policy();
START TRANSACTION;
INSERT INTO bpm_business_approval_policy(
    tenant_id,data_domain,system_code,object_type,action_code,object_state,policy_mode,process_definition_key,
    effect_executor_code,form_policy_type,form_slots_json,status,remark,creator,create_time,updater,update_time,deleted)
SELECT source.tenant_id,source.data_domain,source.system_code,source.object_type,source.action_code,
       'CONTROLLED_PENDING_EFFECTIVE',source.policy_mode,source.process_definition_key,
       source.effect_executor_code,source.form_policy_type,source.form_slots_json,'PUBLISHED',
       '已受控待生效版本独立作废审批','dcc-a',NOW(),'dcc-a',NOW(),b'0'
FROM bpm_business_approval_policy source
WHERE source.tenant_id IN (1,122) AND BINARY source.data_domain=BINARY 'DCC' AND BINARY source.system_code=BINARY 'DCC'
  AND BINARY source.object_type=BINARY 'CONTROLLED_FILE' AND BINARY source.action_code=BINARY 'OBSOLETE'
  AND BINARY source.object_state=BINARY 'ACTIVE' AND source.status='PUBLISHED' AND source.deleted=b'0'
  AND NOT EXISTS (SELECT 1 FROM bpm_business_approval_policy target
    WHERE target.tenant_id=source.tenant_id AND BINARY target.data_domain=BINARY source.data_domain
      AND BINARY target.system_code=BINARY source.system_code AND BINARY target.object_type=BINARY source.object_type
      AND BINARY target.action_code=BINARY source.action_code AND BINARY target.object_state=BINARY 'CONTROLLED_PENDING_EFFECTIVE'
      AND target.status='PUBLISHED' AND target.deleted=b'0');
CALL validate_dcc_a_future_obsolete_policy();
COMMIT;
DROP PROCEDURE validate_dcc_a_future_obsolete_policy;
