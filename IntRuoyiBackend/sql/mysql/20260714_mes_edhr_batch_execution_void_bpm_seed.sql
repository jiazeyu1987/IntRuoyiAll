-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260714_bpm_category_seed_fix; type=seed; riskLevel=low
-- Purpose: deploy the formal BPM process definition required by MES eDHR batch execution void requests.
-- The application code starts this workflow with processDefinitionKey=mes-edhr-batch-execution-void-v1.

SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO bpm_category (
    name, code, description, status, sort, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT '批记录', 'BATCH_RECORD', '批次执行作废流程分类', 0, 20,
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
SELECT '批次执行作废管理员', 'edhr_batch_void_admin', 66, category_id, 1, '', 0, 2,
       'eDHR批次执行作废 BPM 审批角色', 'codex', NOW(), 'codex', NOW(), b'0', tenant_id
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
    WHERE r.code = 'edhr_batch_void_admin'
      AND r.tenant_id = tenant_scope.tenant_id
      AND r.deleted = b'0'
);

INSERT INTO system_user_role (
    user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT u.id, r.id, 'codex', NOW(), 'codex', NOW(), b'0', u.tenant_id
FROM system_users u
JOIN system_role r
  ON r.code = 'edhr_batch_void_admin'
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

SET @edhr_batch_void_admin_role_id_tenant_1 = (
    SELECT id FROM system_role
    WHERE code = 'edhr_batch_void_admin' AND tenant_id = 1 AND deleted = b'0'
    LIMIT 1
);
SET @edhr_batch_void_admin_role_id_tenant_122 = (
    SELECT id FROM system_role
    WHERE code = 'edhr_batch_void_admin' AND tenant_id = 122 AND deleted = b'0'
    LIMIT 1
);

SET @edhr_batch_execution_void_bpmn_tenant_1 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/mes/edhr-batch-execution-void">
  <process id="mes-edhr-batch-execution-void-v1" name="eDHR批次执行作废" isExecutable="true">
    <startEvent id="StartEvent" name="提交作废" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="batchExecutionVoidApprove" />
    <userTask id="batchExecutionVoidApprove" name="作废 ${batchExecutionCode} ${workOrderCode}">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @edhr_batch_void_admin_role_id_tenant_1, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="batchExecutionVoidApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
</definitions>');

SET @edhr_batch_execution_void_bpmn_tenant_122 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/mes/edhr-batch-execution-void">
  <process id="mes-edhr-batch-execution-void-v1" name="eDHR批次执行作废" isExecutable="true">
    <startEvent id="StartEvent" name="提交作废" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="batchExecutionVoidApprove" />
    <userTask id="batchExecutionVoidApprove" name="作废 ${batchExecutionCode} ${workOrderCode}">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @edhr_batch_void_admin_role_id_tenant_122, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="batchExecutionVoidApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
</definitions>');

INSERT INTO act_re_deployment (
    ID_, NAME_, CATEGORY_, KEY_, TENANT_ID_, DEPLOY_TIME_, DERIVED_FROM_, DERIVED_FROM_ROOT_,
    PARENT_DEPLOYMENT_ID_, ENGINE_VERSION_
)
SELECT deploy_id, 'eDHR批次执行作废', 'BATCH_RECORD', 'mes-edhr-batch-execution-void-v1',
       tenant_id_text, NOW(3), NULL, NULL, deploy_id, NULL
FROM (
    SELECT 'edhr-batch-void-deploy-tenant-1' AS deploy_id, '1' AS tenant_id_text
    UNION ALL
    SELECT 'edhr-batch-void-deploy-tenant-122' AS deploy_id, '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-edhr-batch-execution-void-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT bytearray_id, 1, 'source', NULL, CONVERT(bpmn_xml USING BINARY), 0
FROM (
    SELECT 'edhr-batch-void-model-src-tenant-1' AS bytearray_id,
           '1' AS tenant_id_text,
           @edhr_batch_execution_void_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'edhr-batch-void-model-src-tenant-122' AS bytearray_id,
           '122' AS tenant_id_text,
           @edhr_batch_execution_void_bpmn_tenant_122 AS bpmn_xml
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-edhr-batch-execution-void-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
) AND NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ = tenant_scope.bytearray_id
);

INSERT INTO act_re_model (
    ID_, REV_, NAME_, KEY_, CATEGORY_, CREATE_TIME_, LAST_UPDATE_TIME_, VERSION_,
    META_INFO_, DEPLOYMENT_ID_, EDITOR_SOURCE_VALUE_ID_, EDITOR_SOURCE_EXTRA_VALUE_ID_, TENANT_ID_
)
SELECT model_id, 1, 'eDHR批次执行作废', 'mes-edhr-batch-execution-void-v1', 'BATCH_RECORD',
       NOW(3), NOW(3), 1,
       '{"icon":null,"description":"MES eDHR batch execution void","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/feedback/edhr-batch-execution","formCustomViewPath":"/mes/pro/feedback/edhr-batch-execution","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":null,"sort":1784083300000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"作废批次执行 {batchExecutionCode}"},"summarySetting":{"enable":true,"summary":["batchExecutionCode","workOrderCode","batchCode","reasonCategory"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}',
       deploy_id, source_id, NULL, tenant_id_text
FROM (
    SELECT 'edhr-batch-void-model-tenant-1' AS model_id,
           'edhr-batch-void-deploy-tenant-1' AS deploy_id,
           'edhr-batch-void-model-src-tenant-1' AS source_id,
           '1' AS tenant_id_text
    UNION ALL
    SELECT 'edhr-batch-void-model-tenant-122' AS model_id,
           'edhr-batch-void-deploy-tenant-122' AS deploy_id,
           'edhr-batch-void-model-src-tenant-122' AS source_id,
           '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-edhr-batch-execution-void-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT bytearray_id, 1, 'mes-edhr-batch-execution-void-v1.bpmn', deploy_id,
       CONVERT(bpmn_xml USING BINARY), 0
FROM (
    SELECT 'edhr-batch-void-bpmn-tenant-1' AS bytearray_id,
           'edhr-batch-void-deploy-tenant-1' AS deploy_id,
           '1' AS tenant_id_text,
           @edhr_batch_execution_void_bpmn_tenant_1 AS bpmn_xml
    UNION ALL
    SELECT 'edhr-batch-void-bpmn-tenant-122' AS bytearray_id,
           'edhr-batch-void-deploy-tenant-122' AS deploy_id,
           '122' AS tenant_id_text,
           @edhr_batch_execution_void_bpmn_tenant_122 AS bpmn_xml
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-edhr-batch-execution-void-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
) AND NOT EXISTS (
    SELECT 1 FROM act_ge_bytearray b WHERE b.ID_ = tenant_scope.bytearray_id
);

INSERT INTO act_re_procdef (
    ID_, REV_, CATEGORY_, NAME_, KEY_, VERSION_, DEPLOYMENT_ID_, RESOURCE_NAME_, DGRM_RESOURCE_NAME_,
    DESCRIPTION_, HAS_START_FORM_KEY_, HAS_GRAPHICAL_NOTATION_, SUSPENSION_STATE_, TENANT_ID_,
    ENGINE_VERSION_, DERIVED_FROM_, DERIVED_FROM_ROOT_, DERIVED_VERSION_
)
SELECT procdef_id, 1, 'BATCH_RECORD', 'eDHR批次执行作废', 'mes-edhr-batch-execution-void-v1',
       1, deploy_id, 'mes-edhr-batch-execution-void-v1.bpmn', NULL,
       'MES eDHR batch execution void', 0, 0, 1, tenant_id_text,
       NULL, NULL, NULL, 0
FROM (
    SELECT 'mes-edhr-batch-execution-void-v1:1:void-admin' AS procdef_id,
           'edhr-batch-void-deploy-tenant-1' AS deploy_id,
           '1' AS tenant_id_text
    UNION ALL
    SELECT 'mes-edhr-batch-execution-void-v1:1:void-test' AS procdef_id,
           'edhr-batch-void-deploy-tenant-122' AS deploy_id,
           '122' AS tenant_id_text
) tenant_scope
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_procdef p
    WHERE p.KEY_ = 'mes-edhr-batch-execution-void-v1'
      AND p.TENANT_ID_ = tenant_scope.tenant_id_text
);

INSERT INTO bpm_process_definition_info (
    process_definition_id, model_id, model_type, category, icon, description, form_type, form_id,
    form_conf, form_fields, form_custom_create_path, form_custom_view_path, simple_model, visible, sort,
    start_user_ids, start_dept_ids, manager_user_ids, allow_cancel_running_process, allow_withdraw_task,
    process_id_rule, auto_approval_type, title_setting, summary_setting, process_before_trigger_setting,
    process_after_trigger_setting, task_before_trigger_setting, task_after_trigger_setting, print_template_setting,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT procdef_id, model_id, 10, 'BATCH_RECORD', NULL, 'MES eDHR batch execution void',
       20, NULL, NULL, NULL, '/mes/pro/feedback/edhr-batch-execution', '/mes/pro/feedback/edhr-batch-execution',
       NULL, b'0', 1784083300000, NULL, NULL, manager_user_ids, NULL, NULL, NULL, NULL,
       '{"enable":true,"title":"作废批次执行 {batchExecutionCode}"}',
       '{"enable":true,"summary":["batchExecutionCode","workOrderCode","batchCode","reasonCategory"]}',
       NULL, NULL, NULL, NULL, NULL,
       'codex', NOW(), 'codex', NOW(), b'0', tenant_id
FROM (
    SELECT 'mes-edhr-batch-execution-void-v1:1:void-admin' AS procdef_id,
           'edhr-batch-void-model-tenant-1' AS model_id,
           '1' AS manager_user_ids,
           1 AS tenant_id
    UNION ALL
    SELECT 'mes-edhr-batch-execution-void-v1:1:void-test' AS procdef_id,
           'edhr-batch-void-model-tenant-122' AS model_id,
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
