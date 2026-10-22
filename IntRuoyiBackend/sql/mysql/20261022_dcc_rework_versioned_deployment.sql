-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260518_dcc_approval_task_name_fix; supersedes=20260717_dcc_applicant_rework_bpmn_contract; type=data; riskLevel=medium; requiresTargetPreflight=true
-- Capture an immutable current definition per authorized tenant. The new backend
-- publishes a new version transactionally through Flowable before reporting UP.
-- Historical resources, running tasks, matrices and history are never rewritten.
DELIMITER $$
CREATE PROCEDURE release_dcc_versioned_rework_precondition()
BEGIN
 DECLARE source_ready BOOLEAN DEFAULT FALSE;
WITH latest AS (
 SELECT d.* FROM act_re_procdef d
 WHERE d.KEY_='dcc-controlled-file-approval' AND NOT EXISTS (
 SELECT 1 FROM act_re_procdef n WHERE n.KEY_=d.KEY_ AND n.TENANT_ID_=d.TENANT_ID_ AND n.VERSION_>d.VERSION_)
), sources AS (
 SELECT d.*,b.BYTES_ AS body FROM latest d JOIN act_ge_bytearray b
 ON b.DEPLOYMENT_ID_=d.DEPLOYMENT_ID_ AND b.NAME_=d.RESOURCE_NAME_
)
 SELECT (SELECT COUNT(*) FROM sources)=2
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
 WHERE m.KEY_='dcc-controlled-file-approval' AND m.TENANT_ID_='1' AND SHA2(b.BYTES_,256)=SHA2(d.body,256)) INTO source_ready;
 IF NOT source_ready THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC_VERSIONED_RELEASE_SOURCE_REQUIRES_REVIEW';
 END IF;
END$$
DELIMITER ;
CALL release_dcc_versioned_rework_precondition();
DROP PROCEDURE release_dcc_versioned_rework_precondition;
CREATE TABLE bpm_dcc_rework_release (
 tenant_id BIGINT NOT NULL PRIMARY KEY,
 source_definition_id VARCHAR(64) NOT NULL,
 source_model_id VARCHAR(64) NULL,
 source_sha256 CHAR(64) NOT NULL,
 target_definition_id VARCHAR(64) NULL,
 status VARCHAR(16) NOT NULL,
 finished_at DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO bpm_dcc_rework_release(tenant_id,source_definition_id,source_model_id,source_sha256,status)
SELECT CAST(d.TENANT_ID_ AS UNSIGNED),d.ID_,m.ID_,SHA2(b.BYTES_,256),'PENDING'
FROM act_re_procdef d JOIN act_ge_bytearray b ON b.DEPLOYMENT_ID_=d.DEPLOYMENT_ID_ AND b.NAME_=d.RESOURCE_NAME_
LEFT JOIN act_re_model m ON m.KEY_=d.KEY_ AND m.TENANT_ID_=d.TENANT_ID_ AND m.DEPLOYMENT_ID_=d.DEPLOYMENT_ID_
WHERE d.KEY_='dcc-controlled-file-approval' AND d.TENANT_ID_ IN ('1','122')
AND NOT EXISTS (SELECT 1 FROM act_re_procdef n WHERE n.KEY_=d.KEY_ AND n.TENANT_ID_=d.TENANT_ID_ AND n.VERSION_>d.VERSION_);
