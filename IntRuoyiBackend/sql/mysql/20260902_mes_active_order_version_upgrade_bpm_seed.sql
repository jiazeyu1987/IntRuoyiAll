-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260719_business_approval_policy; type=seed; riskLevel=low
-- MES active-order latest-version upgrade restart BPM process seed.

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ensure_mes_active_order_version_upgrade_bpm_seed;

DELIMITER //
CREATE PROCEDURE ensure_mes_active_order_version_upgrade_bpm_seed()
BEGIN
  SET @active_order_upgrade_admin_role_id_tenant_1 = (
    SELECT `id`
    FROM `system_role`
    WHERE `code` = 'mes_route_version_admin'
      AND `tenant_id` = 1
      AND `deleted` = b'0'
    LIMIT 1
  );
  SET @active_order_upgrade_admin_role_id_tenant_122 = (
    SELECT `id`
    FROM `system_role`
    WHERE `code` = 'mes_route_version_admin'
      AND `tenant_id` = 122
      AND `deleted` = b'0'
    LIMIT 1
  );
  SET @active_order_upgrade_manager_user_id_tenant_1 = (
    SELECT `id`
    FROM `system_users`
    WHERE `username` = 'admin'
      AND `tenant_id` = 1
      AND `deleted` = b'0'
    LIMIT 1
  );
  SET @active_order_upgrade_manager_user_id_tenant_122 = (
    SELECT `id`
    FROM `system_users`
    WHERE `username` = 'aoteman'
      AND `tenant_id` = 122
      AND `deleted` = b'0'
    LIMIT 1
  );

  IF @active_order_upgrade_admin_role_id_tenant_1 IS NULL
     OR @active_order_upgrade_admin_role_id_tenant_122 IS NULL
     OR @active_order_upgrade_manager_user_id_tenant_1 IS NULL
     OR @active_order_upgrade_manager_user_id_tenant_122 IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'MES active-order version-upgrade BPM seed requires route-version admin principals';
  END IF;

  SET @active_order_upgrade_bpmn_tenant_1 = CONCAT('<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/mes/active-order-version-upgrade">
  <process id="mes-active-order-version-upgrade-v1" name="活跃订单升级重启审批" isExecutable="true">
    <startEvent id="StartEvent" name="提交升级" />
    <sequenceFlow id="flow_start_approve" sourceRef="StartEvent" targetRef="activeOrderVersionUpgradeApprove" />
    <userTask id="activeOrderVersionUpgradeApprove" name="活跃订单升级重启审核">
      <extensionElements>
        <flowable:candidateStrategy>10</flowable:candidateStrategy>
        <flowable:candidateParam>', @active_order_upgrade_admin_role_id_tenant_1, '</flowable:candidateParam>
      </extensionElements>
    </userTask>
    <sequenceFlow id="flow_approve_end" sourceRef="activeOrderVersionUpgradeApprove" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_mes-active-order-version-upgrade-v1">
    <bpmndi:BPMNPlane id="BPMNPlane_mes-active-order-version-upgrade-v1" bpmnElement="mes-active-order-version-upgrade-v1">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="180" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="activeOrderVersionUpgradeApprove_di" bpmnElement="activeOrderVersionUpgradeApprove">
        <omgdc:Bounds x="300" y="198" width="210" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_di" bpmnElement="EndEvent">
        <omgdc:Bounds x="600" y="220" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="flow_start_approve_di" bpmnElement="flow_start_approve">
        <omgdi:waypoint x="216" y="238" />
        <omgdi:waypoint x="300" y="238" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow_approve_end_di" bpmnElement="flow_approve_end">
        <omgdi:waypoint x="510" y="238" />
        <omgdi:waypoint x="600" y="238" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>');

  SET @active_order_upgrade_bpmn_tenant_122 = CONCAT(REPLACE(@active_order_upgrade_bpmn_tenant_1, CONCAT('<flowable:candidateParam>', @active_order_upgrade_admin_role_id_tenant_1, '</flowable:candidateParam>'), CONCAT('<flowable:candidateParam>', @active_order_upgrade_admin_role_id_tenant_122, '</flowable:candidateParam>')));

  INSERT INTO `act_re_deployment` (`ID_`, `NAME_`, `CATEGORY_`, `KEY_`, `TENANT_ID_`, `DEPLOY_TIME_`, `DERIVED_FROM_`, `DERIVED_FROM_ROOT_`, `PARENT_DEPLOYMENT_ID_`, `ENGINE_VERSION_`)
  SELECT `deploy_id`, '活跃订单升级重启审批', 'MES_ACTIVE_ORDER', 'mes-active-order-version-upgrade-v1', `tenant_id_text`, NOW(3), NULL, NULL, `deploy_id`, NULL
  FROM (
    SELECT 'mes-active-order-upgrade-deploy-tenant-1' AS `deploy_id`, '1' AS `tenant_id_text`
    UNION ALL
    SELECT 'mes-active-order-upgrade-deploy-tenant-122' AS `deploy_id`, '122' AS `tenant_id_text`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1 FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'mes-active-order-version-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  );

  INSERT INTO `act_ge_bytearray` (`ID_`, `REV_`, `NAME_`, `DEPLOYMENT_ID_`, `BYTES_`, `GENERATED_`)
  SELECT `bytearray_id`, 1, 'source', NULL, CONVERT(`bpmn_xml` USING BINARY), 0
  FROM (
    SELECT 'mes-active-order-upgrade-model-src-tenant-1' AS `bytearray_id`, '1' AS `tenant_id_text`, @active_order_upgrade_bpmn_tenant_1 AS `bpmn_xml`
    UNION ALL
    SELECT 'mes-active-order-upgrade-model-src-tenant-122' AS `bytearray_id`, '122' AS `tenant_id_text`, @active_order_upgrade_bpmn_tenant_122 AS `bpmn_xml`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1 FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'mes-active-order-version-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  ) AND NOT EXISTS (
    SELECT 1 FROM `act_ge_bytearray` AS `bytearray` WHERE `bytearray`.`ID_` = `tenant_scope`.`bytearray_id`
  );

  INSERT INTO `act_re_model` (`ID_`, `REV_`, `NAME_`, `KEY_`, `CATEGORY_`, `CREATE_TIME_`, `LAST_UPDATE_TIME_`, `VERSION_`, `META_INFO_`, `DEPLOYMENT_ID_`, `EDITOR_SOURCE_VALUE_ID_`, `EDITOR_SOURCE_EXTRA_VALUE_ID_`, `TENANT_ID_`)
  SELECT `model_id`, 1, '活跃订单升级重启审批', 'mes-active-order-version-upgrade-v1', 'MES_ACTIVE_ORDER', NOW(3), NOW(3), 1, `meta_info`, `deploy_id`, `source_id`, NULL, `tenant_id_text`
  FROM (
    SELECT 'mes-active-order-upgrade-model-tenant-1' AS `model_id`, 'mes-active-order-upgrade-deploy-tenant-1' AS `deploy_id`, 'mes-active-order-upgrade-model-src-tenant-1' AS `source_id`, '1' AS `tenant_id_text`,
           CONCAT('{"icon":null,"description":"MES active-order latest-version upgrade restart approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/process-pool/team-leader","formCustomViewPath":"/mes/pro/process-pool/team-leader","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @active_order_upgrade_manager_user_id_tenant_1, '],"sort":1784740000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"活跃订单升级重启 {requestCode}"},"summarySetting":{"enable":true,"summary":["requestCode","sourceActiveOrderId","sourceWorkOrderId","targetVersions"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS `meta_info`
    UNION ALL
    SELECT 'mes-active-order-upgrade-model-tenant-122' AS `model_id`, 'mes-active-order-upgrade-deploy-tenant-122' AS `deploy_id`, 'mes-active-order-upgrade-model-src-tenant-122' AS `source_id`, '122' AS `tenant_id_text`,
           CONCAT('{"icon":null,"description":"MES active-order latest-version upgrade restart approval","type":10,"formType":20,"formId":null,"formCustomCreatePath":"/mes/pro/process-pool/team-leader","formCustomViewPath":"/mes/pro/process-pool/team-leader","visible":false,"startUserIds":null,"startDeptIds":null,"managerUserIds":[', @active_order_upgrade_manager_user_id_tenant_122, '],"sort":1784740000000,"allowCancelRunningProcess":null,"allowWithdrawTask":null,"processIdRule":null,"autoApprovalType":null,"titleSetting":{"enable":true,"title":"活跃订单升级重启 {requestCode}"},"summarySetting":{"enable":true,"summary":["requestCode","sourceActiveOrderId","sourceWorkOrderId","targetVersions"]},"processBeforeTriggerSetting":null,"processAfterTriggerSetting":null,"taskBeforeTriggerSetting":null,"taskAfterTriggerSetting":null,"printTemplateSetting":null}') AS `meta_info`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1 FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'mes-active-order-version-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  );

  INSERT INTO `act_ge_bytearray` (`ID_`, `REV_`, `NAME_`, `DEPLOYMENT_ID_`, `BYTES_`, `GENERATED_`)
  SELECT `bytearray_id`, 1, 'mes-active-order-version-upgrade-v1.bpmn', `deploy_id`, CONVERT(`bpmn_xml` USING BINARY), 0
  FROM (
    SELECT 'mes-active-order-upgrade-bpmn-tenant-1' AS `bytearray_id`, 'mes-active-order-upgrade-deploy-tenant-1' AS `deploy_id`, '1' AS `tenant_id_text`, @active_order_upgrade_bpmn_tenant_1 AS `bpmn_xml`
    UNION ALL
    SELECT 'mes-active-order-upgrade-bpmn-tenant-122' AS `bytearray_id`, 'mes-active-order-upgrade-deploy-tenant-122' AS `deploy_id`, '122' AS `tenant_id_text`, @active_order_upgrade_bpmn_tenant_122 AS `bpmn_xml`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1 FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'mes-active-order-version-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  ) AND NOT EXISTS (
    SELECT 1 FROM `act_ge_bytearray` AS `bytearray` WHERE `bytearray`.`ID_` = `tenant_scope`.`bytearray_id`
  );

  INSERT INTO `act_re_procdef` (`ID_`, `REV_`, `CATEGORY_`, `NAME_`, `KEY_`, `VERSION_`, `DEPLOYMENT_ID_`, `RESOURCE_NAME_`, `DGRM_RESOURCE_NAME_`, `DESCRIPTION_`, `HAS_START_FORM_KEY_`, `HAS_GRAPHICAL_NOTATION_`, `SUSPENSION_STATE_`, `TENANT_ID_`, `ENGINE_VERSION_`, `DERIVED_FROM_`, `DERIVED_FROM_ROOT_`, `DERIVED_VERSION_`)
  SELECT `procdef_id`, 1, 'MES_ACTIVE_ORDER', '活跃订单升级重启审批', 'mes-active-order-version-upgrade-v1', 1, `deploy_id`, 'mes-active-order-version-upgrade-v1.bpmn', NULL, 'MES active-order latest-version upgrade restart approval', 0, 1, 1, `tenant_id_text`, NULL, NULL, NULL, 0
  FROM (
    SELECT 'mes-active-order-version-upgrade-v1:1:active-order-admin' AS `procdef_id`, 'mes-active-order-upgrade-deploy-tenant-1' AS `deploy_id`, '1' AS `tenant_id_text`
    UNION ALL
    SELECT 'mes-active-order-version-upgrade-v1:1:active-order-test' AS `procdef_id`, 'mes-active-order-upgrade-deploy-tenant-122' AS `deploy_id`, '122' AS `tenant_id_text`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1 FROM `act_re_procdef` AS `procdef`
    WHERE `procdef`.`KEY_` = 'mes-active-order-version-upgrade-v1'
      AND `procdef`.`TENANT_ID_` = `tenant_scope`.`tenant_id_text`
  );

  UPDATE `act_re_procdef`
  SET `HAS_GRAPHICAL_NOTATION_` = 1
  WHERE `KEY_` = 'mes-active-order-version-upgrade-v1'
    AND `TENANT_ID_` IN ('1', '122');

  INSERT INTO `bpm_process_definition_info` (`process_definition_id`, `model_id`, `model_type`, `category`, `icon`, `description`, `form_type`, `form_id`, `form_conf`, `form_fields`, `form_custom_create_path`, `form_custom_view_path`, `simple_model`, `visible`, `sort`, `start_user_ids`, `start_dept_ids`, `manager_user_ids`, `allow_cancel_running_process`, `allow_withdraw_task`, `process_id_rule`, `auto_approval_type`, `title_setting`, `summary_setting`, `process_before_trigger_setting`, `process_after_trigger_setting`, `task_before_trigger_setting`, `task_after_trigger_setting`, `print_template_setting`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
  SELECT `procdef_id`, `model_id`, 10, 'MES_ACTIVE_ORDER', NULL, 'MES active-order latest-version upgrade restart approval', 20, NULL, NULL, NULL, '/mes/pro/process-pool/team-leader', '/mes/pro/process-pool/team-leader', NULL, b'0', 1784740000000, NULL, NULL, CAST(`manager_user_id` AS CHAR), NULL, NULL, NULL, NULL, '{"enable":true,"title":"活跃订单升级重启 {requestCode}"}', '{"enable":true,"summary":["requestCode","sourceActiveOrderId","sourceWorkOrderId","targetVersions"]}', NULL, NULL, NULL, NULL, NULL, 'codex', NOW(), 'codex', NOW(), b'0', `tenant_id`
  FROM (
    SELECT 'mes-active-order-version-upgrade-v1:1:active-order-admin' AS `procdef_id`, 'mes-active-order-upgrade-model-tenant-1' AS `model_id`, @active_order_upgrade_manager_user_id_tenant_1 AS `manager_user_id`, 1 AS `tenant_id`
    UNION ALL
    SELECT 'mes-active-order-version-upgrade-v1:1:active-order-test' AS `procdef_id`, 'mes-active-order-upgrade-model-tenant-122' AS `model_id`, @active_order_upgrade_manager_user_id_tenant_122 AS `manager_user_id`, 122 AS `tenant_id`
  ) AS `tenant_scope`
  WHERE NOT EXISTS (
    SELECT 1 FROM `bpm_process_definition_info` AS `info`
    WHERE `info`.`process_definition_id` = `tenant_scope`.`procdef_id`
      AND `info`.`tenant_id` = `tenant_scope`.`tenant_id`
      AND `info`.`deleted` = b'0'
  );
END//
DELIMITER ;

START TRANSACTION;
CALL ensure_mes_active_order_version_upgrade_bpm_seed();
COMMIT;

DROP PROCEDURE IF EXISTS ensure_mes_active_order_version_upgrade_bpm_seed;
