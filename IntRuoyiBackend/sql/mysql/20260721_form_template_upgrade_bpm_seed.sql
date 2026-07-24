-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy; type=seed; riskLevel=low
-- Purpose: deploy the dedicated BPM process and platform business approval policy for Form Center template upgrades.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_form_template_upgrade_bpm_seed;

DELIMITER //
CREATE PROCEDURE ensure_form_template_upgrade_bpm_seed()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_business_approval_policy'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template upgrade seed requires bpm_business_approval_policy';
  END IF;

  INSERT INTO `bpm_category` (
    `name`, `code`, `description`, `status`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT '表单模板', 'FORM_TEMPLATE', '表单模板升版审批流程分类', 0, 30,
         'codex', NOW(), 'codex', NOW(), b'0', `tenant_scope`.`tenant_id`
  FROM (
    SELECT 1 AS `tenant_id`
    UNION ALL
    SELECT 122 AS `tenant_id`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_category` AS `category`
    WHERE `category`.`code` = 'FORM_TEMPLATE'
      AND `category`.`tenant_id` = `tenant_scope`.`tenant_id`
      AND `category`.`deleted` = b'0'
  );

  INSERT INTO `system_role_category` (
    `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT '表单中心', 'form-center', 30, 0, '表单中心审批权限角色',
         'codex', NOW(), 'codex', NOW(), b'0', `tenant_scope`.`tenant_id`
  FROM (
    SELECT 1 AS `tenant_id`
    UNION ALL
    SELECT 122 AS `tenant_id`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role_category` AS `category`
    WHERE `category`.`code` = 'form-center'
      AND `category`.`tenant_id` = `tenant_scope`.`tenant_id`
      AND `category`.`deleted` = b'0'
  );

  INSERT INTO `system_role` (
    `name`, `code`, `sort`, `category_id`, `data_scope`, `data_scope_dept_ids`, `status`, `type`,
    `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT '表单模板升版审批员', 'form_template_upgrade_approver', 70, `category`.`id`, 1, '', 0, 2,
         '表单模板升版审批角色', 'codex', NOW(), 'codex', NOW(), b'0', `tenant_scope`.`tenant_id`
  FROM (
    SELECT 1 AS `tenant_id`
    UNION ALL
    SELECT 122 AS `tenant_id`
  ) AS `tenant_scope`
  JOIN `system_role_category` AS `category`
    ON `category`.`code` = 'form-center'
   AND `category`.`tenant_id` = `tenant_scope`.`tenant_id`
   AND `category`.`deleted` = b'0'
  WHERE NOT EXISTS (
    SELECT 1
    FROM `system_role` AS `role`
    WHERE `role`.`code` = 'form_template_upgrade_approver'
      AND `role`.`tenant_id` = `tenant_scope`.`tenant_id`
      AND `role`.`deleted` = b'0'
  );

  IF EXISTS (
    SELECT 1
    FROM `system_role`
    WHERE `code` = 'form_template_upgrade_approver'
      AND `tenant_id` IN (1, 122)
      AND `deleted` = b'0'
    GROUP BY `tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate active form template upgrade approver role';
  END IF;

  INSERT INTO `system_user_role` (
    `user_id`, `role_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT `user`.`id`, `role`.`id`, 'codex', NOW(), 'codex', NOW(), b'0', `user`.`tenant_id`
  FROM `system_users` AS `user`
  JOIN `system_role` AS `role`
    ON `role`.`code` = 'form_template_upgrade_approver'
   AND `role`.`tenant_id` = `user`.`tenant_id`
   AND `role`.`deleted` = b'0'
  WHERE ((`user`.`tenant_id` = 1 AND `user`.`username` = 'admin')
      OR (`user`.`tenant_id` = 122 AND `user`.`username` = 'aoteman'))
    AND `user`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_user_role` AS `existing`
      WHERE `existing`.`user_id` = `user`.`id`
        AND `existing`.`role_id` = `role`.`id`
        AND `existing`.`tenant_id` = `user`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  INSERT INTO `system_role_menu` (
    `role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT `role`.`id`, `menu_scope`.`menu_id`, 'codex', NOW(), 'codex', NOW(), b'0', `role`.`tenant_id`
  FROM `system_role` AS `role`
  JOIN (
    SELECT 1200 AS `menu_id`
    UNION ALL SELECT 1207 AS `menu_id`
    UNION ALL SELECT 1208 AS `menu_id`
    UNION ALL SELECT 1221 AS `menu_id`
    UNION ALL SELECT 1222 AS `menu_id`
  ) AS `menu_scope`
  JOIN `system_menu` AS `menu`
    ON `menu`.`id` = `menu_scope`.`menu_id`
   AND `menu`.`deleted` = b'0'
  WHERE `role`.`code` = 'form_template_upgrade_approver'
    AND `role`.`tenant_id` IN (1, 122)
    AND `role`.`deleted` = b'0'
    AND NOT EXISTS (
      SELECT 1
      FROM `system_role_menu` AS `existing`
      WHERE `existing`.`role_id` = `role`.`id`
        AND `existing`.`menu_id` = `menu_scope`.`menu_id`
        AND `existing`.`tenant_id` = `role`.`tenant_id`
        AND `existing`.`deleted` = b'0'
    );

  SET @form_template_admin_role_id_tenant_1 = (
    SELECT `id`
    FROM `system_role`
    WHERE `code` = 'form_template_upgrade_approver'
      AND `tenant_id` = 1
      AND `deleted` = b'0'
    LIMIT 1
  );
  SET @form_template_admin_role_id_tenant_122 = (
    SELECT `id`
    FROM `system_role`
    WHERE `code` = 'form_template_upgrade_approver'
      AND `tenant_id` = 122
      AND `deleted` = b'0'
    LIMIT 1
  );
  SET @form_template_manager_user_id_tenant_1 = (
    SELECT `id`
    FROM `system_users`
    WHERE `username` = 'admin'
      AND `tenant_id` = 1
      AND `deleted` = b'0'
    LIMIT 1
  );
  SET @form_template_manager_user_id_tenant_122 = (
    SELECT `id`
    FROM `system_users`
    WHERE `username` = 'aoteman'
      AND `tenant_id` = 122
      AND `deleted` = b'0'
    LIMIT 1
  );

  IF @form_template_admin_role_id_tenant_1 IS NULL
     OR @form_template_admin_role_id_tenant_122 IS NULL
     OR @form_template_manager_user_id_tenant_1 IS NULL
     OR @form_template_manager_user_id_tenant_122 IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Form template upgrade seed requires admin and aoteman approver principals';
  END IF;

  SET @form_template_upgrade_bpmn_tenant_1 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/form-center/template-upgrade">
  <process id="form-template-upgrade-v1" name="表单模板升版审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交升版" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="formTemplateUpgradeApprove" />
    <userTask id="formTemplateUpgradeApprove" name="表单模板升版审核">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @form_template_admin_role_id_tenant_1, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="formTemplateUpgradeApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_form-template-upgrade-v1">
    <bpmndi:BPMNPlane id="BPMNPlane_form-template-upgrade-v1" bpmnElement="form-template-upgrade-v1">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="180" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="formTemplateUpgradeApprove_di" bpmnElement="formTemplateUpgradeApprove">
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

  SET @form_template_upgrade_bpmn_tenant_122 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/form-center/template-upgrade">
  <process id="form-template-upgrade-v1" name="表单模板升版审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交升版" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="formTemplateUpgradeApprove" />
    <userTask id="formTemplateUpgradeApprove" name="表单模板升版审核">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @form_template_admin_role_id_tenant_122, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="formTemplateUpgradeApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_form-template-upgrade-v1">
    <bpmndi:BPMNPlane id="BPMNPlane_form-template-upgrade-v1" bpmnElement="form-template-upgrade-v1">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="180" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="formTemplateUpgradeApprove_di" bpmnElement="formTemplateUpgradeApprove">
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

  INSERT INTO `act_re_deployment` (
    `ID_`, `NAME_`, `CATEGORY_`, `KEY_`, `TENANT_ID_`, `DEPLOY_TIME_`, `DERIVED_FROM_`, `DERIVED_FROM_ROOT_`,
    `PARENT_DEPLOYMENT_ID_`, `ENGINE_VERSION_`
  )
  SELECT `deploy_id`, '表单模板升版审批', 'FORM_TEMPLATE', 'form-template-upgrade-v1',
         `tenant_id_text`, NOW(3), NULL, NULL, `deploy_id`, NULL
  FROM (
    SELECT 'form-template-upgrade-deploy-tenant-1' AS `deploy_id`, '1' AS tenant_id_text
    UNION ALL
    SELECT 'form-template-upgrade-deploy-tenant-122' AS `deploy_id`, '122' AS tenant_id_text
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'form-template-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  );

  INSERT INTO `act_ge_bytearray` (`ID_`, `REV_`, `NAME_`, `DEPLOYMENT_ID_`, `BYTES_`, `GENERATED_`)
  SELECT `bytearray_id`, 1, 'source', NULL, CONVERT(`bpmn_xml` USING BINARY), 0
  FROM (
    SELECT 'form-template-upgrade-model-src-tenant-1' AS `bytearray_id`,
           '1' AS `tenant_id_text`,
           @form_template_upgrade_bpmn_tenant_1 AS `bpmn_xml`
    UNION ALL
    SELECT 'form-template-upgrade-model-src-tenant-122' AS `bytearray_id`,
           '122' AS `tenant_id_text`,
           @form_template_upgrade_bpmn_tenant_122 AS `bpmn_xml`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'form-template-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  ) AND NOT EXISTS (
    SELECT 1 FROM `act_ge_bytearray` AS `bytearray` WHERE `bytearray`.`ID_` = `tenant_scope`.`bytearray_id`
  );

  INSERT INTO `act_re_model` (
    `ID_`, `REV_`, `NAME_`, `KEY_`, `CATEGORY_`, `CREATE_TIME_`, `LAST_UPDATE_TIME_`, `VERSION_`,
    `META_INFO_`, `DEPLOYMENT_ID_`, `EDITOR_SOURCE_VALUE_ID_`, `EDITOR_SOURCE_EXTRA_VALUE_ID_`, `TENANT_ID_`
  )
  SELECT `model_id`, 1, '表单模板升版审批', 'form-template-upgrade-v1', 'FORM_TEMPLATE',
         NOW(3), NOW(3), 1, `meta_info`, `deploy_id`, `source_id`, NULL, `tenant_id_text`
  FROM (
    SELECT 'form-template-upgrade-model-tenant-1' AS `model_id`,
           'form-template-upgrade-deploy-tenant-1' AS `deploy_id`,
           'form-template-upgrade-model-src-tenant-1' AS `source_id`,
           '1' AS `tenant_id_text`,
           CONCAT('{"icon":null,"description":"Form Center template upgrade approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/approval-center/manager/form-center/template","formCustomViewPath":"/approval-center/manager/form-center/template","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @form_template_manager_user_id_tenant_1, '],"sort":1784680000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"表单模板升版 {objectVersion}"},"summarySetting":{"enable":true,"summary":["objectType","objectId","objectVersion","approvalRequestId"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS `meta_info`
    UNION ALL
    SELECT 'form-template-upgrade-model-tenant-122' AS `model_id`,
           'form-template-upgrade-deploy-tenant-122' AS `deploy_id`,
           'form-template-upgrade-model-src-tenant-122' AS `source_id`,
           '122' AS `tenant_id_text`,
           CONCAT('{"icon":null,"description":"Form Center template upgrade approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/approval-center/manager/form-center/template","formCustomViewPath":"/approval-center/manager/form-center/template","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @form_template_manager_user_id_tenant_122, '],"sort":1784680000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"表单模板升版 {objectVersion}"},"summarySetting":{"enable":true,"summary":["objectType","objectId","objectVersion","approvalRequestId"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS `meta_info`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'form-template-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  );

  INSERT INTO `act_ge_bytearray` (`ID_`, `REV_`, `NAME_`, `DEPLOYMENT_ID_`, `BYTES_`, `GENERATED_`)
  SELECT `bytearray_id`, 1, 'form-template-upgrade-v1.bpmn', `deploy_id`, CONVERT(`bpmn_xml` USING BINARY), 0
  FROM (
    SELECT 'form-template-upgrade-bpmn-tenant-1' AS `bytearray_id`,
           'form-template-upgrade-deploy-tenant-1' AS `deploy_id`,
           '1' AS `tenant_id_text`,
           @form_template_upgrade_bpmn_tenant_1 AS `bpmn_xml`
    UNION ALL
    SELECT 'form-template-upgrade-bpmn-tenant-122' AS `bytearray_id`,
           'form-template-upgrade-deploy-tenant-122' AS `deploy_id`,
           '122' AS `tenant_id_text`,
           @form_template_upgrade_bpmn_tenant_122 AS `bpmn_xml`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'form-template-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  ) AND NOT EXISTS (
    SELECT 1 FROM `act_ge_bytearray` AS `bytearray` WHERE `bytearray`.`ID_` = `tenant_scope`.`bytearray_id`
  );

  UPDATE `act_ge_bytearray` AS `bytearray`
  JOIN (
    SELECT 'form-template-upgrade-model-src-tenant-1' AS `bytearray_id`, @form_template_upgrade_bpmn_tenant_1 AS `bpmn_xml`
    UNION ALL
    SELECT 'form-template-upgrade-bpmn-tenant-1' AS `bytearray_id`, @form_template_upgrade_bpmn_tenant_1 AS `bpmn_xml`
    UNION ALL
    SELECT 'form-template-upgrade-model-src-tenant-122' AS `bytearray_id`, @form_template_upgrade_bpmn_tenant_122 AS `bpmn_xml`
    UNION ALL
    SELECT 'form-template-upgrade-bpmn-tenant-122' AS `bytearray_id`, @form_template_upgrade_bpmn_tenant_122 AS `bpmn_xml`
  ) AS `seed`
    ON `seed`.`bytearray_id` = `bytearray`.`ID_`
  SET `bytearray`.`BYTES_` = CONVERT(`seed`.`bpmn_xml` USING BINARY),
      `bytearray`.`REV_` = COALESCE(`bytearray`.`REV_`, 0) + 1
  WHERE LOCATE('formTemplateUpgradeApprove', CONVERT(`bytearray`.`BYTES_` USING utf8mb4)) = 0
     OR LOCATE('BPMNDiagram', CONVERT(`bytearray`.`BYTES_` USING utf8mb4)) = 0;

  UPDATE `act_re_model` AS `model`
  JOIN (
    SELECT 'form-template-upgrade-model-tenant-1' AS `model_id`,
           CONCAT('{"icon":null,"description":"Form Center template upgrade approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/approval-center/manager/form-center/template","formCustomViewPath":"/approval-center/manager/form-center/template","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @form_template_manager_user_id_tenant_1, '],"sort":1784680000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"表单模板升版 {objectVersion}"},"summarySetting":{"enable":true,"summary":["objectType","objectId","objectVersion","approvalRequestId"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS `meta_info`
    UNION ALL
    SELECT 'form-template-upgrade-model-tenant-122' AS `model_id`,
           CONCAT('{"icon":null,"description":"Form Center template upgrade approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/approval-center/manager/form-center/template","formCustomViewPath":"/approval-center/manager/form-center/template","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @form_template_manager_user_id_tenant_122, '],"sort":1784680000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"表单模板升版 {objectVersion}"},"summarySetting":{"enable":true,"summary":["objectType","objectId","objectVersion","approvalRequestId"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS `meta_info`
  ) AS `seed`
    ON `seed`.`model_id` = `model`.`ID_`
  SET `model`.`META_INFO_` = `seed`.`meta_info`,
      `model`.`LAST_UPDATE_TIME_` = NOW(3),
      `model`.`REV_` = COALESCE(`model`.`REV_`, 0) + 1
  WHERE `model`.`KEY_` = 'form-template-upgrade-v1'
    AND `model`.`TENANT_ID_` IN ('1', '122');

  INSERT INTO `act_re_procdef` (
    `ID_`, `REV_`, `CATEGORY_`, `NAME_`, `KEY_`, `VERSION_`, `DEPLOYMENT_ID_`, `RESOURCE_NAME_`, `DGRM_RESOURCE_NAME_`,
    `DESCRIPTION_`, `HAS_START_FORM_KEY_`, `HAS_GRAPHICAL_NOTATION_`, `SUSPENSION_STATE_`, `TENANT_ID_`,
    `ENGINE_VERSION_`, `DERIVED_FROM_`, `DERIVED_FROM_ROOT_`, `DERIVED_VERSION_`
  )
  SELECT `procdef_id`, 1, 'FORM_TEMPLATE', '表单模板升版审批', 'form-template-upgrade-v1',
         1, `deploy_id`, 'form-template-upgrade-v1.bpmn', NULL,
         'Form Center template upgrade approval', 0, 1, 1, `tenant_id_text`,
         NULL, NULL, NULL, 0
  FROM (
    SELECT 'form-template-upgrade-v1:1:form-template-admin' AS `procdef_id`,
           'form-template-upgrade-deploy-tenant-1' AS `deploy_id`,
           '1' AS `tenant_id_text`
    UNION ALL
    SELECT 'form-template-upgrade-v1:1:form-template-test' AS `procdef_id`,
           'form-template-upgrade-deploy-tenant-122' AS `deploy_id`,
           '122' AS `tenant_id_text`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'form-template-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  );

  UPDATE `act_re_procdef`
  SET `HAS_GRAPHICAL_NOTATION_` = 1
  WHERE `KEY_` = 'form-template-upgrade-v1'
    AND `TENANT_ID_` IN ('1', '122');

  INSERT INTO `bpm_process_definition_info` (
    `process_definition_id`, `model_id`, `model_type`, `category`, `icon`, `description`, `form_type`, `form_id`,
    `form_conf`, `form_fields`, `form_custom_create_path`, `form_custom_view_path`, `simple_model`, `visible`, `sort`,
    `start_user_ids`, `start_dept_ids`, `manager_user_ids`, `allow_cancel_running_process`, `allow_withdraw_task`,
    `process_id_rule`, `auto_approval_type`, `title_setting`, `summary_setting`, `process_before_trigger_setting`,
    `process_after_trigger_setting`, `task_before_trigger_setting`, `task_after_trigger_setting`, `print_template_setting`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT `procdef_id`, `model_id`, 10, 'FORM_TEMPLATE', NULL, 'Form Center template upgrade approval',
         20, NULL, NULL, NULL, '/approval-center/manager/form-center/template',
         '/approval-center/manager/form-center/template', NULL, b'0', 1784680000000, NULL, NULL,
         CAST(`manager_user_id` AS CHAR), NULL, NULL, NULL, NULL,
         '{"enable":true,"title":"表单模板升版 {objectVersion}"}',
         '{"enable":true,"summary":["objectType","objectId","objectVersion","approvalRequestId"]}',
         NULL, NULL, NULL, NULL, NULL,
         'codex', NOW(), 'codex', NOW(), b'0', `tenant_id`
  FROM (
    SELECT 'form-template-upgrade-v1:1:form-template-admin' AS `procdef_id`,
           'form-template-upgrade-model-tenant-1' AS `model_id`,
           @form_template_manager_user_id_tenant_1 AS `manager_user_id`,
           1 AS tenant_id
    UNION ALL
    SELECT 'form-template-upgrade-v1:1:form-template-test' AS `procdef_id`,
           'form-template-upgrade-model-tenant-122' AS `model_id`,
           @form_template_manager_user_id_tenant_122 AS `manager_user_id`,
           122 AS tenant_id
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_process_definition_info` AS `info`
    WHERE `info`.`process_definition_id` = `tenant_scope`.`procdef_id`
      AND `info`.`tenant_id` = `tenant_scope`.`tenant_id`
      AND `info`.`deleted` = b'0'
  );

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy`
    WHERE `tenant_id` IN (1, 122)
      AND `data_domain` = 'FORM_CENTER'
      AND `system_code` = 'FORM_CENTER'
      AND `object_type` = 'FORM_TEMPLATE'
      AND `action_code` = 'UPGRADE'
      AND `object_state` = 'DRAFT'
      AND `status` = 'PUBLISHED'
      AND `deleted` = b'0'
    GROUP BY `tenant_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Duplicate published form template upgrade business approval policy';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`tenant_id` IN (1, 122)
      AND `policy`.`data_domain` = 'FORM_CENTER'
      AND `policy`.`system_code` = 'FORM_CENTER'
      AND `policy`.`object_type` = 'FORM_TEMPLATE'
      AND `policy`.`action_code` = 'UPGRADE'
      AND `policy`.`object_state` = 'DRAFT'
      AND `policy`.`status` = 'PUBLISHED'
      AND `policy`.`deleted` = b'0'
      AND (
        COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'
        OR COALESCE(`policy`.`process_definition_key`, '') <> 'form-template-upgrade-v1'
        OR COALESCE(`policy`.`effect_executor_code`, '') <> 'FORM_TEMPLATE_UPGRADE'
      )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Conflicting published form template upgrade business approval policy';
  END IF;

  INSERT INTO `bpm_business_approval_policy` (
    `tenant_id`, `data_domain`, `system_code`, `object_type`, `action_code`, `object_state`,
    `policy_mode`, `process_definition_key`, `effect_executor_code`, `status`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
  )
  SELECT `tenant_scope`.`tenant_id`, 'FORM_CENTER', 'FORM_CENTER', 'FORM_TEMPLATE', 'UPGRADE', 'DRAFT',
         'BPM_REQUIRED', 'form-template-upgrade-v1', 'FORM_TEMPLATE_UPGRADE', 'PUBLISHED',
         'Form template upgrade approval through platform business approval policy',
         'codex', NOW(), 'codex', NOW(), b'0'
  FROM (
    SELECT 1 AS tenant_id
    UNION ALL
    SELECT 122 AS tenant_id
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1
    FROM `bpm_business_approval_policy` AS `policy`
    WHERE `policy`.`tenant_id` = `tenant_scope`.`tenant_id`
      AND `policy`.`data_domain` = 'FORM_CENTER'
      AND `policy`.`system_code` = 'FORM_CENTER'
      AND `policy`.`object_type` = 'FORM_TEMPLATE'
      AND `policy`.`action_code` = 'UPGRADE'
      AND `policy`.`object_state` = 'DRAFT'
      AND `policy`.`status` = 'PUBLISHED'
      AND `policy`.`deleted` = b'0'
  );
END//
DELIMITER ;

START TRANSACTION;
CALL ensure_form_template_upgrade_bpm_seed();
COMMIT;

DROP PROCEDURE IF EXISTS ensure_form_template_upgrade_bpm_seed;
