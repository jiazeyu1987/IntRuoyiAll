-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260612_mes_edhr_record_change_menu,20261028_mes_edhr_record_change_tasks; type=menu; riskLevel=low
-- 租户 1（芋道源码）正式业务角色，不绑定任何用户，不改变 QA 或既有角色。
-- 前置：目标租户使用 PACKAGE_ID_SYSTEM=0（TenantDO/TenantServiceImpl 的系统套餐）。
-- 系统套餐已包含全量菜单，无套餐行需要扩权；普通套餐必须另行评审，不自动扩大共享套餐。
-- 入口：个人工作台 / 审批中心查询 -> 已有隐藏变更详情 -> 变更审批接口。
-- 只授 3 项 eDHR 权限与审批中心查询；不授流程任务更新、不授申请、执行或追溯权限。
-- 变更权限菜单的祖先带有其他业务权限，隐藏详情无需这些祖先，故不机械补齐。
-- 幂等：按 tenant_id + code 定位；既有同码角色合同冲突或多余授权即失败，不覆盖。
-- 恢复：无业务数据改写；停用本迁移新建角色前先通过用户界面核对后续用户绑定。
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS ensure_edhr_record_change_approver_role;
DELIMITER $$
CREATE PROCEDURE ensure_edhr_record_change_approver_role()
BEGIN
  DECLARE target_role_id BIGINT DEFAULT NULL;
  DECLARE target_package_id BIGINT DEFAULT NULL;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;

  IF NOT EXISTS (SELECT 1 FROM system_tenant
                 WHERE id=1 AND name='芋道源码' AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing enabled target tenant 1 (YuDao)';
  END IF;
  SELECT package_id INTO target_package_id FROM system_tenant WHERE id=1 FOR UPDATE;
  IF target_package_id IS NULL OR target_package_id<>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tenant 1 requires reviewed system package 0; no package expansion allowed';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM system_role_category
                 WHERE id=5 AND tenant_id=1 AND name='eDHR批记录' AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing tenant 1 eDHR business role category 5';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM system_menu WHERE id=1200 AND parent_id=0
                  AND path='/approval-center' AND permission='' AND type=1 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=1207 AND parent_id=1200
                  AND permission='' AND type=2 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=1221 AND parent_id=1207
                  AND permission='bpm:task:query' AND type=3 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900231
                  AND permission='mes:pro-edhr-work-task:query' AND type=3 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900236
                  AND permission='mes:pro-edhr-change:query' AND type=3 AND status=0 AND deleted=b'0')
     OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=900240
                  AND permission='mes:pro-edhr-change:approve' AND type=3 AND status=0 AND deleted=b'0') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Missing exact existing record-change or approval-center permission menus';
  END IF;

  IF (SELECT COUNT(*) FROM system_role WHERE tenant_id=1 AND code='edhr_record_change_approver')>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate tenant 1 record-change approver role code';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role WHERE tenant_id=1 AND code='edhr_record_change_approver'
              AND (deleted<>b'0' OR status<>0 OR type<>2 OR name<>'eDHR记录变更审批人'
                   OR category_id IS NULL OR category_id<>5 OR data_scope<>5
                   OR data_scope_dept_ids IS NULL OR data_scope_dept_ids<>'')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing record-change approver role conflicts with reviewed contract';
  END IF;
  SELECT MAX(id) INTO target_role_id FROM system_role
   WHERE tenant_id=1 AND code='edhr_record_change_approver' AND deleted=b'0';
  IF target_role_id IS NOT NULL AND EXISTS (
      SELECT 1 FROM system_role_menu WHERE role_id=target_role_id AND deleted=b'0'
       AND (tenant_id<>1 OR menu_id NOT IN (1200,1207,1221,900231,900236,900240))) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing approver role has grants outside reviewed minimal scope';
  END IF;

  IF target_role_id IS NULL THEN
    INSERT INTO system_role
      (name,code,sort,category_id,data_scope,data_scope_dept_ids,status,type,remark,
       creator,create_time,updater,update_time,deleted,tenant_id)
    VALUES ('eDHR记录变更审批人','edhr_record_change_approver',80,5,5,'',0,2,
      '记录变更审批专岗；每次申请显式指定且禁止自审；仅个人工作台、审批中心查询及记录变更查询审批，不授申请或执行权限。',
      'edhr-change-role',NOW(),'edhr-change-role',NOW(),b'0',1);
    SET target_role_id=LAST_INSERT_ID();
  END IF;

  INSERT INTO system_role_menu
    (role_id,menu_id,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT target_role_id,required_menu.id,'edhr-change-role',NOW(),'edhr-change-role',NOW(),b'0',1
    FROM system_menu required_menu
   WHERE required_menu.id IN (1200,1207,1221,900231,900236,900240)
     AND NOT EXISTS (SELECT 1 FROM system_role_menu existing_grant
                      WHERE existing_grant.role_id=target_role_id AND existing_grant.tenant_id=1
                        AND existing_grant.menu_id=required_menu.id AND existing_grant.deleted=b'0');
  COMMIT;
END$$
DELIMITER ;
CALL ensure_edhr_record_change_approver_role();
DROP PROCEDURE ensure_edhr_record_change_approver_role;
