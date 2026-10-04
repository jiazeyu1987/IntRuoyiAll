-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260930_dcc_a_lifecycle,20260926_dcc_three_workflow_matrix_multi_instance_fix; type=seed; riskLevel=high
-- IC-1 v4, new deployments only; no rewriting existing definitions or running process variables.
SET NAMES utf8mb4;
SET @dcc_upload_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/dcc/controlled-file-upload">
  <process id="dcc-controlled-file-upload" name="DCC受控文件上传审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交上传" />
    <sequenceFlow id="flow_start_matrix_review" sourceRef="StartEvent" targetRef="MATRIX_REVIEW" />
    <userTask id="MATRIX_REVIEW" name="会签指派与签名">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
      <multiInstanceLoopCharacteristics isSequential="false">
        <loopCardinality>${PROCESS_DCC_TASK_OBLIGATION_IDS["MATRIX_REVIEW"].size()}</loopCardinality>
        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>
    <sequenceFlow id="flow_matrix_review_approval" sourceRef="MATRIX_REVIEW" targetRef="MATRIX_APPROVAL" />
    <userTask id="MATRIX_APPROVAL" name="批准">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approval_training_gateway" sourceRef="MATRIX_APPROVAL" targetRef="NeedTrainingGateway" />
    <exclusiveGateway id="NeedTrainingGateway" name="是否培训" />
    <sequenceFlow id="flow_need_training" sourceRef="NeedTrainingGateway" targetRef="TRAINING">
      <conditionExpression xsi:type="tFormalExpression" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${needTraining == true}</conditionExpression>
    </sequenceFlow>
    <sequenceFlow id="flow_skip_training" sourceRef="NeedTrainingGateway" targetRef="DOC_CONTROL_REVIEW">
      <conditionExpression xsi:type="tFormalExpression" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${needTraining != true}</conditionExpression>
    </sequenceFlow>
    <receiveTask id="TRAINING" name="培训等待" />
    <sequenceFlow id="flow_training_doc_control_review" sourceRef="TRAINING" targetRef="DOC_CONTROL_REVIEW" />
    <userTask id="DOC_CONTROL_REVIEW" name="文控审核">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_doc_control_review_end" sourceRef="DOC_CONTROL_REVIEW" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="文控审核完成，受控处理" />
  </process>
</definitions>';

SET @dcc_revision_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/dcc/controlled-file-revision">
  <process id="dcc-controlled-file-revision" name="DCC受控文件升版审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交升版" />
    <sequenceFlow id="flow_start_matrix_review" sourceRef="StartEvent" targetRef="MATRIX_REVIEW" />
    <userTask id="MATRIX_REVIEW" name="会签指派与签名">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
      <multiInstanceLoopCharacteristics isSequential="false">
        <loopCardinality>${PROCESS_DCC_TASK_OBLIGATION_IDS["MATRIX_REVIEW"].size()}</loopCardinality>
        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>
    <sequenceFlow id="flow_matrix_review_approval" sourceRef="MATRIX_REVIEW" targetRef="MATRIX_APPROVAL" />
    <userTask id="MATRIX_APPROVAL" name="批准">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approval_training_gateway" sourceRef="MATRIX_APPROVAL" targetRef="NeedTrainingGateway" />
    <exclusiveGateway id="NeedTrainingGateway" name="是否培训" />
    <sequenceFlow id="flow_need_training" sourceRef="NeedTrainingGateway" targetRef="TRAINING">
      <conditionExpression xsi:type="tFormalExpression" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${needTraining == true}</conditionExpression>
    </sequenceFlow>
    <sequenceFlow id="flow_skip_training" sourceRef="NeedTrainingGateway" targetRef="DOC_CONTROL_REVIEW">
      <conditionExpression xsi:type="tFormalExpression" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${needTraining != true}</conditionExpression>
    </sequenceFlow>
    <receiveTask id="TRAINING" name="培训等待" />
    <sequenceFlow id="flow_training_doc_control_review" sourceRef="TRAINING" targetRef="DOC_CONTROL_REVIEW" />
    <userTask id="DOC_CONTROL_REVIEW" name="文控审核">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_doc_control_review_end" sourceRef="DOC_CONTROL_REVIEW" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="文控审核完成，受控处理" />
  </process>
</definitions>';

SET @dcc_obsolete_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/dcc/controlled-file-obsolete">
  <process id="dcc-controlled-file-obsolete" name="DCC受控文件作废审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交作废" />
    <sequenceFlow id="flow_start_matrix_review" sourceRef="StartEvent" targetRef="MATRIX_REVIEW" />
    <userTask id="MATRIX_REVIEW" name="会签指派与签名">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
      <multiInstanceLoopCharacteristics isSequential="false">
        <loopCardinality>${PROCESS_DCC_TASK_OBLIGATION_IDS["MATRIX_REVIEW"].size()}</loopCardinality>
        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>
    <sequenceFlow id="flow_matrix_review_approval" sourceRef="MATRIX_REVIEW" targetRef="MATRIX_APPROVAL" />
    <userTask id="MATRIX_APPROVAL" name="批准">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_matrix_approval_end" sourceRef="MATRIX_APPROVAL" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="文件作废" />
  </process>
</definitions>';
CREATE TEMPORARY TABLE IF NOT EXISTS tmp_dcc_a_v4_models (
 process_key VARCHAR(128) PRIMARY KEY, bpmn_xml LONGTEXT NOT NULL
);
DELETE FROM tmp_dcc_a_v4_models;
INSERT INTO tmp_dcc_a_v4_models VALUES
 ('dcc-controlled-file-upload',@dcc_upload_bpmn),
 ('dcc-controlled-file-revision',@dcc_revision_bpmn),
 ('dcc-controlled-file-obsolete',@dcc_obsolete_bpmn);
CREATE TEMPORARY TABLE IF NOT EXISTS tmp_dcc_three_workflow_processes (
 process_key VARCHAR(128) PRIMARY KEY, process_name VARCHAR(128) NOT NULL
);
DELETE FROM tmp_dcc_three_workflow_processes;
INSERT INTO tmp_dcc_three_workflow_processes VALUES
 ('dcc-controlled-file-upload','DCC受控文件上传审批'),
 ('dcc-controlled-file-revision','DCC受控文件升版审批'),
 ('dcc-controlled-file-obsolete','DCC受控文件作废审批');
DROP PROCEDURE IF EXISTS validate_dcc_a_v4;
DELIMITER $$
CREATE PROCEDURE validate_dcc_a_v4()
BEGIN
 IF (SELECT COUNT(*) FROM tmp_dcc_three_workflow_processes p
 JOIN (SELECT '1' tenant_id_text UNION ALL SELECT '122') t
 JOIN act_re_procdef source ON BINARY source.ID_=BINARY CONCAT(p.process_key,':3:dcc-three-workflow-tenant-',t.tenant_id_text)) <> 6 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC IC-1 missing exact v3 source definitions';
 END IF;
 IF EXISTS (SELECT 1 FROM act_ge_bytearray b JOIN tmp_dcc_a_v4_models m
 ON b.ID_ IN (CONCAT(m.process_key,'-bpmn-v4-tenant-1'),CONCAT(m.process_key,'-bpmn-v4-tenant-122'))
 WHERE BINARY CONVERT(b.BYTES_ USING utf8mb4) <> BINARY m.bpmn_xml) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC IC-1 v4 identity exists with conflicting BPMN';
 END IF;
 IF (SELECT COUNT(*) FROM tmp_dcc_three_workflow_processes p
 JOIN (SELECT '1' tenant_id_text UNION ALL SELECT '122') t
 JOIN act_re_model m ON BINARY m.ID_=BINARY CONCAT(p.process_key,'-model-v3-tenant-',t.tenant_id_text)
 JOIN bpm_process_definition_info i ON BINARY i.process_definition_id=BINARY CONCAT(p.process_key,':3:dcc-three-workflow-tenant-',t.tenant_id_text)
 AND i.tenant_id=CAST(t.tenant_id_text AS UNSIGNED) AND i.deleted=b'0') <> 6 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC IC-1 missing v3 model or candidate configuration';
 END IF;
END$$
DELIMITER ;
CALL validate_dcc_a_v4();
START TRANSACTION;
INSERT INTO act_re_deployment (
    ID_, NAME_, CATEGORY_, KEY_, TENANT_ID_, DEPLOY_TIME_, DERIVED_FROM_, DERIVED_FROM_ROOT_,
    PARENT_DEPLOYMENT_ID_, ENGINE_VERSION_
)
SELECT CONCAT(p.process_key, '-deploy-v4-tenant-', t.tenant_id_text),
       p.process_name, 'DCC', p.process_key, t.tenant_id_text,
       NOW(3), NULL, NULL, CONCAT(p.process_key, '-deploy-v4-tenant-', t.tenant_id_text), NULL
FROM tmp_dcc_three_workflow_processes p
JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_deployment d
    WHERE BINARY d.ID_ = BINARY CONCAT(p.process_key, '-deploy-v4-tenant-', t.tenant_id_text)
);

INSERT INTO act_ge_bytearray(ID_,REV_,NAME_,DEPLOYMENT_ID_,BYTES_,GENERATED_)
SELECT CONCAT(m.process_key,'-bpmn-v4-tenant-',t.tenant_id_text),1,CONCAT(m.process_key,'.bpmn'),
 CONCAT(m.process_key,'-deploy-v4-tenant-',t.tenant_id_text),CONVERT(m.bpmn_xml USING BINARY),0
FROM tmp_dcc_a_v4_models m JOIN (SELECT '1' tenant_id_text UNION ALL SELECT '122') t
WHERE NOT EXISTS(SELECT 1 FROM act_ge_bytearray b WHERE BINARY b.ID_=BINARY CONCAT(m.process_key,'-bpmn-v4-tenant-',t.tenant_id_text));
INSERT INTO act_re_model (
    ID_, REV_, NAME_, KEY_, CATEGORY_, CREATE_TIME_, LAST_UPDATE_TIME_, VERSION_,
    META_INFO_, DEPLOYMENT_ID_, EDITOR_SOURCE_VALUE_ID_, EDITOR_SOURCE_EXTRA_VALUE_ID_, TENANT_ID_
)
SELECT CONCAT(p.process_key, '-model-v4-tenant-', t.tenant_id_text),
       1, p.process_name, p.process_key, 'DCC', NOW(3), NOW(3), 4,
       old_model.META_INFO_,
       CONCAT(p.process_key, '-deploy-v4-tenant-', t.tenant_id_text),
       CONCAT(p.process_key, '-bpmn-v4-tenant-', t.tenant_id_text),
       NULL, t.tenant_id_text
FROM tmp_dcc_three_workflow_processes p
JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
JOIN act_re_model old_model
  ON BINARY old_model.ID_ = BINARY CONCAT(p.process_key, '-model-v3-tenant-', t.tenant_id_text)
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_model m
    WHERE BINARY m.ID_ = BINARY CONCAT(p.process_key, '-model-v4-tenant-', t.tenant_id_text)
);

INSERT INTO act_re_procdef (
    ID_, REV_, CATEGORY_, NAME_, KEY_, VERSION_, DEPLOYMENT_ID_, RESOURCE_NAME_, DGRM_RESOURCE_NAME_,
    DESCRIPTION_, HAS_START_FORM_KEY_, HAS_GRAPHICAL_NOTATION_, SUSPENSION_STATE_, TENANT_ID_,
    ENGINE_VERSION_, DERIVED_FROM_, DERIVED_FROM_ROOT_, DERIVED_VERSION_
)
SELECT CONCAT(p.process_key, ':4:dcc-three-workflow-tenant-', t.tenant_id_text),
       1, old_procdef.CATEGORY_, old_procdef.NAME_, old_procdef.KEY_, 4,
       CONCAT(p.process_key, '-deploy-v4-tenant-', t.tenant_id_text),
       old_procdef.RESOURCE_NAME_, old_procdef.DGRM_RESOURCE_NAME_, old_procdef.DESCRIPTION_,
       old_procdef.HAS_START_FORM_KEY_, old_procdef.HAS_GRAPHICAL_NOTATION_, 1,
       t.tenant_id_text, old_procdef.ENGINE_VERSION_, old_procdef.ID_, old_procdef.DERIVED_FROM_ROOT_, 0
FROM tmp_dcc_three_workflow_processes p
JOIN (SELECT '1' AS tenant_id_text UNION ALL SELECT '122') t
JOIN act_re_procdef old_procdef
  ON BINARY old_procdef.ID_ = BINARY CONCAT(p.process_key, ':3:dcc-three-workflow-tenant-', t.tenant_id_text)
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef procdef
    WHERE BINARY procdef.ID_ = BINARY CONCAT(p.process_key, ':4:dcc-three-workflow-tenant-', t.tenant_id_text)
);

INSERT INTO bpm_process_definition_info (
    process_definition_id, model_id, model_type, category, icon, description, form_type, form_id,
    form_conf, form_fields, form_custom_create_path, form_custom_view_path, simple_model, visible, sort,
    start_user_ids, start_dept_ids, manager_user_ids, allow_cancel_running_process, allow_withdraw_task,
    process_id_rule, auto_approval_type, title_setting, summary_setting, process_before_trigger_setting,
    process_after_trigger_setting, task_before_trigger_setting, task_after_trigger_setting, print_template_setting,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT CONCAT(p.process_key, ':4:dcc-three-workflow-tenant-', t.tenant_id_text),
       CONCAT(p.process_key, '-model-v4-tenant-', t.tenant_id_text),
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
       BINARY CONCAT(p.process_key, ':3:dcc-three-workflow-tenant-', t.tenant_id_text)
 AND old_info.tenant_id = CAST(t.tenant_id_text AS UNSIGNED)
 AND old_info.deleted = b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_process_definition_info info
    WHERE BINARY info.process_definition_id =
        BINARY CONCAT(p.process_key, ':4:dcc-three-workflow-tenant-', t.tenant_id_text)
      AND info.tenant_id = CAST(t.tenant_id_text AS UNSIGNED)
      AND info.deleted = b'0'
);


CALL validate_dcc_a_v4();
COMMIT;
DROP PROCEDURE validate_dcc_a_v4;
