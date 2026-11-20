-- release-target-preflight: migrationId=20261023_registration_certificate_management_menu_hierarchy_identity; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  NOT (NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990200
         AND `deleted` = b'0'
         AND `path` = '/mdm'
  ))
  AND NOT (EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` IN (991601, 990231, 990232, 990249)
         AND `deleted` = b'1'
  ))
  AND NOT (EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990253
         AND `deleted` = b'0'
         AND NOT (
           `name` = '注册证管理'
           AND `permission` = ''
           AND `type` = 1
           AND `parent_id` = 990200
           AND `path` = 'registration-certificate-management'
           AND `component` IS NULL
           AND `component_name` IS NULL
         )
  ))
  AND NOT (EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND `parent_id` = 990200
         AND `path` = 'registration-certificate-management'
         AND `id` <> 990253
  ))
  AND NOT (EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `deleted` = b'0'
         AND `id` NOT IN (991601, 990231, 990232, 990249)
         AND `path` IN (
           '/mdm/registration-certificate',
           '/mdm/registration-certificate/detail/:id',
           '/mdm/registration-certificate/history/:id',
           '/mdm/enterprise'
         )
  ))
  AND NOT (NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 991601
         AND `deleted` = b'0'
         AND `name` = '注册证'
         AND `permission` = 'dcc:registration-certificate:query-current'
         AND `type` = 2
         AND `component` = 'dcc/registration-certificate/index/index'
         AND `component_name` = 'DccRegistrationCertificateIndex'
         AND (
           (`parent_id` = 990200 AND `path` = 'registration-certificate')
           OR (`parent_id` = 990253 AND `path` = '/mdm/registration-certificate')
         )
  ))
  AND NOT (NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990249
         AND `deleted` = b'0'
         AND `name` = '关联公司'
         AND `permission` = 'mdm:enterprise:query'
         AND `type` = 2
         AND `component` = 'mdm/enterprise/index'
         AND `component_name` = 'MdmEnterprise'
         AND (
           (`parent_id` = 990200 AND `path` = 'enterprise')
           OR (`parent_id` = 990253 AND `path` = '/mdm/enterprise')
         )
  ))
  AND NOT (EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 605071320
         AND `deleted` = b'0'
         AND NOT (
           `name` = '企业公司范围'
           AND `permission` = 'mdm:company-scope:query'
           AND `type` = 2
           AND `component` = 'mdm/company-scope/index'
           AND `component_name` = 'MdmCompanyScope'
         )
  ))
  AND NOT (EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 605071321
         AND `deleted` = b'0'
         AND NOT (
           `name` = '注册证历史导入'
           AND `permission` = 'dcc:registration-certificate:historical-import'
           AND `type` = 2
           AND `component` = 'dcc/registration-certificate/historical-import/index'
           AND `component_name` = 'DccRegistrationCertificateHistoricalImport'
         )
  ))
  AND NOT (NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990231
         AND `deleted` = b'0'
         AND `name` = '注册证详情'
         AND `permission` = 'dcc:registration-certificate:query-current'
         AND `type` = 2
         AND `component` = 'dcc/registration-certificate/detail/index'
         AND `component_name` = 'DccRegistrationCertificateDetail'
         AND (
           (`parent_id` = 990200 AND `path` = 'registration-certificate/detail/:id')
           OR (`parent_id` = 990253 AND `path` = '/mdm/registration-certificate/detail/:id')
         )
  ))
  AND NOT (NOT EXISTS (
      SELECT 1
        FROM `system_menu`
       WHERE `id` = 990232
         AND `deleted` = b'0'
         AND `name` = '注册证履历'
         AND `permission` = 'dcc:registration-certificate:query-current'
         AND `type` = 2
         AND `component` = 'dcc/registration-certificate/history/index'
         AND `component_name` = 'DccRegistrationCertificateHistory'
         AND (
           (`parent_id` = 990200 AND `path` = 'registration-certificate/history/:id')
           OR (`parent_id` = 990253 AND `path` = '/mdm/registration-certificate/history/:id')
         )
  ))
  AND NOT (EXISTS (
    SELECT 1
      FROM `system_tenant_package` AS `package`
     WHERE `package`.`deleted` = b'0'
       AND NOT JSON_VALID(`package`.`menu_ids`)
  ))
  AND NOT EXISTS(SELECT 1 FROM system_menu WHERE id=990253 AND deleted<>0)
THEN 'TARGET_PREFLIGHT_PASS:20261023_registration_certificate_management_menu_hierarchy_identity' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_registration_certificate_management_menu_hierarchy_identity:MENU_IDENTITY_CONFLICT' END;
