-- release-target-preflight: migrationId=20261125_registration_certificate_config_menu_parent; allowedEnvironments=test,backup,prod
-- Mirror the immutable migration's assertions before any target mutation.
WITH live_packages AS (
  SELECT menu_ids,
    CASE WHEN JSON_VALID(menu_ids)=1 THEN CAST(menu_ids AS JSON) ELSE JSON_ARRAY() END AS safe_menu_ids
  FROM system_tenant_package WHERE deleted=0
)
SELECT CASE
  WHEN NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990253 AND deleted=0 AND status=0
      AND type=1 AND parent_id=990200 AND path='registration-certificate-management')
    OR NOT EXISTS (SELECT 1 FROM system_menu WHERE id=990200 AND deleted=0 AND status=0
      AND type=1 AND parent_id=0 AND path='/mdm')
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_registration_certificate_config_menu_parent:PARENT_HIERARCHY'
  WHEN NOT EXISTS (SELECT 1 FROM system_menu config WHERE config.id=991503
      AND config.deleted=0 AND config.status=0 AND config.type=2 AND config.visible=1
      AND config.component='dcc/registration-certificate/config/index'
      AND config.component_name='DccRegistrationCertificateConfig'
      AND config.permission='dcc:registration-certificate:config:query'
      AND ((config.parent_id=990253 AND config.path='/dcc/registration-certificate/config')
        OR (config.path='registration-certificate/config' AND EXISTS (
          SELECT 1 FROM system_menu root WHERE root.id=config.parent_id AND root.deleted=0
            AND root.status=0 AND root.type=1 AND root.parent_id=0 AND root.path='/dcc'))))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_registration_certificate_config_menu_parent:CONFIG_IDENTITY'
  WHEN EXISTS (SELECT 1 FROM system_menu WHERE deleted=0 AND id<>991503
      AND (path='/dcc/registration-certificate/config' OR component_name='DccRegistrationCertificateConfig'))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_registration_certificate_config_menu_parent:ROUTE_CONFLICT'
  WHEN EXISTS (SELECT 1 FROM live_packages WHERE menu_ids IS NULL OR NOT JSON_VALID(menu_ids))
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_registration_certificate_config_menu_parent:INVALID_PACKAGE_JSON'
  WHEN EXISTS (SELECT 1 FROM live_packages WHERE JSON_TYPE(safe_menu_ids)<>'ARRAY')
    THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_registration_certificate_config_menu_parent:PACKAGE_NOT_ARRAY'
  ELSE 'TARGET_PREFLIGHT_PASS:20261125_registration_certificate_config_menu_parent'
END;
