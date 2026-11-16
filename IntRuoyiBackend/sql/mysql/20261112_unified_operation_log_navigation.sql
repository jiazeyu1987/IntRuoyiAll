-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=medium
-- 将日志展示入口集中到系统操作日志，原表、原权限和角色的数据范围不变。
-- Target roles/packages: only existing holders of source query permissions or retired log menus.
-- Grant only the permission-free navigation shell and its ancestors, never system:operate-log:query.
-- Former page nodes become hidden permission nodes so their existing query/export grants remain valid.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS consolidate_operation_log_navigation_20261112;
DELIMITER $$
CREATE PROCEDURE consolidate_operation_log_navigation_20261112()
BEGIN
  DECLARE log_menu_id BIGINT;
  DECLARE ancestor_id BIGINT;
  DECLARE next_id BIGINT;
  DECLARE ancestor_count INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF (SELECT COUNT(*) FROM system_menu WHERE component='system/operatelog/index' AND deleted=0 AND status=0) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Exactly one enabled system operation log menu is required';
  END IF;
  SELECT id INTO log_menu_id FROM system_menu WHERE component='system/operatelog/index' AND deleted=0 AND status=0;
  IF EXISTS(SELECT 1 FROM system_menu WHERE id=log_menu_id AND COALESCE(permission,'')<>'') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Operation log navigation shell must not grant a data permission';
  END IF;
  IF EXISTS(SELECT 1 FROM system_tenant_package WHERE deleted=0 AND (NOT JSON_VALID(menu_ids) OR JSON_TYPE(menu_ids)<>'ARRAY')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tenant package menu_ids must be a valid JSON array';
  END IF;
  DROP TEMPORARY TABLE IF EXISTS operation_log_nav;
  CREATE TEMPORARY TABLE operation_log_nav(menu_id BIGINT PRIMARY KEY);
  SET ancestor_id=log_menu_id;
  WHILE ancestor_id<>0 DO
    IF ancestor_count>=20 OR EXISTS(SELECT 1 FROM operation_log_nav WHERE menu_id=ancestor_id) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Operation log navigation ancestry is cyclic or too deep';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM system_menu WHERE id=ancestor_id AND deleted=0 AND status=0 AND COALESCE(permission,'')='') THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Operation log navigation ancestor is missing, disabled or permission-bearing';
    END IF;
    INSERT INTO operation_log_nav VALUES(ancestor_id);
    SELECT parent_id INTO next_id FROM system_menu WHERE id=ancestor_id;
    SET ancestor_id=next_id;
    SET ancestor_count=ancestor_count+1;
  END WHILE;
  DROP TEMPORARY TABLE IF EXISTS operation_log_sources;
  CREATE TEMPORARY TABLE operation_log_sources(menu_id BIGINT PRIMARY KEY);
  -- Source query permissions are kept in their existing business modules.
  INSERT INTO operation_log_sources
  SELECT id FROM system_menu WHERE deleted=0 AND status=0 AND (
    permission IN (
      'system:operate-log:query','dcc:controlled-file:log:query','dcc:registration-certificate:query-current',
      'mes:pro-batch-record-execution:field-audit-query','mes:pro-edhr-operation-audit:query',
      'mes:pro-edhr-form-fill-log:query','system:login-log:query','system:sms-log:query',
      'system:mail-log:query','infra:api-access-log:query','infra:api-error-log:query',
      'infra:job:query','pay:notify:query','system:notify-message:query',
      'mes:pro-batch-record-execution:golden-finger','mes:pro-workrecord:query',
      'mes:pro-edhr-release:query','mes:pro-edhr-form-instance:query','mes:pro-edhr-traveler:query',
      'mes:pro-edhr-change:event-query','infra:runtime-control:query',
      'crm:clue:query','crm:customer:query','crm:contact:query','crm:business:query',
      'crm:contract:query','crm:product:query','crm:receivable:query','crm:receivable-plan:query',
      'mes:pro-schedule-order:query','trade:order:query','trade:after-sale:query',
      'system:gxp-audit:query','system:temporary-role-grant:query',
      'intern-user:time-maintenance:file-upload-time-audit:query',
      'intern-user:time-maintenance:dcc-published-time-audit:query',
      'intern-user:time-maintenance:dcc-obsoleted-time-audit:query',
      'dcc:controlled-file:signature:manage','mes:pro-edhr-flow-intervention:event-query',
      'mes:pro-edhr-report:query','mes:pro-process-pool-team-leader:query',
      'mes:pro-edhr-batch-execution:query','mes:pro-production-release:query',
      -- Holders of the former workflow failure/cancel reader also need the navigation shell.
      'mes:pro-feedback:query','mes:pro-edhr-work-task:query'
    ) OR component IN (
      'dcc/controlled-file/logs/index','dcc/registration-certificate/history/index','mes/pro/edhr/FieldAuditPage','mes/pro/edhr/FieldAuditDetailPage',
      'mes/pro/edhr/OperationAuditPage','mes/pro/edhr/FormFillLogPage','system/loginlog/index',
      'system/sms/log/index','system/mail/log/index','infra/apiAccessLog/index',
      'infra/apiErrorLog/index','infra/job/logger/index','pay/notify/index','system/notify/message/index'
    )
  );
  DROP TEMPORARY TABLE IF EXISTS operation_log_roles;
  CREATE TEMPORARY TABLE operation_log_roles AS
  SELECT DISTINCT rm.role_id,rm.tenant_id FROM system_role_menu rm
    JOIN operation_log_sources s ON s.menu_id=rm.menu_id
    JOIN system_role r ON r.id=rm.role_id AND r.tenant_id=rm.tenant_id AND r.deleted=0 AND r.status=0
    WHERE rm.deleted=0;
  DROP TEMPORARY TABLE IF EXISTS operation_log_packages;
  CREATE TEMPORARY TABLE operation_log_packages AS
  SELECT DISTINCT p.id FROM system_tenant_package p JOIN operation_log_sources s
    ON JSON_CONTAINS(p.menu_ids,CAST(s.menu_id AS JSON),'$') WHERE p.deleted=0;
  -- Retire the API-log-only container as well; preserve containers with business pages.
  DROP TEMPORARY TABLE IF EXISTS operation_log_containers;
  CREATE TEMPORARY TABLE operation_log_containers AS
  SELECT DISTINCT p.id FROM system_menu p JOIN system_menu c ON c.parent_id=p.id
  WHERE p.deleted=0 AND COALESCE(p.component,'')='' AND COALESCE(p.permission,'')=''
    AND c.deleted=0 AND c.component IN ('infra/apiAccessLog/index','infra/apiErrorLog/index')
    AND NOT EXISTS(SELECT 1 FROM system_menu other WHERE other.parent_id=p.id AND other.deleted=0
      AND other.type<>3 AND COALESCE(other.component,'') NOT IN ('infra/apiAccessLog/index','infra/apiErrorLog/index'));
  START TRANSACTION;
  INSERT INTO system_role_menu(role_id,menu_id,tenant_id,creator,updater)
  SELECT r.role_id,n.menu_id,r.tenant_id,'log-consolidation','log-consolidation'
  FROM operation_log_roles r CROSS JOIN operation_log_nav n
  WHERE NOT EXISTS(SELECT 1 FROM system_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=n.menu_id AND rm.tenant_id=r.tenant_id AND rm.deleted=0);
  SET ancestor_id=log_menu_id;
  WHILE ancestor_id<>0 DO
    UPDATE system_tenant_package p JOIN operation_log_packages t ON p.id=t.id
    SET p.menu_ids=JSON_ARRAY_APPEND(p.menu_ids,'$',ancestor_id),p.updater='log-consolidation'
    WHERE NOT JSON_CONTAINS(p.menu_ids,CAST(ancestor_id AS JSON),'$');
    SELECT parent_id INTO next_id FROM system_menu WHERE id=ancestor_id;
    SET ancestor_id=next_id;
  END WHILE;
  -- Move button permissions before retiring their page parents. Do not delete role_menu links.
  UPDATE system_menu child JOIN system_menu parent ON child.parent_id=parent.id
  SET child.parent_id=log_menu_id,child.updater='log-consolidation'
  WHERE parent.deleted=0 AND parent.component IN (
    'dcc/controlled-file/logs/index','dcc/registration-certificate/history/index','mes/pro/edhr/FieldAuditPage','mes/pro/edhr/FieldAuditDetailPage',
    'mes/pro/edhr/OperationAuditPage','mes/pro/edhr/FormFillLogPage','system/loginlog/index',
    'system/sms/log/index','system/mail/log/index','infra/apiAccessLog/index',
    'infra/apiErrorLog/index','infra/job/logger/index','pay/notify/index','system/notify/message/index'
  ) AND child.type=3 AND child.deleted=0;
  UPDATE system_menu SET type=3,visible=0,parent_id=log_menu_id,path='',component='',component_name='',updater='log-consolidation'
  WHERE deleted=0 AND component IN (
    'dcc/controlled-file/logs/index','dcc/registration-certificate/history/index','mes/pro/edhr/FieldAuditPage','mes/pro/edhr/FieldAuditDetailPage',
    'mes/pro/edhr/OperationAuditPage','mes/pro/edhr/FormFillLogPage','system/loginlog/index',
    'system/sms/log/index','system/mail/log/index','infra/apiAccessLog/index',
    'infra/apiErrorLog/index','infra/job/logger/index','pay/notify/index','system/notify/message/index'
  );
  UPDATE system_menu m JOIN operation_log_containers c ON m.id=c.id
  SET m.type=3,m.visible=0,m.parent_id=log_menu_id,m.path='',m.component='',m.component_name='',m.updater='log-consolidation';
  COMMIT;
  DROP TEMPORARY TABLE operation_log_containers;
  DROP TEMPORARY TABLE operation_log_packages;
  DROP TEMPORARY TABLE operation_log_roles;
  DROP TEMPORARY TABLE operation_log_sources;
  DROP TEMPORARY TABLE operation_log_nav;
END$$
DELIMITER ;
CALL consolidate_operation_log_navigation_20261112();
DROP PROCEDURE consolidate_operation_log_navigation_20261112;
