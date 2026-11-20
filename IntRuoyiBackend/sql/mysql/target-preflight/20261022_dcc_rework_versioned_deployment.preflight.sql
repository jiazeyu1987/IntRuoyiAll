-- release-target-preflight: migrationId=20261022_dcc_rework_versioned_deployment; allowedEnvironments=test,backup,prod
WITH latest AS (
 SELECT d.* FROM act_re_procdef d
 WHERE d.KEY_='dcc-controlled-file-approval' AND NOT EXISTS (
 SELECT 1 FROM act_re_procdef n WHERE n.KEY_=d.KEY_ AND n.TENANT_ID_=d.TENANT_ID_ AND n.VERSION_>d.VERSION_)
), sources AS (
 SELECT d.*,b.BYTES_ AS body FROM latest d JOIN act_ge_bytearray b
 ON b.DEPLOYMENT_ID_=d.DEPLOYMENT_ID_ AND b.NAME_=d.RESOURCE_NAME_
)
SELECT CASE WHEN (SELECT COUNT(*) FROM sources)=2
 AND (SELECT COUNT(DISTINCT TENANT_ID_) FROM sources WHERE TENANT_ID_ IN ('1','122') AND SUSPENSION_STATE_=1)=2
 AND NOT EXISTS (SELECT 1 FROM sources WHERE
 CONVERT(body USING utf8mb4) NOT LIKE '%id="DOC_CONTROL_REVIEW"%'
 OR CONVERT(body USING utf8mb4) NOT LIKE '%id="MATRIX_REVIEW"%'
 OR CONVERT(body USING utf8mb4) NOT LIKE '%id="MATRIX_APPROVAL"%'
 OR CONVERT(body USING utf8mb4) NOT LIKE '%id="DOC_CONTROL_APPROVAL"%'
 OR CONVERT(body USING utf8mb4) LIKE '%APPLICANT_REWORK%'
 OR ((CHAR_LENGTH(CONVERT(body USING utf8mb4))-CHAR_LENGTH(REGEXP_REPLACE(CONVERT(body USING utf8mb4),'<userTask','')))/9)<>4)
 AND NOT EXISTS (SELECT 1 FROM sources d WHERE
 (SELECT COUNT(*) FROM bpm_process_definition_info i WHERE i.process_definition_id=d.ID_ AND i.tenant_id=CAST(d.TENANT_ID_ AS UNSIGNED) AND i.deleted=b'0')<>1)
 AND (SELECT COUNT(*) FROM act_re_model WHERE KEY_='dcc-controlled-file-approval')=1
 AND EXISTS (SELECT 1 FROM act_re_model m JOIN sources d ON m.TENANT_ID_=d.TENANT_ID_ AND m.DEPLOYMENT_ID_=d.DEPLOYMENT_ID_
 JOIN act_ge_bytearray b ON b.ID_=m.EDITOR_SOURCE_VALUE_ID_
 WHERE m.KEY_='dcc-controlled-file-approval' AND m.TENANT_ID_='1' AND SHA2(b.BYTES_,256)=SHA2(d.body,256)) THEN 'TARGET_PREFLIGHT_PASS:20261022_dcc_rework_versioned_deployment' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261022_dcc_rework_versioned_deployment' END;
