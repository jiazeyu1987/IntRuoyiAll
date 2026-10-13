-- READ-ONLY DRAFT, NOT EXECUTED. No historical ownership INSERT is authorized here.
-- A historical row needs a verified real source authority/version plus Java canonical
-- evidenceDigest and original start-writer proof. A title, numeric business key,
-- current role, process variable, or missing relation cannot supply that proof.
-- The query lists every still-uncovered process; any result blocks query activation.
WITH engine_process AS (
 SELECT TENANT_ID_,PROC_INST_ID_,PROC_DEF_ID_,BUSINESS_KEY_,START_TIME_,'HISTORY' inventory_scope
 FROM ACT_HI_PROCINST
 UNION ALL
 SELECT TENANT_ID_,ID_,PROC_DEF_ID_,BUSINESS_KEY_,START_TIME_,'RUNTIME'
 FROM ACT_RU_EXECUTION WHERE BINARY ID_=BINARY PROC_INST_ID_
)
SELECT p.TENANT_ID_,p.PROC_INST_ID_,p.PROC_DEF_ID_,d.KEY_,p.BUSINESS_KEY_,p.START_TIME_,p.inventory_scope
FROM engine_process p
LEFT JOIN ACT_RE_PROCDEF d ON BINARY d.ID_=BINARY p.PROC_DEF_ID_
LEFT JOIN bpm_approval_process_ownership o ON BINARY CAST(o.tenant_id AS CHAR)=BINARY p.TENANT_ID_
  AND BINARY o.process_instance_id=BINARY p.PROC_INST_ID_
WHERE o.id IS NULL OR d.ID_ IS NULL OR BINARY o.process_definition_id<>BINARY p.PROC_DEF_ID_
  OR BINARY o.process_definition_key<>BINARY d.KEY_ OR NOT(BINARY o.business_key <=> BINARY p.BUSINESS_KEY_)
ORDER BY p.TENANT_ID_,CAST(p.PROC_INST_ID_ AS BINARY),p.inventory_scope;

-- Relation inventory, not a migration verdict. Java inspect validates the concrete
-- engine, formal context/snapshot, original writer and candidates. Current DCC
-- pointers are not submission facts. An APPLIED old DCC_UPLOAD execution returned
-- decimal fileId (executor source at 4426f0c93); exact effect/approval event linkage
-- can prove that relation was an approval effect. BusinessRequest presence alone
-- cannot prove this. Unresolved rows remain visible and BLOCKED.
WITH platform_submission AS (
 SELECT tenant_id,process_instance_id,'BUSINESS_REQUEST' authority_type,CAST(id AS CHAR) authority_id
 FROM bpm_business_approval_request WHERE process_instance_id IS NOT NULL
 UNION ALL
 SELECT tenant_id,bpm_process_instance_id,'FORM_ACTION',CAST(id AS CHAR)
 FROM bpm_form_action_instance WHERE bpm_process_instance_id IS NOT NULL
 UNION ALL
 SELECT tenant_id,bpm_process_instance_id,'REGISTRATION_REQUEST',CAST(request_id AS CHAR)
 FROM dcc_registration_certificate_bpm_binding WHERE bpm_process_instance_id IS NOT NULL
 UNION ALL
 SELECT tenant_id,process_instance_id,'CONTROLLED_FILE',authority_id
 FROM bpm_approval_process_ownership WHERE writer_code='DCC_CONTROLLED_FILE_SUBMIT'
   AND projection_code='DCC_TASK' AND authority_type='CONTROLLED_FILE'
), dcc_pointer AS (
 SELECT tenant_id,process_instance_id,id controlled_file_id,version_no
 FROM dcc_controlled_file WHERE process_instance_id IS NOT NULL
 UNION
 SELECT tenant_id,approval_process_instance_id,native_version_id,version_no
 FROM controlled_content_version_ref WHERE content_type='DCC_CONTROLLED_FILE'
   AND approval_process_instance_id IS NOT NULL
), upload_effect_relation AS (
 SELECT f.tenant_id,f.bpm_process_instance_id,e.result_ref controlled_file_id,
   JSON_UNQUOTE(JSON_EXTRACT(s.form_data_json,'$.versionNo')) version_no
 FROM bpm_form_action_instance f JOIN bpm_form_action_snapshot s
   ON s.tenant_id=f.tenant_id AND s.instance_id=f.id AND s.snapshot_type='SUBMIT'
   AND BINARY s.business_context_json=BINARY f.business_context_json
 JOIN bpm_form_effect_execution e ON e.tenant_id=f.tenant_id AND e.instance_id=f.id
   AND e.status='APPLIED' AND BINARY e.idempotency_key=BINARY f.idempotency_key
   AND BINARY e.execution_code=BINARY CONCAT('EFFECT-',f.idempotency_key)
 JOIN controlled_content_version_ref v ON v.tenant_id=f.tenant_id AND v.content_type='DCC_CONTROLLED_FILE'
   AND BINARY CAST(v.native_version_id AS CHAR)=BINARY e.result_ref
   AND BINARY v.approval_process_instance_id=BINARY f.bpm_process_instance_id
   AND BINARY v.version_no=BINARY JSON_UNQUOTE(JSON_EXTRACT(s.form_data_json,'$.versionNo'))
 JOIN controlled_content_transition_audit a ON a.tenant_id=v.tenant_id AND a.version_ref_id=v.id
   AND a.action='APPROVE' AND BINARY a.event_key=BINARY CONCAT(f.idempotency_key,':approve')
 WHERE e.result_ref REGEXP '^[1-9][0-9]*$'
   AND f.system_code='DCC' AND f.object_type='CONTROLLED_FILE' AND f.action_code='UPLOAD'
   AND (SELECT COUNT(*) FROM bpm_form_action_snapshot x WHERE x.tenant_id=f.tenant_id
        AND x.instance_id=f.id AND x.snapshot_type='SUBMIT')=1
), classified AS (
 SELECT tenant_id,process_instance_id,authority_type,authority_id,'SUBMISSION_CANDIDATE' source_semantics
 FROM platform_submission
 UNION ALL
 SELECT p.tenant_id,p.process_instance_id,'CONTROLLED_FILE',CAST(p.controlled_file_id AS CHAR),
   CASE WHEN EXISTS (SELECT 1 FROM upload_effect_relation e WHERE e.tenant_id=p.tenant_id
       AND BINARY e.bpm_process_instance_id=BINARY p.process_instance_id
       AND BINARY e.controlled_file_id=BINARY CAST(p.controlled_file_id AS CHAR)
       AND BINARY e.version_no=BINARY p.version_no)
     THEN 'APPROVAL_EFFECT_RELATION_REQUIRES_JAVA_VERIFICATION'
     ELSE 'BLOCKED_ORIGINAL_DCC_SUBMISSION_PROOF_REQUIRED' END
 FROM dcc_pointer p WHERE NOT EXISTS (SELECT 1 FROM bpm_approval_process_ownership o
   WHERE o.tenant_id=p.tenant_id AND BINARY o.process_instance_id=BINARY p.process_instance_id
     AND o.writer_code='DCC_CONTROLLED_FILE_SUBMIT' AND o.projection_code='DCC_TASK'
     AND o.authority_type='CONTROLLED_FILE' AND BINARY o.authority_id=BINARY CAST(p.controlled_file_id AS CHAR)
     AND BINARY o.authority_version=BINARY p.version_no)
 UNION ALL
 SELECT tenant_id,process_instance_id,'OA_LEAVE',CAST(id AS CHAR),'BLOCKED_ORIGINAL_SUBMIT_VERSION_REQUIRED'
 FROM bpm_oa_leave WHERE process_instance_id IS NOT NULL
 UNION ALL
 SELECT tenant_id,process_instance_id,'CRM_CONTRACT',CAST(id AS CHAR),'BLOCKED_ORIGINAL_SUBMIT_VERSION_REQUIRED'
 FROM crm_contract WHERE process_instance_id IS NOT NULL
 UNION ALL
 SELECT tenant_id,process_instance_id,'CRM_RECEIVABLE',CAST(id AS CHAR),'BLOCKED_ORIGINAL_SUBMIT_VERSION_REQUIRED'
 FROM crm_receivable WHERE process_instance_id IS NOT NULL
)
SELECT c.*,SUM(c.source_semantics='SUBMISSION_CANDIDATE')
 OVER(PARTITION BY c.tenant_id,BINARY c.process_instance_id) submission_candidate_count
FROM classified c ORDER BY c.tenant_id,BINARY c.process_instance_id,c.source_semantics,c.authority_type,c.authority_id;
-- count>1 marks potential competing original authorities. It includes native DCC
-- start facts even when the same file has a platform effect; no blanket NOT EXISTS
-- platform suppression. Every BLOCKED row needs exact source evidence before switching.
