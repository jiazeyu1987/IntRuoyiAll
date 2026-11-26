-- release-target-preflight: migrationId=20261125_dcc_folder_template_menu; allowedEnvironments=test,backup,prod
-- Mirror the immutable migration's prerequisites without changing menus, roles, or tenant packages.
WITH master_root AS (
  SELECT COUNT(*) AS matches, MAX(id) AS id FROM system_menu
  WHERE path='/mdm' AND type=1 AND status=0 AND deleted=0
), project_query AS (
  SELECT COUNT(*) AS matches FROM system_menu
  WHERE permission='dcc:project-code:query' AND status=0 AND deleted=0
), project_update AS (
  SELECT COUNT(*) AS matches FROM system_menu
  WHERE permission='dcc:project-code:update' AND status=0 AND deleted=0
), product_catalog AS (
  SELECT COUNT(*) AS matches, MAX(sort) AS sort FROM system_menu
  WHERE parent_id=(SELECT id FROM master_root)
    AND component='dcc/controlled-file/basic-data/product-catalog/index' AND status=0 AND deleted=0
)
SELECT CASE
  WHEN (SELECT matches FROM master_root)<>1
    OR (SELECT matches FROM project_query)<>1 OR (SELECT matches FROM project_update)<>1
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_dcc_folder_template_menu:ROOT_OR_PROJECT_PERMISSIONS'
  WHEN (SELECT matches FROM product_catalog)<>1
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_dcc_folder_template_menu:PRODUCT_CATALOG'
  WHEN NOT EXISTS (SELECT 1 FROM system_menu
      WHERE parent_id=(SELECT id FROM master_root) AND deleted=0 AND status=0
        AND sort>(SELECT sort FROM product_catalog)
        AND (path IN ('registration-certificate-management','/mdm/registration-certificate','registration-certificate')
          OR component='dcc/registration-certificate/index/index'))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_dcc_folder_template_menu:CERTIFICATE_MENU_ORDER'
  WHEN EXISTS (SELECT 1 FROM system_menu WHERE id=2026112501
      AND (deleted<>0 OR parent_id<>(SELECT id FROM master_root) OR path<>'folder-template'
        OR component<>'dcc/controlled-file/basic-data/folder-template/index'))
    OR EXISTS (SELECT 1 FROM system_menu WHERE deleted=0 AND id<>2026112501
      AND (component='dcc/controlled-file/basic-data/folder-template/index'
        OR (parent_id=(SELECT id FROM master_root) AND path='folder-template')))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_dcc_folder_template_menu:MENU_IDENTITY_CONFLICT'
  WHEN EXISTS (SELECT 1 FROM system_tenant_package
      WHERE deleted=0 AND (menu_ids IS NULL OR NOT JSON_VALID(menu_ids)))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_dcc_folder_template_menu:INVALID_PACKAGE_JSON'
  ELSE 'TARGET_PREFLIGHT_PASS:20261125_dcc_folder_template_menu'
END;
