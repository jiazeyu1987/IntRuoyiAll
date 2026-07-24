-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_bpm_category_seed_fix; type=seed; riskLevel=low
-- Purpose: deploy the formal BPM process definition required by MES manual schedule replan approvals.
-- The form-center policy starts this workflow with processDefinitionKey=mes-schedule-replan-approval-v1.

SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO bpm_category (
    name, code, description, status, sort, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT '排产重排', 'SCHEDULE_REPLAN', '排产重排审批流程分类', 0, 30,
       'codex', NOW(), 'codex', NOW(), b'0', tenant_id
FROM (
    SELECT 1 AS tenant_id
    UNION ALL
    SELECT 122 AS tenant_id
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_category c
    WHERE c.code = 'SCHEDULE_REPLAN'
      AND c.tenant_id = tenant_scope.tenant_id
      AND c.deleted = b'0'
);

INSERT INTO system_role (
    name, code, sort, category_id, data_scope, data_scope_dept_ids, status, type,
    remark, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT '排产重排审批人', 'mes_schedule_replan_approver', 67, category_id, 1, '', 0, 2,
       'MES排产手动重排 BPM 审批角色', 'codex', NOW(), 'codex', NOW(), b'0', tenant_id
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
    WHERE r.code = 'mes_schedule_replan_approver'
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
WHERE r.code = 'mes_schedule_replan_approver'
  AND r.tenant_id IN (1, 122)
  AND r.deleted = b'0'
  AND r.category_id IS NULL;

INSERT INTO system_user_role (
    user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT u.id, r.id, 'codex', NOW(), 'codex', NOW(), b'0', u.tenant_id
FROM system_users u
JOIN system_role r
  ON r.code = 'mes_schedule_replan_approver'
 AND r.tenant_id = u.tenant_id
 AND r.deleted = b'0'
WHERE (
        (u.tenant_id = 1 AND u.`username` = 'admin')
        OR (u.tenant_id = 122 AND u.`username` = 'smokeappr1')
      )
  AND u.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1 FROM system_user_role ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
        AND ur.tenant_id = u.tenant_id
        AND ur.deleted = b'0'
  );

SET @schedule_replan_approver_role_id_tenant_1 = (
    SELECT id FROM system_role
    WHERE code = 'mes_schedule_replan_approver' AND tenant_id = 1 AND deleted = b'0'
    LIMIT 1
);
SET @schedule_replan_approver_role_id_tenant_122 = (
    SELECT id FROM system_role
    WHERE code = 'mes_schedule_replan_approver' AND tenant_id = 122 AND deleted = b'0'
    LIMIT 1
);
SET @schedule_replan_approver_user_id_tenant_1 = (
    SELECT id FROM system_users
    WHERE username = 'admin' AND tenant_id = 1 AND deleted = b'0'
    LIMIT 1
);
SET @schedule_replan_approver_user_id_tenant_122 = (
    SELECT id FROM system_users
    WHERE username = 'smokeappr1' AND tenant_id = 122 AND deleted = b'0'
    LIMIT 1
);

SET @schedule_replan_approval_bpmn_tenant_1 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/mes/schedule-replan">
  <process id="mes-schedule-replan-approval-v1" name="排产重排审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交重排" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="scheduleReplanApprove" />
    <userTask id="scheduleReplanApprove" name="排产重排审批 ${scheduleOrderIds}">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @schedule_replan_approver_role_id_tenant_1, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="scheduleReplanApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_mes-schedule-replan-approval-v1">
    <bpmndi:BPMNPlane id="BPMNPlane_mes-schedule-replan-approval-v1" bpmnElement="mes-schedule-replan-approval-v1">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="180" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="scheduleReplanApprove_di" bpmnElement="scheduleReplanApprove">
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

SET @schedule_replan_approval_bpmn_tenant_122 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/mes/schedule-replan">
  <process id="mes-schedule-replan-approval-v1" name="排产重排审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交重排" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="scheduleReplanApprove" />
    <userTask id="scheduleReplanApprove" name="排产重排审批 ${scheduleOrderIds}">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @schedule_replan_approver_role_id_tenant_122, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="scheduleReplanApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_mes-schedule-replan-approval-v1">
    <bpmndi:BPMNPlane id="BPMNPlane_mes-schedule-replan-approval-v1" bpmnElement="mes-schedule-replan-approval-v1">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="180" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="scheduleReplanApprove_di" bpmnElement="scheduleReplanApprove">
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
SELECT deploy_id, '排产重排审批', 'SCHEDULE_REPLAN', 'mes-schedule-replan-approval-v1',
       tenant_id_text, NOW(3), NULL, NULL, deploy_id, NULL
FROM (
    SELECT 'schedule-replan-approval-deploy-tenant-1' AS deploy_id, '1' AS tenant_id_text
    UNION ALL
    SELECT 'schedule-replan-approval-deploy-tenant-122' AS deploy_id, '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-schedule-replan-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT bytearray_id, 1, 'source', NULL, CONVERT(bpmn_xml USING BINARY), 0
FROM (
    SELECT 'schedule-replan-approval-model-src-tenant-1' AS bytearray_id,
           '1' AS tenant_id_text,
           @schedule_replan_approval_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'schedule-replan-approval-model-src-tenant-122' AS bytearray_id,
           '122' AS tenant_id_text,
           @schedule_replan_approval_bpmn_tenant_122 AS bpmn_xml
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-schedule-replan-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
) AND NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ = tenant_scope.bytearray_id
);

INSERT INTO act_re_model (
    ID_, REV_, NAME_, KEY_, CATEGORY_, CREATE_TIME_, LAST_UPDATE_TIME_, VERSION_,
    META_INFO_, DEPLOYMENT_ID_, EDITOR_SOURCE_VALUE_ID_, EDITOR_SOURCE_EXTRA_VALUE_ID_, TENANT_ID_
)
SELECT model_id, 1, '排产重排审批', 'mes-schedule-replan-approval-v1', 'SCHEDULE_REPLAN',
       NOW(3), NOW(3), 1,
       meta_info,
       deploy_id, source_id, NULL, tenant_id_text
FROM (
    SELECT 'schedule-replan-approval-model-tenant-1' AS model_id,
           'schedule-replan-approval-deploy-tenant-1' AS deploy_id,
           'schedule-replan-approval-model-src-tenant-1' AS source_id,
           '1' AS tenant_id_text,
           CONCAT('{"icon":null,"description":"MES schedule replan approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/schedule-order","formCustomViewPath":"/mes/pro/schedule-order","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @schedule_replan_approver_user_id_tenant_1, '],"sort":1784548000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"排产重排审批 {scheduleOrderIds}"},"summarySetting":{"enable":true,"summary":["scheduleOrderIds","startTime","reason"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
    UNION ALL
    SELECT 'schedule-replan-approval-model-tenant-122' AS model_id,
           'schedule-replan-approval-deploy-tenant-122' AS deploy_id,
           'schedule-replan-approval-model-src-tenant-122' AS source_id,
           '122' AS tenant_id_text,
           CONCAT('{"icon":null,"description":"MES schedule replan approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/schedule-order","formCustomViewPath":"/mes/pro/schedule-order","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @schedule_replan_approver_user_id_tenant_122, '],"sort":1784548000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"排产重排审批 {scheduleOrderIds}"},"summarySetting":{"enable":true,"summary":["scheduleOrderIds","startTime","reason"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-schedule-replan-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT bytearray_id, 1, 'mes-schedule-replan-approval-v1.bpmn', deploy_id,
       CONVERT(bpmn_xml USING BINARY), 0
FROM (
    SELECT 'schedule-replan-approval-bpmn-tenant-1' AS bytearray_id,
           'schedule-replan-approval-deploy-tenant-1' AS deploy_id,
           '1' AS tenant_id_text,
           @schedule_replan_approval_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'schedule-replan-approval-bpmn-tenant-122' AS bytearray_id,
           'schedule-replan-approval-deploy-tenant-122' AS deploy_id,
           '122' AS tenant_id_text,
           @schedule_replan_approval_bpmn_tenant_122 AS bpmn_xml
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-schedule-replan-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
) AND NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ = tenant_scope.bytearray_id
);

UPDATE act_ge_bytearray b
JOIN (
    SELECT 'schedule-replan-approval-model-src-tenant-1' AS bytearray_id,
           @schedule_replan_approval_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'schedule-replan-approval-bpmn-tenant-1' AS bytearray_id,
           @schedule_replan_approval_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'schedule-replan-approval-model-src-tenant-122' AS bytearray_id,
           @schedule_replan_approval_bpmn_tenant_122 AS bpmn_xml
    UNION ALL
    SELECT 'schedule-replan-approval-bpmn-tenant-122' AS bytearray_id,
           @schedule_replan_approval_bpmn_tenant_122 AS bpmn_xml
) seed
  ON seed.bytearray_id = b.ID_
SET b.BYTES_ = CONVERT(seed.bpmn_xml USING BINARY),
    b.REV_ = COALESCE(b.REV_, 0) + 1
WHERE LOCATE('candidateStrategy', CONVERT(b.BYTES_ USING utf8mb4)) = 0
   OR LOCATE('BPMNDiagram', CONVERT(b.BYTES_ USING utf8mb4)) = 0
   OR LOCATE('scheduleReplanApprove', CONVERT(b.BYTES_ USING utf8mb4)) = 0;

UPDATE act_re_model m
JOIN (
    SELECT 'schedule-replan-approval-model-tenant-1' AS model_id,
           CONCAT('{"icon":null,"description":"MES schedule replan approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/schedule-order","formCustomViewPath":"/mes/pro/schedule-order","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @schedule_replan_approver_user_id_tenant_1, '],"sort":1784548000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"排产重排审批 {scheduleOrderIds}"},"summarySetting":{"enable":true,"summary":["scheduleOrderIds","startTime","reason"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
    UNION ALL
    SELECT 'schedule-replan-approval-model-tenant-122' AS model_id,
           CONCAT('{"icon":null,"description":"MES schedule replan approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/schedule-order","formCustomViewPath":"/mes/pro/schedule-order","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @schedule_replan_approver_user_id_tenant_122, '],"sort":1784548000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"排产重排审批 {scheduleOrderIds}"},"summarySetting":{"enable":true,"summary":["scheduleOrderIds","startTime","reason"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS meta_info
) seed
  ON seed.model_id = m.ID_
SET m.META_INFO_ = seed.meta_info,
    m.LAST_UPDATE_TIME_ = NOW(3),
    m.REV_ = COALESCE(m.REV_, 0) + 1
WHERE m.KEY_ = 'mes-schedule-replan-approval-v1'
  AND (LOCATE('"managerUserIds":null', m.META_INFO_) > 0
       OR LOCATE('"managerUserIds":[', m.META_INFO_) = 0);

INSERT INTO act_re_procdef (
    ID_, REV_, CATEGORY_, NAME_, KEY_, VERSION_, DEPLOYMENT_ID_, RESOURCE_NAME_, DGRM_RESOURCE_NAME_,
    DESCRIPTION_, HAS_START_FORM_KEY_, HAS_GRAPHICAL_NOTATION_, SUSPENSION_STATE_, TENANT_ID_,
    ENGINE_VERSION_, DERIVED_FROM_, DERIVED_FROM_ROOT_, DERIVED_VERSION_
)
SELECT procdef_id, 1, 'SCHEDULE_REPLAN', '排产重排审批', 'mes-schedule-replan-approval-v1',
       1, deploy_id, 'mes-schedule-replan-approval-v1.bpmn', NULL,
       'MES schedule replan approval', 0, 1, 1, tenant_id_text,
       NULL, NULL, NULL, 0
FROM (
    SELECT 'mes-schedule-replan-approval-v1:1:replan-admin' AS procdef_id,
           'schedule-replan-approval-deploy-tenant-1' AS deploy_id,
           '1' AS tenant_id_text
    UNION ALL
    SELECT 'mes-schedule-replan-approval-v1:1:replan-test' AS procdef_id,
           'schedule-replan-approval-deploy-tenant-122' AS deploy_id,
           '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-schedule-replan-approval-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

UPDATE act_re_procdef
SET HAS_GRAPHICAL_NOTATION_ = 1
WHERE KEY_ = 'mes-schedule-replan-approval-v1'
  AND TENANT_ID_ IN ('1', '122');

INSERT INTO bpm_process_definition_info (
    process_definition_id, model_id, model_type, category, icon, description, form_type, form_id,
    form_conf, form_fields, form_custom_create_path, form_custom_view_path, simple_model, visible, sort,
    start_user_ids, start_dept_ids, manager_user_ids, allow_cancel_running_process, allow_withdraw_task,
    process_id_rule, auto_approval_type, title_setting, summary_setting, process_before_trigger_setting,
    process_after_trigger_setting, task_before_trigger_setting, task_after_trigger_setting, print_template_setting,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT procdef_id, model_id, 10, 'SCHEDULE_REPLAN', NULL, 'MES schedule replan approval',
       20, NULL, NULL, NULL, '/mes/pro/schedule-order', '/mes/pro/schedule-order',
       NULL, b'0', 1784548000000, NULL, NULL, manager_user_ids, NULL, NULL, NULL, NULL,
       '{"enable":true,"title":"排产重排审批 {scheduleOrderIds}"}',
       '{"enable":true,"summary":["scheduleOrderIds","startTime","reason"]}',
       NULL, NULL, NULL, NULL, NULL,
       'codex', NOW(), 'codex', NOW(), b'0', tenant_id
FROM (
    SELECT 'mes-schedule-replan-approval-v1:1:replan-admin' AS procdef_id,
           'schedule-replan-approval-model-tenant-1' AS model_id,
           @schedule_replan_approver_user_id_tenant_1 AS manager_user_ids,
           1 AS tenant_id
    UNION ALL
    SELECT 'mes-schedule-replan-approval-v1:1:replan-test' AS procdef_id,
           'schedule-replan-approval-model-tenant-122' AS model_id,
           @schedule_replan_approver_user_id_tenant_122 AS manager_user_ids,
           122 AS tenant_id
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_process_definition_info i
    WHERE i.process_definition_id = tenant_scope.procdef_id
      AND i.tenant_id = tenant_scope.tenant_id
      AND i.deleted = b'0'
);

COMMIT;
