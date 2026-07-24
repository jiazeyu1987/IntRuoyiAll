-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=seed; riskLevel=low
START TRANSACTION;

-- approveNode must remain the eDHR start-user-select parallel approval node:
-- flowable:candidateStrategy="35"
-- <multiInstanceLoopCharacteristics isSequential="false"><completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition></multiInstanceLoopCharacteristics>

INSERT INTO act_re_deployment (
    ID_, NAME_, CATEGORY_, KEY_, TENANT_ID_, DEPLOY_TIME_, DERIVED_FROM_, DERIVED_FROM_ROOT_,
    PARENT_DEPLOYMENT_ID_, ENGINE_VERSION_
)
SELECT
    '6b667314-70f9-4bc0-8f15-eb27d3ab5556',
    'eDHR Approval V1',
    'MES',
    'mes-edhr-approval-v1',
    '1',
    NOW(3),
    NULL,
    NULL,
    '6b667314-70f9-4bc0-8f15-eb27d3ab5556',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM act_re_deployment WHERE ID_ = '6b667314-70f9-4bc0-8f15-eb27d3ab5556'
);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT
    '479cc5fc-40d4-4179-9010-73b0539665c0',
    1,
    'source',
    NULL,
    CONVERT(REPLACE(REPLACE(REPLACE(
        CONVERT(BYTES_ USING utf8mb4),
        'flowable:candidateParam="113"',
        'flowable:candidateStrategy="35" flowable:candidateParam=""'
    ),
        'flowable:candidateParam="1"',
        'flowable:candidateStrategy="35" flowable:candidateParam=""'
    ),
        '</bpmn2:userTask>',
        '<multiInstanceLoopCharacteristics isSequential="false"><completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition></multiInstanceLoopCharacteristics></bpmn2:userTask>'
    ) USING BINARY),
    GENERATED_
FROM act_ge_bytearray
WHERE ID_ = '45227363-58d5-11f1-ab8c-00155d615441'
  AND NOT EXISTS (
      SELECT 1 FROM act_ge_bytearray WHERE ID_ = '479cc5fc-40d4-4179-9010-73b0539665c0'
  );

INSERT INTO act_re_model (
    ID_, REV_, NAME_, KEY_, CATEGORY_, CREATE_TIME_, LAST_UPDATE_TIME_, VERSION_,
    META_INFO_, DEPLOYMENT_ID_, EDITOR_SOURCE_VALUE_ID_, EDITOR_SOURCE_EXTRA_VALUE_ID_, TENANT_ID_
)
VALUES (
    '444fbee3-c112-45e6-bc13-d7459ca60b68',
    1,
    'eDHR Approval V1',
    'mes-edhr-approval-v1',
    'MES',
    NOW(3),
    NOW(3),
    1,
    '{"icon":null,"description":"Admin tenant eDHR approval flow","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/feedback/edhr-batch-execution","formCustomViewPath":"/mes/pro/feedback/edhr-approval/detail","visible":true,"startUserIds":[1],"startDeptIds":null,"managerUserIds":[1],"sort":1779780843594,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":null,"summarySetting":null,"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}',
    '6b667314-70f9-4bc0-8f15-eb27d3ab5556',
    '479cc5fc-40d4-4179-9010-73b0539665c0',
    NULL,
    '1'
)
ON DUPLICATE KEY UPDATE
    LAST_UPDATE_TIME_ = VALUES(LAST_UPDATE_TIME_),
    META_INFO_ = VALUES(META_INFO_),
    DEPLOYMENT_ID_ = VALUES(DEPLOYMENT_ID_),
    EDITOR_SOURCE_VALUE_ID_ = VALUES(EDITOR_SOURCE_VALUE_ID_),
    TENANT_ID_ = VALUES(TENANT_ID_);

INSERT INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
SELECT
    '4e80b759-0285-4d77-bf50-048b68d7ce29',
    1,
    'mes-edhr-approval-v1.bpmn',
    '6b667314-70f9-4bc0-8f15-eb27d3ab5556',
    CONVERT(REPLACE(REPLACE(REPLACE(
        CONVERT(BYTES_ USING utf8mb4),
        'flowable:candidateParam="113"',
        'flowable:candidateStrategy="35" flowable:candidateParam=""'
    ),
        'flowable:candidateParam="1"',
        'flowable:candidateStrategy="35" flowable:candidateParam=""'
    ),
        '</bpmn2:userTask>',
        '<multiInstanceLoopCharacteristics isSequential="false"><completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition></multiInstanceLoopCharacteristics></bpmn2:userTask>'
    ) USING BINARY),
    GENERATED_
FROM act_ge_bytearray
WHERE ID_ = '45440525-58d5-11f1-ab8c-00155d615441'
  AND NOT EXISTS (
      SELECT 1 FROM act_ge_bytearray WHERE ID_ = '4e80b759-0285-4d77-bf50-048b68d7ce29'
  );

INSERT INTO act_re_procdef (
    ID_, REV_, CATEGORY_, NAME_, KEY_, VERSION_, DEPLOYMENT_ID_, RESOURCE_NAME_, DGRM_RESOURCE_NAME_,
    DESCRIPTION_, HAS_START_FORM_KEY_, HAS_GRAPHICAL_NOTATION_, SUSPENSION_STATE_, TENANT_ID_,
    ENGINE_VERSION_, DERIVED_FROM_, DERIVED_FROM_ROOT_, DERIVED_VERSION_
)
SELECT
    'mes-edhr-approval-v1:1:4e80b759-0285-4d77-bf50-048b68d7ce29',
    REV_,
    CATEGORY_,
    NAME_,
    KEY_,
    VERSION_,
    '6b667314-70f9-4bc0-8f15-eb27d3ab5556',
    RESOURCE_NAME_,
    DGRM_RESOURCE_NAME_,
    DESCRIPTION_,
    HAS_START_FORM_KEY_,
    HAS_GRAPHICAL_NOTATION_,
    SUSPENSION_STATE_,
    '1',
    ENGINE_VERSION_,
    DERIVED_FROM_,
    DERIVED_FROM_ROOT_,
    DERIVED_VERSION_
FROM act_re_procdef
WHERE ID_ = 'mes-edhr-approval-v1:1:45469d36-58d5-11f1-ab8c-00155d615441'
  AND NOT EXISTS (
      SELECT 1 FROM act_re_procdef WHERE ID_ = 'mes-edhr-approval-v1:1:4e80b759-0285-4d77-bf50-048b68d7ce29'
  );

INSERT INTO bpm_process_definition_info (
    process_definition_id, model_id, model_type, category, icon, description, form_type, form_id,
    form_conf, form_fields, form_custom_create_path, form_custom_view_path, simple_model, visible, sort,
    start_user_ids, start_dept_ids, manager_user_ids, allow_cancel_running_process, allow_withdraw_task,
    process_id_rule, auto_approval_type, title_setting, summary_setting, process_before_trigger_setting,
    process_after_trigger_setting, task_before_trigger_setting, task_after_trigger_setting, print_template_setting,
    creator, create_time, updater, update_time, deleted, tenant_id
)
VALUES (
    'mes-edhr-approval-v1:1:4e80b759-0285-4d77-bf50-048b68d7ce29',
    '444fbee3-c112-45e6-bc13-d7459ca60b68',
    10,
    'MES',
    NULL,
    'Admin tenant eDHR approval flow',
    20,
    NULL,
    NULL,
    NULL,
    '/mes/pro/feedback/edhr-batch-execution',
    '/mes/pro/feedback/edhr-approval/detail',
    NULL,
    b'1',
    1779780843594,
    '1',
    NULL,
    '1',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    'codex',
    NOW(),
    'codex',
    NOW(),
    b'0',
    1
)
ON DUPLICATE KEY UPDATE
    model_id = VALUES(model_id),
    description = VALUES(description),
    visible = VALUES(visible),
    start_user_ids = VALUES(start_user_ids),
    manager_user_ids = VALUES(manager_user_ids),
    updater = VALUES(updater),
    update_time = VALUES(update_time);

COMMIT;

START TRANSACTION;

SET @edhr_approval_v1_parallel_bpmn = '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://yudao.iocoder.cn/edhr">
  <process id="mes-edhr-approval-v1" name="eDHR Approval V1" isExecutable="true">
    <startEvent id="StartEvent" name="提交" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="approveNode" />
    <userTask id="approveNode" name="eDHR审批" flowable:candidateStrategy="35" flowable:candidateParam="">
      <multiInstanceLoopCharacteristics isSequential="false" flowable:collection="${coll_userList}">
        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>
      </multiInstanceLoopCharacteristics>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="approveNode" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
</definitions>';

-- Existing local/test deployments may predate this task and still use edhr_approve or USER strategy.
-- Keep the process definition IDs stable, but replace the deployment/model BPMN bytes with the required
-- approveNode start-user-select parallel multi-instance node. The application must be restarted after
-- applying this seed so Flowable reloads the updated process model from ACT_GE_BYTEARRAY.
UPDATE act_ge_bytearray bytearray
JOIN act_re_procdef procdef ON procdef.DEPLOYMENT_ID_ = bytearray.DEPLOYMENT_ID_
SET bytearray.BYTES_ = CONVERT(@edhr_approval_v1_parallel_bpmn USING BINARY)
WHERE procdef.KEY_ = 'mes-edhr-approval-v1'
  AND bytearray.NAME_ LIKE '%.bpmn';

UPDATE act_ge_bytearray bytearray
JOIN act_re_model model ON model.EDITOR_SOURCE_VALUE_ID_ = bytearray.ID_
SET bytearray.BYTES_ = CONVERT(@edhr_approval_v1_parallel_bpmn USING BINARY)
WHERE model.KEY_ = 'mes-edhr-approval-v1';

UPDATE bpm_process_definition_info
SET form_custom_create_path = '/mes/pro/feedback/edhr-batch-execution',
    form_custom_view_path = '/mes/pro/feedback/edhr-approval/detail',
    update_time = NOW()
WHERE process_definition_id LIKE 'mes-edhr-approval-v1:%';

COMMIT;
