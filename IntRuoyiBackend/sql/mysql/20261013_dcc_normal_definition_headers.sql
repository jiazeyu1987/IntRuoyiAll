-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_workflow_bpmn_v4; type=data; riskLevel=high
-- Correct the known seed's ordinary-version headers, not workflow XML or historical application/signature facts.
-- Caller must freeze/backup exact target headers and XML and explicitly supply the approved tenant scope.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS repair_dcc_normal_definition_headers;
DELIMITER //
CREATE PROCEDURE repair_dcc_normal_definition_headers()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF @dcc_native_header_tenant_id IS NULL OR @dcc_native_header_tenant_id<=0 OR @dcc_native_header_repair_authorized IS NULL OR @dcc_native_header_repair_authorized<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Explicit approved DCC definition header tenant scope required';
  END IF;
  IF (SELECT COUNT(*) FROM act_re_procdef WHERE TENANT_ID_=CAST(@dcc_native_header_tenant_id AS CHAR)
      AND KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete')
      AND VERSION_ IN (2,3,4) AND ID_=CONCAT(KEY_,':',VERSION_,':dcc-three-workflow-tenant-',TENANT_ID_))<>9 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Exact three-workflow seed version headers missing';
  END IF;
  IF EXISTS(SELECT 1 FROM act_re_procdef WHERE TENANT_ID_=CAST(@dcc_native_header_tenant_id AS CHAR)
      AND KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete')
      AND VERSION_ IN (2,3,4) AND ID_=CONCAT(KEY_,':',VERSION_,':dcc-three-workflow-tenant-',TENANT_ID_)
      AND (DERIVED_VERSION_<>0 OR DERIVED_FROM_ROOT_ IS NOT NULL
        OR (DERIVED_FROM_ IS NOT NULL AND DERIVED_FROM_<>CONCAT(KEY_,':',VERSION_-1,':dcc-three-workflow-tenant-',TENANT_ID_)))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Header is not the proven ordinary seed defect';
  END IF;
  START TRANSACTION;
  UPDATE act_re_procdef child JOIN act_re_procdef predecessor ON predecessor.ID_=child.DERIVED_FROM_
      AND predecessor.KEY_=child.KEY_ AND predecessor.TENANT_ID_=child.TENANT_ID_ AND predecessor.VERSION_=child.VERSION_-1
    SET child.DERIVED_FROM_=NULL
    WHERE child.TENANT_ID_=CAST(@dcc_native_header_tenant_id AS CHAR)
      AND child.KEY_ IN ('dcc-controlled-file-upload','dcc-controlled-file-revision','dcc-controlled-file-obsolete')
      AND child.VERSION_ IN (2,3,4) AND child.ID_=CONCAT(child.KEY_,':',child.VERSION_,':dcc-three-workflow-tenant-',child.TENANT_ID_)
      AND child.DERIVED_VERSION_=0 AND child.DERIVED_FROM_ROOT_ IS NULL;
  SELECT JSON_OBJECT('correctedOrdinaryHeaders',ROW_COUNT(),'tenantId',@dcc_native_header_tenant_id);
  COMMIT;
END//
DELIMITER ;
CALL repair_dcc_normal_definition_headers();
DROP PROCEDURE repair_dcc_normal_definition_headers;
