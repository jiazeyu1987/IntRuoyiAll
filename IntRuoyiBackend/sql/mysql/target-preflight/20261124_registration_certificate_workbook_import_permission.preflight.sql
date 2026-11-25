-- release-target-preflight: migrationId=20261124_registration_certificate_workbook_import_permission; allowedEnvironments=test,backup,prod
-- Mirror the immutable migration's assertions without opting any package in or granting permissions.
WITH certificate_page AS (
  SELECT COUNT(*) AS matches, MAX(id) AS id
  FROM system_menu
  WHERE deleted=0 AND status=0 AND type=2
    AND path='/mdm/registration-certificate'
    AND component='dcc/registration-certificate/index/index'
    AND permission='dcc:registration-certificate:query-current'
), upload_approval AS (
  SELECT COUNT(*) AS matches, MAX(m.id) AS id
  FROM system_menu m JOIN certificate_page p ON m.parent_id=p.id
  WHERE m.deleted=0 AND m.status=0 AND m.type=3
    AND m.permission='dcc:registration-certificate:upload:approve'
), live_packages AS (
  SELECT menu_ids,
    CASE WHEN JSON_VALID(menu_ids)=1 THEN CAST(menu_ids AS JSON) ELSE JSON_ARRAY() END AS safe_menu_ids
  FROM system_tenant_package WHERE deleted=0
)
SELECT CASE
  WHEN (SELECT matches FROM certificate_page)<>1
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261124_registration_certificate_workbook_import_permission:CERTIFICATE_PAGE'
  WHEN (SELECT matches FROM upload_approval)<>1
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261124_registration_certificate_workbook_import_permission:UPLOAD_APPROVAL'
  WHEN EXISTS (
    SELECT 1 FROM system_menu
    WHERE id=2026112401 AND (deleted<>0 OR status<>0 OR type<>3
      OR parent_id<>(SELECT id FROM certificate_page)
      OR permission<>'dcc:registration-certificate:historical-import')
  ) OR EXISTS (
    SELECT 1 FROM system_menu WHERE deleted=0 AND id<>2026112401
      AND permission='dcc:registration-certificate:historical-import'
  ) THEN 'TARGET_PREFLIGHT_BLOCKED:20261124_registration_certificate_workbook_import_permission:MENU_IDENTITY_CONFLICT'
  WHEN EXISTS (SELECT 1 FROM live_packages WHERE menu_ids IS NULL OR NOT JSON_VALID(menu_ids))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261124_registration_certificate_workbook_import_permission:INVALID_PACKAGE_JSON'
  WHEN EXISTS (
    SELECT 1 FROM live_packages
    WHERE JSON_CONTAINS(safe_menu_ids,'2026112401','$')
      AND NOT JSON_CONTAINS(safe_menu_ids,CAST((SELECT id FROM upload_approval) AS JSON),'$')
  ) THEN 'TARGET_PREFLIGHT_BLOCKED:20261124_registration_certificate_workbook_import_permission:PACKAGE_UPLOAD_APPROVAL'
  ELSE 'TARGET_PREFLIGHT_PASS:20261124_registration_certificate_workbook_import_permission'
END;
