-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy,20260719_dcc_obsolete_form_policy_seed; type=seed; riskLevel=low
-- Purpose: deploy the dedicated single-step BPM process required by DCC controlled-file obsolete form-center actions.
-- The process key must stay aligned with DCC obsolete policy effect_executor_code=DCC_OBSOLETE.

SET NAMES utf8mb4;

START TRANSACTION;

SET @dcc_obsolete_manager_user_id_tenant_1 = (
    SELECT id FROM system_users
    WHERE username = 'admin' AND tenant_id = 1 AND deleted = b'0'
    LIMIT 1
);
SET @dcc_obsolete_manager_user_id_tenant_122 = (
    SELECT id FROM system_users
    WHERE username = 'aoteman' AND tenant_id = 122 AND deleted = b'0'
    LIMIT 1
);

SET @dcc_obsolete_approval_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/dcc/controlled-file-obsolete">
  <process id="dcc-controlled-file-obsolete-approval" name="DCC受控文件作废审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交作废" />
    <sequenceFlow id="flow_start_doc_control_review" sourceRef="StartEvent" targetRef="DOC_CONTROL_REVIEW" />
    <userTask id="DOC_CONTROL_REVIEW" name="文控审核">
      <extensionElements>
        <flowable:candidateStrategy>35</flowable:candidateStrategy>
        <flowable:approveMethod>1</flowable:approveMethod>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_doc_control_review_end" sourceRef="DOC_CONTROL_REVIEW" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_dcc-controlled-file-obsolete-approval">
    <bpmndi:BPMNPlane id="BPMNPlane_dcc-controlled-file-obsolete-approval" bpmnElement="dcc-controlled-file-obsolete-approval">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="180" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="DOC_CONTROL_REVIEW_di" bpmnElement="DOC_CONTROL_REVIEW">
        <omgdc:Bounds x="300" y="198" width="180" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_di" bpmnElement="EndEvent">
        <omgdc:Bounds x="570" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="flow_start_doc_control_review_di" bpmnElement="flow_start_doc_control_review">
        <omgdi:waypoint x="216" y="238" />
        <omgdi:waypoint x="300" y="238" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow_doc_control_review_end_di" bpmnElement="flow_doc_control_review_end">
        <omgdi:waypoint x="480" y="238" />
        <omgdi:waypoint x="570" y="238" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>';

INSERT INTO act_re_deployment (
    ID_, NAME_, CATEGORY_, KEY_, TENANT_ID_, DEPLOY_TIME_, DERIVED_FROM_, DERIVED_FROM_ROOT_,
    PARENT_DEPLOYMENT_ID_, ENGINE_VERSION_
)
SELECT deploy_id, 'DCC受控文件作废审批', 'DCC', 'dcc-controlled-file-obsolete-approval',
       tenant_id_text, NOW(3), NULL, NULL, deploy_id, NULL
FROM (
    SELECT 'dcc-obsolete-approval-deploy-tenant-1' AS deploy_id, '1' AS tenant_id_text
    UNION ALL
    SELECT 'dcc-obsolete-approval-deploy-tenant-122' AS deploy_id, '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'dcc-controlled-file-obsolete-approval'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT bytearray_id, 1, 'source', NULL, CONVERT(@dcc_obsolete_approval_bpmn USING BINARY), 0
FROM (
    SELECT 'dcc-obsolete-approval-model-src-tenant-1' AS bytearray_id, '1' AS tenant_id_text
    UNION ALL
    SELECT 'dcc-obsolete-approval-model-src-tenant-122' AS bytearray_id, '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'dcc-controlled-file-obsolete-approval'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
) AND NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ = tenant_scope.bytearray_id
);

INSERT INTO act_re_model (
    ID_, REV_, NAME_, KEY_, CATEGORY_, CREATE_TIME_, LAST_UPDATE_TIME_, VERSION_,
    META_INFO_, DEPLOYMENT_ID_, EDITOR_SOURCE_VALUE_ID_, EDITOR_SOURCE_EXTRA_VALUE_ID_, TENANT_ID_
)
SELECT model_id, 1, 'DCC受控文件作废审批', 'dcc-controlled-file-obsolete-approval', 'DCC',
       NOW(3), NOW(3), 1,
       meta_info,
       deploy_id, source_id, NULL, tenant_id_text
FROM (
    SELECT 'dcc-obsolete-approval-model-tenant-1' AS model_id,
           'dcc-obsolete-approval-deploy-tenant-1' AS deploy_id,
           'dcc-obsolete-approval-model-src-tenant-1' AS source_id,
           '1' AS tenant_id_text,
           CONCAT('{"icon":null,"description":"DCC controlled file obsolete approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/dcc/controlled-file/detail","formCustomViewPath":"/dcc/controlled-file/detail","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @dcc_obsolete_manager_user_id_tenant_1, '],"sort":1784592000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"DCC受控文件作废 {fileCode}"},"summarySetting":{"enable":true,"summary":["fileCode","fileName","controlledFileId"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
    UNION ALL
    SELECT 'dcc-obsolete-approval-model-tenant-122' AS model_id,
           'dcc-obsolete-approval-deploy-tenant-122' AS deploy_id,
           'dcc-obsolete-approval-model-src-tenant-122' AS source_id,
           '122' AS tenant_id_text,
           CONCAT('{"icon":null,"description":"DCC controlled file obsolete approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/dcc/controlled-file/detail","formCustomViewPath":"/dcc/controlled-file/detail","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @dcc_obsolete_manager_user_id_tenant_122, '],"sort":1784592000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"DCC受控文件作废 {fileCode}"},"summarySetting":{"enable":true,"summary":["fileCode","fileName","controlledFileId"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'dcc-controlled-file-obsolete-approval'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT bytearray_id, 1, 'dcc-controlled-file-obsolete-approval.bpmn', deploy_id,
       CONVERT(@dcc_obsolete_approval_bpmn USING BINARY), 0
FROM (
    SELECT 'dcc-obsolete-approval-bpmn-tenant-1' AS bytearray_id,
           'dcc-obsolete-approval-deploy-tenant-1' AS deploy_id,
           '1' AS tenant_id_text
    UNION ALL
    SELECT 'dcc-obsolete-approval-bpmn-tenant-122' AS bytearray_id,
           'dcc-obsolete-approval-deploy-tenant-122' AS deploy_id,
           '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'dcc-controlled-file-obsolete-approval'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
) AND NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ = tenant_scope.bytearray_id
);

UPDATE act_ge_bytearray b
JOIN (
    SELECT 'dcc-obsolete-approval-model-src-tenant-1' AS bytearray_id, @dcc_obsolete_approval_bpmn AS bpmn_xml
    UNION ALL
    SELECT 'dcc-obsolete-approval-bpmn-tenant-1' AS bytearray_id, @dcc_obsolete_approval_bpmn AS bpmn_xml
    UNION ALL
    SELECT 'dcc-obsolete-approval-model-src-tenant-122' AS bytearray_id, @dcc_obsolete_approval_bpmn AS bpmn_xml
    UNION ALL
    SELECT 'dcc-obsolete-approval-bpmn-tenant-122' AS bytearray_id, @dcc_obsolete_approval_bpmn AS bpmn_xml
) seed
  ON seed.bytearray_id = b.ID_
SET b.BYTES_ = CONVERT(seed.bpmn_xml USING BINARY),
    b.REV_ = COALESCE(b.REV_, 0) + 1
WHERE LOCATE('MATRIX_REVIEW', CONVERT(b.BYTES_ USING utf8mb4)) > 0
   OR LOCATE('DOC_CONTROL_REVIEW', CONVERT(b.BYTES_ USING utf8mb4)) = 0
   OR LOCATE('BPMNDiagram', CONVERT(b.BYTES_ USING utf8mb4)) = 0;

UPDATE act_re_model m
JOIN (
    SELECT 'dcc-obsolete-approval-model-tenant-1' AS model_id,
           CONCAT('{"icon":null,"description":"DCC controlled file obsolete approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/dcc/controlled-file/detail","formCustomViewPath":"/dcc/controlled-file/detail","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @dcc_obsolete_manager_user_id_tenant_1, '],"sort":1784592000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"DCC受控文件作废 {fileCode}"},"summarySetting":{"enable":true,"summary":["fileCode","fileName","controlledFileId"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
    UNION ALL
    SELECT 'dcc-obsolete-approval-model-tenant-122' AS model_id,
           CONCAT('{"icon":null,"description":"DCC controlled file obsolete approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/dcc/controlled-file/detail","formCustomViewPath":"/dcc/controlled-file/detail","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @dcc_obsolete_manager_user_id_tenant_122, '],"sort":1784592000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"DCC受控文件作废 {fileCode}"},"summarySetting":{"enable":true,"summary":["fileCode","fileName","controlledFileId"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
) seed
  ON seed.model_id = m.ID_
SET m.META_INFO_ = seed.meta_info,
    m.LAST_UPDATE_TIME_ = NOW(3),
    m.REV_ = COALESCE(m.REV_, 0) + 1
WHERE m.KEY_ = 'dcc-controlled-file-obsolete-approval';

INSERT INTO act_re_procdef (
    ID_, REV_, CATEGORY_, NAME_, KEY_, VERSION_, DEPLOYMENT_ID_, RESOURCE_NAME_, DGRM_RESOURCE_NAME_,
    DESCRIPTION_, HAS_START_FORM_KEY_, HAS_GRAPHICAL_NOTATION_, SUSPENSION_STATE_, TENANT_ID_,
    ENGINE_VERSION_, DERIVED_FROM_, DERIVED_FROM_ROOT_, DERIVED_VERSION_
)
SELECT procdef_id, 1, 'DCC', 'DCC受控文件作废审批', 'dcc-controlled-file-obsolete-approval',
       1, deploy_id, 'dcc-controlled-file-obsolete-approval.bpmn', NULL,
       'DCC controlled file obsolete approval', 0, 1, 1, tenant_id_text,
       NULL, NULL, NULL, 0
FROM (
    SELECT 'dcc-controlled-file-obsolete-approval:1:dcc-obsolete-admin' AS procdef_id,
           'dcc-obsolete-approval-deploy-tenant-1' AS deploy_id,
           '1' AS tenant_id_text
    UNION ALL
    SELECT 'dcc-controlled-file-obsolete-approval:1:dcc-obsolete-test' AS procdef_id,
           'dcc-obsolete-approval-deploy-tenant-122' AS deploy_id,
           '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'dcc-controlled-file-obsolete-approval'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

UPDATE act_re_procdef
SET HAS_GRAPHICAL_NOTATION_ = 1
WHERE KEY_ = 'dcc-controlled-file-obsolete-approval'
  AND TENANT_ID_ IN ('1', '122');

INSERT INTO bpm_process_definition_info (
    process_definition_id, model_id, model_type, category, icon, description, form_type, form_id,
    form_conf, form_fields, form_custom_create_path, form_custom_view_path, simple_model, visible, sort,
    start_user_ids, start_dept_ids, manager_user_ids, allow_cancel_running_process, allow_withdraw_task,
    process_id_rule, auto_approval_type, title_setting, summary_setting, process_before_trigger_setting,
    process_after_trigger_setting, task_before_trigger_setting, task_after_trigger_setting, print_template_setting,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT procdef_id, model_id, 10, 'DCC', NULL, 'DCC controlled file obsolete approval',
       20, NULL, NULL, NULL, '/dcc/controlled-file/detail', '/dcc/controlled-file/detail',
       NULL, b'0', 1784592000000, NULL, NULL, manager_user_ids, NULL, NULL, NULL, NULL,
       '{"enable":true,"title":"DCC受控文件作废 {fileCode}"}',
       '{"enable":true,"summary":["fileCode","fileName","controlledFileId"]}',
       NULL, NULL, NULL, NULL, NULL,
       'codex', NOW(), 'codex', NOW(), b'0', tenant_id
FROM (
    SELECT 'dcc-controlled-file-obsolete-approval:1:dcc-obsolete-admin' AS procdef_id,
           'dcc-obsolete-approval-model-tenant-1' AS model_id,
           CAST(@dcc_obsolete_manager_user_id_tenant_1 AS CHAR) AS manager_user_ids,
           1 AS tenant_id
    UNION ALL
    SELECT 'dcc-controlled-file-obsolete-approval:1:dcc-obsolete-test' AS procdef_id,
           'dcc-obsolete-approval-model-tenant-122' AS model_id,
           CAST(@dcc_obsolete_manager_user_id_tenant_122 AS CHAR) AS manager_user_ids,
           122 AS tenant_id
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_process_definition_info i
    WHERE i.process_definition_id = tenant_scope.procdef_id
      AND i.tenant_id = tenant_scope.tenant_id
      AND i.deleted = b'0'
);

UPDATE bpm_business_approval_policy
SET policy_mode = 'BPM_REQUIRED',
    process_definition_key = 'dcc-controlled-file-obsolete-approval',
    effect_executor_code = 'DCC_OBSOLETE',
    form_policy_type = 'NONE',
    form_slots_json = '[]',
    updater = 'codex',
    update_time = NOW()
WHERE tenant_id IN (1, 122)
  AND data_domain = 'DCC'
  AND system_code = 'DCC'
  AND object_type = 'CONTROLLED_FILE'
  AND action_code = 'OBSOLETE'
  AND object_state = 'ACTIVE'
  AND deleted = b'0';

COMMIT;
