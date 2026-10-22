-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260922_intern_user_time_maintenance_module,20261006_dcc_native_distribution_menu,20261013_dcc_project_reviewer_role; type=permission; riskLevel=medium
-- Extend only packages used by tenants whose explicitly targeted QMS roles already hold these menus.
-- Does not assign users, introduce roles, or grant these capabilities to unrelated packages.
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE system_tenant_package package
SET package.menu_ids = JSON_MERGE_PRESERVE(CAST(package.menu_ids AS JSON),
  (SELECT JSON_ARRAYAGG(menu.id) FROM system_menu menu
   WHERE menu.deleted=0 AND menu.status=0
     AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST(menu.id AS JSON),'$')
     AND EXISTS (
       SELECT 1 FROM system_tenant tenant
       JOIN system_role role ON role.tenant_id=tenant.id AND role.deleted=0 AND role.status=0
       JOIN system_role_menu grant_row ON grant_row.role_id=role.id AND grant_row.tenant_id=tenant.id
         AND grant_row.menu_id=menu.id AND grant_row.deleted=0
       WHERE tenant.package_id=package.id AND tenant.deleted=0 AND tenant.status=0
         AND ((role.code='doc_control' AND menu.permission='dcc:controlled-file:distribute')
           OR (role.code='dcc_project_reviewer' AND menu.permission='dcc:project-code:review')
           OR (role.code='intern_user' AND menu.permission IN (
             'intern-user:time-maintenance:file-upload-time:update',
             'intern-user:time-maintenance:file-upload-time-audit:query',
             'intern-user:time-maintenance:dcc-published-time:update',
             'intern-user:time-maintenance:dcc-published-time-audit:query',
             'intern-user:time-maintenance:dcc-obsoleted-time:update',
             'intern-user:time-maintenance:dcc-obsoleted-time-audit:query'))))))
WHERE package.deleted=0 AND package.status=0 AND JSON_VALID(package.menu_ids)
  AND EXISTS (
    SELECT 1 FROM system_tenant tenant
    JOIN system_role role ON role.tenant_id=tenant.id AND role.deleted=0 AND role.status=0
    JOIN system_role_menu grant_row ON grant_row.role_id=role.id AND grant_row.tenant_id=tenant.id AND grant_row.deleted=0
    JOIN system_menu menu ON menu.id=grant_row.menu_id AND menu.deleted=0 AND menu.status=0
    WHERE tenant.package_id=package.id AND tenant.deleted=0 AND tenant.status=0
      AND NOT JSON_CONTAINS(CAST(package.menu_ids AS JSON),CAST(menu.id AS JSON),'$')
      AND ((role.code='doc_control' AND menu.permission='dcc:controlled-file:distribute')
        OR (role.code='dcc_project_reviewer' AND menu.permission='dcc:project-code:review')
        OR (role.code='intern_user' AND menu.permission IN (
          'intern-user:time-maintenance:file-upload-time:update',
          'intern-user:time-maintenance:file-upload-time-audit:query',
          'intern-user:time-maintenance:dcc-published-time:update',
          'intern-user:time-maintenance:dcc-published-time-audit:query',
          'intern-user:time-maintenance:dcc-obsoleted-time:update',
          'intern-user:time-maintenance:dcc-obsoleted-time-audit:query')))));
COMMIT;
