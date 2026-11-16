-- release-target-preflight: migrationId=20261031_mdm_company_scope_menu_supplement; allowedEnvironments=test,backup,prod
-- Migration-time guards remain authoritative after pending schema dependencies.
WITH tmp_word14_scope_menus AS (
SELECT 990255 id,'授权公司' name,'mdm:company-scope:query' permission,2 type,16 sort,990200 parent_id,'company-scope' path,'mdm/company-scope/index' component,'MdmCompanyScope' component_name
UNION ALL SELECT 990256,'授权公司新增','mdm:company-scope:create',3,10,990255,'','',''
UNION ALL SELECT 990257,'授权公司修改','mdm:company-scope:update',3,20,990255,'','',''
UNION ALL SELECT 990258,'授权公司删除','mdm:company-scope:delete',3,30,990255,'','',''
)
SELECT CASE
 WHEN ((SELECT COUNT(*) FROM information_schema.tables
       WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
         AND table_name IN ('mdm_enterprise', 'mdm_user_company_scope', 'mdm_role_company_scope',
                            'system_menu', 'system_role', 'system_role_menu', 'system_tenant', 'system_tenant_package')) <> 8) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_mdm_company_scope_menu_supplement:CONDITION_1'
 WHEN (NOT EXISTS (SELECT 1 FROM system_menu
                  WHERE id = 990200 AND deleted = b'0' AND status = 0 AND visible = b'1'
                    AND type = 1 AND parent_id = 0 AND path = '/mdm' AND permission = ''
                    AND component IS NULL AND component_name IS NULL)
     OR EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND path = '/mdm' AND id <> 990200)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_mdm_company_scope_menu_supplement:CONDITION_2'
 WHEN (EXISTS (SELECT 1 FROM system_tenant_package WHERE deleted = b'0'
              AND CASE WHEN menu_ids IS NULL OR NOT JSON_VALID(menu_ids) THEN TRUE
                       ELSE JSON_TYPE(menu_ids) <> 'ARRAY' END)) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_mdm_company_scope_menu_supplement:CONDITION_3'
 WHEN (EXISTS (SELECT 1 FROM tmp_word14_scope_menus expected JOIN system_menu actual ON actual.id = expected.id
              WHERE NOT (actual.name <=> expected.name) OR NOT (actual.permission <=> expected.permission)
                 OR actual.type <> expected.type OR actual.sort <> expected.sort
                 OR actual.parent_id <> expected.parent_id OR NOT (actual.path <=> expected.path)
                 OR NOT (actual.component <=> expected.component) OR NOT (actual.component_name <=> expected.component_name)
                 OR actual.status <> 0 OR actual.visible <> b'1' OR actual.deleted <> b'0')) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_mdm_company_scope_menu_supplement:CONDITION_4'
 WHEN (EXISTS (SELECT 1 FROM system_menu actual JOIN tmp_word14_scope_menus expected
                    ON actual.permission = expected.permission AND actual.id <> expected.id
                   WHERE actual.deleted = b'0')
     OR EXISTS (SELECT 1 FROM system_menu WHERE deleted = b'0' AND id <> 990255
                  AND ((parent_id = 990200 AND path = 'company-scope') OR path = '/mdm/company-scope'
                       OR component = 'mdm/company-scope/index' OR component_name = 'MdmCompanyScope'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_mdm_company_scope_menu_supplement:CONDITION_5'
 WHEN (NOT EXISTS (SELECT 1 FROM system_role r JOIN system_tenant t ON t.id=r.tenant_id WHERE t.id=1 AND t.deleted=0 AND r.deleted=0 AND r.code IN ('tenant_admin','super_admin'))) THEN 'TARGET_PREFLIGHT_BLOCKED:20261031_mdm_company_scope_menu_supplement:CONDITION_6'
 ELSE 'TARGET_PREFLIGHT_PASS:20261031_mdm_company_scope_menu_supplement' END;
