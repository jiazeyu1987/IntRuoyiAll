-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260807_test_tenant1_all_role_permission_sync; type=role; riskLevel=high
-- Merge role 990819811 into retained role 990819799; keep the union of permissions and all user assignments.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS merge_dcc_roles;
DELIMITER $$
CREATE PROCEDURE merge_dcc_roles()
BEGIN
  DECLARE keep_tenant BIGINT;
  SELECT tenant_id INTO keep_tenant FROM system_role WHERE id=990819799 AND deleted=0;
  IF keep_tenant IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Retained DCC role 990819799 is missing';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role WHERE id=990819811 AND deleted=0 AND tenant_id<>keep_tenant) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DCC roles belong to different tenants';
  END IF;
  IF EXISTS (SELECT 1 FROM system_role WHERE id=990819811 AND deleted=0) THEN
    UPDATE system_role SET code='doc_control', name='文控', remark='DCC文控中心全操作权限（合并原本机 admin 文控预览编辑授权）', updater='role-merge-990819799'
      WHERE id=990819799 AND deleted=0;
    INSERT INTO system_role_menu(role_id,menu_id,creator,updater,deleted,tenant_id)
      SELECT 990819799,menu_id,'role-merge-990819799','role-merge-990819799',b'0',tenant_id
      FROM system_role_menu source
      WHERE source.role_id=990819811 AND source.deleted=0
        AND NOT EXISTS (SELECT 1 FROM system_role_menu target WHERE target.role_id=990819799 AND target.menu_id=source.menu_id AND target.deleted=0);
    INSERT INTO system_user_role(user_id,role_id,creator,updater,deleted,tenant_id)
      SELECT source.user_id,990819799,source.creator,'role-merge-990819799',b'0',source.tenant_id
      FROM system_user_role source
      WHERE source.role_id=990819811 AND source.deleted=0
        AND NOT EXISTS (SELECT 1 FROM system_user_role target WHERE target.user_id=source.user_id AND target.role_id=990819799 AND target.deleted=0);
    UPDATE system_temporary_role_grant SET role_id=990819799,updater='role-merge-990819799' WHERE role_id=990819811 AND deleted=0;
    INSERT INTO mdm_role_company_scope(role_id,company_id,status,revision,creator,updater,deleted,tenant_id)
      SELECT 990819799,source.company_id,source.status,source.revision,source.creator,'role-merge-990819799',b'0',source.tenant_id
      FROM mdm_role_company_scope source
      WHERE source.role_id=990819811 AND source.deleted=0
        AND NOT EXISTS (SELECT 1 FROM mdm_role_company_scope target WHERE target.role_id=990819799 AND target.company_id=source.company_id AND target.tenant_id=source.tenant_id AND target.deleted=0);
    UPDATE system_user_role SET deleted=b'1',updater='role-merge-990819799' WHERE role_id=990819811 AND deleted=0;
    UPDATE system_role_menu SET deleted=b'1',updater='role-merge-990819799' WHERE role_id=990819811 AND deleted=0;
    UPDATE mdm_role_company_scope SET deleted=b'1',updater='role-merge-990819799' WHERE role_id=990819811 AND deleted=0;
    UPDATE system_role SET deleted=b'1',updater='role-merge-990819799' WHERE id=990819811 AND deleted=0;
  END IF;
END$$
DELIMITER ;
CALL merge_dcc_roles();
DROP PROCEDURE merge_dcc_roles;
