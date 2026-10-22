-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260921_dcc_category_approval_route_action_type; type=seed; riskLevel=medium
-- Purpose: seed the independent DCC controlled-file upload, revision and obsolete BPMN definitions.
-- The BPMN node ids must stay aligned with DCC action route stage codes and runtime variables.

SET NAMES utf8mb4;

START TRANSACTION;

SET @dcc_upload_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/dcc/controlled-file-upload">
  <process id="dcc-controlled-file-upload" name="DCC受控文件上传审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交上传" />
    <sequenceFlow id="flow_start_matrix_review" sourceRef="StartEvent" targetRef="MATRIX_REVIEW" />
    <userTask id="MATRIX_REVIEW" name="会签">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
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
    <sequenceFlow id="flow_skip_training" sourceRef="NeedTrainingGateway" targetRef="DISTRIBUTION">
      <conditionExpression xsi:type="tFormalExpression" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${needTraining != true}</conditionExpression>
    </sequenceFlow>
    <receiveTask id="TRAINING" name="培训等待" />
    <sequenceFlow id="flow_training_distribution" sourceRef="TRAINING" targetRef="DISTRIBUTION" />
    <receiveTask id="DISTRIBUTION" name="分发等待" />
    <sequenceFlow id="flow_distribution_doc_control_review" sourceRef="DISTRIBUTION" targetRef="DOC_CONTROL_REVIEW" />
    <userTask id="DOC_CONTROL_REVIEW" name="文控审核">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_doc_control_review_end" sourceRef="DOC_CONTROL_REVIEW" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="文件受控生效" />
  </process>
</definitions>';

SET @dcc_revision_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/dcc/controlled-file-revision">
  <process id="dcc-controlled-file-revision" name="DCC受控文件升版审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交升版" />
    <sequenceFlow id="flow_start_matrix_review" sourceRef="StartEvent" targetRef="MATRIX_REVIEW" />
    <userTask id="MATRIX_REVIEW" name="会签">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
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
    <sequenceFlow id="flow_skip_training" sourceRef="NeedTrainingGateway" targetRef="DISTRIBUTION">
      <conditionExpression xsi:type="tFormalExpression" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${needTraining != true}</conditionExpression>
    </sequenceFlow>
    <receiveTask id="TRAINING" name="培训等待" />
    <sequenceFlow id="flow_training_distribution" sourceRef="TRAINING" targetRef="DISTRIBUTION" />
    <receiveTask id="DISTRIBUTION" name="分发等待" />
    <sequenceFlow id="flow_distribution_doc_control_review" sourceRef="DISTRIBUTION" targetRef="DOC_CONTROL_REVIEW" />
    <userTask id="DOC_CONTROL_REVIEW" name="文控审核">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_doc_control_review_end" sourceRef="DOC_CONTROL_REVIEW" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="文件受控生效" />
  </process>
</definitions>';

SET @dcc_obsolete_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/dcc/controlled-file-obsolete">
  <process id="dcc-controlled-file-obsolete" name="DCC受控文件作废审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交作废" />
    <sequenceFlow id="flow_start_matrix_review" sourceRef="StartEvent" targetRef="MATRIX_REVIEW" />
    <userTask id="MATRIX_REVIEW" name="会签">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>2</flowable:approveMethod>
        <flowable:approveRatio>100</flowable:approveRatio>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_matrix_review_approval" sourceRef="MATRIX_REVIEW" targetRef="MATRIX_APPROVAL" />
    <userTask id="MATRIX_APPROVAL" name="批准">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_matrix_approval_doc_control_review" sourceRef="MATRIX_APPROVAL" targetRef="DOC_CONTROL_REVIEW" />
    <userTask id="DOC_CONTROL_REVIEW" name="文控审核">
      <extensionElements>
        <flowable:candidateStrategy>34</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_doc_control_review_end" sourceRef="DOC_CONTROL_REVIEW" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="文件作废" />
  </process>
</definitions>';

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_dcc_three_workflow_defs (
  process_key varchar(128) NOT NULL PRIMARY KEY,
  process_name varchar(128) NOT NULL,
  bpmn_xml longtext NOT NULL,
  sort_value bigint NOT NULL
);

DELETE FROM tmp_dcc_three_workflow_defs;

INSERT INTO tmp_dcc_three_workflow_defs (process_key, process_name, bpmn_xml, sort_value) VALUES
('dcc-controlled-file-upload', 'DCC受控文件上传审批', @dcc_upload_bpmn, 1784678400001),
('dcc-controlled-file-revision', 'DCC受控文件升版审批', @dcc_revision_bpmn, 1784678400002),
('dcc-controlled-file-obsolete', 'DCC受控文件作废审批', @dcc_obsolete_bpmn, 1784678400003);

INSERT INTO act_re_deployment (
    ID_, NAME_, CATEGORY_, KEY_, TENANT_ID_, DEPLOY_TIME_, DERIVED_FROM_, DERIVED_FROM_ROOT_,
    PARENT_DEPLOYMENT_ID_, ENGINE_VERSION_
)
SELECT CONCAT(defs.process_key, '-deploy-tenant-', tenant_scope.tenant_id_text),
       defs.process_name, 'DCC', defs.process_key, tenant_scope.tenant_id_text,
       NOW(3), NULL, NULL, CONCAT(defs.process_key, '-deploy-tenant-', tenant_scope.tenant_id_text), NULL
FROM tmp_dcc_three_workflow_defs defs
JOIN (
    SELECT '1' AS tenant_id_text
    UNION ALL
    SELECT '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE BINARY p.KEY_ = BINARY defs.process_key
      AND BINARY p.TENANT_ID_ = BINARY tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT CONCAT(defs.process_key, '-bpmn-tenant-', tenant_scope.tenant_id_text),
       1, CONCAT(defs.process_key, '.bpmn'),
       CONCAT(defs.process_key, '-deploy-tenant-', tenant_scope.tenant_id_text),
       CONVERT(defs.bpmn_xml USING BINARY), 0
FROM tmp_dcc_three_workflow_defs defs
JOIN (
    SELECT '1' AS tenant_id_text
    UNION ALL
    SELECT '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b
    WHERE BINARY b.ID_ = BINARY CONCAT(defs.process_key, '-bpmn-tenant-', tenant_scope.tenant_id_text)
);

UPDATE act_ge_bytearray b
JOIN tmp_dcc_three_workflow_defs defs
  ON b.ID_ IN (
      CONCAT(defs.process_key, '-bpmn-tenant-1'),
      CONCAT(defs.process_key, '-bpmn-tenant-122')
  )
SET b.BYTES_ = CONVERT(defs.bpmn_xml USING BINARY),
    b.REV_ = COALESCE(b.REV_, 0) + 1
WHERE b.BYTES_ IS NULL
   OR BINARY CONVERT(b.BYTES_ USING utf8mb4) <> BINARY defs.bpmn_xml;

INSERT INTO act_re_model (
    ID_, REV_, NAME_, KEY_, CATEGORY_, CREATE_TIME_, LAST_UPDATE_TIME_, VERSION_,
    META_INFO_, DEPLOYMENT_ID_, EDITOR_SOURCE_VALUE_ID_, EDITOR_SOURCE_EXTRA_VALUE_ID_, TENANT_ID_
)
SELECT CONCAT(defs.process_key, '-model-tenant-', tenant_scope.tenant_id_text),
       1, defs.process_name, defs.process_key, 'DCC', NOW(3), NOW(3), 1,
       CONCAT('{"icon":null,"description":"', defs.process_name, '","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/dcc/controlled-file/detail","formCustomViewPath":"/dcc/controlled-file/detail","visible":false,"sort":', defs.sort_value, '}'),
       CONCAT(defs.process_key, '-deploy-tenant-', tenant_scope.tenant_id_text),
       CONCAT(defs.process_key, '-bpmn-tenant-', tenant_scope.tenant_id_text),
       NULL,
       tenant_scope.tenant_id_text
FROM tmp_dcc_three_workflow_defs defs
JOIN (
    SELECT '1' AS tenant_id_text
    UNION ALL
    SELECT '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_model m
    WHERE BINARY m.KEY_ = BINARY defs.process_key
      AND BINARY m.TENANT_ID_ = BINARY tenant_scope.tenant_id_text
);

INSERT INTO act_re_procdef (
    ID_, REV_, CATEGORY_, NAME_, KEY_, VERSION_, DEPLOYMENT_ID_, RESOURCE_NAME_, DGRM_RESOURCE_NAME_,
    DESCRIPTION_, HAS_START_FORM_KEY_, HAS_GRAPHICAL_NOTATION_, SUSPENSION_STATE_, TENANT_ID_,
    ENGINE_VERSION_, DERIVED_FROM_, DERIVED_FROM_ROOT_, DERIVED_VERSION_
)
SELECT CONCAT(defs.process_key, ':1:dcc-three-workflow-tenant-', tenant_scope.tenant_id_text),
       1, 'DCC', defs.process_name, defs.process_key, 1,
       CONCAT(defs.process_key, '-deploy-tenant-', tenant_scope.tenant_id_text),
       CONCAT(defs.process_key, '.bpmn'), NULL,
       defs.process_name, 0, 1, 1, tenant_scope.tenant_id_text,
       NULL, NULL, NULL, 0
FROM tmp_dcc_three_workflow_defs defs
JOIN (
    SELECT '1' AS tenant_id_text
    UNION ALL
    SELECT '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE BINARY p.KEY_ = BINARY defs.process_key
      AND BINARY p.TENANT_ID_ = BINARY tenant_scope.tenant_id_text
);

INSERT INTO bpm_process_definition_info (
    process_definition_id, model_id, model_type, category, icon, description, form_type, form_id,
    form_conf, form_fields, form_custom_create_path, form_custom_view_path, simple_model, visible, sort,
    start_user_ids, start_dept_ids, manager_user_ids, allow_cancel_running_process, allow_withdraw_task,
    process_id_rule, auto_approval_type, title_setting, summary_setting, process_before_trigger_setting,
    process_after_trigger_setting, task_before_trigger_setting, task_after_trigger_setting, print_template_setting,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT CONCAT(defs.process_key, ':1:dcc-three-workflow-tenant-', tenant_scope.tenant_id_text),
       CONCAT(defs.process_key, '-model-tenant-', tenant_scope.tenant_id_text),
       10, 'DCC', NULL, defs.process_name,
       20, NULL, NULL, NULL, '/dcc/controlled-file/detail', '/dcc/controlled-file/detail',
       NULL, b'0', defs.sort_value, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
       CONCAT('{"enable":true,"title":"', defs.process_name, ' {fileCode}"}'),
       '{"enable":true,"summary":["fileCode","fileName","controlledFileId"]}',
       NULL, NULL, NULL, NULL, NULL,
       'codex', NOW(), 'codex', NOW(), b'0', CAST(tenant_scope.tenant_id_text AS UNSIGNED)
FROM tmp_dcc_three_workflow_defs defs
JOIN (
    SELECT '1' AS tenant_id_text
    UNION ALL
    SELECT '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_process_definition_info i
    WHERE BINARY i.process_definition_id =
              BINARY CONCAT(defs.process_key, ':1:dcc-three-workflow-tenant-', tenant_scope.tenant_id_text)
      AND i.tenant_id = CAST(tenant_scope.tenant_id_text AS UNSIGNED)
      AND i.deleted = b'0'
);

UPDATE bpm_business_approval_policy
SET policy_mode = 'BPM_REQUIRED',
    process_definition_key = 'dcc-controlled-file-obsolete',
    effect_executor_code = 'DCC_OBSOLETE',
    form_policy_type = 'NONE',
    form_slots_json = '[]',
    updater = 'codex',
    update_time = NOW()
WHERE tenant_id IN (1, 122)
  AND BINARY data_domain = BINARY 'DCC'
  AND BINARY system_code = BINARY 'DCC'
  AND BINARY object_type = BINARY 'CONTROLLED_FILE'
  AND BINARY action_code = BINARY 'OBSOLETE'
  AND BINARY object_state = BINARY 'ACTIVE'
  AND deleted = b'0';

COMMIT;
