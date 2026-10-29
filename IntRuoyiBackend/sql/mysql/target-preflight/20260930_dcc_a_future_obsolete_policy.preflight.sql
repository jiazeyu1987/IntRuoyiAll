-- release-target-preflight: migrationId=20260930_dcc_a_future_obsolete_policy; allowedEnvironments=test,prod,backup
-- Evaluated immediately before this migration, after its declared predecessors.
SELECT CASE WHEN
 ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND ENGINE='InnoDB' AND TABLE_NAME IN ('bpm_business_approval_policy'))=1)
 AND ((SELECT COUNT(*) FROM bpm_business_approval_policy WHERE tenant_id IN (1,122) AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='OBSOLETE' AND status='PUBLISHED' AND deleted=0 AND object_state='ACTIVE' AND policy_mode='BPM_REQUIRED' AND BINARY process_definition_key=BINARY 'dcc-controlled-file-obsolete' AND effect_executor_code='DCC_OBSOLETE')=2)
 AND (NOT EXISTS(SELECT 1 FROM bpm_business_approval_policy WHERE tenant_id IN (1,122) AND data_domain='DCC' AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND action_code='OBSOLETE' AND status='PUBLISHED' AND deleted=0 AND object_state='CONTROLLED_PENDING_EFFECTIVE' AND (policy_mode<>'BPM_REQUIRED' OR process_definition_key IS NULL OR BINARY process_definition_key<>BINARY 'dcc-controlled-file-obsolete' OR effect_executor_code<>'DCC_OBSOLETE')))
 AND (NOT EXISTS(SELECT 1 FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME IN ('validate_dcc_a_future_obsolete_policy')))
 THEN 'TARGET_PREFLIGHT_PASS:20260930_dcc_a_future_obsolete_policy' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260930_dcc_a_future_obsolete_policy:TARGET_PREREQUISITE_OR_CONFLICT' END;
