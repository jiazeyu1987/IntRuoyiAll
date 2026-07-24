-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_bpm_category_seed_fix; type=seed; riskLevel=low
-- Purpose: deploy the formal BPM process definition required by MES batch-record version upgrade approvals.
-- The application code starts this workflow with processDefinitionKey=mes-batch-record-version-approval-v1.

START TRANSACTION;

INSERT INTO bpm_category (
    name, code, description, status, sort, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT '批记录', 'BATCH_RECORD', '批记录升版审批流程分类', 0, 20,
       'codex', NOW(), 'codex', NOW(), b'0', tenant_id
FROM (
    SELECT 1 AS tenant_id
    UNION ALL
    SELECT 122 AS tenant_id
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_category c
    WHERE c.code = 'BATCH_RECORD'
      AND c.tenant_id = tenant_scope.tenant_id
      AND c.deleted = b'0'
);

INSERT INTO system_role (
    name, code, sort, category_id, data_scope, data_scope_dept_ids, status, type,
    remark, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT '批记录管理员', 'edhr_batch_record_admin', 65, category_id, 1, '', 0, 2,
       '批记录升版审批最终审核角色', 'codex', NOW(), 'codex', NOW(), b'0', tenant_id
FROM (
    SELECT 1 AS tenant_id,
           (SELECT c.id FROM system_role_category c
            WHERE c.code = 'batch-record' AND c.tenant_id = 1 AND c.deleted = b'0' LIMIT 1) AS category_id
    UNION ALL
    SELECT 122 AS tenant_id,
           (SELECT c.id FROM system_role_category c
            WHERE c.code = 'batch-record' AND c.tenant_id = 122 AND c.deleted = b'0' LIMIT 1) AS category_id
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM system_role r
    WHERE r.code = 'edhr_batch_record_admin'
      AND r.tenant_id = tenant_scope.tenant_id
      AND r.deleted = b'0'
);

UPDATE system_role r
JOIN system_role_category c
  ON c.code = 'batch-record'
 AND c.tenant_id = r.tenant_id
 AND c.deleted = b'0'
SET r.category_id = c.id,
    r.updater = 'codex',
    r.update_time = NOW()
WHERE r.code = 'edhr_batch_record_admin'
  AND r.tenant_id IN (1, 122)
  AND r.deleted = b'0'
  AND r.category_id IS NULL;

INSERT INTO system_user_role (
    user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT u.id, r.id, 'codex', NOW(), 'codex', NOW(), b'0', u.tenant_id
FROM system_users u
JOIN system_role r
  ON r.code = 'edhr_batch_record_admin'
 AND r.tenant_id = u.tenant_id
 AND r.deleted = b'0'
WHERE u.`username` = 'admin'
  AND u.tenant_id IN (1, 122)
  AND u.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
        AND ur.tenant_id = u.tenant_id
        AND ur.deleted = b'0'
  );

INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT r.id, menu_scope.menu_id, 'codex', NOW(), 'codex', NOW(), b'0', r.tenant_id
FROM system_role r
JOIN (
    SELECT 1200 AS menu_id
    UNION ALL SELECT 1207 AS menu_id
    UNION ALL SELECT 1208 AS menu_id
    UNION ALL SELECT 1221 AS menu_id
    UNION ALL SELECT 1222 AS menu_id
) menu_scope
JOIN system_menu m
  ON m.id = menu_scope.menu_id
 AND m.deleted = b'0'
WHERE r.code = 'edhr_batch_record_admin'
  AND r.tenant_id IN (1, 122)
  AND r.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_role_menu rm
      WHERE rm.role_id = r.id
        AND rm.menu_id = menu_scope.menu_id
        AND rm.tenant_id = r.tenant_id
        AND rm.deleted = b'0'
  );

SET @batch_record_admin_role_id_tenant_1 = (
    SELECT id FROM system_role
    WHERE code = 'edhr_batch_record_admin' AND tenant_id = 1 AND deleted = b'0'
    LIMIT 1
);
SET @batch_record_admin_role_id_tenant_122 = (
    SELECT id FROM system_role
    WHERE code = 'edhr_batch_record_admin' AND tenant_id = 122 AND deleted = b'0'
    LIMIT 1
);
SET @batch_record_admin_user_id_tenant_1 = (
    SELECT id FROM system_users
    WHERE username = 'admin' AND tenant_id = 1 AND deleted = b'0'
    LIMIT 1
);
SET @batch_record_admin_user_id_tenant_122 = (
    SELECT id FROM system_users
    WHERE username = 'admin' AND tenant_id = 122 AND deleted = b'0'
    LIMIT 1
);

SET @batch_record_version_approval_bpmn_tenant_1 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/mes/batch-record-version">
  <process id="mes-batch-record-version-approval-v1" name="批记录升版审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交升版" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="batchRecordVersionApprove" />
    <userTask id="batchRecordVersionApprove" name="批记录升版 ${batchRecordName} ${versionNo}">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @batch_record_admin_role_id_tenant_1, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="batchRecordVersionApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_mes-batch-record-version-approval-v1">
    <bpmndi:BPMNPlane id="BPMNPlane_mes-batch-record-version-approval-v1" bpmnElement="mes-batch-record-version-approval-v1">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="180" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="batchRecordVersionApprove_di" bpmnElement="batchRecordVersionApprove">
        <omgdc:Bounds x="300" y="198" width="180" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_di" bpmnElement="EndEvent">
        <omgdc:Bounds x="570" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="flow_start_approve_di" bpmnElement="flow_start_approve">
        <omgdi:waypoint x="216" y="238" />
        <omgdi:waypoint x="300" y="238" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow_approve_end_di" bpmnElement="flow_approve_end">
        <omgdi:waypoint x="480" y="238" />
        <omgdi:waypoint x="570" y="238" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>');

SET @batch_record_version_approval_bpmn_tenant_122 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/mes/batch-record-version">
  <process id="mes-batch-record-version-approval-v1" name="批记录升版审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交升版" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="batchRecordVersionApprove" />
    <userTask id="batchRecordVersionApprove" name="批记录升版 ${batchRecordName} ${versionNo}">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @batch_record_admin_role_id_tenant_122, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="batchRecordVersionApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_mes-batch-record-version-approval-v1">
    <bpmndi:BPMNPlane id="BPMNPlane_mes-batch-record-version-approval-v1" bpmnElement="mes-batch-record-version-approval-v1">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="180" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="batchRecordVersionApprove_di" bpmnElement="batchRecordVersionApprove">
        <omgdc:Bounds x="300" y="198" width="180" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_di" bpmnElement="EndEvent">
        <omgdc:Bounds x="570" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="flow_start_approve_di" bpmnElement="flow_start_approve">
        <omgdi:waypoint x="216" y="238" />
        <omgdi:waypoint x="300" y="238" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow_approve_end_di" bpmnElement="flow_approve_end">
        <omgdi:waypoint x="480" y="238" />
        <omgdi:waypoint x="570" y="238" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>');

INSERT INTO act_re_deployment (
    ID_, NAME_, CATEGORY_, KEY_, TENANT_ID_, DEPLOY_TIME_, DERIVED_FROM_, DERIVED_FROM_ROOT_,
    PARENT_DEPLOYMENT_ID_, ENGINE_VERSION_
)
SELECT deploy_id, '批记录升版审批', 'BATCH_RECORD', 'mes-batch-record-version-approval-v1',
       tenant_id_text, NOW(3), NULL, NULL, deploy_id, NULL
FROM (
    SELECT 'brv-approval-deploy-tenant-1' AS deploy_id, '1' AS tenant_id_text
    UNION ALL
    SELECT 'brv-approval-deploy-tenant-122' AS deploy_id, '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-batch-record-version-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT bytearray_id, 1, 'source', NULL, CONVERT(bpmn_xml USING BINARY), 0
FROM (
    SELECT 'brv-approval-model-src-tenant-1' AS bytearray_id,
           '1' AS tenant_id_text,
           @batch_record_version_approval_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'brv-approval-model-src-tenant-122' AS bytearray_id,
           '122' AS tenant_id_text,
           @batch_record_version_approval_bpmn_tenant_122 AS bpmn_xml
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-batch-record-version-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
) AND NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ = tenant_scope.bytearray_id
);

INSERT INTO act_re_model (
    ID_, REV_, NAME_, KEY_, CATEGORY_, CREATE_TIME_, LAST_UPDATE_TIME_, VERSION_,
    META_INFO_, DEPLOYMENT_ID_, EDITOR_SOURCE_VALUE_ID_, EDITOR_SOURCE_EXTRA_VALUE_ID_, TENANT_ID_
)
SELECT model_id, 1, '批记录升版审批', 'mes-batch-record-version-approval-v1', 'BATCH_RECORD',
       NOW(3), NOW(3), 1,
       meta_info,
       deploy_id, source_id, NULL, tenant_id_text
FROM (
    SELECT 'brv-approval-model-tenant-1' AS model_id,
           'brv-approval-deploy-tenant-1' AS deploy_id,
           'brv-approval-model-src-tenant-1' AS source_id,
           '1' AS tenant_id_text,
           CONCAT('{"icon":null,"description":"MES batch record version approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/batch-record-form-list","formCustomViewPath":"/mes/pro/batch-record-form-list","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @batch_record_admin_user_id_tenant_1, '],"sort":1784083200000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"批记录升版 {batchRecordName} {versionNo}"},"summarySetting":{"enable":true,"summary":["batchRecordName","versionNo","sourceVersionNo"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
    UNION ALL
    SELECT 'brv-approval-model-tenant-122' AS model_id,
           'brv-approval-deploy-tenant-122' AS deploy_id,
           'brv-approval-model-src-tenant-122' AS source_id,
           '122' AS tenant_id_text,
           CONCAT('{"icon":null,"description":"MES batch record version approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/batch-record-form-list","formCustomViewPath":"/mes/pro/batch-record-form-list","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @batch_record_admin_user_id_tenant_122, '],"sort":1784083200000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"批记录升版 {batchRecordName} {versionNo}"},"summarySetting":{"enable":true,"summary":["batchRecordName","versionNo","sourceVersionNo"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-batch-record-version-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT bytearray_id, 1, 'mes-batch-record-version-approval-v1.bpmn', deploy_id,
       CONVERT(bpmn_xml USING BINARY), 0
FROM (
    SELECT 'brv-approval-bpmn-tenant-1' AS bytearray_id,
           'brv-approval-deploy-tenant-1' AS deploy_id,
           '1' AS tenant_id_text,
           @batch_record_version_approval_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'brv-approval-bpmn-tenant-122' AS bytearray_id,
           'brv-approval-deploy-tenant-122' AS deploy_id,
           '122' AS tenant_id_text,
           @batch_record_version_approval_bpmn_tenant_122 AS bpmn_xml
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-batch-record-version-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
) AND NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ = tenant_scope.bytearray_id
);

UPDATE act_ge_bytearray b
JOIN (
    SELECT 'brv-approval-model-src-tenant-1' AS bytearray_id,
           @batch_record_version_approval_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'brv-approval-bpmn-tenant-1' AS bytearray_id,
           @batch_record_version_approval_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'brv-approval-model-src-tenant-122' AS bytearray_id,
           @batch_record_version_approval_bpmn_tenant_122 AS bpmn_xml
    UNION ALL
    SELECT 'brv-approval-bpmn-tenant-122' AS bytearray_id,
           @batch_record_version_approval_bpmn_tenant_122 AS bpmn_xml
) seed
  ON seed.bytearray_id = b.ID_
SET b.BYTES_ = CONVERT(seed.bpmn_xml USING BINARY),
    b.REV_ = COALESCE(b.REV_, 0) + 1
WHERE LOCATE('submittedBy', CONVERT(b.BYTES_ USING utf8mb4)) > 0
   OR LOCATE('candidateStrategy', CONVERT(b.BYTES_ USING utf8mb4)) = 0
   OR LOCATE('BPMNDiagram', CONVERT(b.BYTES_ USING utf8mb4)) = 0;

UPDATE act_re_model m
JOIN (
    SELECT 'brv-approval-model-tenant-1' AS model_id,
           CONCAT('{"icon":null,"description":"MES batch record version approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/batch-record-form-list","formCustomViewPath":"/mes/pro/batch-record-form-list","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @batch_record_admin_user_id_tenant_1, '],"sort":1784083200000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"批记录升版 {batchRecordName} {versionNo}"},"summarySetting":{"enable":true,"summary":["batchRecordName","versionNo","sourceVersionNo"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
    UNION ALL
    SELECT 'brv-approval-model-tenant-122' AS model_id,
           CONCAT('{"icon":null,"description":"MES batch record version approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/batch-record-form-list","formCustomViewPath":"/mes/pro/batch-record-form-list","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @batch_record_admin_user_id_tenant_122, '],"sort":1784083200000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"批记录升版 {batchRecordName} {versionNo}"},"summarySetting":{"enable":true,"summary":["batchRecordName","versionNo","sourceVersionNo"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
) seed
  ON seed.model_id = m.ID_
SET m.META_INFO_ = seed.meta_info,
    m.LAST_UPDATE_TIME_ = NOW(3),
    m.REV_ = COALESCE(m.REV_, 0) + 1
WHERE m.KEY_ = 'mes-batch-record-version-approval-v1'
  AND (LOCATE('"managerUserIds":null', m.META_INFO_) > 0
       OR LOCATE('"managerUserIds":[', m.META_INFO_) = 0);

INSERT INTO act_re_procdef (
    ID_, REV_, CATEGORY_, NAME_, KEY_, VERSION_, DEPLOYMENT_ID_, RESOURCE_NAME_, DGRM_RESOURCE_NAME_,
    DESCRIPTION_, HAS_START_FORM_KEY_, HAS_GRAPHICAL_NOTATION_, SUSPENSION_STATE_, TENANT_ID_,
    ENGINE_VERSION_, DERIVED_FROM_, DERIVED_FROM_ROOT_, DERIVED_VERSION_
)
SELECT procdef_id, 1, 'BATCH_RECORD', '批记录升版审批', 'mes-batch-record-version-approval-v1',
       1, deploy_id, 'mes-batch-record-version-approval-v1.bpmn', NULL,
       'MES batch record version approval', 0, 1, 1, tenant_id_text,
       NULL, NULL, NULL, 0
FROM (
    SELECT 'mes-batch-record-version-approval-v1:1:brv-admin' AS procdef_id,
           'brv-approval-deploy-tenant-1' AS deploy_id,
           '1' AS tenant_id_text
    UNION ALL
    SELECT 'mes-batch-record-version-approval-v1:1:brv-test' AS procdef_id,
           'brv-approval-deploy-tenant-122' AS deploy_id,
           '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-batch-record-version-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

UPDATE act_re_procdef
SET HAS_GRAPHICAL_NOTATION_ = 1
WHERE KEY_ = 'mes-batch-record-version-approval-v1'
  AND TENANT_ID_ IN ('1', '122');

INSERT INTO bpm_process_definition_info (
    process_definition_id, model_id, model_type, category, icon, description, form_type, form_id,
    form_conf, form_fields, form_custom_create_path, form_custom_view_path, simple_model, visible, sort,
    start_user_ids, start_dept_ids, manager_user_ids, allow_cancel_running_process, allow_withdraw_task,
    process_id_rule, auto_approval_type, title_setting, summary_setting, process_before_trigger_setting,
    process_after_trigger_setting, task_before_trigger_setting, task_after_trigger_setting, print_template_setting,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT procdef_id, model_id, 10, 'BATCH_RECORD', NULL, 'MES batch record version approval',
       20, NULL, NULL, NULL, '/mes/pro/batch-record-form-list', '/mes/pro/batch-record-form-list',
       NULL, b'0', 1784083200000, NULL, NULL, manager_user_ids, NULL, NULL, NULL, NULL,
       '{"enable":true,"title":"批记录升版 {batchRecordName} {versionNo}"}',
       '{"enable":true,"summary":["batchRecordName","versionNo","sourceVersionNo"]}',
       NULL, NULL, NULL, NULL, NULL,
       'codex', NOW(), 'codex', NOW(), b'0', tenant_id
FROM (
    SELECT 'mes-batch-record-version-approval-v1:1:brv-admin' AS procdef_id,
           'brv-approval-model-tenant-1' AS model_id,
           '1' AS manager_user_ids,
           1 AS tenant_id
    UNION ALL
    SELECT 'mes-batch-record-version-approval-v1:1:brv-test' AS procdef_id,
           'brv-approval-model-tenant-122' AS model_id,
           '914520' AS manager_user_ids,
           122 AS tenant_id
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_process_definition_info i
    WHERE i.process_definition_id = tenant_scope.procdef_id
      AND i.tenant_id = tenant_scope.tenant_id
      AND i.deleted = b'0'
);

COMMIT;
