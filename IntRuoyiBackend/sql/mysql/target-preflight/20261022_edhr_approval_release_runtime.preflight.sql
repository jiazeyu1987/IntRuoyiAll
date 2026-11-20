-- release-target-preflight: migrationId=20261022_edhr_approval_release_runtime; allowedEnvironments=test,prod,backup
-- Source drafts 20261010_edhr_approval_content_v2_draft.sql:11 and
-- 20261011_edhr_approval_runtime_v2.sql:116 require explicit legacy qualification.
-- INITIAL is a new-profile policy, RESET_REQUIRED is an audited reset policy;
-- neither authorizes assigning a legacy credential status during release.
-- Block all non-deleted standalone legacy profiles (including disabled profiles,
-- which may be re-enabled). Linked formal users use SYSTEM_USER credentials.
-- No employee status or password is changed by this check or inferred as ACTIVE.
SELECT CASE
 WHEN EXISTS (SELECT 1 FROM mes_pro_process_pool_team_employee_profile
              WHERE system_user_id IS NULL AND deleted=b'0')
 THEN 'TARGET_PREFLIGHT_BLOCKED:20261022_edhr_approval_release_runtime:EMPLOYEE_SIGNATURE_QUALIFICATION_REQUIRED'
 WHEN ((SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('system_approval_authority_revision','system_signature_authentication_event','mes_employee_signature_password_history','bpm_approval_review_prepare','bpm_approval_review_receipt','bpm_approval_process_ownership','bpm_approval_selected_candidate','mes_edhr_source_creation_binding','mes_edhr_release_submission_material_association','mes_edhr_managed_handoff_material_association'))
 +(SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME IN ('system_users_signature_identity_insert','system_users_signature_identity_update','system_signature_authentication_event_no_update','system_signature_authentication_event_no_delete','mes_employee_signature_generation_insert','mes_employee_signature_generation_update','trg_approval_owner_no_update','trg_approval_owner_no_delete','trg_approval_candidate_no_update','trg_approval_candidate_no_delete'))
 +(SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE() AND ROUTINE_NAME='intruoyi_release_add_column')
 +(SELECT COUNT(*) FROM (SELECT tenant_id,LOWER(TRIM(username)) AS name FROM system_users GROUP BY tenant_id,LOWER(TRIM(username)) HAVING COUNT(*)>1) duplicate_identities))=0 THEN 'TARGET_PREFLIGHT_PASS:20261022_edhr_approval_release_runtime' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_edhr_approval_release_runtime' END;
