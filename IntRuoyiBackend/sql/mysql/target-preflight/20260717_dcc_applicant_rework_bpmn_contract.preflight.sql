-- release-target-preflight: migrationId=20260717_dcc_applicant_rework_bpmn_contract; allowedEnvironments=test,backup,prod
-- Do not replace missing/custom task graphs or rewrite deployed definitions while their process instances/tasks are running.
WITH raw_resources AS (
  SELECT d.ID_ AS procdef_id,CONVERT(b.BYTES_ USING utf8mb4) AS body
    FROM act_re_procdef d JOIN act_ge_bytearray b ON b.DEPLOYMENT_ID_=d.DEPLOYMENT_ID_ AND b.NAME_=d.RESOURCE_NAME_
   WHERE d.KEY_='dcc-controlled-file-approval'
  UNION ALL
  SELECT NULL,CONVERT(b.BYTES_ USING utf8mb4) FROM act_re_model m JOIN act_ge_bytearray b ON b.ID_=m.EDITOR_SOURCE_VALUE_ID_
   WHERE m.KEY_='dcc-controlled-file-approval'
), resources AS (
  SELECT *,CASE WHEN body LIKE '%id="APPLICANT_REWORK"%' AND body LIKE '%flow_applicant_rework_doc_control_review%' THEN 0 ELSE 1 END AS needs_rewrite
  FROM raw_resources
)
SELECT CASE WHEN

  EXISTS (SELECT 1 FROM act_re_procdef WHERE KEY_='dcc-controlled-file-approval')
  AND EXISTS (SELECT 1 FROM act_re_model WHERE KEY_='dcc-controlled-file-approval')
  AND NOT EXISTS (SELECT 1 FROM act_re_model m LEFT JOIN act_ge_bytearray b ON b.ID_=m.EDITOR_SOURCE_VALUE_ID_
                  WHERE m.KEY_='dcc-controlled-file-approval' AND (b.ID_ IS NULL OR b.BYTES_ IS NULL))
  AND NOT EXISTS (SELECT 1 FROM act_re_procdef d LEFT JOIN act_ge_bytearray b ON b.DEPLOYMENT_ID_=d.DEPLOYMENT_ID_ AND b.NAME_=d.RESOURCE_NAME_
                  WHERE d.KEY_='dcc-controlled-file-approval' GROUP BY d.ID_ HAVING COUNT(b.ID_)<>1)
  AND NOT EXISTS (SELECT 1 FROM resources WHERE body IS NULL OR body NOT LIKE '%id="dcc-controlled-file-approval"%')
  AND NOT EXISTS (SELECT 1 FROM resources WHERE needs_rewrite=1 AND
       (body NOT LIKE '%id="DOC_CONTROL_REVIEW"%' OR body NOT LIKE '%id="MATRIX_REVIEW"%'
        OR body NOT LIKE '%id="MATRIX_APPROVAL"%' OR body NOT LIKE '%id="DOC_CONTROL_APPROVAL"%'
        OR ((CHAR_LENGTH(body)-CHAR_LENGTH(REGEXP_REPLACE(body,'<userTask','')))/9)<>4))
  AND NOT EXISTS (SELECT 1 FROM act_ru_execution e JOIN resources r ON r.procdef_id=e.PROC_DEF_ID_ WHERE r.needs_rewrite=1)
  AND NOT EXISTS (SELECT 1 FROM act_ru_task t JOIN resources r ON r.procdef_id=t.PROC_DEF_ID_ WHERE r.needs_rewrite=1)
THEN 'TARGET_PREFLIGHT_PASS:20260717_dcc_applicant_rework_bpmn_contract' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260717_dcc_applicant_rework_bpmn_contract' END;
