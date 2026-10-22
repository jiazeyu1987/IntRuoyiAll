-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260923_dcc_three_workflow_candidate_strategy_fix; type=seed; riskLevel=medium
-- Purpose: publish DCC three-workflow BPMN version 3 with department-obligation multi-instance MATRIX_REVIEW.

SET NAMES utf8mb4;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_dcc_three_workflow_processes (
  process_key varchar(128) NOT NULL PRIMARY KEY,
  process_name varchar(128) NOT NULL
);

DELETE FROM tmp_dcc_three_workflow_processes;

INSERT INTO tmp_dcc_three_workflow_processes (process_key, process_name) VALUES
  ('dcc-controlled-file-upload', 'DCC受控文件上传审批'),
  ('dcc-controlled-file-revision', 'DCC受控文件升版审批'),
  ('dcc-controlled-file-obsolete', 'DCC受控文件作废审批');

DROP PROCEDURE IF EXISTS validate_dcc_three_workflow_matrix_multi_instance_v3;
DELIMITER $$
CREATE PROCEDURE validate_dcc_three_workflow_matrix_multi_instance_v3()
BEGIN
  DECLARE required_bpmn_count INT DEFAULT 6;
  DECLARE source_bpmn_count INT DEFAULT 0;
  DECLARE v3_bpmn_count INT DEFAULT 0;
  DECLARE v3_multi_instance_count INT DEFAULT 0;

  SELECT COUNT(*) INTO source_bpmn_count
  FROM tmp_dcc_three_workflow_processes p
  JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
  JOIN act_ge_bytearray source_bpmn
    ON BINARY source_bpmn.ID_ = BINARY CONCAT(p.process_key, '-bpmn-v2-tenant-', t.tenant_id_text)
  WHERE LOCATE(
      '<userTask id="MATRIX_REVIEW" name="会签">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
    </userTask>',
      CONVERT(source_bpmn.BYTES_ USING utf8mb4)
  ) > 0;
  IF source_bpmn_count <> required_bpmn_count THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC three-workflow v3 migration missing exact v2 MATRIX_REVIEW source definition';
  END IF;

  SELECT COUNT(*) INTO v3_bpmn_count
  FROM tmp_dcc_three_workflow_processes p
  JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
  JOIN act_ge_bytearray target_bpmn
    ON BINARY target_bpmn.ID_ = BINARY CONCAT(p.process_key, '-bpmn-v3-tenant-', t.tenant_id_text);
  IF v3_bpmn_count NOT IN (0, required_bpmn_count) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC three-workflow v3 BPMN source is partially deployed';
  END IF;

  IF v3_bpmn_count = required_bpmn_count THEN
    SELECT COUNT(*) INTO v3_multi_instance_count
    FROM tmp_dcc_three_workflow_processes p
    JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
    JOIN act_ge_bytearray target_bpmn
      ON BINARY target_bpmn.ID_ = BINARY CONCAT(p.process_key, '-bpmn-v3-tenant-', t.tenant_id_text)
    WHERE LOCATE(
        '<multiInstanceLoopCharacteristics isSequential="false">
        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
      </multiInstanceLoopCharacteristics>',
        CONVERT(target_bpmn.BYTES_ USING utf8mb4)
    ) > 0;
    IF v3_multi_instance_count <> required_bpmn_count THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC three-workflow published v3 BPMN is missing MATRIX_REVIEW multi-instance';
    END IF;
  END IF;
END$$
DELIMITER ;

CALL validate_dcc_three_workflow_matrix_multi_instance_v3();

START TRANSACTION;

INSERT INTO act_re_deployment (
    ID_, NAME_, CATEGORY_, KEY_, TENANT_ID_, DEPLOY_TIME_, DERIVED_FROM_, DERIVED_FROM_ROOT_,
    PARENT_DEPLOYMENT_ID_, ENGINE_VERSION_
)
SELECT CONCAT(p.process_key, '-deploy-v3-tenant-', t.tenant_id_text),
       p.process_name, 'DCC', p.process_key, t.tenant_id_text,
       NOW(3), NULL, NULL, CONCAT(p.process_key, '-deploy-v3-tenant-', t.tenant_id_text), NULL
FROM tmp_dcc_three_workflow_processes p
JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_deployment d
    WHERE BINARY d.ID_ = BINARY CONCAT(p.process_key, '-deploy-v3-tenant-', t.tenant_id_text)
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT CONCAT(p.process_key, '-bpmn-v3-tenant-', t.tenant_id_text),
       1,
       CONCAT(p.process_key, '.bpmn'),
       CONCAT(p.process_key, '-deploy-v3-tenant-', t.tenant_id_text),
       CONVERT(
         REPLACE(
           CONVERT(old_bytes.BYTES_ USING utf8mb4),
           '<userTask id="MATRIX_REVIEW" name="会签">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
    </userTask>',
           '<userTask id="MATRIX_REVIEW" name="会签">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
      <multiInstanceLoopCharacteristics isSequential="false">
        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>'
         ) USING BINARY
       ),
       0
FROM tmp_dcc_three_workflow_processes p
JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
JOIN act_ge_bytearray old_bytes
  ON BINARY old_bytes.ID_ = BINARY CONCAT(p.process_key, '-bpmn-v2-tenant-', t.tenant_id_text)
WHERE NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b
    WHERE BINARY b.ID_ = BINARY CONCAT(p.process_key, '-bpmn-v3-tenant-', t.tenant_id_text)
);

INSERT INTO act_re_model (
    ID_, REV_, NAME_, KEY_, CATEGORY_, CREATE_TIME_, LAST_UPDATE_TIME_, VERSION_,
    META_INFO_, DEPLOYMENT_ID_, EDITOR_SOURCE_VALUE_ID_, EDITOR_SOURCE_EXTRA_VALUE_ID_, TENANT_ID_
)
SELECT CONCAT(p.process_key, '-model-v3-tenant-', t.tenant_id_text),
       1, p.process_name, p.process_key, 'DCC', NOW(3), NOW(3), 3,
       old_model.META_INFO_,
       CONCAT(p.process_key, '-deploy-v3-tenant-', t.tenant_id_text),
       CONCAT(p.process_key, '-bpmn-v3-tenant-', t.tenant_id_text),
       NULL, t.tenant_id_text
FROM tmp_dcc_three_workflow_processes p
JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
JOIN act_re_model old_model
  ON BINARY old_model.ID_ = BINARY CONCAT(p.process_key, '-model-v2-tenant-', t.tenant_id_text)
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_model m
    WHERE BINARY m.ID_ = BINARY CONCAT(p.process_key, '-model-v3-tenant-', t.tenant_id_text)
);

INSERT INTO act_re_procdef (
    ID_, REV_, CATEGORY_, NAME_, KEY_, VERSION_, DEPLOYMENT_ID_, RESOURCE_NAME_, DGRM_RESOURCE_NAME_,
    DESCRIPTION_, HAS_START_FORM_KEY_, HAS_GRAPHICAL_NOTATION_, SUSPENSION_STATE_, TENANT_ID_,
    ENGINE_VERSION_, DERIVED_FROM_, DERIVED_FROM_ROOT_, DERIVED_VERSION_
)
SELECT CONCAT(p.process_key, ':3:dcc-three-workflow-tenant-', t.tenant_id_text),
       1, old_procdef.CATEGORY_, old_procdef.NAME_, old_procdef.KEY_, 3,
       CONCAT(p.process_key, '-deploy-v3-tenant-', t.tenant_id_text),
       old_procdef.RESOURCE_NAME_, old_procdef.DGRM_RESOURCE_NAME_, old_procdef.DESCRIPTION_,
       old_procdef.HAS_START_FORM_KEY_, old_procdef.HAS_GRAPHICAL_NOTATION_, 1,
       t.tenant_id_text, old_procdef.ENGINE_VERSION_, old_procdef.ID_, old_procdef.DERIVED_FROM_ROOT_, 0
FROM tmp_dcc_three_workflow_processes p
JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
JOIN act_re_procdef old_procdef
  ON BINARY old_procdef.ID_ = BINARY CONCAT(p.process_key, ':2:dcc-three-workflow-tenant-', t.tenant_id_text)
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef procdef
    WHERE BINARY procdef.ID_ = BINARY CONCAT(p.process_key, ':3:dcc-three-workflow-tenant-', t.tenant_id_text)
);

INSERT INTO bpm_process_definition_info (
    process_definition_id, model_id, model_type, category, icon, description, form_type, form_id,
    form_conf, form_fields, form_custom_create_path, form_custom_view_path, simple_model, visible, sort,
    start_user_ids, start_dept_ids, manager_user_ids, allow_cancel_running_process, allow_withdraw_task,
    process_id_rule, auto_approval_type, title_setting, summary_setting, process_before_trigger_setting,
    process_after_trigger_setting, task_before_trigger_setting, task_after_trigger_setting, print_template_setting,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT CONCAT(p.process_key, ':3:dcc-three-workflow-tenant-', t.tenant_id_text),
       CONCAT(p.process_key, '-model-v3-tenant-', t.tenant_id_text),
       old_info.model_type, old_info.category, old_info.icon, old_info.description, old_info.form_type, old_info.form_id,
       old_info.form_conf, old_info.form_fields, old_info.form_custom_create_path, old_info.form_custom_view_path,
       old_info.simple_model, old_info.visible, old_info.sort, old_info.start_user_ids, old_info.start_dept_ids,
       old_info.manager_user_ids, old_info.allow_cancel_running_process, old_info.allow_withdraw_task,
       old_info.process_id_rule, old_info.auto_approval_type, old_info.title_setting, old_info.summary_setting,
       old_info.process_before_trigger_setting, old_info.process_after_trigger_setting,
       old_info.task_before_trigger_setting, old_info.task_after_trigger_setting, old_info.print_template_setting,
       'codex', NOW(), 'codex', NOW(), b'0', CAST(t.tenant_id_text AS UNSIGNED)
FROM tmp_dcc_three_workflow_processes p
JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
JOIN bpm_process_definition_info old_info
  ON BINARY old_info.process_definition_id =
       BINARY CONCAT(p.process_key, ':2:dcc-three-workflow-tenant-', t.tenant_id_text)
 AND old_info.tenant_id = CAST(t.tenant_id_text AS UNSIGNED)
 AND old_info.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_process_definition_info info
    WHERE BINARY info.process_definition_id =
        BINARY CONCAT(p.process_key, ':3:dcc-three-workflow-tenant-', t.tenant_id_text)
      AND info.tenant_id = CAST(t.tenant_id_text AS UNSIGNED)
      AND info.deleted = b'0'
);

UPDATE act_re_procdef
SET SUSPENSION_STATE_ = 2
WHERE KEY_ IN ('dcc-controlled-file-upload', 'dcc-controlled-file-revision', 'dcc-controlled-file-obsolete')
  AND VERSION_ = 2
  AND TENANT_ID_ IN ('1', '122')
  AND SUSPENSION_STATE_ = 1;

CALL validate_dcc_three_workflow_matrix_multi_instance_v3();

COMMIT;

DROP PROCEDURE IF EXISTS validate_dcc_three_workflow_matrix_multi_instance_v3;
